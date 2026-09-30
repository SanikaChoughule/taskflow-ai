package com.taskflowai.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.taskflowai.data.database.dao.ActionLogDao;
import com.taskflowai.data.database.dao.ActionLogDao_Impl;
import com.taskflowai.data.database.dao.CalendarEventDao;
import com.taskflowai.data.database.dao.CalendarEventDao_Impl;
import com.taskflowai.data.database.dao.ReminderDao;
import com.taskflowai.data.database.dao.ReminderDao_Impl;
import com.taskflowai.data.database.dao.ScheduleHistoryDao;
import com.taskflowai.data.database.dao.ScheduleHistoryDao_Impl;
import com.taskflowai.data.database.dao.SubTaskDao;
import com.taskflowai.data.database.dao.SubTaskDao_Impl;
import com.taskflowai.data.database.dao.SuggestionDao;
import com.taskflowai.data.database.dao.SuggestionDao_Impl;
import com.taskflowai.data.database.dao.TaskDao;
import com.taskflowai.data.database.dao.TaskDao_Impl;
import com.taskflowai.data.database.dao.UserDao;
import com.taskflowai.data.database.dao.UserDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TaskFlowDatabase_Impl extends TaskFlowDatabase {
  private volatile TaskDao _taskDao;

  private volatile SubTaskDao _subTaskDao;

  private volatile ActionLogDao _actionLogDao;

  private volatile CalendarEventDao _calendarEventDao;

  private volatile ReminderDao _reminderDao;

  private volatile SuggestionDao _suggestionDao;

  private volatile UserDao _userDao;

  private volatile ScheduleHistoryDao _scheduleHistoryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `originalCommand` TEXT NOT NULL, `status` TEXT NOT NULL, `priority` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER, `linkedCalendarEventId` TEXT, `linkedReminderId` TEXT, `isUndoable` INTEGER NOT NULL, `errorMessage` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `subtasks` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `stepOrder` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `status` TEXT NOT NULL, `executedAt` INTEGER, `errorMessage` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subtasks_taskId` ON `subtasks` (`taskId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `action_logs` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `actionType` TEXT NOT NULL, `status` TEXT NOT NULL, `description` TEXT NOT NULL, `metadataJson` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_action_logs_taskId` ON `action_logs` (`taskId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `calendar_events` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `location` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `attendeesJson` TEXT NOT NULL, `calendarName` TEXT NOT NULL, `colorTag` TEXT NOT NULL, `isAllDay` INTEGER NOT NULL, `googleEventId` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` TEXT NOT NULL, `taskId` TEXT, `calendarEventId` TEXT, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `triggerTime` INTEGER NOT NULL, `timingMinutesBefore` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `suggestions` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `message` TEXT NOT NULL, `actionCommand` TEXT NOT NULL, `type` TEXT NOT NULL, `isDismissed` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `photoUrl` TEXT, `isGoogleConnected` INTEGER NOT NULL, `isCalendarConnected` INTEGER NOT NULL, `lastLoginAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `schedule_history` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `scheduledDate` TEXT NOT NULL, `startTimeFormatted` TEXT NOT NULL, `endTimeFormatted` TEXT NOT NULL, `startTimeMs` INTEGER NOT NULL, `endTimeMs` INTEGER NOT NULL, `attendee` TEXT, `attendeeEmail` TEXT, `action` TEXT NOT NULL, `actionTime` INTEGER NOT NULL, `calendarEventId` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '27aef869b714fcfaa6821c6f983bfa63')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `tasks`");
        db.execSQL("DROP TABLE IF EXISTS `subtasks`");
        db.execSQL("DROP TABLE IF EXISTS `action_logs`");
        db.execSQL("DROP TABLE IF EXISTS `calendar_events`");
        db.execSQL("DROP TABLE IF EXISTS `reminders`");
        db.execSQL("DROP TABLE IF EXISTS `suggestions`");
        db.execSQL("DROP TABLE IF EXISTS `users`");
        db.execSQL("DROP TABLE IF EXISTS `schedule_history`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsTasks = new HashMap<String, TableInfo.Column>(13);
        _columnsTasks.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("originalCommand", new TableInfo.Column("originalCommand", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("priority", new TableInfo.Column("priority", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("linkedCalendarEventId", new TableInfo.Column("linkedCalendarEventId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("linkedReminderId", new TableInfo.Column("linkedReminderId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("isUndoable", new TableInfo.Column("isUndoable", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("errorMessage", new TableInfo.Column("errorMessage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTasks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTasks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTasks = new TableInfo("tasks", _columnsTasks, _foreignKeysTasks, _indicesTasks);
        final TableInfo _existingTasks = TableInfo.read(db, "tasks");
        if (!_infoTasks.equals(_existingTasks)) {
          return new RoomOpenHelper.ValidationResult(false, "tasks(com.taskflowai.data.database.entity.TaskEntity).\n"
                  + " Expected:\n" + _infoTasks + "\n"
                  + " Found:\n" + _existingTasks);
        }
        final HashMap<String, TableInfo.Column> _columnsSubtasks = new HashMap<String, TableInfo.Column>(8);
        _columnsSubtasks.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubtasks.put("taskId", new TableInfo.Column("taskId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubtasks.put("stepOrder", new TableInfo.Column("stepOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubtasks.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubtasks.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubtasks.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubtasks.put("executedAt", new TableInfo.Column("executedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSubtasks.put("errorMessage", new TableInfo.Column("errorMessage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSubtasks = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysSubtasks.add(new TableInfo.ForeignKey("tasks", "CASCADE", "NO ACTION", Arrays.asList("taskId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesSubtasks = new HashSet<TableInfo.Index>(1);
        _indicesSubtasks.add(new TableInfo.Index("index_subtasks_taskId", false, Arrays.asList("taskId"), Arrays.asList("ASC")));
        final TableInfo _infoSubtasks = new TableInfo("subtasks", _columnsSubtasks, _foreignKeysSubtasks, _indicesSubtasks);
        final TableInfo _existingSubtasks = TableInfo.read(db, "subtasks");
        if (!_infoSubtasks.equals(_existingSubtasks)) {
          return new RoomOpenHelper.ValidationResult(false, "subtasks(com.taskflowai.data.database.entity.SubTaskEntity).\n"
                  + " Expected:\n" + _infoSubtasks + "\n"
                  + " Found:\n" + _existingSubtasks);
        }
        final HashMap<String, TableInfo.Column> _columnsActionLogs = new HashMap<String, TableInfo.Column>(7);
        _columnsActionLogs.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActionLogs.put("taskId", new TableInfo.Column("taskId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActionLogs.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActionLogs.put("actionType", new TableInfo.Column("actionType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActionLogs.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActionLogs.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsActionLogs.put("metadataJson", new TableInfo.Column("metadataJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysActionLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesActionLogs = new HashSet<TableInfo.Index>(1);
        _indicesActionLogs.add(new TableInfo.Index("index_action_logs_taskId", false, Arrays.asList("taskId"), Arrays.asList("ASC")));
        final TableInfo _infoActionLogs = new TableInfo("action_logs", _columnsActionLogs, _foreignKeysActionLogs, _indicesActionLogs);
        final TableInfo _existingActionLogs = TableInfo.read(db, "action_logs");
        if (!_infoActionLogs.equals(_existingActionLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "action_logs(com.taskflowai.data.database.entity.ActionLogEntity).\n"
                  + " Expected:\n" + _infoActionLogs + "\n"
                  + " Found:\n" + _existingActionLogs);
        }
        final HashMap<String, TableInfo.Column> _columnsCalendarEvents = new HashMap<String, TableInfo.Column>(11);
        _columnsCalendarEvents.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("location", new TableInfo.Column("location", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("endTime", new TableInfo.Column("endTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("attendeesJson", new TableInfo.Column("attendeesJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("calendarName", new TableInfo.Column("calendarName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("colorTag", new TableInfo.Column("colorTag", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("isAllDay", new TableInfo.Column("isAllDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("googleEventId", new TableInfo.Column("googleEventId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCalendarEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCalendarEvents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCalendarEvents = new TableInfo("calendar_events", _columnsCalendarEvents, _foreignKeysCalendarEvents, _indicesCalendarEvents);
        final TableInfo _existingCalendarEvents = TableInfo.read(db, "calendar_events");
        if (!_infoCalendarEvents.equals(_existingCalendarEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "calendar_events(com.taskflowai.data.database.entity.CalendarEventEntity).\n"
                  + " Expected:\n" + _infoCalendarEvents + "\n"
                  + " Found:\n" + _existingCalendarEvents);
        }
        final HashMap<String, TableInfo.Column> _columnsReminders = new HashMap<String, TableInfo.Column>(9);
        _columnsReminders.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminders.put("taskId", new TableInfo.Column("taskId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminders.put("calendarEventId", new TableInfo.Column("calendarEventId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminders.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminders.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminders.put("triggerTime", new TableInfo.Column("triggerTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminders.put("timingMinutesBefore", new TableInfo.Column("timingMinutesBefore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminders.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminders.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysReminders = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesReminders = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoReminders = new TableInfo("reminders", _columnsReminders, _foreignKeysReminders, _indicesReminders);
        final TableInfo _existingReminders = TableInfo.read(db, "reminders");
        if (!_infoReminders.equals(_existingReminders)) {
          return new RoomOpenHelper.ValidationResult(false, "reminders(com.taskflowai.data.database.entity.ReminderEntity).\n"
                  + " Expected:\n" + _infoReminders + "\n"
                  + " Found:\n" + _existingReminders);
        }
        final HashMap<String, TableInfo.Column> _columnsSuggestions = new HashMap<String, TableInfo.Column>(7);
        _columnsSuggestions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuggestions.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuggestions.put("message", new TableInfo.Column("message", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuggestions.put("actionCommand", new TableInfo.Column("actionCommand", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuggestions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuggestions.put("isDismissed", new TableInfo.Column("isDismissed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuggestions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSuggestions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSuggestions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSuggestions = new TableInfo("suggestions", _columnsSuggestions, _foreignKeysSuggestions, _indicesSuggestions);
        final TableInfo _existingSuggestions = TableInfo.read(db, "suggestions");
        if (!_infoSuggestions.equals(_existingSuggestions)) {
          return new RoomOpenHelper.ValidationResult(false, "suggestions(com.taskflowai.data.database.entity.SuggestionEntity).\n"
                  + " Expected:\n" + _infoSuggestions + "\n"
                  + " Found:\n" + _existingSuggestions);
        }
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(7);
        _columnsUsers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("email", new TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("photoUrl", new TableInfo.Column("photoUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("isGoogleConnected", new TableInfo.Column("isGoogleConnected", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("isCalendarConnected", new TableInfo.Column("isCalendarConnected", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("lastLoginAt", new TableInfo.Column("lastLoginAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(com.taskflowai.data.database.entity.UserEntity).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsScheduleHistory = new HashMap<String, TableInfo.Column>(13);
        _columnsScheduleHistory.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("scheduledDate", new TableInfo.Column("scheduledDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("startTimeFormatted", new TableInfo.Column("startTimeFormatted", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("endTimeFormatted", new TableInfo.Column("endTimeFormatted", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("startTimeMs", new TableInfo.Column("startTimeMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("endTimeMs", new TableInfo.Column("endTimeMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("attendee", new TableInfo.Column("attendee", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("attendeeEmail", new TableInfo.Column("attendeeEmail", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("action", new TableInfo.Column("action", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("actionTime", new TableInfo.Column("actionTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScheduleHistory.put("calendarEventId", new TableInfo.Column("calendarEventId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysScheduleHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesScheduleHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoScheduleHistory = new TableInfo("schedule_history", _columnsScheduleHistory, _foreignKeysScheduleHistory, _indicesScheduleHistory);
        final TableInfo _existingScheduleHistory = TableInfo.read(db, "schedule_history");
        if (!_infoScheduleHistory.equals(_existingScheduleHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "schedule_history(com.taskflowai.data.database.entity.ScheduleHistoryEntity).\n"
                  + " Expected:\n" + _infoScheduleHistory + "\n"
                  + " Found:\n" + _existingScheduleHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "27aef869b714fcfaa6821c6f983bfa63", "37346929494f4ebef117b1fb3ac1767d");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "tasks","subtasks","action_logs","calendar_events","reminders","suggestions","users","schedule_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `tasks`");
      _db.execSQL("DELETE FROM `subtasks`");
      _db.execSQL("DELETE FROM `action_logs`");
      _db.execSQL("DELETE FROM `calendar_events`");
      _db.execSQL("DELETE FROM `reminders`");
      _db.execSQL("DELETE FROM `suggestions`");
      _db.execSQL("DELETE FROM `users`");
      _db.execSQL("DELETE FROM `schedule_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TaskDao.class, TaskDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SubTaskDao.class, SubTaskDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ActionLogDao.class, ActionLogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CalendarEventDao.class, CalendarEventDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ReminderDao.class, ReminderDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SuggestionDao.class, SuggestionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ScheduleHistoryDao.class, ScheduleHistoryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TaskDao taskDao() {
    if (_taskDao != null) {
      return _taskDao;
    } else {
      synchronized(this) {
        if(_taskDao == null) {
          _taskDao = new TaskDao_Impl(this);
        }
        return _taskDao;
      }
    }
  }

  @Override
  public SubTaskDao subTaskDao() {
    if (_subTaskDao != null) {
      return _subTaskDao;
    } else {
      synchronized(this) {
        if(_subTaskDao == null) {
          _subTaskDao = new SubTaskDao_Impl(this);
        }
        return _subTaskDao;
      }
    }
  }

  @Override
  public ActionLogDao actionLogDao() {
    if (_actionLogDao != null) {
      return _actionLogDao;
    } else {
      synchronized(this) {
        if(_actionLogDao == null) {
          _actionLogDao = new ActionLogDao_Impl(this);
        }
        return _actionLogDao;
      }
    }
  }

  @Override
  public CalendarEventDao calendarEventDao() {
    if (_calendarEventDao != null) {
      return _calendarEventDao;
    } else {
      synchronized(this) {
        if(_calendarEventDao == null) {
          _calendarEventDao = new CalendarEventDao_Impl(this);
        }
        return _calendarEventDao;
      }
    }
  }

  @Override
  public ReminderDao reminderDao() {
    if (_reminderDao != null) {
      return _reminderDao;
    } else {
      synchronized(this) {
        if(_reminderDao == null) {
          _reminderDao = new ReminderDao_Impl(this);
        }
        return _reminderDao;
      }
    }
  }

  @Override
  public SuggestionDao suggestionDao() {
    if (_suggestionDao != null) {
      return _suggestionDao;
    } else {
      synchronized(this) {
        if(_suggestionDao == null) {
          _suggestionDao = new SuggestionDao_Impl(this);
        }
        return _suggestionDao;
      }
    }
  }

  @Override
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public ScheduleHistoryDao scheduleHistoryDao() {
    if (_scheduleHistoryDao != null) {
      return _scheduleHistoryDao;
    } else {
      synchronized(this) {
        if(_scheduleHistoryDao == null) {
          _scheduleHistoryDao = new ScheduleHistoryDao_Impl(this);
        }
        return _scheduleHistoryDao;
      }
    }
  }
}
