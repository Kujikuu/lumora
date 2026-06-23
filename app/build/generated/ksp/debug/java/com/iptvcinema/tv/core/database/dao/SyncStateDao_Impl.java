package com.iptvcinema.tv.core.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SyncStateDao_Impl implements SyncStateDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalSourceSyncStateEntity> __insertionAdapterOfLocalSourceSyncStateEntity;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public SyncStateDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalSourceSyncStateEntity = new EntityInsertionAdapter<LocalSourceSyncStateEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `source_sync_state` (`sourceId`,`lastSyncedAtEpochMs`,`liveChannelCount`,`movieCount`,`seriesCount`,`epgAvailable`,`lastError`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalSourceSyncStateEntity entity) {
        statement.bindString(1, entity.getSourceId());
        if (entity.getLastSyncedAtEpochMs() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getLastSyncedAtEpochMs());
        }
        statement.bindLong(3, entity.getLiveChannelCount());
        statement.bindLong(4, entity.getMovieCount());
        statement.bindLong(5, entity.getSeriesCount());
        final int _tmp = entity.getEpgAvailable() ? 1 : 0;
        statement.bindLong(6, _tmp);
        if (entity.getLastError() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getLastError());
        }
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM source_sync_state WHERE sourceId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final LocalSourceSyncStateEntity state,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalSourceSyncStateEntity.insert(state);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final String sourceId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
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
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<LocalSourceSyncStateEntity> observe(final String sourceId) {
    final String _sql = "SELECT * FROM source_sync_state WHERE sourceId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"source_sync_state"}, new Callable<LocalSourceSyncStateEntity>() {
      @Override
      @Nullable
      public LocalSourceSyncStateEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfLastSyncedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAtEpochMs");
          final int _cursorIndexOfLiveChannelCount = CursorUtil.getColumnIndexOrThrow(_cursor, "liveChannelCount");
          final int _cursorIndexOfMovieCount = CursorUtil.getColumnIndexOrThrow(_cursor, "movieCount");
          final int _cursorIndexOfSeriesCount = CursorUtil.getColumnIndexOrThrow(_cursor, "seriesCount");
          final int _cursorIndexOfEpgAvailable = CursorUtil.getColumnIndexOrThrow(_cursor, "epgAvailable");
          final int _cursorIndexOfLastError = CursorUtil.getColumnIndexOrThrow(_cursor, "lastError");
          final LocalSourceSyncStateEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final Long _tmpLastSyncedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAtEpochMs)) {
              _tmpLastSyncedAtEpochMs = null;
            } else {
              _tmpLastSyncedAtEpochMs = _cursor.getLong(_cursorIndexOfLastSyncedAtEpochMs);
            }
            final int _tmpLiveChannelCount;
            _tmpLiveChannelCount = _cursor.getInt(_cursorIndexOfLiveChannelCount);
            final int _tmpMovieCount;
            _tmpMovieCount = _cursor.getInt(_cursorIndexOfMovieCount);
            final int _tmpSeriesCount;
            _tmpSeriesCount = _cursor.getInt(_cursorIndexOfSeriesCount);
            final boolean _tmpEpgAvailable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEpgAvailable);
            _tmpEpgAvailable = _tmp != 0;
            final String _tmpLastError;
            if (_cursor.isNull(_cursorIndexOfLastError)) {
              _tmpLastError = null;
            } else {
              _tmpLastError = _cursor.getString(_cursorIndexOfLastError);
            }
            _result = new LocalSourceSyncStateEntity(_tmpSourceId,_tmpLastSyncedAtEpochMs,_tmpLiveChannelCount,_tmpMovieCount,_tmpSeriesCount,_tmpEpgAvailable,_tmpLastError);
          } else {
            _result = null;
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
  public Object get(final String sourceId,
      final Continuation<? super LocalSourceSyncStateEntity> $completion) {
    final String _sql = "SELECT * FROM source_sync_state WHERE sourceId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalSourceSyncStateEntity>() {
      @Override
      @Nullable
      public LocalSourceSyncStateEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfLastSyncedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAtEpochMs");
          final int _cursorIndexOfLiveChannelCount = CursorUtil.getColumnIndexOrThrow(_cursor, "liveChannelCount");
          final int _cursorIndexOfMovieCount = CursorUtil.getColumnIndexOrThrow(_cursor, "movieCount");
          final int _cursorIndexOfSeriesCount = CursorUtil.getColumnIndexOrThrow(_cursor, "seriesCount");
          final int _cursorIndexOfEpgAvailable = CursorUtil.getColumnIndexOrThrow(_cursor, "epgAvailable");
          final int _cursorIndexOfLastError = CursorUtil.getColumnIndexOrThrow(_cursor, "lastError");
          final LocalSourceSyncStateEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final Long _tmpLastSyncedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAtEpochMs)) {
              _tmpLastSyncedAtEpochMs = null;
            } else {
              _tmpLastSyncedAtEpochMs = _cursor.getLong(_cursorIndexOfLastSyncedAtEpochMs);
            }
            final int _tmpLiveChannelCount;
            _tmpLiveChannelCount = _cursor.getInt(_cursorIndexOfLiveChannelCount);
            final int _tmpMovieCount;
            _tmpMovieCount = _cursor.getInt(_cursorIndexOfMovieCount);
            final int _tmpSeriesCount;
            _tmpSeriesCount = _cursor.getInt(_cursorIndexOfSeriesCount);
            final boolean _tmpEpgAvailable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEpgAvailable);
            _tmpEpgAvailable = _tmp != 0;
            final String _tmpLastError;
            if (_cursor.isNull(_cursorIndexOfLastError)) {
              _tmpLastError = null;
            } else {
              _tmpLastError = _cursor.getString(_cursorIndexOfLastError);
            }
            _result = new LocalSourceSyncStateEntity(_tmpSourceId,_tmpLastSyncedAtEpochMs,_tmpLiveChannelCount,_tmpMovieCount,_tmpSeriesCount,_tmpEpgAvailable,_tmpLastError);
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
