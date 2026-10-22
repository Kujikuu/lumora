package com.iptvcinema.launcher.inputs

/** How to show an input, decided from what the box can do; pure so the order is unit tested. */
sealed interface InputSwitchPlan {
    /** The system has a viewer for passthrough channel URIs (ACTION_VIEW). */
    data object SystemViewer : InputSwitchPlan

    /** The launcher's own TvView viewer. */
    data object OwnViewer : InputSwitchPlan

    /** The system's input picker (or Settings when there is none). */
    data object SystemPicker : InputSwitchPlan
}

object InputSwitcher {
    fun plan(input: InputDescriptor, hasSystemViewer: Boolean): InputSwitchPlan = when {
        // Tuners need a channel, not a passthrough URI; only the TV app can show them.
        input.type == InputType.Tuner || !input.isPassthrough -> InputSwitchPlan.SystemPicker
        hasSystemViewer -> InputSwitchPlan.SystemViewer
        else -> InputSwitchPlan.OwnViewer
    }
}
