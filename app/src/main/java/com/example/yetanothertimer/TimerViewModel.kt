package com.example.yetanothertimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.yetanothertimer.data.SettingsRepository
import com.example.yetanothertimer.audio.ChimePlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimerState(
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val chimeEnabled: Boolean,
    val keepScreenOn: Boolean,
    val helpIconVisible: Boolean,
    val languageIconVisible: Boolean,
    val isCountUp: Boolean,
    val languageTag: String
) {
    val minutes: Int get() = remainingSeconds / 60
    val seconds: Int get() = remainingSeconds % 60
    val display: String
        get() {
            val minPart = if (minutes >= 10) "%02d".format(minutes) else "%d".format(minutes)
            val secPart = "%02d".format(seconds)
            return "$minPart:$secPart"
        }
}

class TimerViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsRepository(app)
    private val appContext = app.applicationContext
    private val _startSeconds = MutableStateFlow(120)
    private val _remaining = MutableStateFlow(120)
    private val _running = MutableStateFlow(false)
    private val _chimeEnabled = MutableStateFlow(false)
    private val _keepScreenOn = MutableStateFlow(true)
    private val _helpIconVisible = MutableStateFlow(true)
    private val _languageIconVisible = MutableStateFlow(true)
    private val _isCountUp = MutableStateFlow(false)
    private val _languageTag = MutableStateFlow("en")
    private var ticker: Job? = null
    private var postZeroJob: Job? = null
    private var hasInitializedStart: Boolean = false
    private var hasInitializedMode: Boolean = false
    private var pendingAdjustOnModeChange: Boolean = false


    private val coreState = combine(_startSeconds, _remaining, _running) { start, remain, running ->
        Triple(start, remain, running)
    }
    private val opts5 = combine(_chimeEnabled, _keepScreenOn, _helpIconVisible, _isCountUp, _languageTag) { chime, keepOn, helpVisible, countUp, lang ->
        listOf(chime, keepOn, helpVisible, countUp, lang)
    }
    private val optsState = combine(opts5, _languageIconVisible) { opts, langVisible ->
        Pair(opts, langVisible)
    }
    val state: StateFlow<TimerState> = combine(coreState, optsState) { core, optsPair ->
        val (start, remain, running) = core
        val opts = optsPair.first
        val langVisible = optsPair.second
        val chime = opts[0] as Boolean
        val keepOn = opts[1] as Boolean
        val helpVisible = opts[2] as Boolean
        val countUp = opts[3] as Boolean
        val lang = opts[4] as String
        TimerState(
            totalSeconds = start,
            remainingSeconds = remain,
            isRunning = running,
            chimeEnabled = chime,
            keepScreenOn = keepOn,
            helpIconVisible = helpVisible,
            languageIconVisible = langVisible,
            isCountUp = countUp,
            languageTag = lang
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TimerState(120, 120, false, false, true, true, true, false, "en"))

    init {
        viewModelScope.launch {
            settings.startDurationFlow.collect { d ->
                val start = (d.minutes * 60) + d.seconds
                _startSeconds.value = start
                // Only initialize remaining from start once on app start.
                // Subsequent settings changes should not alter the current remaining
                // so that opening/saving Settings doesn't reset or pause the timer.
                if (!hasInitializedStart) {
                    _remaining.value = start
                    hasInitializedStart = true
                } else {
                    // If a recent mode change requested adjusting to the new countdown start,
                    // do so now that we have the updated start value.
                    if (pendingAdjustOnModeChange && !_isCountUp.value && !_running.value) {
                        _remaining.value = _startSeconds.value
                        pendingAdjustOnModeChange = false
                    }
                }
            }
        }
        viewModelScope.launch {
            settings.chimeEnabledFlow.collect { enabled ->
                _chimeEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            settings.keepScreenOnFlow.collect { enabled ->
                _keepScreenOn.value = enabled
            }
        }
        viewModelScope.launch {
            settings.helpIconVisibleFlow.collect { visible ->
                _helpIconVisible.value = visible
            }
        }
        viewModelScope.launch {
            settings.languageIconVisibleFlow.collect { visible ->
                _languageIconVisible.value = visible
            }
        }
        viewModelScope.launch {
            settings.countUpEnabledFlow.collect { enabled ->
                // On first load, just initialize the mode.
                if (!hasInitializedMode) {
                    _isCountUp.value = enabled
                    hasInitializedMode = true
                    return@collect
                }
                val previous = _isCountUp.value
                _isCountUp.value = enabled
                if (previous != enabled) {
                    // Mode changed by the user in Settings. If not actively counting,
                    // set the display to 0:00 for count up, or to the configured start for count down.
                    if (!_running.value) {
                        if (enabled) {
                            // Count up selected
                            _remaining.value = 0
                            pendingAdjustOnModeChange = false
                        } else {
                            // Count down selected. Set to current start now,
                            // and also flag to adjust after startDuration updates (in case user also changed minutes/seconds).
                            _remaining.value = _startSeconds.value
                            pendingAdjustOnModeChange = true
                        }
                    } else {
                        // If actively running, do not adjust here.
                        pendingAdjustOnModeChange = false
                    }
                }
            }
        }
        viewModelScope.launch {
            settings.languageTagFlow.collect { tag ->
                // Use stored tag if present; otherwise best match for device locale
                val resolved = if (tag.isNotBlank()) tag else com.example.yetanothertimer.data.SupportedLanguages.bestMatchFor(java.util.Locale.getDefault())
                _languageTag.value = resolved
            }
        }
    }

    fun start() {
        if (_running.value) return
        if (!_isCountUp.value && _remaining.value <= 0) return
        _running.value = true
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (_running.value) {
                delay(1000)
                if (_isCountUp.value) {
                    val next = (_remaining.value + 1).coerceAtMost(_startSeconds.value)
                    _remaining.value = next
                    if (next >= _startSeconds.value) {
                        _running.value = false
                        onReachedLimitForCountUp()
                    }
                } else {
                    if (_remaining.value <= 0) {
                        _running.value = false
                        onReachedZero()
                    } else {
                        val next = (_remaining.value - 1).coerceAtLeast(0)
                        _remaining.value = next
                        if (next == 0) {
                            _running.value = false
                            onReachedZero()
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        _running.value = false
        ticker?.cancel()
    }

    fun resetToStart() {
        stop()
        _remaining.value = if (_isCountUp.value) 0 else _startSeconds.value
    }

    fun setStart(minutes: Int, seconds: Int) {
        viewModelScope.launch {
            settings.setStartDuration(minutes, seconds)
        }
    }

    fun setChimeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setChimeEnabled(enabled)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settings.setKeepScreenOn(enabled)
        }
    }

    fun setHelpIconVisible(visible: Boolean) {
        viewModelScope.launch {
            settings.setHelpIconVisible(visible)
        }
    }

    fun setLanguageIconVisible(visible: Boolean) {
        viewModelScope.launch {
            settings.setLanguageIconVisible(visible)
        }
    }

    // Lower the current remaining time to the provided value (in seconds) if it's higher.
    // Does not alter the running/paused state.
    fun lowerActiveCountdownTo(targetSeconds: Int) {
        val safeTarget = targetSeconds.coerceAtLeast(0)
        if (_remaining.value > safeTarget) {
            _remaining.value = safeTarget
        }
    }

    // New: tap action => reset to settings value and start immediately
    fun tapToRestartAndStart() {
        // Cancel any pending post-zero reset to avoid race
        postZeroJob?.cancel()
        _remaining.value = if (_isCountUp.value) 0 else _startSeconds.value
        _running.value = false
        ticker?.cancel()
        start()
    }

    // Single-tap behavior requested:
    // 1) If not running or paused -> start/resume
    // 2) If running -> pause
    fun toggleStartPause() {
        // Cancel any pending auto-reset after zero since user interacts now
        postZeroJob?.cancel()
        if (_running.value) {
            // Currently running: pause
            stop()
        } else {
            if (_isCountUp.value) {
                // In count up mode, allow starting from 0 up to the configured max (if max > 0)
                if (_startSeconds.value > 0 && _remaining.value < _startSeconds.value) {
                    start()
                }
            } else {
                // Count down mode: only start if there's time remaining
                if (_remaining.value > 0) {
                    start()
                }
            }
        }
    }

    // New: double-tap action => reset to settings value, but do NOT start
    fun doubleTapToResetOnly() {
        postZeroJob?.cancel()
        stop()
        _remaining.value = if (_isCountUp.value) 0 else _startSeconds.value
    }

    private fun onReachedZero() {
        // Play chime if enabled
        if (_chimeEnabled.value) {
            // Play chime off the main thread
            viewModelScope.launch {
                ChimePlayer.playCustomOrFallback(appContext, baseName = "chime")
            }
        }
        // After exactly 1s, reset remaining to start value but do not auto-start
        postZeroJob?.cancel()
        postZeroJob = viewModelScope.launch {
            delay(1000)
            _remaining.value = _startSeconds.value
        }
    }

    private fun onReachedLimitForCountUp() {
        // Play chime if enabled
        if (_chimeEnabled.value) {
            viewModelScope.launch {
                ChimePlayer.playCustomOrFallback(appContext, baseName = "chime")
            }
        }
        // After exactly 1s, reset timer display to 0:00
        postZeroJob?.cancel()
        postZeroJob = viewModelScope.launch {
            delay(1000)
            _remaining.value = 0
        }
    }

    fun setCountUpEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setCountUpEnabled(enabled)
        }
    }

    fun setLanguageTag(tag: String) {
        viewModelScope.launch {
            settings.setLanguageTag(tag)
        }
    }

    // Raise the current count up time to target if it's lower; used after settings save
    fun raiseActiveCountUpTo(targetSeconds: Int) {
        val safeTarget = targetSeconds.coerceAtLeast(0)
        if (_remaining.value < safeTarget) {
            _remaining.value = safeTarget
        }
    }
}