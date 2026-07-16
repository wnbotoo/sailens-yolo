package com.sailens.presentation.scene

sealed interface SceneAnalysisUiEffect {
    data class ShowToast(val message: String) : SceneAnalysisUiEffect
}
