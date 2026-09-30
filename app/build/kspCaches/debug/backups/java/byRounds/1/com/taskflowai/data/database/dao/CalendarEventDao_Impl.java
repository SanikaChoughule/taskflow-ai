package com.taskflowai.data.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.taskflowai.data.database.entity.CalendarEventEntity;
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
public final class CalendarEventDao_Impl implements CalendarEventDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CalendarEventEntity> __insertionAdapterOfCalendarEventEntity;

  private final EntityDeletionOrUpdateAdapter<CalendarEventEntity> __updateAdapterOfCalendarEventEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteEventById;

  public CalendarEventDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCalendarEventEntity = new EntityInsertionAdapter<CalendarEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `calendar_events` (`id`,`title`,`description`,`location`,`startTime`,`endTime`,`attendeesJson`,`calendarName`,`colorTag`,`isAllDay`,`googleEventId`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CalendarEventEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getLocation());
        statement.bindLong(5, entity.getStartTime());
        statement.bindLong(6, entity.getEndTime());
        statement.bindString(7, entity.getAttendeesJson());
        statement.bindString(8, entity.getCalendarName());
        statement.bindString(9, entity.getColorTag());
        final int _tmp = entity.isAllDay() ? 1 : 0;
        statement.bindLong(10, _tmp);
        if (entity.getGoogleEventId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getGoogleEventId());
        }
      }
    };
    this.__updateAdapterOfCalendarEventEntity = new EntityDeletionOrUpdateAdapter<CalendarEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `calendar_events` SET `id` = ?,`title` = ?,`description` = ?,`location` = ?,`startTime` = ?,`endTime` = ?,`attendeesJson` = ?,`calendarName` = ?,`colorTag` = ?,`isAllDay` = ?,`googleEventId` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CalendarEventEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getLocation());
        statement.bindLong(5, entity.getStartTime());
        statement.bindLong(6, entity.getEndTime());
        statement.bindString(7, entity.getAttendeesJson());
        statement.bindString(8, entity.getCalendarName());
        statement.bindString(9, entity.getColorTag());
        final int _tmp = entity.isAllDay() ? 1 : 0;
        statement.bindLong(10, _tmp);
        if (entity.getGoogleEventId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getGoogleEventId());
        }
        statement.bindString(12, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteEventById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM calendar_events WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertEvent(final CalendarEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCalendarEventEntity.insert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateEvent(final CalendarEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCalendarEventEntity.handle(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteEventById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteEventById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDeleteEventById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CalendarEventEntity>> getAllEvents() {
    final String _sql = "SELECT * FROM calendar_events ORDER BY startTime ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"calendar_events"}, new Callable<List<CalendarEventEntity>>() {
      @Override
      @NonNull
      public List<CalendarEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfAttendeesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "attendeesJson");
          final int _cursorIndexOfCalendarName = CursorUtil.getColumnIndexOrThrow(_cursor, "calendarName");
          final int _cursorIndexOfColorTag = CursorUtil.getColumnIndexOrThrow(_cursor, "colorTag");
          final int _cursorIndexOfIsAllDay = CursorUtil.getColumnIndexOrThrow(_cursor, "isAllDay");
          final int _cursorIndexOfGoogleEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "googleEventId");
          final List<CalendarEventEntity> _result = new ArrayList<CalendarEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CalendarEventEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final long _tmpEndTime;
            _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            final String _tmpAttendeesJson;
            _tmpAttendeesJson = _cursor.getString(_cursorIndexOfAttendeesJson);
            final String _tmpCalendarName;
            _tmpCalendarName = _cursor.getString(_cursorIndexOfCalendarName);
            final String _tmpColorTag;
            _tmpColorTag = _cursor.getString(_cursorIndexOfColorTag);
            final boolean _tmpIsAllDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAllDay);
            _tmpIsAllDay = _tmp != 0;
            final String _tmpGoogleEventId;
            if (_cursor.isNull(_cursorIndexOfGoogleEventId)) {
              _tmpGoogleEventId = null;
            } else {
              _tmpGoogleEventId = _cursor.getString(_cursorIndexOfGoogleEventId);
            }
            _item = new CalendarEventEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpLocation,_tmpStartTime,_tmpEndTime,_tmpAttendeesJson,_tmpCalendarName,_tmpColorTag,_tmpIsAllDay,_tmpGoogleEventId);
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
  public Flow<List<CalendarEventEntity>> getEventsInRange(final long startTime,
      final long endTime) {
    final String _sql = "SELECT * FROM calendar_events WHERE endTime >= ? AND startTime <= ? ORDER BY startTime ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endTime);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"calendar_events"}, new Callable<List<CalendarEventEntity>>() {
      @Override
      @NonNull
      public List<CalendarEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfAttendeesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "attendeesJson");
          final int _cursorIndexOfCalendarName = CursorUtil.getColumnIndexOrThrow(_cursor, "calendarName");
          final int _cursorIndexOfColorTag = CursorUtil.getColumnIndexOrThrow(_cursor, "colorTag");
          final int _cursorIndexOfIsAllDay = CursorUtil.getColumnIndexOrThrow(_cursor, "isAllDay");
          final int _cursorIndexOfGoogleEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "googleEventId");
          final List<CalendarEventEntity> _result = new ArrayList<CalendarEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CalendarEventEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final long _tmpEndTime;
            _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            final String _tmpAttendeesJson;
            _tmpAttendeesJson = _cursor.getString(_cursorIndexOfAttendeesJson);
            final String _tmpCalendarName;
            _tmpCalendarName = _cursor.getString(_cursorIndexOfCalendarName);
            final String _tmpColorTag;
            _tmpColorTag = _cursor.getString(_cursorIndexOfColorTag);
            final boolean _tmpIsAllDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAllDay);
            _tmpIsAllDay = _tmp != 0;
            final String _tmpGoogleEventId;
            if (_cursor.isNull(_cursorIndexOfGoogleEventId)) {
              _tmpGoogleEventId = null;
            } else {
              _tmpGoogleEventId = _cursor.getString(_cursorIndexOfGoogleEventId);
            }
            _item = new CalendarEventEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpLocation,_tmpStartTime,_tmpEndTime,_tmpAttendeesJson,_tmpCalendarName,_tmpColorTag,_tmpIsAllDay,_tmpGoogleEventId);
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
  public Object getEventById(final String id,
      final Continuation<? super CalendarEventEntity> $completion) {
    final String _sql = "SELECT * FROM calendar_events WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CalendarEventEntity>() {
      @Override
      @Nullable
      public CalendarEventEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfAttendeesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "attendeesJson");
          final int _cursorIndexOfCalendarName = CursorUtil.getColumnIndexOrThrow(_cursor, "calendarName");
          final int _cursorIndexOfColorTag = CursorUtil.getColumnIndexOrThrow(_cursor, "colorTag");
          final int _cursorIndexOfIsAllDay = CursorUtil.getColumnIndexOrThrow(_cursor, "isAllDay");
          final int _cursorIndexOfGoogleEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "googleEventId");
          final CalendarEventEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final long _tmpEndTime;
            _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            final String _tmpAttendeesJson;
            _tmpAttendeesJson = _cursor.getString(_cursorIndexOfAttendeesJson);
            final String _tmpCalendarName;
            _tmpCalendarName = _cursor.getString(_cursorIndexOfCalendarName);
            final String _tmpColorTag;
            _tmpColorTag = _cursor.getString(_cursorIndexOfColorTag);
            final boolean _tmpIsAllDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAllDay);
            _tmpIsAllDay = _tmp != 0;
            final String _tmpGoogleEventId;
            if (_cursor.isNull(_cursorIndexOfGoogleEventId)) {
              _tmpGoogleEventId = null;
            } else {
              _tmpGoogleEventId = _cursor.getString(_cursorIndexOfGoogleEventId);
            }
            _result = new CalendarEventEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpLocation,_tmpStartTime,_tmpEndTime,_tmpAttendeesJson,_tmpCalendarName,_tmpColorTag,_tmpIsAllDay,_tmpGoogleEventId);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
