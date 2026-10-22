package com.iptvcinema.launcher.inputs

enum class InputType { Hdmi, Vga, Component, Av, Tuner, Other }

enum class InputState { Connected, Standby, Disconnected, Unknown }

/** A TV input as the launcher sees it; built from TvInputInfo in Android code, pure here. */
data class InputDescriptor(
    val id: String,
    val type: InputType,
    val label: String,
    val state: InputState,
    val isPassthrough: Boolean,
)
