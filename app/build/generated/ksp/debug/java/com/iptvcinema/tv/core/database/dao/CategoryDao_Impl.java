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
import androidx.sqlite.db.SupportSQLiteStatement;
import com.iptvcinema.tv.core.database.entity.LocalCategoryEntity;
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
public final class CategoryDao_Impl implements CategoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalCategoryEntity> __insertionAdapterOfLocalCategoryEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByType;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySource;

  public CategoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalCategoryEntity = new EntityInsertionAdapter<LocalCategoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `categories` (`id`,`sourceId`,`name`,`contentType`,`sortOrder`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalCategoryEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSourceId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getContentType());
        statement.bindLong(5, entity.getSortOrder());
      }
    };
    this.__preparedStmtOfDeleteByType = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM categories WHERE sourceId = ? AND contentType = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteBySource = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM categories WHERE sourceId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsertAll(final List<LocalCategoryEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalCategoryEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByType(final String sourceId, final String contentType,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByType.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, sourceId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, contentType);
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
          __preparedStmtOfDeleteByType.release(_stmt);
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
  public Flow<List<LocalCategoryEntity>> observeByType(final String sourceId,
      final String contentType) {
    final String _sql = "SELECT * FROM categories WHERE sourceId = ? AND contentType = ? ORDER BY sortOrder, name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, contentType);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"categories"}, new Callable<List<LocalCategoryEntity>>() {
      @Override
      @NonNull
      public List<LocalCategoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<LocalCategoryEntity> _result = new ArrayList<LocalCategoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalCategoryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new LocalCategoryEntity(_tmpId,_tmpSourceId,_tmpName,_tmpContentType,_tmpSortOrder);
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
  public Object getByType(final String sourceId, final String contentType,
      final Continuation<? super List<LocalCategoryEntity>> $completion) {
    final String _sql = "SELECT * FROM categories WHERE sourceId = ? AND contentType = ? ORDER BY sortOrder, name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceId);
    _argIndex = 2;
    _statement.bindString(_argIndex, contentType);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalCategoryEntity>>() {
      @Override
      @NonNull
      public List<LocalCategoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<LocalCategoryEntity> _result = new ArrayList<LocalCategoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalCategoryEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSourceId;
            _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new LocalCategoryEntity(_tmpId,_tmpSourceId,_tmpName,_tmpContentType,_tmpSortOrder);
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
