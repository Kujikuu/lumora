package com.iptvcinema.tv.core.design.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellImmersionPolicyTest {

    @Test
    fun `stays un-immersed at start with no scroll offset`() {
        assertFalse(
            ShellImmersionPolicy.nextHideNavRail(
                currentlyHidden = false,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
            ),
        )
    }

    @Test
    fun `immerses when horizontal scroll exceeds enter threshold`() {
        assertTrue(
            ShellImmersionPolicy.nextHideNavRail(
                currentlyHidden = false,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = ShellImmersionPolicy.IMMERSE_ENTER_OFFSET_PX + 1,
            ),
        )
    }

    @Test
    fun `does not immerse in hysteresis dead zone`() {
        assertFalse(
            ShellImmersionPolicy.nextHideNavRail(
                currentlyHidden = false,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = ShellImmersionPolicy.IMMERSE_RELEASE_OFFSET_PX + 1,
            ),
        )
    }

    @Test
    fun `releases immersion when scrolled back to start`() {
        assertFalse(
            ShellImmersionPolicy.nextHideNavRail(
                currentlyHidden = true,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = ShellImmersionPolicy.IMMERSE_RELEASE_OFFSET_PX - 1,
            ),
        )
    }

    @Test
    fun `stays immersed in hysteresis dead zone`() {
        assertTrue(
            ShellImmersionPolicy.nextHideNavRail(
                currentlyHidden = true,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = ShellImmersionPolicy.IMMERSE_RELEASE_OFFSET_PX + 1,
            ),
        )
    }

    @Test
    fun `immerses when first visible item is not zero`() {
        assertTrue(
            ShellImmersionPolicy.nextHideNavRail(
                currentlyHidden = false,
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
            ),
        )
    }
}
