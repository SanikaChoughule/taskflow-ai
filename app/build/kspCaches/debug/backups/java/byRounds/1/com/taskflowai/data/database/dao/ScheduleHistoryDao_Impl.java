package com.taskflowai.data.database.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.taskflowai.data.database.entity.ScheduleHistoryEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ScheduleHistoryDao_Impl implements ScheduleHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ScheduleHistoryEntity> __insertionAdapterOfScheduleHistoryEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateActionByCalendarEventId;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByCalendarEventId;

  private final SharedSQLiteStatement __preparedStmtOfClearUserHistory;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  private final SharedSQLiteStatement __preparedStmtOfAssociateUnassignedHistoryToUser;

  public ScheduleHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfScheduleHistoryEntity = new EntityInsertionAdapter<ScheduleHistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `schedule_history` (`id`,`userId`,`title`,`scheduledDate`,`startTimeFormatted`,`endTimeFormatted`,`startTimeMs`,`endTimeMs`,`attendee`,`attendeeEmail`,`action`,`actionTime`,`calendarEventId`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ScheduleHistoryEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getScheduledDate());
        statement.bindString(5, entity.getStartTimeFormatted());
        statement.bindString(6, entity.getEndTimeFormatted());
        statement.bindLong(7, entity.getStartTimeMs());
        statement.bindLong(8, entity.getEndTimeMs());
        if (entity.getAttendee() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAttendee());
        }
        if (entity.getAttendeeEmail() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getAttendeeEmail());
        }
        statement.bindString(11, entity.getAction());
        statement.bindLong(12, entity.getActionTime());
        statement.bindString(13, entity.getCalendarEventId());
      }
    };
    this.__preparedStmtOfUpdateActionByCalendarEventId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE schedule_history SET action = ?, actionTime = ? WHERE calendarEventId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByCalendarEventId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM schedule_history WHERE calendarEventId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearUserHistory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM schedule_history WHERE userId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM schedule_history";
        return _query;
      }
    };
    this.__preparedStmtOfAssociateUnassignedHistoryToUser = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE schedule_history SET userId = ? WHERE userId = '' OR userId IS NULL";
        return _query;
      }
    };
  }

  @Override
  public Object insertHistory(final ScheduleHistoryEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfScheduleHistoryEntity.insert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateActionByCalendarEventId(final String calendarEventId, final String action,
      final long actionTime, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateActionByCalendarEventId.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, action);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, actionTime);
        _argIndex = 3;
        _stmt.bindString(_argIndex, calendarEventId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateActionByCalendarEventId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByCalendarEventId(final String calendarEventId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByCalendarEventId.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, calendarEventId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByCalendarEventId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearUserHistory(final String userId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearUserHistory.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, userId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearUserHistory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object associateUnassignedHistoryToUser(final String userId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfAssociateUnassignedHistoryToUser.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, userId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfAssociateUnassignedHistoryToUser.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ScheduleHistoryEntity>> getHistoryForUser(final String userId) {
    final String _sql = "SELECT * FROM schedule_history WHERE ? = '' OR userId = ? OR userId = '' ORDER BY actionTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"schedule_history"}, new Callable<List<ScheduleHistoryEntity>>() {
      @Override
      @NonNull
      public List<ScheduleHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfScheduledDate = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledDate");
          final int _cursorIndexOfStartTimeFormatted = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimeFormatted");
          final int _cursorIndexOfEndTimeFormatted = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimeFormatted");
          final int _cursorIndexOfStartTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimeMs");
          final int _cursorIndexOfEndTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimeMs");
          final int _cursorIndexOfAttendee = CursorUtil.getColumnIndexOrThrow(_cursor, "attendee");
          final int _cursorIndexOfAttendeeEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "attendeeEmail");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfActionTime = CursorUtil.getColumnIndexOrThrow(_cursor, "actionTime");
          final int _cursorIndexOfCalendarEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "calendarEventId");
          final List<ScheduleHistoryEntity> _result = new ArrayList<ScheduleHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScheduleHistoryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpScheduledDate;
            _tmpScheduledDate = _cursor.getString(_cursorIndexOfScheduledDate);
            final String _tmpStartTimeFormatted;
            _tmpStartTimeFormatted = _cursor.getString(_cursorIndexOfStartTimeFormatted);
            final String _tmpEndTimeFormatted;
            _tmpEndTimeFormatted = _cursor.getString(_cursorIndexOfEndTimeFormatted);
            final long _tmpStartTimeMs;
            _tmpStartTimeMs = _cursor.getLong(_cursorIndexOfStartTimeMs);
            final long _tmpEndTimeMs;
            _tmpEndTimeMs = _cursor.getLong(_cursorIndexOfEndTimeMs);
            final String _tmpAttendee;
            if (_cursor.isNull(_cursorIndexOfAttendee)) {
              _tmpAttendee = null;
            } else {
              _tmpAttendee = _cursor.getString(_cursorIndexOfAttendee);
            }
            final String _tmpAttendeeEmail;
            if (_cursor.isNull(_cursorIndexOfAttendeeEmail)) {
              _tmpAttendeeEmail = null;
            } else {
              _tmpAttendeeEmail = _cursor.getString(_cursorIndexOfAttendeeEmail);
            }
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final long _tmpActionTime;
            _tmpActionTime = _cursor.getLong(_cursorIndexOfActionTime);
            final String _tmpCalendarEventId;
            _tmpCalendarEventId = _cursor.getString(_cursorIndexOfCalendarEventId);
            _item = new ScheduleHistoryEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpScheduledDate,_tmpStartTimeFormatted,_tmpEndTimeFormatted,_tmpStartTimeMs,_tmpEndTimeMs,_tmpAttendee,_tmpAttendeeEmail,_tmpAction,_tmpActionTime,_tmpCalendarEventId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ScheduleHistoryEntity>> getAllHistory() {
    final String _sql = "SELECT * FROM schedule_history ORDER BY actionTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"schedule_history"}, new Callable<List<ScheduleHistoryEntity>>() {
      @Override
      @NonNull
      public List<ScheduleHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfScheduledDate = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledDate");
          final int _cursorIndexOfStartTimeFormatted = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimeFormatted");
          final int _cursorIndexOfEndTimeFormatted = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimeFormatted");
          final int _cursorIndexOfStartTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimeMs");
          final int _cursorIndexOfEndTimeMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimeMs");
          final int _cursorIndexOfAttendee = CursorUtil.getColumnIndexOrThrow(_cursor, "attendee");
          final int _cursorIndexOfAttendeeEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "attendeeEmail");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfActionTime = CursorUtil.getColumnIndexOrThrow(_cursor, "actionTime");
          final int _cursorIndexOfCalendarEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "calendarEventId");
          final List<ScheduleHistoryEntity> _result = new ArrayList<ScheduleHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScheduleHistoryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpScheduledDate;
            _tmpScheduledDate = _cursor.getString(_cursorIndexOfScheduledDate);
            final String _tmpStartTimeFormatted;
            _tmpStartTimeFormatted = _cursor.getString(_cursorIndexOfStartTimeFormatted);
            final String _tmpEndTimeFormatted;
            _tmpEndTimeFormatted = _cursor.getString(_cursorIndexOfEndTimeFormatted);
            final long _tmpStartTimeMs;
            _tmpStartTimeMs = _cursor.getLong(_cursorIndexOfStartTimeMs);
            final long _tmpEndTimeMs;
            _tmpEndTimeMs = _cursor.getLong(_cursorIndexOfEndTimeMs);
            final String _tmpAttendee;
            if (_cursor.isNull(_cursorIndexOfAttendee)) {
              _tmpAttendee = null;
            } else {
              _tmpAttendee = _cursor.getString(_cursorIndexOfAttendee);
            }
            final String _tmpAttendeeEmail;
            if (_cursor.isNull(_cursorIndexOfAttendeeEmail)) {
              _tmpAttendeeEmail = null;
            } else {
              _tmpAttendeeEmail = _cursor.getString(_cursorIndexOfAttendeeEmail);
            }
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final long _tmpActionTime;
            _tmpActionTime = _cursor.getLong(_cursorIndexOfActionTime);
            final String _tmpCalendarEventId;
            _tmpCalendarEventId = _cursor.getString(_cursorIndexOfCalendarEventId);
            _item = new ScheduleHistoryEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpScheduledDate,_tmpStartTimeFormatted,_tmpEndTimeFormatted,_tmpStartTimeMs,_tmpEndTimeMs,_tmpAttendee,_tmpAttendeeEmail,_tmpAction,_tmpActionTime,_tmpCalendarEventId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
