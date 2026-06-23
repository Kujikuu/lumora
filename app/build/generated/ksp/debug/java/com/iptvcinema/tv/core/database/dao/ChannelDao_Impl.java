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
import com.iptvcinema.tv.core.database.entity.LocalChannelEntity;
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
public final class ChannelDao_Impl implements ChannelDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalChannelEntity> __insertionAdapterOfLocalChannelEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySource;

  public ChannelDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalChannelEntity = new EntityInsertionAdapter<LocalChannelEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `channels` (`id`,`sourceId`,`name`,`streamUrl`,`logoUrl`,`categoryId`,`categoryName`,`tvgId`,`channelNumber`,`isAdult`,`sortOrder`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalChannelEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSourceId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getStreamUrl());
        if (entity.getLogoUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getLogoUrl());
        }
        if (entity.getCategoryId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getCategoryId());
        }
        if (entity.getCategoryName() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getCategoryName());
        }
        if (entity.getTvgId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getTvgId());
        }
        if (entity.getChannelNumber() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getChannelNumber());
        }
        final int _tmp = entity.isAdult() ? 1 : 0;
        statement.bindLong(10, _tmp);
        statement.bindLong(11, entity.getSortOrder());
      }
    };
    this.__preparedStmtOfDeleteBySource = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM channels WHERE sourceId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsertAll(final List<LocalChannelEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalChannelEntity.insert(items);
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
  public Flow<List<LocalChannelEntity>> observeAll(final String sourceId) {
    final String _sql = "SELECT * FROM channels WHERE sourceId = ? ORDER BY sortOrder, channelNumber, name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<LocalChannelEntity>>() {
      @Override
      @NonNull
      public List<LocalChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfChannelNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "channelNumber");
          final int _cursorIndexOfIsAdult = CursorUtil.getColumnIndexOrThrow(_cursor, "isAdult");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<LocalChannelEntity> _result = new ArrayList<LocalChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalChannelEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
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
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final Integer _tmpChannelNumber;
            if (_cursor.isNull(_cursorIndexOfChannelNumber)) {
              _tmpChannelNumber = null;
            } else {
              _tmpChannelNumber = _cursor.getInt(_cursorIndexOfChannelNumber);
            }
            final boolean _tmpIsAdult;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAdult);
            _tmpIsAdult = _tmp != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new LocalChannelEntity(_tmpId,_tmpSourceId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpCategoryId,_tmpCategoryName,_tmpTvgId,_tmpChannelNumber,_tmpIsAdult,_tmpSortOrder);
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
  public Flow<List<LocalChannelEntity>> observeByCategory(final String sourceId,
      final String categoryId) {
    final String _sql = "\n"
            + "        SELECT * FROM channels\n"
            + "        WHERE sourceId = ? AND categoryId = ?\n"
            + "        ORDER BY sortOrder, channelNumber, name\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, categoryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<LocalChannelEntity>>() {
      @Override
      @NonNull
      public List<LocalChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfChannelNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "channelNumber");
          final int _cursorIndexOfIsAdult = CursorUtil.getColumnIndexOrThrow(_cursor, "isAdult");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<LocalChannelEntity> _result = new ArrayList<LocalChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalChannelEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
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
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final Integer _tmpChannelNumber;
            if (_cursor.isNull(_cursorIndexOfChannelNumber)) {
              _tmpChannelNumber = null;
            } else {
              _tmpChannelNumber = _cursor.getInt(_cursorIndexOfChannelNumber);
            }
            final boolean _tmpIsAdult;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAdult);
            _tmpIsAdult = _tmp != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new LocalChannelEntity(_tmpId,_tmpSourceId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpCategoryId,_tmpCategoryName,_tmpTvgId,_tmpChannelNumber,_tmpIsAdult,_tmpSortOrder);
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
  public Flow<List<LocalChannelEntity>> observeByCategoryName(final String sourceId,
      final String categoryName) {
    final String _sql = "\n"
            + "        SELECT * FROM channels\n"
            + "        WHERE sourceId = ? AND categoryName = ?\n"
            + "        ORDER BY sortOrder, channelNumber, name\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, categoryName);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<LocalChannelEntity>>() {
      @Override
      @NonNull
      public List<LocalChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfChannelNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "channelNumber");
          final int _cursorIndexOfIsAdult = CursorUtil.getColumnIndexOrThrow(_cursor, "isAdult");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<LocalChannelEntity> _result = new ArrayList<LocalChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalChannelEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
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
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final Integer _tmpChannelNumber;
            if (_cursor.isNull(_cursorIndexOfChannelNumber)) {
              _tmpChannelNumber = null;
            } else {
              _tmpChannelNumber = _cursor.getInt(_cursorIndexOfChannelNumber);
            }
            final boolean _tmpIsAdult;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAdult);
            _tmpIsAdult = _tmp != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new LocalChannelEntity(_tmpId,_tmpSourceId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpCategoryId,_tmpCategoryName,_tmpTvgId,_tmpChannelNumber,_tmpIsAdult,_tmpSortOrder);
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
  public Flow<List<LocalChannelEntity>> observeFeatured(final String sourceId, final int limit) {
    final String _sql = "SELECT * FROM channels WHERE sourceId = ? ORDER BY sortOrder LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<LocalChannelEntity>>() {
      @Override
      @NonNull
      public List<LocalChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfChannelNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "channelNumber");
          final int _cursorIndexOfIsAdult = CursorUtil.getColumnIndexOrThrow(_cursor, "isAdult");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<LocalChannelEntity> _result = new ArrayList<LocalChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalChannelEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
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
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final Integer _tmpChannelNumber;
            if (_cursor.isNull(_cursorIndexOfChannelNumber)) {
              _tmpChannelNumber = null;
            } else {
              _tmpChannelNumber = _cursor.getInt(_cursorIndexOfChannelNumber);
            }
            final boolean _tmpIsAdult;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAdult);
            _tmpIsAdult = _tmp != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new LocalChannelEntity(_tmpId,_tmpSourceId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpCategoryId,_tmpCategoryName,_tmpTvgId,_tmpChannelNumber,_tmpIsAdult,_tmpSortOrder);
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
  public Object getById(final String sourceId, final String channelId,
      final Continuation<? super LocalChannelEntity> $completion) {
    final String _sql = "SELECT * FROM channels WHERE sourceId = ? AND id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, channelId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalChannelEntity>() {
      @Override
      @Nullable
      public LocalChannelEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfChannelNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "channelNumber");
          final int _cursorIndexOfIsAdult = CursorUtil.getColumnIndexOrThrow(_cursor, "isAdult");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final LocalChannelEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
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
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final Integer _tmpChannelNumber;
            if (_cursor.isNull(_cursorIndexOfChannelNumber)) {
              _tmpChannelNumber = null;
            } else {
              _tmpChannelNumber = _cursor.getInt(_cursorIndexOfChannelNumber);
            }
            final boolean _tmpIsAdult;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAdult);
            _tmpIsAdult = _tmp != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _result = new LocalChannelEntity(_tmpId,_tmpSourceId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpCategoryId,_tmpCategoryName,_tmpTvgId,_tmpChannelNumber,_tmpIsAdult,_tmpSortOrder);
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
  public Object getAllOrdered(final String sourceId,
      final Continuation<? super List<LocalChannelEntity>> $completion) {
    final String _sql = "SELECT * FROM channels WHERE sourceId = ? ORDER BY sortOrder, channelNumber, name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalChannelEntity>>() {
      @Override
      @NonNull
      public List<LocalChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfChannelNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "channelNumber");
          final int _cursorIndexOfIsAdult = CursorUtil.getColumnIndexOrThrow(_cursor, "isAdult");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<LocalChannelEntity> _result = new ArrayList<LocalChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalChannelEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
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
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final Integer _tmpChannelNumber;
            if (_cursor.isNull(_cursorIndexOfChannelNumber)) {
              _tmpChannelNumber = null;
            } else {
              _tmpChannelNumber = _cursor.getInt(_cursorIndexOfChannelNumber);
            }
            final boolean _tmpIsAdult;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAdult);
            _tmpIsAdult = _tmp != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new LocalChannelEntity(_tmpId,_tmpSourceId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpCategoryId,_tmpCategoryName,_tmpTvgId,_tmpChannelNumber,_tmpIsAdult,_tmpSortOrder);
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
    final String _sql = "SELECT COUNT(*) FROM channels WHERE sourceId = ?";
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
  public Object searchByName(final String sourceId, final String query, final int limit,
      final Continuation<? super List<LocalChannelEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM channels\n"
            + "        WHERE sourceId = ? AND name LIKE '%' || ? || '%'\n"
            + "        ORDER BY sortOrder, channelNumber, name\n"
            + "        LIMIT ?\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    _argIndex = 3;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalChannelEntity>>() {
      @Override
      @NonNull
      public List<LocalChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfChannelNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "channelNumber");
          final int _cursorIndexOfIsAdult = CursorUtil.getColumnIndexOrThrow(_cursor, "isAdult");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<LocalChannelEntity> _result = new ArrayList<LocalChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalChannelEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
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
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final Integer _tmpChannelNumber;
            if (_cursor.isNull(_cursorIndexOfChannelNumber)) {
              _tmpChannelNumber = null;
            } else {
              _tmpChannelNumber = _cursor.getInt(_cursorIndexOfChannelNumber);
            }
            final boolean _tmpIsAdult;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAdult);
            _tmpIsAdult = _tmp != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new LocalChannelEntity(_tmpId,_tmpSourceId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpCategoryId,_tmpCategoryName,_tmpTvgId,_tmpChannelNumber,_tmpIsAdult,_tmpSortOrder);
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
