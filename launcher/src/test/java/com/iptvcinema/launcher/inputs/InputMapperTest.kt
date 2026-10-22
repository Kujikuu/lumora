package com.iptvcinema.launcher.inputs

import org.junit.Assert.assertEquals
import org.junit.Test

class InputMapperTest {
    private fun input(id: String, type: InputType, label: String, passthrough: Boolean = true) =
        InputDescriptor(id, type, label, InputState.Unknown, passthrough)

    @Test
    fun type_mapsEveryFrameworkConstant() {
        assertEquals(InputType.Hdmi, InputMapper.typeOf(InputMapper.TYPE_HDMI))
        assertEquals(InputType.Hdmi, InputMapper.typeOf(InputMapper.TYPE_DVI))
        assertEquals(InputType.Vga, InputMapper.typeOf(InputMapper.TYPE_VGA))
        assertEquals(InputType.Component, InputMapper.typeOf(InputMapper.TYPE_COMPONENT))
        assertEquals(InputType.Av, InputMapper.typeOf(InputMapper.TYPE_COMPOSITE))
        assertEquals(InputType.Av, InputMapper.typeOf(InputMapper.TYPE_SCART))
        assertEquals(InputType.Tuner, InputMapper.typeOf(InputMapper.TYPE_TUNER))
        assertEquals(InputType.Other, InputMapper.typeOf(InputMapper.TYPE_OTHER))
    }

    @Test
    fun state_mapsConnectedStandbyDisconnectedAndUnknown() {
        assertEquals(InputState.Connected, InputMapper.stateOf(InputMapper.STATE_CONNECTED))
        assertEquals(InputState.Standby, InputMapper.stateOf(InputMapper.STATE_CONNECTED_STANDBY))
        assertEquals(InputState.Disconnected, InputMapper.stateOf(InputMapper.STATE_DISCONNECTED))
        assertEquals(InputState.Unknown, InputMapper.stateOf(null))
    }

    @Test
    fun order_hdmiFirstWithNaturalNumbersThenOtherTypesAndTuners() {
        val ordered = InputMapper.order(
            listOf(
                input("t", InputType.Tuner, "TV", passthrough = false),
                input("h10", InputType.Hdmi, "HDMI 10"),
                input("av", InputType.Av, "AV"),
                input("h2", InputType.Hdmi, "HDMI 2"),
                input("vga", InputType.Vga, "VGA"),
                input("h2", InputType.Hdmi, "HDMI 2"),
                input("soft", InputType.Other, "Some app input", passthrough = false),
            ),
        )
        assertEquals(listOf("h2", "h10", "vga", "av", "t"), ordered.map { it.id })
    }

    @Test
    fun fallbackLabel_numbersHdmiAndNamesTheRest() {
        val names = mapOf(InputType.Hdmi to "HDMI", InputType.Av to "AV")
        assertEquals("HDMI 1", InputMapper.fallbackLabel(InputType.Hdmi, 0, names))
        assertEquals("AV", InputMapper.fallbackLabel(InputType.Av, 3, names))
        assertEquals("Vga", InputMapper.fallbackLabel(InputType.Vga, 0, names))
    }
}
