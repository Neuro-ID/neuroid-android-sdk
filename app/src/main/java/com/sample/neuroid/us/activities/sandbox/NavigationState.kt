package com.sample.neuroid.us.activities.sandbox

sealed class NavigationState {
    object Idle : NavigationState()
    object NavigateToScore : NavigationState()
    object NavigateBack : NavigationState()
}