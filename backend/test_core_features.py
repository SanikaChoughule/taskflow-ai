import os
import sys
import json
import uuid
import asyncio
from datetime import datetime, timedelta, timezone
from unittest.mock import MagicMock, patch

# Add parent path for imports
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from app.db.session import SessionLocal, engine
from app.db.models import Base, Action, User, Session as DBSession
from app.agent.actions_logger import log_action, undo_last_action
from app.tools.calendar import check_calendar_conflict, parse_google_datetime
from app.agent.scheduler import start_proactive_calendar_checker, proactive_suggestions

# Ensure database tables exist
Base.metadata.create_all(bind=engine)

# Set up test context
TEST_USER_ID = "abcdef12-abcd-abcd-abcd-abcdef123456"
TEST_USER_EMAIL = "test_user@gmail.com"

def setup_test_user_and_context():
    db = SessionLocal()
    try:
        from sqlalchemy import text
        db.execute(text("DELETE FROM users WHERE email = :email"), {"email": TEST_USER_EMAIL})
        db.commit()
            
        user = User(
            user_id=uuid.UUID(TEST_USER_ID),
            name="Test User",
            email=TEST_USER_EMAIL,
            google_oauth_token="mock_token"
        )
        db.add(user)
        db.commit()
        db.refresh(user)
            
        # Mock thread/context variables
        from app.tools.calendar import active_user_id, active_user_token, active_user_email
        active_user_id.set(TEST_USER_ID)
        active_user_token.set("mock_token")
        active_user_email.set(TEST_USER_EMAIL)
        
        # Clear out existing test actions
        db.query(Action).filter(Action.user_id == uuid.UUID(TEST_USER_ID)).delete()
        db.commit()
    finally:
        db.close()

# ----------------- TEST CASE 1: UNDO REVERSAL VERIFICATION -----------------
@patch("app.agent.actions_logger.delete_calendar_event")
@patch("app.agent.actions_logger.delete_reminder")
def test_undo_reversal(mock_delete_reminder, mock_delete_calendar):
    setup_test_user_and_context()
    
    # Configure mock return values
    mock_delete_calendar.return_value = {"status": "success"}
    mock_delete_reminder.return_value = {"status": "success"}
    
    parent_uuid = uuid.uuid4()
    
    print("\n[TEST 1] Logging events to actions table...")
    # Log two actions under the same parent task id (grouped action execution)
    log_action(
        action_type="create_calendar_event:Sync with Rahul",
        action_payload={"summary": "Sync with Rahul", "start_iso": "2026-08-15T15:00:00Z"},
        reason="It was the only free slot.",
        undo_action={"event_id": "mock_event_123"},
        parent_task_id=parent_uuid
    )
    log_action(
        action_type="set_reminder:Prep for Rahul",
        action_payload={"task": "Prep for Rahul"},
        reason="To notify you 30m prior.",
        undo_action={"reminder_id": "mock_reminder_456"},
        parent_task_id=parent_uuid
    )
    
    db = SessionLocal()
    try:
        actions_before = db.query(Action).filter(
            Action.user_id == uuid.UUID(TEST_USER_ID),
            Action.status == "active"
        ).all()
        assert len(actions_before) == 2, "Expected 2 active action logs before undo!"
        print(f"Verified: {len(actions_before)} active logs found.")
        
        # Execute undo_last_action
        print("[TEST 1] Executing undo_last_action()...")
        undo_res = undo_last_action()
        print(f"Undo Result Message: {undo_res['message']}")
        assert undo_res["status"] == "success"
        
        # Verify status is now 'undone'
        db.expire_all()
        actions_after = db.query(Action).filter(Action.user_id == uuid.UUID(TEST_USER_ID)).all()
        for act in actions_after:
            assert act.status == "undone", f"Action {act.action_type} was not marked as 'undone'!"
            
        print("Verified: All logged actions marked status='undone' in database.")
        
        # Verify deletions were called
        mock_delete_calendar.assert_called_once_with("mock_event_123")
        mock_delete_reminder.assert_called_once_with("mock_reminder_456")
        print("Verified: Deletion tools were invoked with correct target IDs.")
        
    finally:
        db.close()

# ----------------- TEST CASE 2: CONFLICT DETECTION ACCURACY -----------------
@patch("app.tools.calendar.get_calendar_service")
def test_conflict_detection(mock_get_service):
    setup_test_user_and_context()
    
    # Mock calendar event overlaps
    # Event: 15:00 to 16:00 UTC
    mock_events = [{
        "id": "existing_event_id",
        "summary": "Existing Meeting",
        "status": "confirmed",
        "start": {"dateTime": "2026-08-15T15:00:00Z"},
        "end": {"dateTime": "2026-08-15T16:00:00Z"}
    }]
    
    service_mock = MagicMock()
    mock_get_service.return_value = service_mock
    service_mock.events().list().execute.return_value = {"items": mock_events}
    
    print("\n[TEST 2] Verifying conflict detection overlaps...")
    # Overlapping Request: 15:30 to 16:30 (should conflict)
    conflict_res = check_calendar_conflict("2026-08-15T15:30:00Z", "2026-08-15T16:30:00Z")
    print(f"Overlapping Request Result: {conflict_res}")
    assert conflict_res["conflict"] is True
    assert conflict_res["event_summary"] == "Existing Meeting"
    
    # Free Request: 17:00 to 18:00 (should be free)
    free_res = check_calendar_conflict("2026-08-15T17:00:00Z", "2026-08-15T18:00:00Z")
    print(f"Free Request Result: {free_res}")
    assert free_res["conflict"] is False
    print("Verified: Overlapping vs free slot bounds evaluated correctly.")

# ----------------- TEST CASE 3: PROACTIVE OVERRUN CONDITIONS -----------------
@patch("app.db.session.SessionLocal")
@patch("app.tools.calendar.get_calendar_service")
async def test_proactive_overrun(mock_get_service, mock_session_local):
    setup_test_user_and_context()
    
    db_mock = MagicMock()
    mock_session_local.return_value = db_mock
    
    # Mock active session
    user_mock = MagicMock(user_id=uuid.UUID(TEST_USER_ID), email=TEST_USER_EMAIL, google_oauth_token="mock_token")
    sess_mock = MagicMock(user=user_mock)
    db_mock.query().filter().all.return_value = [sess_mock]
    
    # Mock calendar API response
    service_mock = MagicMock()
    mock_get_service.return_value = service_mock
    
    now_dt = datetime.utcnow()
    meeting_a_start = (now_dt - timedelta(minutes=30)).strftime("%Y-%m-%dT%H:%M:%SZ")
    meeting_a_end = (now_dt - timedelta(minutes=5)).strftime("%Y-%m-%dT%H:%M:%SZ")  # Overran 5 mins ago
    
    meeting_b_start = (now_dt + timedelta(minutes=5)).strftime("%Y-%m-%dT%H:%M:%SZ")  # Starting in 5 mins
    meeting_b_end = (now_dt + timedelta(minutes=35)).strftime("%Y-%m-%dT%H:%M:%SZ")
    
    # 1. Overrun scenario (triggers suggestions)
    print("\n[TEST 3] Testing overrun conditions matching rules...")
    overrun_events = [
        {
            "id": "event_a",
            "summary": "Meeting A",
            "status": "confirmed",
            "start": {"dateTime": meeting_a_start},
            "end": {"dateTime": meeting_a_end}
        },
        {
            "id": "event_b",
            "summary": "Meeting B",
            "status": "confirmed",
            "start": {"dateTime": meeting_b_start},
            "end": {"dateTime": meeting_b_end}
        }
    ]
    service_mock.events().list().execute.return_value = {"items": overrun_events}
    
    # Reset suggestions cache
    proactive_suggestions.clear()
    
    # Run loop once
    sleep_count_1 = 0
    async def mock_sleep_1(seconds):
        nonlocal sleep_count_1
        sleep_count_1 += 1
        if sleep_count_1 > 1:
            raise asyncio.CancelledError()
            
    with patch("asyncio.sleep", side_effect=mock_sleep_1):
        try:
            await start_proactive_calendar_checker()
        except asyncio.CancelledError:
            pass
            
    print(f"Overrun cache status: {proactive_suggestions}")
    assert TEST_USER_ID in proactive_suggestions
    assert len(proactive_suggestions[TEST_USER_ID]) == 1
    assert "Meeting A" in proactive_suggestions[TEST_USER_ID][0]["message"]
    print("Verified: Suggestions successfully created for overrunning meetings.")

    # 2. Healthy scenario (does not trigger suggestion)
    print("[TEST 3] Testing healthy calendar rules...")
    healthy_events = [
        {
            "id": "event_a",
            "summary": "Meeting A",
            "status": "confirmed",
            "start": {"dateTime": meeting_a_start},
            "end": {"dateTime": meeting_a_end}
        }
        # No upcoming meeting starting soon
    ]
    service_mock.events().list().execute.return_value = {"items": healthy_events}
    proactive_suggestions.clear()
    
    sleep_count_2 = 0
    async def mock_sleep_2(seconds):
        nonlocal sleep_count_2
        sleep_count_2 += 1
        if sleep_count_2 > 1:
            raise asyncio.CancelledError()
            
    with patch("asyncio.sleep", side_effect=mock_sleep_2):
        try:
            await start_proactive_calendar_checker()
        except asyncio.CancelledError:
            pass
            
    print(f"Healthy cache status: {proactive_suggestions}")
    assert TEST_USER_ID not in proactive_suggestions or len(proactive_suggestions[TEST_USER_ID]) == 0
    print("Verified: No alerts triggered under healthy rule matches.")

# ----------------- TEST CASE 4: PROACTIVE B2B SUGGESTIONS -----------------
@patch("app.db.session.SessionLocal")
@patch("app.tools.calendar.get_calendar_service")
async def test_proactive_back_to_back(mock_get_service, mock_session_local):
    setup_test_user_and_context()
    
    db_mock = MagicMock()
    mock_session_local.return_value = db_mock
    
    # Mock active session
    user_mock = MagicMock(user_id=uuid.UUID(TEST_USER_ID), email=TEST_USER_EMAIL, google_oauth_token="mock_token")
    sess_mock = MagicMock(user=user_mock)
    db_mock.query().filter().all.return_value = [sess_mock]
    
    # Mock calendar API response
    service_mock = MagicMock()
    mock_get_service.return_value = service_mock
    
    # Configure back-to-back events (Meeting A: 15:00-16:00, Meeting B: 16:00-17:00)
    b2b_events = [
        {
            "id": "event_a",
            "summary": "Scrum Meeting",
            "status": "confirmed",
            "start": {"dateTime": "2026-08-15T15:00:00Z"},
            "end": {"dateTime": "2026-08-15T16:00:00Z"}
        },
        {
            "id": "event_b",
            "summary": "Product Review",
            "status": "confirmed",
            "start": {"dateTime": "2026-08-15T16:00:00Z"},
            "end": {"dateTime": "2026-08-15T17:00:00Z"}
        }
    ]
    service_mock.events().list().execute.return_value = {"items": b2b_events}
    
    # Reset suggestions cache
    proactive_suggestions.clear()
    
    print("\n[TEST 4] Testing back-to-back meeting checking (Rule 2)...")
    
    sleep_count = 0
    async def mock_sleep(seconds):
        nonlocal sleep_count
        sleep_count += 1
        if sleep_count > 1:
            raise asyncio.CancelledError()
            
    with patch("asyncio.sleep", side_effect=mock_sleep):
        try:
            await start_proactive_calendar_checker()
        except asyncio.CancelledError:
            pass
            
    print(f"B2B cache status: {proactive_suggestions}")
    assert TEST_USER_ID in proactive_suggestions
    assert len(proactive_suggestions[TEST_USER_ID]) == 1
    assert "back-to-back" in proactive_suggestions[TEST_USER_ID][0]["message"]
    print("Verified: Suggestions successfully created for back-to-back meetings.")

# ----------------- TEST CASE 5: SPECIFIC ACTION UNDO -----------------
@patch("app.agent.actions_logger.delete_calendar_event")
@patch("app.agent.actions_logger.delete_reminder")
def test_specific_action_undo(mock_delete_reminder, mock_delete_calendar):
    setup_test_user_and_context()
    
    mock_delete_calendar.return_value = {"status": "success"}
    mock_delete_reminder.return_value = {"status": "success"}
    
    db = SessionLocal()
    # Create two distinct parent tasks
    parent_1 = uuid.uuid4()
    parent_2 = uuid.uuid4()
    
    # Action 1 (Calendar event + reminder)
    action_1a = Action(
        action_type="create_calendar_event:Meeting A",
        action_payload=json.dumps({"event_id": "event_a"}),
        reason="Scheduled Meeting A",
        undo_action=json.dumps({"event_id": "event_a"}),
        status="active",
        user_id=uuid.UUID(TEST_USER_ID),
        parent_task_id=parent_1
    )
    action_1b = Action(
        action_type="set_reminder:Prep for Meeting A",
        action_payload=json.dumps({"reminder_id": "rem_a"}),
        reason="Set reminder for Meeting A",
        undo_action=json.dumps({"reminder_id": "rem_a"}),
        status="active",
        user_id=uuid.UUID(TEST_USER_ID),
        parent_task_id=parent_1
    )
    
    # Action 2 (Calendar event)
    action_2a = Action(
        action_type="create_calendar_event:Meeting B",
        action_payload=json.dumps({"event_id": "event_b"}),
        reason="Scheduled Meeting B",
        undo_action=json.dumps({"event_id": "event_b"}),
        status="active",
        user_id=uuid.UUID(TEST_USER_ID),
        parent_task_id=parent_2
    )
    
    db.add(action_1a)
    db.add(action_1b)
    db.add(action_2a)
    db.commit()
    
    action_1a_id = action_1a.id
    action_2a_id = action_2a.id
    
    print("\n[TEST 5] Testing Specific Action Undo (Phase 11)...")
    from app.agent.actions_logger import undo_specific_action
    
    # Undo Action 1 (not the last action, Action 2 was added later)
    result = undo_specific_action(action_1a_id)
    print(f"Undo Result: {result}")
    
    db.expire_all()
    # Verify action 1a and 1b (siblings) are undone
    a1a = db.query(Action).filter(Action.id == action_1a_id).first()
    assert a1a.status == "undone"
    
    # Sibling should also be undone
    a1b = db.query(Action).filter(Action.action_type == "set_reminder:Prep for Meeting A").first()
    assert a1b.status == "undone"
    
    # Action 2 should remain active
    a2a = db.query(Action).filter(Action.id == action_2a_id).first()
    assert a2a.status == "active"
    
    # Clean up test records
    db.query(Action).filter(Action.user_id == uuid.UUID(TEST_USER_ID)).delete()
    db.commit()
    db.close()
    print("Verified: Specific action and its siblings reverted successfully, leaving others unaffected.")

if __name__ == "__main__":
    print("==================================================")
    print("    TASKFLOW AI UNIFIED CORE VERIFICATION SUITE   ")
    print("==================================================")
    
    test_undo_reversal()
    test_conflict_detection()
    asyncio.run(test_proactive_overrun())
    asyncio.run(test_proactive_back_to_back())
    test_specific_action_undo()
    
    print("\n==================================================")
    print("  SUCCESS: All core feature unit assertions passed!")
    print("==================================================")
