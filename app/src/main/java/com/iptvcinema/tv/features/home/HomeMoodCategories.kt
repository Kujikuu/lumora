package com.iptvcinema.tv.features.home

import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.home.MoodBrowseTarget
import com.iptvcinema.tv.core.model.home.MoodCategory

object HomeMoodCategories {
    val defaults: List<MoodCategory> = listOf(
        MoodCategory(
            id = "action",
            labelRes = R.string.mood_action,
            filter = "Action",
            target = MoodBrowseTarget.Movies,
            gradientStartArgb = 0xFFE50914,
            gradientEndArgb = 0xFF831010,
        ),
        MoodCategory(
            id = "drama",
            labelRes = R.string.mood_drama,
            filter = "Drama",
            target = MoodBrowseTarget.Series,
            gradientStartArgb = 0xFF6B4EFF,
            gradientEndArgb = 0xFF3D2A99,
        ),
        MoodCategory(
            id = "comedy",
            labelRes = R.string.mood_comedy,
            filter = "Comedy",
            target = MoodBrowseTarget.Movies,
            gradientStartArgb = 0xFFFFB800,
            gradientEndArgb = 0xFFCC7A00,
        ),
        MoodCategory(
            id = "kids",
            labelRes = R.string.mood_kids,
            filter = "Kids",
            target = MoodBrowseTarget.Movies,
            gradientStartArgb = 0xFF46D369,
            gradientEndArgb = 0xFF1E8449,
        ),
        MoodCategory(
            id = "arabic",
            labelRes = R.string.mood_arabic,
            filter = "Arabic",
            target = MoodBrowseTarget.Series,
            gradientStartArgb = 0xFF3DA9FC,
            gradientEndArgb = 0xFF1565C0,
        ),
        MoodCategory(
            id = "new",
            labelRes = R.string.mood_new,
            filter = "",
            target = MoodBrowseTarget.Movies,
            gradientStartArgb = 0xFFFF6B6B,
            gradientEndArgb = 0xFFC0392B,
        ),
    )
}
