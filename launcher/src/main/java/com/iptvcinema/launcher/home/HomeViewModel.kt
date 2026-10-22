package com.iptvcinema.launcher.home

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvcinema.launcher.LauncherGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Assembles the rows off the main thread whenever a source or the language changes. */
class HomeViewModel(private val graph: LauncherGraph = LauncherGraph) : ViewModel() {
    private val labels = MutableStateFlow<HomeLabels?>(null)

    val rows: StateFlow<List<HomeRow>?> = combine(
        graph.contentRows.state,
        graph.apps.apps,
        graph.inputs.inputs,
        labels,
        snapshotFlow { graph.clock.nowMs },
    ) { content, apps, inputs, labels, nowMs ->
        labels?.let { HomeRowsAssembler.assemble(content, apps, inputs, nowMs, it) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setLabels(value: HomeLabels) {
        labels.value = value
    }

    /** Rows for the very first frame, from whatever is cached, before the flow has emitted. */
    fun initialRows(labels: HomeLabels): List<HomeRow> = HomeRowsAssembler.assemble(
        graph.contentRows.state.value,
        graph.apps.apps.value,
        graph.inputs.inputs.value,
        System.currentTimeMillis(),
        labels,
    )

    /** Back in front: the rows may have changed while another app was open. */
    fun onResume() {
        graph.contentRows.refresh()
        graph.contentRows.requestAppRefresh()
        graph.inputs.reload()
    }
}
