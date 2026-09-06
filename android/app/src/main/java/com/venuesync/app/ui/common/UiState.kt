package com.venuesync.app.ui.common

import com.venuesync.app.core.model.ApiError

/*
 * The uniform state pattern from architecture.md — also the living answer to the
 * Zomato Flow fumble. State TRANSITIONS (who emits what, when) are ViewModel logic:
 * Yash's hands, every time.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: ApiError) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
