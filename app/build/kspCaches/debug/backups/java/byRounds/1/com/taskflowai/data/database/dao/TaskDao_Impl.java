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
import com.taskflowai.data.database.entity.TaskEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
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
public final class TaskDao_Impl implements TaskDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TaskEntity> __insertionAdapterOfTaskEntity;

  private final EntityDeletionOrUpdateAdapter<TaskEntity> __updateAdapterOfTaskEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTask;

  private final SharedSQLiteStatement __preparedStmtOfAssociateUnassignedTasksToUser;

  public TaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTaskEntity = new EntityInsertionAdapter<TaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tasks` (`id`,`userId`,`title`,`description`,`originalCommand`,`status`,`priority`,`createdAt`,`completedAt`,`linkedCalendarEventId`,`linkedReminderId`,`isUndoable`,`errorMessage`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TaskEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getDescription());
        statement.bindString(5, entity.getOriginalCommand());
        statement.bindString(6, entity.getStatus());
        statement.bindString(7, entity.getPriority());
        statement.bindLong(8, entity.getCreatedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getCompletedAt());
        }
        if (entity.getLinkedCalendarEventId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getLinkedCalendarEventId());
        }
        if (entity.getLinkedReminderId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getLinkedReminderId());
        }
        final int _tmp = entity.isUndoable() ? 1 : 0;
        statement.bindLong(12, _tmp);
        if (entity.getErrorMessage() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getErrorMessage());
        }
      }
    };
    this.__updateAdapterOfTaskEntity = new EntityDeletionOrUpdateAdapter<TaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `tasks` SET `id` = ?,`userId` = ?,`title` = ?,`description` = ?,`originalCommand` = ?,`status` = ?,`priority` = ?,`createdAt` = ?,`completedAt` = ?,`linkedCalendarEventId` = ?,`linkedReminderId` = ?,`isUndoable` = ?,`errorMessage` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TaskEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getDescription());
        statement.bindString(5, entity.getOriginalCommand());
        statement.bindString(6, entity.getStatus());
        statement.bindString(7, entity.getPriority());
        statement.bindLong(8, entity.getCreatedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getCompletedAt());
        }
        if (entity.getLinkedCalendarEventId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getLinkedCalendarEventId());
        }
        if (entity.getLinkedReminderId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getLinkedReminderId());
        }
        final int _tmp = entity.isUndoable() ? 1 : 0;
        statement.bindLong(12, _tmp);
        if (entity.getErrorMessage() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getErrorMessage());
        }
        statement.bindString(14, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteTask = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tasks WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfAssociateUnassignedTasksToUser = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE tasks SET userId = ? WHERE userId = '' OR userId IS NULL";
        return _query;
      }
    };
  }

  @Override
  public Object insertTask(final TaskEntity task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTaskEntity.insert(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTask(final TaskEntity task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTaskEntity.handle(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTask(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTask.acquire();
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
          __preparedStmtOfDeleteTask.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object associateUnassignedTasksToUser(final String userId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfAssociateUnassignedTasksToUser.acquire();
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
          __preparedStmtOfAssociateUnassignedTasksToUser.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TaskEntity>> getTasksForUser(final String userId) {
    final String _sql = "SELECT * FROM tasks WHERE ? = '' OR userId = ? OR userId = '' ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfOriginalCommand = CursorUtil.getColumnIndexOrThrow(_cursor, "originalCommand");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfLinkedCalendarEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedCalendarEventId");
          final int _cursorIndexOfLinkedReminderId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedReminderId");
          final int _cursorIndexOfIsUndoable = CursorUtil.getColumnIndexOrThrow(_cursor, "isUndoable");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpOriginalCommand;
            _tmpOriginalCommand = _cursor.getString(_cursorIndexOfOriginalCommand);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final String _tmpLinkedCalendarEventId;
            if (_cursor.isNull(_cursorIndexOfLinkedCalendarEventId)) {
              _tmpLinkedCalendarEventId = null;
            } else {
              _tmpLinkedCalendarEventId = _cursor.getString(_cursorIndexOfLinkedCalendarEventId);
            }
            final String _tmpLinkedReminderId;
            if (_cursor.isNull(_cursorIndexOfLinkedReminderId)) {
              _tmpLinkedReminderId = null;
            } else {
              _tmpLinkedReminderId = _cursor.getString(_cursorIndexOfLinkedReminderId);
            }
            final boolean _tmpIsUndoable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsUndoable);
            _tmpIsUndoable = _tmp != 0;
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new TaskEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpDescription,_tmpOriginalCommand,_tmpStatus,_tmpPriority,_tmpCreatedAt,_tmpCompletedAt,_tmpLinkedCalendarEventId,_tmpLinkedReminderId,_tmpIsUndoable,_tmpErrorMessage);
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
  public Flow<List<TaskEntity>> getAllTasks() {
    final String _sql = "SELECT * FROM tasks ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfOriginalCommand = CursorUtil.getColumnIndexOrThrow(_cursor, "originalCommand");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfLinkedCalendarEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedCalendarEventId");
          final int _cursorIndexOfLinkedReminderId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedReminderId");
          final int _cursorIndexOfIsUndoable = CursorUtil.getColumnIndexOrThrow(_cursor, "isUndoable");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpOriginalCommand;
            _tmpOriginalCommand = _cursor.getString(_cursorIndexOfOriginalCommand);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final String _tmpLinkedCalendarEventId;
            if (_cursor.isNull(_cursorIndexOfLinkedCalendarEventId)) {
              _tmpLinkedCalendarEventId = null;
            } else {
              _tmpLinkedCalendarEventId = _cursor.getString(_cursorIndexOfLinkedCalendarEventId);
            }
            final String _tmpLinkedReminderId;
            if (_cursor.isNull(_cursorIndexOfLinkedReminderId)) {
              _tmpLinkedReminderId = null;
            } else {
              _tmpLinkedReminderId = _cursor.getString(_cursorIndexOfLinkedReminderId);
            }
            final boolean _tmpIsUndoable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsUndoable);
            _tmpIsUndoable = _tmp != 0;
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new TaskEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpDescription,_tmpOriginalCommand,_tmpStatus,_tmpPriority,_tmpCreatedAt,_tmpCompletedAt,_tmpLinkedCalendarEventId,_tmpLinkedReminderId,_tmpIsUndoable,_tmpErrorMessage);
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
  public Flow<List<TaskEntity>> getRecentTasks(final int limit) {
    final String _sql = "SELECT * FROM tasks ORDER BY createdAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfOriginalCommand = CursorUtil.getColumnIndexOrThrow(_cursor, "originalCommand");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfLinkedCalendarEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedCalendarEventId");
          final int _cursorIndexOfLinkedReminderId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedReminderId");
          final int _cursorIndexOfIsUndoable = CursorUtil.getColumnIndexOrThrow(_cursor, "isUndoable");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpOriginalCommand;
            _tmpOriginalCommand = _cursor.getString(_cursorIndexOfOriginalCommand);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final String _tmpLinkedCalendarEventId;
            if (_cursor.isNull(_cursorIndexOfLinkedCalendarEventId)) {
              _tmpLinkedCalendarEventId = null;
            } else {
              _tmpLinkedCalendarEventId = _cursor.getString(_cursorIndexOfLinkedCalendarEventId);
            }
            final String _tmpLinkedReminderId;
            if (_cursor.isNull(_cursorIndexOfLinkedReminderId)) {
              _tmpLinkedReminderId = null;
            } else {
              _tmpLinkedReminderId = _cursor.getString(_cursorIndexOfLinkedReminderId);
            }
            final boolean _tmpIsUndoable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsUndoable);
            _tmpIsUndoable = _tmp != 0;
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new TaskEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpDescription,_tmpOriginalCommand,_tmpStatus,_tmpPriority,_tmpCreatedAt,_tmpCompletedAt,_tmpLinkedCalendarEventId,_tmpLinkedReminderId,_tmpIsUndoable,_tmpErrorMessage);
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
  public Object getTaskById(final String id, final Continuation<? super TaskEntity> $completion) {
    final String _sql = "SELECT * FROM tasks WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TaskEntity>() {
      @Override
      @Nullable
      public TaskEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfOriginalCommand = CursorUtil.getColumnIndexOrThrow(_cursor, "originalCommand");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfLinkedCalendarEventId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedCalendarEventId");
          final int _cursorIndexOfLinkedReminderId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedReminderId");
          final int _cursorIndexOfIsUndoable = CursorUtil.getColumnIndexOrThrow(_cursor, "isUndoable");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final TaskEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpOriginalCommand;
            _tmpOriginalCommand = _cursor.getString(_cursorIndexOfOriginalCommand);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpPriority;
            _tmpPriority = _cursor.getString(_cursorIndexOfPriority);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final String _tmpLinkedCalendarEventId;
            if (_cursor.isNull(_cursorIndexOfLinkedCalendarEventId)) {
              _tmpLinkedCalendarEventId = null;
            } else {
              _tmpLinkedCalendarEventId = _cursor.getString(_cursorIndexOfLinkedCalendarEventId);
            }
            final String _tmpLinkedReminderId;
            if (_cursor.isNull(_cursorIndexOfLinkedReminderId)) {
              _tmpLinkedReminderId = null;
            } else {
              _tmpLinkedReminderId = _cursor.getString(_cursorIndexOfLinkedReminderId);
            }
            final boolean _tmpIsUndoable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsUndoable);
            _tmpIsUndoable = _tmp != 0;
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _result = new TaskEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpDescription,_tmpOriginalCommand,_tmpStatus,_tmpPriority,_tmpCreatedAt,_tmpCompletedAt,_tmpLinkedCalendarEventId,_tmpLinkedReminderId,_tmpIsUndoable,_tmpErrorMessage);
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
