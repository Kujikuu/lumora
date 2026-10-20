package com.iptvcinema.tv.core.database

/**
 * The exact schema Room created for database version 9, copied from the generated
 * IptvDatabase_Impl before the v10 change. Migration tests build a v9 file from it.
 */
object Version9Schema {
    val statements = listOf(
        "CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `name` TEXT NOT NULL, `contentType` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`, `sourceId`))",
        "CREATE TABLE IF NOT EXISTS `channels` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `name` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `logoUrl` TEXT, `categoryId` TEXT, `categoryName` TEXT, `tvgId` TEXT, `channelNumber` INTEGER, `isAdult` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `archiveDays` INTEGER NOT NULL, PRIMARY KEY(`id`, `sourceId`))",
        "CREATE TABLE IF NOT EXISTS `movies` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `posterUrl` TEXT, `backdropUrl` TEXT, `categoryId` TEXT, `categoryName` TEXT, `year` INTEGER, `durationMinutes` INTEGER, `rating` TEXT, `plot` TEXT, `genres` TEXT, `cast` TEXT, `youtubeTrailer` TEXT, `sortOrder` INTEGER NOT NULL, `addedAt` INTEGER, PRIMARY KEY(`id`, `sourceId`))",
        "CREATE TABLE IF NOT EXISTS `series` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `posterUrl` TEXT, `backdropUrl` TEXT, `categoryId` TEXT, `categoryName` TEXT, `plot` TEXT, `rating` TEXT, `year` INTEGER, `cast` TEXT, `youtubeTrailer` TEXT, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`, `sourceId`))",
        "CREATE TABLE IF NOT EXISTS `episodes` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `seriesId` TEXT NOT NULL, `seasonNumber` INTEGER NOT NULL, `episodeNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `durationMinutes` INTEGER, `plot` TEXT, `thumbnailUrl` TEXT, PRIMARY KEY(`id`, `sourceId`))",
        "CREATE TABLE IF NOT EXISTS `programs` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `channelId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `startEpochMs` INTEGER NOT NULL, `endEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`, `sourceId`))",
        "CREATE INDEX IF NOT EXISTS `idx_programs_window` ON `programs` (`sourceId`, `channelId`, `startEpochMs`)",
        "CREATE TABLE IF NOT EXISTS `source_sync_state` (`sourceId` TEXT NOT NULL, `lastSyncedAtEpochMs` INTEGER, `liveChannelCount` INTEGER NOT NULL, `movieCount` INTEGER NOT NULL, `seriesCount` INTEGER NOT NULL, `epgAvailable` INTEGER NOT NULL, `lastError` TEXT, PRIMARY KEY(`sourceId`))",
        "CREATE TABLE IF NOT EXISTS `catalog_sync_metadata` (`sourceId` TEXT NOT NULL, `resourceKey` TEXT NOT NULL, `etag` TEXT, `lastModified` TEXT, `contentFingerprint` TEXT, `lastCheckedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`sourceId`, `resourceKey`))",
        "CREATE TABLE IF NOT EXISTS `cached_favorites` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `sourceId` TEXT, `contentId` TEXT NOT NULL, `contentType` TEXT NOT NULL, `title` TEXT NOT NULL, `posterUrl` TEXT, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_cached_favorites_profileId` ON `cached_favorites` (`profileId`)",
        "CREATE TABLE IF NOT EXISTS `cached_watch_history` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `sourceId` TEXT, `contentId` TEXT NOT NULL, `contentType` TEXT NOT NULL, `seriesId` TEXT, `title` TEXT NOT NULL, `posterUrl` TEXT, `positionMs` INTEGER NOT NULL, `durationMs` INTEGER, `lastWatchedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_cached_watch_history_profileId` ON `cached_watch_history` (`profileId`)",
        "CREATE INDEX IF NOT EXISTS `index_cached_watch_history_profileId_lastWatchedAtEpochMs` ON `cached_watch_history` (`profileId`, `lastWatchedAtEpochMs`)",
        "CREATE TABLE IF NOT EXISTS `cached_user_settings` (`userId` TEXT NOT NULL, `defaultAudioLanguage` TEXT NOT NULL, `defaultSubtitleLanguage` TEXT, `subtitlesEnabled` INTEGER NOT NULL, `autoplayNextEpisode` INTEGER NOT NULL, `continueWatchingEnabled` INTEGER NOT NULL, `skipIntroEnabled` INTEGER NOT NULL, `streamingQuality` TEXT NOT NULL, `theme` TEXT NOT NULL, PRIMARY KEY(`userId`))",
        "CREATE TABLE IF NOT EXISTS `cached_parental_controls` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `profileId` TEXT NOT NULL, `pinHash` TEXT, `hideAdultCategories` INTEGER NOT NULL, `lockPlaylistSettings` INTEGER NOT NULL, `lockLiveCategories` INTEGER NOT NULL, `maxRating` TEXT, `blockedCategoriesJson` TEXT NOT NULL, PRIMARY KEY(`profileId`))",
        "CREATE TABLE IF NOT EXISTS `cached_playlist_sources` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `serverUrl` TEXT, `playlistUrl` TEXT, `epgUrl` TEXT, `isActive` INTEGER NOT NULL, `status` TEXT NOT NULL, `lastSyncedAtEpochMs` INTEGER, `cachedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE INDEX IF NOT EXISTS `index_cached_playlist_sources_userId` ON `cached_playlist_sources` (`userId`)",
    )
}
