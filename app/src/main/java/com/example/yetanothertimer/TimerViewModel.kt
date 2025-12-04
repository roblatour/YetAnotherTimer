package com.example.yetanothertimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.yetanothertimer.data.SettingsRepository
import com.example.yetanothertimer.data.StartDuration
import com.example.yetanothertimer.audio.ChimePlayer
import com.example.yetanothertimer.motion.MotionDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val languageTag: String,
    val touchLockEnabled: Boolean,
    val isMoving: Boolean
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
    companion object {
        // Grace period in milliseconds where touch lock is disabled after app startup
        private const val GRACE_PERIOD_MS = 7000L
    }
    
    private val settings = SettingsRepository(app)
    private val appContext = app.applicationContext
    private val motionDetector = MotionDetector(appContext)
    private val appStartTime = System.currentTimeMillis()
    // Start with neutral initial values to avoid "2:00" flash before DataStore emits
    private val _countUpStartSeconds = MutableStateFlow(0)
    private val _countDownStartSeconds = MutableStateFlow(0)
    private val _remaining = MutableStateFlow(0)
    private val _running = MutableStateFlow(false)
    // Align initial UI toggles with repository defaults (chime on, keep screen off)
    private val _chimeEnabled = MutableStateFlow(true)
    private val _keepScreenOn = MutableStateFlow(false)
    private val _helpIconVisible = MutableStateFlow(true)
    private val _languageIconVisible = MutableStateFlow(true)
    private val _isCountUp = MutableStateFlow(false)
    private val _languageTag = MutableStateFlow("en")
    private val _touchLockEnabled = MutableStateFlow(false)
    private var ticker: Job? = null
    private var postZeroJob: Job? = null
    private var hasInitializedCountUp: Boolean = false
    private var hasInitializedCountDown: Boolean = false
    private var hasInitializedMode: Boolean = false
    private var pendingAdjustOnModeChange: Boolean = false
    private var lastTickRealtimeMs: Long? = null


    private val coreState = combine(_countUpStartSeconds, _countDownStartSeconds, _remaining, _running, _isCountUp) { countUpStart, countDownStart, remain, running, isCountUp ->
        val currentStart = if (isCountUp) countUpStart else countDownStart
        Triple(currentStart, remain, running)
    }
    private val opts5 = combine(_chimeEnabled, _keepScreenOn, _helpIconVisible, _isCountUp, _languageTag) { chime, keepOn, helpVisible, countUp, lang ->
        listOf(chime, keepOn, helpVisible, countUp, lang)
    }
    private val optsState = combine(opts5, _languageIconVisible, _touchLockEnabled) { opts, langVisible, touchLock ->
        Triple(opts, langVisible, touchLock)
    }
    val state: StateFlow<TimerState> by lazy {
        combine(coreState, optsState, motionDetector.isMoving) { core, optsPair, isMoving ->
            val (start, remain, running) = core
            val opts = optsPair.first
            val langVisible = optsPair.second
            val touchLock = optsPair.third
            val chime = opts[0] as Boolean
            val keepOn = opts[1] as Boolean
            val helpVisible = opts[2] as Boolean
            val countUp = opts[3] as Boolean
            val lang = opts[4] as String

            // Check if we're still in the grace period
            val isInGracePeriod = (System.currentTimeMillis() - appStartTime) < GRACE_PERIOD_MS
            // During grace period, treat as not moving regardless of actual motion
            val effectiveMoving = if (isInGracePeriod) false else isMoving

            TimerState(
                totalSeconds = start,
                remainingSeconds = remain,
                isRunning = running,
                chimeEnabled = chime,
                keepScreenOn = keepOn,
                helpIconVisible = helpVisible,
                languageIconVisible = langVisible,
                isCountUp = countUp,
                languageTag = lang,
                touchLockEnabled = touchLock,
                isMoving = effectiveMoving
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            // Use the pre-seeded flows for a correct first emission without flicker
            TimerState(
                totalSeconds = if (_isCountUp.value) _countUpStartSeconds.value else _countDownStartSeconds.value,
                remainingSeconds = _remaining.value,
                isRunning = _running.value,
                chimeEnabled = _chimeEnabled.value,
                keepScreenOn = _keepScreenOn.value,
                helpIconVisible = _helpIconVisible.value,
                languageIconVisible = _languageIconVisible.value,
                isCountUp = _isCountUp.value,
                languageTag = _languageTag.value,
                touchLockEnabled = _touchLockEnabled.value,
                isMoving = false
            )
        )
    }

    init {
        // Start motion detection
        motionDetector.startListening()
        // Bootstrap initial state synchronously to avoid any startup flicker
        try {
            kotlinx.coroutines.runBlocking {
                // Read the essential settings for initial render
                val enabled = settings.countUpEnabledFlow.first()
                val up = settings.countUpDurationFlow.first()
                val down = settings.countDownDurationFlow.first()
                _countUpStartSeconds.value = (up.minutes * 60) + up.seconds
                _countDownStartSeconds.value = (down.minutes * 60) + down.seconds
                _isCountUp.value = enabled
                _remaining.value = if (enabled) 0 else _countDownStartSeconds.value
                hasInitializedMode = true
                hasInitializedCountUp = true
                hasInitializedCountDown = true
            }
        } catch (_: Exception) {
            // In case of any issue, fall back to neutral values already set
        }
        
        viewModelScope.launch {
            settings.countUpDurationFlow.collect { d ->
                val start = (d.minutes * 60) + d.seconds
                _countUpStartSeconds.value = start
                // On first load, only adjust remaining if mode has been initialized and we're in count up
                if (!hasInitializedCountUp) {
                    if (hasInitializedMode && _isCountUp.value) {
                        _remaining.value = 0 // Count up starts at 0
                    }
                    hasInitializedCountUp = true
                }
            }
        }
        viewModelScope.launch {
            settings.countDownDurationFlow.collect { d ->
                val start = (d.minutes * 60) + d.seconds
                _countDownStartSeconds.value = start
                // On first load, only adjust remaining if mode has been initialized and we're in count down
                if (!hasInitializedCountDown) {
                    if (hasInitializedMode && !_isCountUp.value) {
                        _remaining.value = start
                    }
                    hasInitializedCountDown = true
                } else {
                    // If a recent mode change requested adjusting to the new countdown start,
                    // do so now that we have the updated start value.
                    if (pendingAdjustOnModeChange && !_isCountUp.value && !_running.value) {
                        _remaining.value = _countDownStartSeconds.value
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
                // On first load, initialize the mode and set a sane initial remaining to avoid flicker
                if (!hasInitializedMode) {
                    _isCountUp.value = enabled
                    hasInitializedMode = true
                    // Set initial remaining based on mode. Countdown uses current known start (may still be 0 until loaded).
                    _remaining.value = if (enabled) 0 else _countDownStartSeconds.value
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
                            // and also flag to adjust after countDownDurationFlow updates (in case user also changed minutes/seconds).
                            _remaining.value = _countDownStartSeconds.value
                            pendingAdjustOnModeChange = true
                        }
                    } else {
                        // If actively running, keep the current displayed value unchanged and
                        // simply flip direction; the next tick will move +1 (up) or -1 (down).
                        // No remapping here to honor "continue from current value" requirement.
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
        viewModelScope.launch {
            settings.touchLockEnabledFlow.collect { enabled ->
                _touchLockEnabled.value = enabled
            }
        }
    }

    fun start() {
        if (_running.value) return
        if (!_isCountUp.value && _remaining.value <= 0) return
        _running.value = true
        ticker?.cancel()
        // Reset tick reference so new sessions don't inherit elapsed time from previous runs
        lastTickRealtimeMs = null
        ticker = viewModelScope.launch {
            while (_running.value) {
                val now = android.os.SystemClock.elapsedRealtime()
                val last = lastTickRealtimeMs
                val elapsedWholeSeconds = if (last == null) {
                    lastTickRealtimeMs = now
                    1
                } else {
                    val diffMs = now - last
                    if (diffMs < 1000L) {
                        val sleepFor = 1000L - diffMs
                        delay(sleepFor)
                        continue
                    }
                    val secs = (diffMs / 1000L).toInt().coerceAtLeast(1)
                    lastTickRealtimeMs = last + secs * 1000L
                    secs
                }

                if (_isCountUp.value) {
                    val next = (_remaining.value + elapsedWholeSeconds).coerceAtMost(_countUpStartSeconds.value)
                    _remaining.value = next
                    if (next >= _countUpStartSeconds.value) {
                        _running.value = false
                        onReachedLimitForCountUp()
                    }
                } else {
                    if (_remaining.value <= 0) {
                        _running.value = false
                        onReachedZero()
                    } else {
                        val next = (_remaining.value - elapsedWholeSeconds).coerceAtLeast(0)
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
        lastTickRealtimeMs = null
    }

    fun resetToStart() {
        stop()
        _remaining.value = if (_isCountUp.value) 0 else _countDownStartSeconds.value
        lastTickRealtimeMs = null
    }

    fun setStart(minutes: Int, seconds: Int) {
        viewModelScope.launch {
            if (_isCountUp.value) {
                settings.setCountUpDuration(minutes, seconds)
            } else {
                settings.setCountDownDuration(minutes, seconds)
            }
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
        _remaining.value = if (_isCountUp.value) 0 else _countDownStartSeconds.value
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
                if (_countUpStartSeconds.value > 0 && _remaining.value < _countUpStartSeconds.value) {
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
        _remaining.value = if (_isCountUp.value) 0 else _countDownStartSeconds.value
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
            _remaining.value = _countDownStartSeconds.value
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

    // Finish count up immediately as if the limit was reached at the provided limitSeconds.
    // Shows the provided limit for ~1s, plays chime if enabled, then resets to 0:00 and remains stopped.
    fun finishCountUpAtLimit(limitSeconds: Int) {
        val limit = limitSeconds.coerceAtLeast(0)
        // Stop active ticking
        _running.value = false
        ticker?.cancel()
        lastTickRealtimeMs = null
        // Show the reached limit value before resetting
        _remaining.value = limit
        onReachedLimitForCountUp()
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

    fun setTouchLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setTouchLockEnabled(enabled)
        }
    }

    // Get current count up duration for settings dialog
    fun getCurrentCountUpDuration(): StartDuration {
        val totalSeconds = _countUpStartSeconds.value
        return StartDuration(totalSeconds / 60, totalSeconds % 60)
    }

    // Get current count down duration for settings dialog
    fun getCurrentCountDownDuration(): StartDuration {
        val totalSeconds = _countDownStartSeconds.value
        return StartDuration(totalSeconds / 60, totalSeconds % 60)
    }

    // Set both count up and count down durations (for settings save)
    fun setBothDurations(countUpMinutes: Int, countUpSeconds: Int, countDownMinutes: Int, countDownSeconds: Int) {
        viewModelScope.launch {
            settings.setCountUpDuration(countUpMinutes, countUpSeconds)
            settings.setCountDownDuration(countDownMinutes, countDownSeconds)
            // Do not forcibly reset here; allow active session to continue.
            // UI will apply immediate adjustments if needed (clamp or finish) based on new values.
        }
    }

    override fun onCleared() {
        super.onCleared()
        motionDetector.stopListening()
    }
}