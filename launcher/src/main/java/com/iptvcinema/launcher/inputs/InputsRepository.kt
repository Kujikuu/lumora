package com.iptvcinema.launcher.inputs

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.iptvcinema.launcher.content.AppLinks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** TV inputs (HDMI, VGA, YPbPr, AV, tuners) from the system's TvInputManager, when the box has one. */
class InputsRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val manager: TvInputManager? = runCatching {
        context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
    }.getOrNull()
    private val _inputs = MutableStateFlow<List<InputDescriptor>?>(null)
    /** Null until the first read; empty when the box has no TV inputs. */
    val inputs: StateFlow<List<InputDescriptor>?> = _inputs.asStateFlow()
    private var callback: TvInputManager.TvInputCallback? = null

    fun start() {
        val manager = manager ?: run { _inputs.value = emptyList(); return }
        if (callback == null) {
            callback = object : TvInputManager.TvInputCallback() {
                override fun onInputStateChanged(inputId: String, state: Int) = reload()
                override fun onInputAdded(inputId: String) = reload()
                override fun onInputRemoved(inputId: String) = reload()
                override fun onInputUpdated(inputId: String) = reload()
            }.also { runCatching { manager.registerCallback(it, Handler(Looper.getMainLooper())) } }
        }
        reload()
    }

    fun reload() {
        val manager = manager ?: return
        scope.launch(Dispatchers.IO) {
            val typeNames = InputTypeNames.of(context)
            val descriptors = runCatching {
                manager.tvInputList.mapIndexed { index, info -> info.toDescriptor(manager, index, typeNames) }
            }.onFailure { Log.w(TAG, "TV input list failed: ${it.message}") }.getOrDefault(emptyList())
            _inputs.value = InputMapper.order(descriptors)
        }
    }

    private fun TvInputInfo.toDescriptor(manager: TvInputManager, index: Int, names: Map<InputType, String>): InputDescriptor {
        val type = InputMapper.typeOf(this.type)
        val label = runCatching { loadLabel(context)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: InputMapper.fallbackLabel(type, index, names)
        val state = runCatching { manager.getInputState(id) }.getOrNull()
        return InputDescriptor(id = id, type = type, label = label, state = InputMapper.stateOf(state), isPassthrough = isPassthroughInput)
    }

    /** Shows [input] through the first path the box supports; see [InputSwitcher]. */
    fun open(input: InputDescriptor) {
        val viewIntent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(input.id))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val hasSystemViewer = viewIntent.resolveActivity(context.packageManager) != null
        when (InputSwitcher.plan(input, hasSystemViewer)) {
            InputSwitchPlan.SystemViewer -> if (!AppLinks.start(context, viewIntent)) openOwnViewer(input)
            InputSwitchPlan.OwnViewer -> openOwnViewer(input)
            InputSwitchPlan.SystemPicker -> openSystemPicker()
        }
    }

    private fun openOwnViewer(input: InputDescriptor) {
        AppLinks.start(context, InputViewerActivity.intent(context, input.id, input.label))
    }

    /** The box's own input list, or Settings when it has none. */
    fun openSystemPicker() {
        val picker = Intent(ACTION_SYSTEM_INPUT_PICKER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (picker.resolveActivity(context.packageManager) == null || !AppLinks.start(context, picker)) {
            AppLinks.start(context, Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    companion object {
        private const val TAG = "LauncherInputs"
        /** Realtek/Toptech boxes: the "Source" screen of the vendor TV menu. */
        const val ACTION_SYSTEM_INPUT_PICKER = "com.toptech.tv.action.INPUT_ACTIVITY"
    }
}
