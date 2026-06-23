package com.iptvcinema.tv.core.database;

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
import com.iptvcinema.tv.core.database.dao.CategoryDao;
import com.iptvcinema.tv.core.database.dao.CategoryDao_Impl;
import com.iptvcinema.tv.core.database.dao.ChannelDao;
import com.iptvcinema.tv.core.database.dao.ChannelDao_Impl;
import com.iptvcinema.tv.core.database.dao.EpisodeDao;
import com.iptvcinema.tv.core.database.dao.EpisodeDao_Impl;
import com.iptvcinema.tv.core.database.dao.MovieDao;
import com.iptvcinema.tv.core.database.dao.MovieDao_Impl;
import com.iptvcinema.tv.core.database.dao.ProgramDao;
import com.iptvcinema.tv.core.database.dao.ProgramDao_Impl;
import com.iptvcinema.tv.core.database.dao.SeriesDao;
import com.iptvcinema.tv.core.database.dao.SeriesDao_Impl;
import com.iptvcinema.tv.core.database.dao.SyncStateDao;
import com.iptvcinema.tv.core.database.dao.SyncStateDao_Impl;
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
public final class IptvDatabase_Impl extends IptvDatabase {
  private volatile CategoryDao _categoryDao;

  private volatile ChannelDao _channelDao;

  private volatile MovieDao _movieDao;

  private volatile SeriesDao _seriesDao;

  private volatile EpisodeDao _episodeDao;

  private volatile ProgramDao _programDao;

  private volatile SyncStateDao _syncStateDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(5) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `name` TEXT NOT NULL, `contentType` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`, `sourceId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `channels` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `name` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `logoUrl` TEXT, `categoryId` TEXT, `categoryName` TEXT, `tvgId` TEXT, `channelNumber` INTEGER, `isAdult` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`, `sourceId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `movies` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `posterUrl` TEXT, `backdropUrl` TEXT, `categoryId` TEXT, `categoryName` TEXT, `year` INTEGER, `durationMinutes` INTEGER, `rating` TEXT, `plot` TEXT, `genres` TEXT, `cast` TEXT, `sortOrder` INTEGER NOT NULL, `addedAt` INTEGER, PRIMARY KEY(`id`, `sourceId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `series` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `posterUrl` TEXT, `backdropUrl` TEXT, `categoryId` TEXT, `categoryName` TEXT, `plot` TEXT, `rating` TEXT, `year` INTEGER, `cast` TEXT, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`, `sourceId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `episodes` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `seriesId` TEXT NOT NULL, `seasonNumber` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `durationMinutes` INTEGER, `plot` TEXT, `thumbnailUrl` TEXT, PRIMARY KEY(`id`, `sourceId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `programs` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `channelId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `startEpochMs` INTEGER NOT NULL, `endEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`, `sourceId`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_programs_window` ON `programs` (`sourceId`, `channelId`, `startEpochMs`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `source_sync_state` (`sourceId` TEXT NOT NULL, `lastSyncedAtEpochMs` INTEGER, `liveChannelCount` INTEGER NOT NULL, `movieCount` INTEGER NOT NULL, `seriesCount` INTEGER NOT NULL, `epgAvailable` INTEGER NOT NULL, `lastError` TEXT, PRIMARY KEY(`sourceId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7b6ca2a4a9a8bbe5d0fe7f532db3d627')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        db.execSQL("DROP TABLE IF EXISTS `channels`");
        db.execSQL("DROP TABLE IF EXISTS `movies`");
        db.execSQL("DROP TABLE IF EXISTS `series`");
        db.execSQL("DROP TABLE IF EXISTS `episodes`");
        db.execSQL("DROP TABLE IF EXISTS `programs`");
        db.execSQL("DROP TABLE IF EXISTS `source_sync_state`");
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
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(5);
        _columnsCategories.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("sourceId", new TableInfo.Column("sourceId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("contentType", new TableInfo.Column("contentType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(com.iptvcinema.tv.core.database.entity.LocalCategoryEntity).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsChannels = new HashMap<String, TableInfo.Column>(11);
        _columnsChannels.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("sourceId", new TableInfo.Column("sourceId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("streamUrl", new TableInfo.Column("streamUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("logoUrl", new TableInfo.Column("logoUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("categoryId", new TableInfo.Column("categoryId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("categoryName", new TableInfo.Column("categoryName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("tvgId", new TableInfo.Column("tvgId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("channelNumber", new TableInfo.Column("channelNumber", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("isAdult", new TableInfo.Column("isAdult", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChannels = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChannels = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChannels = new TableInfo("channels", _columnsChannels, _foreignKeysChannels, _indicesChannels);
        final TableInfo _existingChannels = TableInfo.read(db, "channels");
        if (!_infoChannels.equals(_existingChannels)) {
          return new RoomOpenHelper.ValidationResult(false, "channels(com.iptvcinema.tv.core.database.entity.LocalChannelEntity).\n"
                  + " Expected:\n" + _infoChannels + "\n"
                  + " Found:\n" + _existingChannels);
        }
        final HashMap<String, TableInfo.Column> _columnsMovies = new HashMap<String, TableInfo.Column>(16);
        _columnsMovies.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("sourceId", new TableInfo.Column("sourceId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("streamUrl", new TableInfo.Column("streamUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("posterUrl", new TableInfo.Column("posterUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("backdropUrl", new TableInfo.Column("backdropUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("categoryId", new TableInfo.Column("categoryId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("categoryName", new TableInfo.Column("categoryName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("year", new TableInfo.Column("year", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("durationMinutes", new TableInfo.Column("durationMinutes", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("rating", new TableInfo.Column("rating", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("plot", new TableInfo.Column("plot", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("genres", new TableInfo.Column("genres", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("cast", new TableInfo.Column("cast", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMovies.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMovies = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMovies = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMovies = new TableInfo("movies", _columnsMovies, _foreignKeysMovies, _indicesMovies);
        final TableInfo _existingMovies = TableInfo.read(db, "movies");
        if (!_infoMovies.equals(_existingMovies)) {
          return new RoomOpenHelper.ValidationResult(false, "movies(com.iptvcinema.tv.core.database.entity.LocalMovieEntity).\n"
                  + " Expected:\n" + _infoMovies + "\n"
                  + " Found:\n" + _existingMovies);
        }
        final HashMap<String, TableInfo.Column> _columnsSeries = new HashMap<String, TableInfo.Column>(12);
        _columnsSeries.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("sourceId", new TableInfo.Column("sourceId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("posterUrl", new TableInfo.Column("posterUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("backdropUrl", new TableInfo.Column("backdropUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("categoryId", new TableInfo.Column("categoryId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("categoryName", new TableInfo.Column("categoryName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("plot", new TableInfo.Column("plot", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("rating", new TableInfo.Column("rating", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("year", new TableInfo.Column("year", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("cast", new TableInfo.Column("cast", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSeries.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSeries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSeries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSeries = new TableInfo("series", _columnsSeries, _foreignKeysSeries, _indicesSeries);
        final TableInfo _existingSeries = TableInfo.read(db, "series");
        if (!_infoSeries.equals(_existingSeries)) {
          return new RoomOpenHelper.ValidationResult(false, "series(com.iptvcinema.tv.core.database.entity.LocalSeriesEntity).\n"
                  + " Expected:\n" + _infoSeries + "\n"
                  + " Found:\n" + _existingSeries);
        }
        final HashMap<String, TableInfo.Column> _columnsEpisodes = new HashMap<String, TableInfo.Column>(10);
        _columnsEpisodes.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodes.put("sourceId", new TableInfo.Column("sourceId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodes.put("seriesId", new TableInfo.Column("seriesId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodes.put("seasonNumber", new TableInfo.Column("seasonNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodes.put("episodeNumber", new TableInfo.Column("episodeNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodes.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodes.put("streamUrl", new TableInfo.Column("streamUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodes.put("durationMinutes", new TableInfo.Column("durationMinutes", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodes.put("plot", new TableInfo.Column("plot", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpisodes.put("thumbnailUrl", new TableInfo.Column("thumbnailUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEpisodes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEpisodes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEpisodes = new TableInfo("episodes", _columnsEpisodes, _foreignKeysEpisodes, _indicesEpisodes);
        final TableInfo _existingEpisodes = TableInfo.read(db, "episodes");
        if (!_infoEpisodes.equals(_existingEpisodes)) {
          return new RoomOpenHelper.ValidationResult(false, "episodes(com.iptvcinema.tv.core.database.entity.LocalEpisodeEntity).\n"
                  + " Expected:\n" + _infoEpisodes + "\n"
                  + " Found:\n" + _existingEpisodes);
        }
        final HashMap<String, TableInfo.Column> _columnsPrograms = new HashMap<String, TableInfo.Column>(7);
        _columnsPrograms.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPrograms.put("sourceId", new TableInfo.Column("sourceId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPrograms.put("channelId", new TableInfo.Column("channelId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPrograms.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPrograms.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPrograms.put("startEpochMs", new TableInfo.Column("startEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPrograms.put("endEpochMs", new TableInfo.Column("endEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPrograms = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPrograms = new HashSet<TableInfo.Index>(1);
        _indicesPrograms.add(new TableInfo.Index("idx_programs_window", false, Arrays.asList("sourceId", "channelId", "startEpochMs"), Arrays.asList("ASC", "ASC", "ASC")));
        final TableInfo _infoPrograms = new TableInfo("programs", _columnsPrograms, _foreignKeysPrograms, _indicesPrograms);
        final TableInfo _existingPrograms = TableInfo.read(db, "programs");
        if (!_infoPrograms.equals(_existingPrograms)) {
          return new RoomOpenHelper.ValidationResult(false, "programs(com.iptvcinema.tv.core.database.entity.LocalProgramEntity).\n"
                  + " Expected:\n" + _infoPrograms + "\n"
                  + " Found:\n" + _existingPrograms);
        }
        final HashMap<String, TableInfo.Column> _columnsSourceSyncState = new HashMap<String, TableInfo.Column>(7);
        _columnsSourceSyncState.put("sourceId", new TableInfo.Column("sourceId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSourceSyncState.put("lastSyncedAtEpochMs", new TableInfo.Column("lastSyncedAtEpochMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSourceSyncState.put("liveChannelCount", new TableInfo.Column("liveChannelCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSourceSyncState.put("movieCount", new TableInfo.Column("movieCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSourceSyncState.put("seriesCount", new TableInfo.Column("seriesCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSourceSyncState.put("epgAvailable", new TableInfo.Column("epgAvailable", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSourceSyncState.put("lastError", new TableInfo.Column("lastError", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSourceSyncState = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSourceSyncState = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSourceSyncState = new TableInfo("source_sync_state", _columnsSourceSyncState, _foreignKeysSourceSyncState, _indicesSourceSyncState);
        final TableInfo _existingSourceSyncState = TableInfo.read(db, "source_sync_state");
        if (!_infoSourceSyncState.equals(_existingSourceSyncState)) {
          return new RoomOpenHelper.ValidationResult(false, "source_sync_state(com.iptvcinema.tv.core.database.entity.LocalSourceSyncStateEntity).\n"
                  + " Expected:\n" + _infoSourceSyncState + "\n"
                  + " Found:\n" + _existingSourceSyncState);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "7b6ca2a4a9a8bbe5d0fe7f532db3d627", "20b1a10523f65c8b446857bb5d61b26c");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "categories","channels","movies","series","episodes","programs","source_sync_state");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `categories`");
      _db.execSQL("DELETE FROM `channels`");
      _db.execSQL("DELETE FROM `movies`");
      _db.execSQL("DELETE FROM `series`");
      _db.execSQL("DELETE FROM `episodes`");
      _db.execSQL("DELETE FROM `programs`");
      _db.execSQL("DELETE FROM `source_sync_state`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ChannelDao.class, ChannelDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MovieDao.class, MovieDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SeriesDao.class, SeriesDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(EpisodeDao.class, EpisodeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProgramDao.class, ProgramDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SyncStateDao.class, SyncStateDao_Impl.getRequiredConverters());
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
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }

  @Override
  public ChannelDao channelDao() {
    if (_channelDao != null) {
      return _channelDao;
    } else {
      synchronized(this) {
        if(_channelDao == null) {
          _channelDao = new ChannelDao_Impl(this);
        }
        return _channelDao;
      }
    }
  }

  @Override
  public MovieDao movieDao() {
    if (_movieDao != null) {
      return _movieDao;
    } else {
      synchronized(this) {
        if(_movieDao == null) {
          _movieDao = new MovieDao_Impl(this);
        }
        return _movieDao;
      }
    }
  }

  @Override
  public SeriesDao seriesDao() {
    if (_seriesDao != null) {
      return _seriesDao;
    } else {
      synchronized(this) {
        if(_seriesDao == null) {
          _seriesDao = new SeriesDao_Impl(this);
        }
        return _seriesDao;
      }
    }
  }

  @Override
  public EpisodeDao episodeDao() {
    if (_episodeDao != null) {
      return _episodeDao;
    } else {
      synchronized(this) {
        if(_episodeDao == null) {
          _episodeDao = new EpisodeDao_Impl(this);
        }
        return _episodeDao;
      }
    }
  }

  @Override
  public ProgramDao programDao() {
    if (_programDao != null) {
      return _programDao;
    } else {
      synchronized(this) {
        if(_programDao == null) {
          _programDao = new ProgramDao_Impl(this);
        }
        return _programDao;
      }
    }
  }

  @Override
  public SyncStateDao syncStateDao() {
    if (_syncStateDao != null) {
      return _syncStateDao;
    } else {
      synchronized(this) {
        if(_syncStateDao == null) {
          _syncStateDao = new SyncStateDao_Impl(this);
        }
        return _syncStateDao;
      }
    }
  }
}
