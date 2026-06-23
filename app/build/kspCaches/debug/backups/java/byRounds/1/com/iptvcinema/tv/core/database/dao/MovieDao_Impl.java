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
import com.iptvcinema.tv.core.database.entity.LocalMovieEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class MovieDao_Impl implements MovieDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalMovieEntity> __insertionAdapterOfLocalMovieEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySource;

  public MovieDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalMovieEntity = new EntityInsertionAdapter<LocalMovieEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `movies` (`id`,`sourceId`,`title`,`streamUrl`,`posterUrl`,`backdropUrl`,`categoryId`,`categoryName`,`year`,`durationMinutes`,`rating`,`plot`,`genres`,`cast`,`sortOrder`,`addedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalMovieEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSourceId());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getStreamUrl());
        if (entity.getPosterUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getPosterUrl());
        }
        if (entity.getBackdropUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getBackdropUrl());
        }
        if (entity.getCategoryId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getCategoryId());
        }
        if (entity.getCategoryName() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getCategoryName());
        }
        if (entity.getYear() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getYear());
        }
        if (entity.getDurationMinutes() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getDurationMinutes());
        }
        if (entity.getRating() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getRating());
        }
        if (entity.getPlot() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getPlot());
        }
        if (entity.getGenres() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getGenres());
        }
        if (entity.getCast() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getCast());
        }
        statement.bindLong(15, entity.getSortOrder());
        if (entity.getAddedAt() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getAddedAt());
        }
      }
    };
    this.__preparedStmtOfDeleteBySource = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM movies WHERE sourceId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsertAll(final List<LocalMovieEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalMovieEntity.insert(items);
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
  public Flow<List<LocalMovieEntity>> observeAll(final String sourceId) {
    final String _sql = "SELECT * FROM movies WHERE sourceId = ? ORDER BY sortOrder, title";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"movies"}, new Callable<List<LocalMovieEntity>>() {
      @Override
      @NonNull
      public List<LocalMovieEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalMovieEntity> _result = new ArrayList<LocalMovieEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalMovieEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _item = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
  public Flow<List<LocalMovieEntity>> observeByCategory(final String sourceId,
      final String categoryId) {
    final String _sql = "\n"
            + "        SELECT * FROM movies\n"
            + "        WHERE sourceId = ? AND categoryId = ?\n"
            + "        ORDER BY sortOrder, title\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, categoryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"movies"}, new Callable<List<LocalMovieEntity>>() {
      @Override
      @NonNull
      public List<LocalMovieEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalMovieEntity> _result = new ArrayList<LocalMovieEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalMovieEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _item = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
  public Flow<List<LocalMovieEntity>> observeByCategoryName(final String sourceId,
      final String categoryName) {
    final String _sql = "\n"
            + "        SELECT * FROM movies\n"
            + "        WHERE sourceId = ? AND categoryName = ?\n"
            + "        ORDER BY sortOrder, title\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, categoryName);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"movies"}, new Callable<List<LocalMovieEntity>>() {
      @Override
      @NonNull
      public List<LocalMovieEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalMovieEntity> _result = new ArrayList<LocalMovieEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalMovieEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _item = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
  public Flow<List<LocalMovieEntity>> observeFeatured(final String sourceId, final int limit) {
    final String _sql = "SELECT * FROM movies WHERE sourceId = ? ORDER BY sortOrder LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"movies"}, new Callable<List<LocalMovieEntity>>() {
      @Override
      @NonNull
      public List<LocalMovieEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalMovieEntity> _result = new ArrayList<LocalMovieEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalMovieEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _item = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
  public Flow<List<LocalMovieEntity>> observeRecentlyAdded(final String sourceId, final int limit) {
    final String _sql = "\n"
            + "        SELECT * FROM movies\n"
            + "        WHERE sourceId = ?\n"
            + "        ORDER BY\n"
            + "            CASE WHEN addedAt IS NULL THEN 0 ELSE 1 END DESC,\n"
            + "            addedAt DESC,\n"
            + "            sortOrder DESC\n"
            + "        LIMIT ?\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"movies"}, new Callable<List<LocalMovieEntity>>() {
      @Override
      @NonNull
      public List<LocalMovieEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalMovieEntity> _result = new ArrayList<LocalMovieEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalMovieEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _item = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
  public Flow<List<LocalMovieEntity>> observeByCategoryLimited(final String sourceId,
      final String categoryId, final int limit) {
    final String _sql = "\n"
            + "        SELECT * FROM movies\n"
            + "        WHERE sourceId = ? AND categoryId = ?\n"
            + "        ORDER BY sortOrder, title\n"
            + "        LIMIT ?\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, categoryId);
    _argIndex = 3;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"movies"}, new Callable<List<LocalMovieEntity>>() {
      @Override
      @NonNull
      public List<LocalMovieEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalMovieEntity> _result = new ArrayList<LocalMovieEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalMovieEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _item = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
  public Flow<List<LocalMovieEntity>> observeAllLimited(final String sourceId, final int limit) {
    final String _sql = "SELECT * FROM movies WHERE sourceId = ? ORDER BY sortOrder, title LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"movies"}, new Callable<List<LocalMovieEntity>>() {
      @Override
      @NonNull
      public List<LocalMovieEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalMovieEntity> _result = new ArrayList<LocalMovieEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalMovieEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _item = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
  public Object getById(final String sourceId, final String movieId,
      final Continuation<? super LocalMovieEntity> $completion) {
    final String _sql = "SELECT * FROM movies WHERE sourceId = ? AND id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, movieId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalMovieEntity>() {
      @Override
      @Nullable
      public LocalMovieEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final LocalMovieEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _result = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
  public Object getByCategory(final String sourceId, final String categoryId, final int limit,
      final Continuation<? super List<LocalMovieEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM movies\n"
            + "        WHERE sourceId = ? AND categoryId = ?\n"
            + "        ORDER BY sortOrder, title\n"
            + "        LIMIT ?\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, categoryId);
    _argIndex = 3;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalMovieEntity>>() {
      @Override
      @NonNull
      public List<LocalMovieEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalMovieEntity> _result = new ArrayList<LocalMovieEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalMovieEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _item = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
  public Object getFeatured(final String sourceId, final int limit,
      final Continuation<? super List<LocalMovieEntity>> $completion) {
    final String _sql = "SELECT * FROM movies WHERE sourceId = ? ORDER BY sortOrder, title LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalMovieEntity>>() {
      @Override
      @NonNull
      public List<LocalMovieEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalMovieEntity> _result = new ArrayList<LocalMovieEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalMovieEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _item = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
  public Object countBySource(final String sourceId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM movies WHERE sourceId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object searchByTitle(final String sourceId, final String query, final int limit,
      final Continuation<? super List<LocalMovieEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM movies\n"
            + "        WHERE sourceId = ?\n"
            + "            AND (title LIKE '%' || ? || '%' OR \"cast\" LIKE '%' || ? || '%')\n"
            + "        ORDER BY title\n"
            + "        LIMIT ?\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 4);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    _argIndex = 3;
    _statement.bindString(_argIndex, query);
    _argIndex = 4;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalMovieEntity>>() {
      @Override
      @NonNull
      public List<LocalMovieEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfBackdropUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfPlot = CursorUtil.getColumnIndexOrThrow(_cursor, "plot");
          final int _cursorIndexOfGenres = CursorUtil.getColumnIndexOrThrow(_cursor, "genres");
          final int _cursorIndexOfCast = CursorUtil.getColumnIndexOrThrow(_cursor, "cast");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LocalMovieEntity> _result = new ArrayList<LocalMovieEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalMovieEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpPosterUrl;
            if (_cursor.isNull(_cursorIndexOfPosterUrl)) {
              _tmpPosterUrl = null;
            } else {
              _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            }
            final String _tmpBackdropUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropUrl)) {
              _tmpBackdropUrl = null;
            } else {
              _tmpBackdropUrl = _cursor.getString(_cursorIndexOfBackdropUrl);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpCategoryName;
            if (_cursor.isNull(_cursorIndexOfCategoryName)) {
              _tmpCategoryName = null;
            } else {
              _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            }
            final String _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getString(_cursorIndexOfRating);
            }
            final String _tmpPlot;
            if (_cursor.isNull(_cursorIndexOfPlot)) {
              _tmpPlot = null;
            } else {
              _tmpPlot = _cursor.getString(_cursorIndexOfPlot);
            }
            final String _tmpGenres;
            if (_cursor.isNull(_cursorIndexOfGenres)) {
              _tmpGenres = null;
            } else {
              _tmpGenres = _cursor.getString(_cursorIndexOfGenres);
            }
            final String _tmpCast;
            if (_cursor.isNull(_cursorIndexOfCast)) {
              _tmpCast = null;
            } else {
              _tmpCast = _cursor.getString(_cursorIndexOfCast);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final Long _tmpAddedAt;
            if (_cursor.isNull(_cursorIndexOfAddedAt)) {
              _tmpAddedAt = null;
            } else {
              _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            }
            _item = new LocalMovieEntity(_tmpId,_tmpSourceId,_tmpTitle,_tmpStreamUrl,_tmpPosterUrl,_tmpBackdropUrl,_tmpCategoryId,_tmpCategoryName,_tmpYear,_tmpDurationMinutes,_tmpRating,_tmpPlot,_tmpGenres,_tmpCast,_tmpSortOrder,_tmpAddedAt);
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
