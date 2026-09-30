package com.taskflowai.data.database.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.taskflowai.data.database.entity.SubTaskEntity;
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
public final class SubTaskDao_Impl implements SubTaskDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SubTaskEntity> __insertionAdapterOfSubTaskEntity;

  private final EntityDeletionOrUpdateAdapter<SubTaskEntity> __updateAdapterOfSubTaskEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSubTasksForTask;

  public SubTaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSubTaskEntity = new EntityInsertionAdapter<SubTaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `subtasks` (`id`,`taskId`,`stepOrder`,`title`,`description`,`status`,`executedAt`,`errorMessage`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SubTaskEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTaskId());
        statement.bindLong(3, entity.getStepOrder());
        statement.bindString(4, entity.getTitle());
        statement.bindString(5, entity.getDescription());
        statement.bindString(6, entity.getStatus());
        if (entity.getExecutedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getExecutedAt());
        }
        if (entity.getErrorMessage() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getErrorMessage());
        }
      }
    };
    this.__updateAdapterOfSubTaskEntity = new EntityDeletionOrUpdateAdapter<SubTaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `subtasks` SET `id` = ?,`taskId` = ?,`stepOrder` = ?,`title` = ?,`description` = ?,`status` = ?,`executedAt` = ?,`errorMessage` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SubTaskEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTaskId());
        statement.bindLong(3, entity.getStepOrder());
        statement.bindString(4, entity.getTitle());
        statement.bindString(5, entity.getDescription());
        statement.bindString(6, entity.getStatus());
        if (entity.getExecutedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getExecutedAt());
        }
        if (entity.getErrorMessage() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getErrorMessage());
        }
        statement.bindString(9, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteSubTasksForTask = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM subtasks WHERE taskId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertSubTasks(final List<SubTaskEntity> subTasks,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSubTaskEntity.insert(subTasks);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSubTask(final SubTaskEntity subTask,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSubTaskEntity.handle(subTask);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSubTasksForTask(final String taskId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSubTasksForTask.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, taskId);
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
          __preparedStmtOfDeleteSubTasksForTask.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SubTaskEntity>> getSubTasksForTask(final String taskId) {
    final String _sql = "SELECT * FROM subtasks WHERE taskId = ? ORDER BY stepOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, taskId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"subtasks"}, new Callable<List<SubTaskEntity>>() {
      @Override
      @NonNull
      public List<SubTaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfStepOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "stepOrder");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfExecutedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "executedAt");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<SubTaskEntity> _result = new ArrayList<SubTaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SubTaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            final int _tmpStepOrder;
            _tmpStepOrder = _cursor.getInt(_cursorIndexOfStepOrder);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final Long _tmpExecutedAt;
            if (_cursor.isNull(_cursorIndexOfExecutedAt)) {
              _tmpExecutedAt = null;
            } else {
              _tmpExecutedAt = _cursor.getLong(_cursorIndexOfExecutedAt);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new SubTaskEntity(_tmpId,_tmpTaskId,_tmpStepOrder,_tmpTitle,_tmpDescription,_tmpStatus,_tmpExecutedAt,_tmpErrorMessage);
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
