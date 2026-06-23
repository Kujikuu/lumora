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
import com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class EpisodeDao_Impl implements EpisodeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalEpisodeEntity> __insertionAdapterOfLocalEpisodeEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySeries;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySource;

  public EpisodeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalEpisodeEntity = new EntityInsertionAdapter<LocalEpisodeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `episodes` (`id`,`sourceId`,`seriesId`,`seasonNumber`,`episodeNumber`,`title`,`streamUrl`,`durationMinutes`,`plot`,`thumbnailUrl`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalEpisodeEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSourceId());
        statement.bindString(3, entity.getSeriesId());
        statement.bindLong(4, entity.getSeasonNumber());
        statement.bindLong(5, entity.getEpisodeNumber());
        statement.bindString(6, entity.getTitle());
        statement.bindString(7, entity.getStreamUrl());
        if (entity.getDurationMinutes() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getDurationMinutes());
        }
        if (entity.getPlot() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPlot());
        }
        if (entity.getThumbnailUrl() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getThumbnailUrl());
        }
      }
    };
    this.__preparedStmtOfDeleteBySeries = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM episodes WHERE sourceId = ? AND seriesId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteBySource = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM episodes WHERE sourceId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsertAll(final List<LocalEpisodeEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalEpisodeEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBySeries(final String sourceId, final String seriesId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBySeries.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, sourceId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, seriesId);
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
          __preparedStmtOfDeleteBySeries.release(_stmt);
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
  public Flow<List<LocalEpisodeEntity>> observeBySeries(final String sourceId,
      final String seriesId) {
    final String _sql = "\n"
            + "        SELECT * FROM episodes\n"
            + "        WHERE sourceId = ? AND seriesId = ?\n"
            + "        ORDER BY seasonNumber, episodeNumber\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, seriesId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"episodes"}, new Callable<List<LocalEpisodeEntity>>() {
      @Override
      @NonNull
      public List<LocalEpisodeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfSeriesId = CursorUtil.getColumnIndexOrThrow(_cursor, "seriesId");
          final int _cursorIndexOfSeasonNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "seasonNumber");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final List<LocalEpisodeEntity> _result = new ArrayList<LocalEpisodeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalEpisodeEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpSeriesId;
            _tmpSeriesId = _cursor.getString(_cursorIndexOfSeriesId);
            final int _tmpSeasonNumber;
            _tmpSeasonNumber = _cursor.getInt(_cursorIndexOfSeasonNumber);
            final int _tmpEpisodeNumber;
            _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            _item = new LocalEpisodeEntity(_tmpId,_tmpSourceId,_tmpSeriesId,_tmpSeasonNumber,_tmpEpisodeNumber,_tmpTitle,_tmpStreamUrl,_tmpDurationMinutes,_tmpPlot,_tmpThumbnailUrl);
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
  public Object getById(final String sourceId, final String episodeId,
      final Continuation<? super LocalEpisodeEntity> $completion) {
    final String _sql = "SELECT * FROM episodes WHERE sourceId = ? AND id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, episodeId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalEpisodeEntity>() {
      @Override
      @Nullable
      public LocalEpisodeEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfSeriesId = CursorUtil.getColumnIndexOrThrow(_cursor, "seriesId");
          final int _cursorIndexOfSeasonNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "seasonNumber");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final LocalEpisodeEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpSeriesId;
            _tmpSeriesId = _cursor.getString(_cursorIndexOfSeriesId);
            final int _tmpSeasonNumber;
            _tmpSeasonNumber = _cursor.getInt(_cursorIndexOfSeasonNumber);
            final int _tmpEpisodeNumber;
            _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            _result = new LocalEpisodeEntity(_tmpId,_tmpSourceId,_tmpSeriesId,_tmpSeasonNumber,_tmpEpisodeNumber,_tmpTitle,_tmpStreamUrl,_tmpDurationMinutes,_tmpPlot,_tmpThumbnailUrl);
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

  @Override
  public Object getBySeries(final String sourceId, final String seriesId,
      final Continuation<? super List<LocalEpisodeEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM episodes\n"
            + "        WHERE sourceId = ? AND seriesId = ?\n"
            + "        ORDER BY seasonNumber, episodeNumber\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, seriesId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalEpisodeEntity>>() {
      @Override
      @NonNull
      public List<LocalEpisodeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfSeriesId = CursorUtil.getColumnIndexOrThrow(_cursor, "seriesId");
          final int _cursorIndexOfSeasonNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "seasonNumber");
          final int _cursorIndexOfEpisodeNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "episodeNumber");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfThumbnailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailUrl");
          final List<LocalEpisodeEntity> _result = new ArrayList<LocalEpisodeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalEpisodeEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpSeriesId;
            _tmpSeriesId = _cursor.getString(_cursorIndexOfSeriesId);
            final int _tmpSeasonNumber;
            _tmpSeasonNumber = _cursor.getInt(_cursorIndexOfSeasonNumber);
            final int _tmpEpisodeNumber;
            _tmpEpisodeNumber = _cursor.getInt(_cursorIndexOfEpisodeNumber);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpThumbnailUrl;
            if (_cursor.isNull(_cursorIndexOfThumbnailUrl)) {
              _tmpThumbnailUrl = null;
            } else {
              _tmpThumbnailUrl = _cursor.getString(_cursorIndexOfThumbnailUrl);
            }
            _item = new LocalEpisodeEntity(_tmpId,_tmpSourceId,_tmpSeriesId,_tmpSeasonNumber,_tmpEpisodeNumber,_tmpTitle,_tmpStreamUrl,_tmpDurationMinutes,_tmpPlot,_tmpThumbnailUrl);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
