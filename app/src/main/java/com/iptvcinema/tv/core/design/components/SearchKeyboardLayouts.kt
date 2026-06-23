package com.iptvcinema.tv.core.design.components

enum class SearchKeyboardLayout {
    English,
    Arabic,
}

object SearchKeyboardLayouts {
    val english: List<List<String>> = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M"),
    )

    val arabic: List<List<String>> = listOf(
        listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"),
        listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"),
        listOf("ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "د", "ذ"),
    )

    fun rowsFor(layout: SearchKeyboardLayout): List<List<String>> = when (layout) {
        SearchKeyboardLayout.English -> english
        SearchKeyboardLayout.Arabic -> arabic
    }
}
