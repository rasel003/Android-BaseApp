package com.rasel.androidbaseapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rasel.androidbaseapp.presentation.utils.AndroidConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    private val connectivityObserver: AndroidConnectivityObserver
): ViewModel() {

    val networkStatus = connectivityObserver
        .networkStatus
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            false
        ).asLiveData()
}