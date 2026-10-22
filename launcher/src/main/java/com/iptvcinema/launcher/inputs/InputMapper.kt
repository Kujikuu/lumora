package com.iptvcinema.launcher.inputs

/** Pure mapping and ordering of TV inputs; the Android types are unwrapped by [InputsRepository]. */
object InputMapper {
    // android.media.tv.TvInputInfo type constants, repeated here so the mapping is testable.
    const val TYPE_TUNER = 0
    const val TYPE_OTHER = 1000
    const val TYPE_COMPOSITE = 1001
    const val TYPE_SVIDEO = 1002
    const val TYPE_SCART = 1003
    const val TYPE_COMPONENT = 1004
    const val TYPE_VGA = 1005
    const val TYPE_DVI = 1006
    const val TYPE_HDMI = 1007
    const val TYPE_DISPLAY_PORT = 1008

    // android.media.tv.TvInputManager input state constants.
    const val STATE_CONNECTED = 0
    const val STATE_CONNECTED_STANDBY = 1
    const val STATE_DISCONNECTED = 2

    fun typeOf(rawType: Int): InputType = when (rawType) {
        TYPE_HDMI, TYPE_DVI, TYPE_DISPLAY_PORT -> InputType.Hdmi
        TYPE_VGA -> InputType.Vga
        TYPE_COMPONENT -> InputType.Component
        TYPE_COMPOSITE, TYPE_SVIDEO, TYPE_SCART -> InputType.Av
        TYPE_TUNER -> InputType.Tuner
        else -> InputType.Other
    }

    fun stateOf(rawState: Int?): InputState = when (rawState) {
        STATE_CONNECTED -> InputState.Connected
        STATE_CONNECTED_STANDBY -> InputState.Standby
        STATE_DISCONNECTED -> InputState.Disconnected
        else -> InputState.Unknown
    }

    /** Hardware inputs only, HDMI first, then by label with a natural number order (HDMI 2 before HDMI 10). */
    fun order(inputs: List<InputDescriptor>): List<InputDescriptor> = inputs
        .filter { it.isPassthrough || it.type == InputType.Tuner }
        .distinctBy { it.id }
        .sortedWith(compareBy<InputDescriptor> { it.type.ordinal }.thenBy { labelNumber(it.label) }.thenBy { it.label })

    /** Trailing number in a label ("HDMI 2" -> 2), or a large value when there is none. */
    fun labelNumber(label: String): Int = Regex("(\\d+)\\s*$").find(label)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE

    /** A short label when the system gives none or an unreadable id. */
    fun fallbackLabel(type: InputType, index: Int, names: Map<InputType, String>): String =
        names[type]?.let { if (type == InputType.Hdmi) "$it ${index + 1}" else it } ?: type.name
}
