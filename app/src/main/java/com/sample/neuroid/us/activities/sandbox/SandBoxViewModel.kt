package com.sample.neuroid.us.activities.sandbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuroid.tracker.NeuroID
import com.sample.neuroid.us.data.network.NIDServices
import com.sample.neuroid.us.data.network.NetworkInteractor
import com.sample.neuroid.us.domain.config.ConfigHelper
import com.sample.neuroid.us.domain.network.Signal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SandBoxViewModel @Inject constructor(
    private val nidServices: NIDServices,
    private val networkInteractor: NetworkInteractor,
    private val configHelper: ConfigHelper
) : ViewModel() {

    private val _navigationState: MutableStateFlow<NavigationState> =
        MutableStateFlow(NavigationState.Idle)
    val navigationState: MutableStateFlow<NavigationState>
        get() = _navigationState

    private val _error = MutableStateFlow("")
    val error: StateFlow<String>
        get() = _error

    private val _score: MutableStateFlow<List<Signal>> = MutableStateFlow(emptyList())
    val score: StateFlow<List<Signal>>
        get() = _score

    fun checkScore() {
        viewModelScope.launch {
            NeuroID.getInstance()?.stopSession()
            delay(2000)

            val result = networkInteractor.safeApiCall {
                nidServices.getProfile(
                    configHelper.apiKey,
                    configHelper.formId,
                    configHelper.userId
                )
            }
            if (result.isError) {
                _error.value = result.networkError?.rawError ?: "No Error Description"
            } else {
                _score.value = result.requiredResult.profile?.signals ?: emptyList()
            }
            _navigationState.value = NavigationState.NavigateToScore
        }
    }

}