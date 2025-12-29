package com.otheruncle.memorydisplay.data.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages session state and emits events when session expires.
 * Used by AuthInterceptor to notify the app of 401 responses.
 */
@Singleton
class SessionManager @Inject constructor() {
    
    private val _sessionExpired = MutableSharedFlow<Unit>(replay = 0)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()
    
    /**
     * Called when a 401 response is received, indicating session has expired
     */
    suspend fun onSessionExpired() {
        _sessionExpired.emit(Unit)
    }
}
