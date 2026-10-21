package com.iptvcinema.tv.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogLabelsTest {
    @Test
    fun `quality tags are removed from titles`() {
        assertEquals("مطلوب عائليا", CatalogLabels.cleanTitle("مطلوب عائليا - CAM"))
        assertEquals("مطلوب عائليا", CatalogLabels.cleanTitle("CAM - مطلوب عائليا"))
        assertEquals("The Fix", CatalogLabels.cleanTitle("The Fix (HDCAM)"))
        assertEquals("The Fix", CatalogLabels.cleanTitle("The Fix [HD-TS]"))
    }

    @Test
    fun `titles without tags stay as they are`() {
        assertEquals("Camera Obscura", CatalogLabels.cleanTitle("Camera Obscura"))
        assertEquals("Escape Room", CatalogLabels.cleanTitle(" Escape Room "))
        // A title that is nothing but a tag keeps its text rather than going blank.
        assertEquals("CAM", CatalogLabels.cleanTitle("CAM"))
    }

    @Test
    fun `cam copies are detected`() {
        assertTrue(CatalogLabels.isCamCopy("مطلوب عائليا - CAM"))
        assertTrue(CatalogLabels.isCamCopy("Sacrifice (TS)"))
        assertFalse(CatalogLabels.isCamCopy("Camera Obscura"))
        assertFalse(CatalogLabels.isCamCopy("Tyson"))
    }

    @Test
    fun `match and show recordings are detected`() {
        assertTrue(CatalogLabels.looksLikeRecording("WWE.RAW.19.10.2025"))
        assertTrue(CatalogLabels.looksLikeRecording("PARIS ST GERMAIN VS BARCELONA"))
        assertTrue(CatalogLabels.looksLikeRecording("Al Ahly v Zamalek 21/10/2025"))
        assertFalse(CatalogLabels.looksLikeRecording("S.W.A.T."))
        assertFalse(CatalogLabels.looksLikeRecording("The Dog Stars"))
        assertFalse(CatalogLabels.looksLikeRecording("Batman v Superman"))
        assertFalse(CatalogLabels.looksLikeRecording("2001: A Space Odyssey"))
    }

    @Test
    fun `bilingual category names keep the viewer's language`() {
        assertEquals("Mohamed Henedi", CatalogLabels.displayCategory("Mohamed HENEDI | محمد هنيدي", preferArabic = false))
        assertEquals("محمد هنيدي", CatalogLabels.displayCategory("Mohamed HENEDI | محمد هنيدي", preferArabic = true))
        assertEquals("Arabic", CatalogLabels.displayCategory("ARABIC | عربي", preferArabic = false))
    }

    @Test
    fun `single language category names are only recased`() {
        assertEquals("Action Movies", CatalogLabels.displayCategory("ACTION MOVIES", preferArabic = true))
        assertEquals("مسلسلات", CatalogLabels.displayCategory("مسلسلات", preferArabic = false))
        assertEquals("4K UHD TV", CatalogLabels.displayCategory("4K UHD TV", preferArabic = false))
    }

    @Test
    fun `provider tags and dash-joined languages are handled`() {
        assertEquals("Infinity HD", CatalogLabels.displayCategory("[X] Infinity HD", preferArabic = false))
        assertEquals("Series Nonstop", CatalogLabels.displayCategory("[X] Series Nonstop - 24 مسلسلات", preferArabic = false))
        assertEquals("24 مسلسلات", CatalogLabels.displayCategory("[X] Series Nonstop - 24 مسلسلات", preferArabic = true))
        assertEquals("Action - Adventure", CatalogLabels.displayCategory("Action - Adventure", preferArabic = false))
    }

    @Test
    fun `sports and wrestling categories count as recordings`() {
        assertTrue(CatalogLabels.isRecordingCategory("WWE Shows"))
        assertTrue(CatalogLabels.isRecordingCategory("UEFA Champions League"))
        assertTrue(CatalogLabels.isRecordingCategory("مباريات كرة القدم"))
        assertFalse(CatalogLabels.isRecordingCategory("Action Movies"))
        assertFalse(CatalogLabels.isRecordingCategory(null))
    }
}
