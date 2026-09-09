package com.qiaoyuang.movie.basicui

sealed interface UIEvent {

    data class ShowToast(val message: String) : UIEvent
    data object CommonNoMoreToast : UIEvent
    data object CommonErrorToast : UIEvent
}
