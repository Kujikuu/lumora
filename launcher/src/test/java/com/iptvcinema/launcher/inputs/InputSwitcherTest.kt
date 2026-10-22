package com.iptvcinema.launcher.inputs

import org.junit.Assert.assertEquals
import org.junit.Test

class InputSwitcherTest {
    private val hdmi = InputDescriptor("hdmi1", InputType.Hdmi, "HDMI 1", InputState.Connected, isPassthrough = true)
    private val tuner = InputDescriptor("dtv", InputType.Tuner, "TV", InputState.Unknown, isPassthrough = false)

    @Test
    fun passthrough_prefersTheSystemViewerWhenThereIsOne() {
        assertEquals(InputSwitchPlan.SystemViewer, InputSwitcher.plan(hdmi, hasSystemViewer = true))
    }

    @Test
    fun passthrough_fallsBackToOurOwnViewer() {
        assertEquals(InputSwitchPlan.OwnViewer, InputSwitcher.plan(hdmi, hasSystemViewer = false))
    }

    @Test
    fun tuner_alwaysGoesToTheSystemPicker() {
        assertEquals(InputSwitchPlan.SystemPicker, InputSwitcher.plan(tuner, hasSystemViewer = true))
    }
}
