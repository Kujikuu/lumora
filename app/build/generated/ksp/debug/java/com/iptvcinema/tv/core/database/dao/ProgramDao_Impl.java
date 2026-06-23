package com.iptvcinema.tv.core.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.iptvcinema.tv.core.database.entity.LocalProgramEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ProgramDao_Impl implements ProgramDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalProgramEntity> __insertionAdapterOfLocalProgramEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySource;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public ProgramDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalProgramEntity = new EntityInsertionAdapter<LocalProgramEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `programs` (`id`,`sourceId`,`channelId`,`title`,`description`,`startEpochMs`,`endEpochMs`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalProgramEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSourceId());
        statement.bindString(3, entity.getChannelId());
        statement.bindString(4, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDescription());
        }
        statement.bindLong(6, entity.getStartEpochMs());
        statement.bindLong(7, entity.getEndEpochMs());
      }
    };
    this.__preparedStmtOfDeleteBySource = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM programs WHERE sourceId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM programs WHERE sourceId = ? AND endEpochMs < ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsertAll(final List<LocalProgramEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalProgramEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBySource(final String sourceId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBySource.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, sourceId);
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
          __preparedStmtOfDeleteBySource.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final String sourceId, final long cutoffMs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, sourceId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, cutoffMs);
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
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getUpcomingForChannel(final String sourceId, final String channelId,
      final long nowMs, final Continuation<? super List<LocalProgramEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM programs\n"
            + "        WHERE sourceId = ? AND channelId = ?\n"
            + "        AND endEpochMs > ?\n"
            + "        ORDER BY startEpochMs\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, channelId);
    _argIndex = 3;
    _statement.bindLong(_argIndex, nowMs);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalProgramEntity>>() {
      @Override
      @NonNull
      public List<LocalProgramEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "channelId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStartEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpochMs");
          final int _cursorIndexOfEndEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpochMs");
          final List<LocalProgramEntity> _result = new ArrayList<LocalProgramEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalProgramEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpChannelId;
            _tmpChannelId = _cursor.getString(_cursorIndexOfChannelId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpStartEpochMs;
            _tmpStartEpochMs = _cursor.getLong(_cursorIndexOfStartEpochMs);
            final long _tmpEndEpochMs;
            _tmpEndEpochMs = _cursor.getLong(_cursorIndexOfEndEpochMs);
            _item = new LocalProgramEntity(_tmpId,_tmpSourceId,_tmpChannelId,_tmpTitle,_tmpDescription,_tmpStartEpochMs,_tmpEndEpochMs);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getProgramsForChannels(final String sourceId, final List<String> channelIds,
      final long windowStartMs, final long windowEndMs,
      final Continuation<? super List<LocalProgramEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("\n");
    _stringBuilder.append("        SELECT * FROM programs");
    _stringBuilder.append("\n");
    _stringBuilder.append("        WHERE sourceId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND channelId IN (");
    final int _inputSize = channelIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    _stringBuilder.append("\n");
    _stringBuilder.append("        AND startEpochMs <= ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND endEpochMs >= ");
    _stringBuilder.append("?");
    _stringBuilder.append("\n");
    _stringBuilder.append("        ORDER BY channelId, startEpochMs");
    _stringBuilder.append("\n");
    _stringBuilder.append("        ");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 3 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    for (String _item : channelIds) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    _argIndex = 2 + _inputSize;
    _statement.bindLong(_argIndex, windowEndMs);
    _argIndex = 3 + _inputSize;
    _statement.bindLong(_argIndex, windowStartMs);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalProgramEntity>>() {
      @Override
      @NonNull
      public List<LocalProgramEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "channelId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStartEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpochMs");
          final int _cursorIndexOfEndEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpochMs");
          final List<LocalProgramEntity> _result = new ArrayList<LocalProgramEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalProgramEntity _item_1;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpChannelId;
            _tmpChannelId = _cursor.getString(_cursorIndexOfChannelId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpStartEpochMs;
            _tmpStartEpochMs = _cursor.getLong(_cursorIndexOfStartEpochMs);
            final long _tmpEndEpochMs;
            _tmpEndEpochMs = _cursor.getLong(_cursorIndexOfEndEpochMs);
            _item_1 = new LocalProgramEntity(_tmpId,_tmpSourceId,_tmpChannelId,_tmpTitle,_tmpDescription,_tmpStartEpochMs,_tmpEndEpochMs);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getCurrentProgramsForChannels(final String sourceId, final List<String> channelIds,
      final long nowMs, final Continuation<? super List<LocalProgramEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("\n");
    _stringBuilder.append("        SELECT * FROM programs");
    _stringBuilder.append("\n");
    _stringBuilder.append("        WHERE sourceId = ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND channelId IN (");
    final int _inputSize = channelIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    _stringBuilder.append("\n");
    _stringBuilder.append("        AND startEpochMs <= ");
    _stringBuilder.append("?");
    _stringBuilder.append(" AND endEpochMs > ");
    _stringBuilder.append("?");
    _stringBuilder.append("\n");
    _stringBuilder.append("        ORDER BY channelId, startEpochMs");
    _stringBuilder.append("\n");
    _stringBuilder.append("        ");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 3 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    for (String _item : channelIds) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    _argIndex = 2 + _inputSize;
    _statement.bindLong(_argIndex, nowMs);
    _argIndex = 3 + _inputSize;
    _statement.bindLong(_argIndex, nowMs);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalProgramEntity>>() {
      @Override
      @NonNull
      public List<LocalProgramEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "channelId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStartEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpochMs");
          final int _cursorIndexOfEndEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpochMs");
          final List<LocalProgramEntity> _result = new ArrayList<LocalProgramEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalProgramEntity _item_1;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpChannelId;
            _tmpChannelId = _cursor.getString(_cursorIndexOfChannelId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpStartEpochMs;
            _tmpStartEpochMs = _cursor.getLong(_cursorIndexOfStartEpochMs);
            final long _tmpEndEpochMs;
            _tmpEndEpochMs = _cursor.getLong(_cursorIndexOfEndEpochMs);
            _item_1 = new LocalProgramEntity(_tmpId,_tmpSourceId,_tmpChannelId,_tmpTitle,_tmpDescription,_tmpStartEpochMs,_tmpEndEpochMs);
            _result.add(_item_1);
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
