package com.iptvcinema.launcher.inputs

import android.content.Context
import com.iptvcinema.launcher.R

/** Localised short names per input type, read once per reload. */
object InputTypeNames {
    fun of(context: Context): Map<InputType, String> = mapOf(
        InputType.Hdmi to context.getString(R.string.input_hdmi),
        InputType.Vga to context.getString(R.string.input_vga),
        InputType.Component to context.getString(R.string.input_component),
        InputType.Av to context.getString(R.string.input_av),
        InputType.Tuner to context.getString(R.string.input_tuner),
    )
}
