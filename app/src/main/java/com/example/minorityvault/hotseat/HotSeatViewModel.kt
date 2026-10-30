package com.example.minorityvault.hotseat

import androidx.lifecycle.ViewModel
import com.example.minorityvault.ai.QuestionBank
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Where match seeds come from. The engine does the rest deterministically. */
fun interface SeedSource {
    fun nextSeed(): Long
}

/** Pass-the-phone play: events go through the pure [on] reducer, the screen renders [uiState]. */
@HiltViewModel
class HotSeatViewModel @Inject constructor(private val seeds: SeedSource) : ViewModel() {
    private var model = HotSeatModel()
    private val _uiState = MutableStateFlow(model.toUiState())
    val uiState: StateFlow<HotSeatUiState> = _uiState.asStateFlow()

    fun onEvent(event: HotSeatEvent) {
        model = model.on(event, seeds::nextSeed, QuestionBank.ids)
        _uiState.update { model.toUiState() }
    }
}
