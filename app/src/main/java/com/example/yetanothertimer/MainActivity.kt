package com.example.yetanothertimer

/*

Context:

This application works as a timer.
The timer can count either up or down depending on the user settings.
When counting up the timer starts at 0:00 and counts up to the target time set in the user settings for counting up.
When counting down the timer starts at the time set in the user settings for counting down and counts down to 0:00.
The application displays on the main screen the amount of time on the timer.
When the user double taps the screen the time displayed will change to 0:00 if the timer is in count up mode, or the target time set in the user settings if the timer is in countdown mode. 
When the timer is not active and the user single taps the screen the timer activates and begins to count either up or down depending on its settings.
If the timer is active and is counting either up or down and the user single taps on the screen the timer will be paused. If the user single taps on the screen when the timer is paused the timer will resume.
If the application is minimized and the timer is active the timer will continue counting.
When the timer reaches its target time when it is counting either up or down a chime will be sounded if the user has selected that option in the user settings. This will happen regardless of if the application is minimized or running in the foreground.
When the timer reaches its target time when it is counting either up or down, And after the optional chime is sounded if that setting is set, the timer will stop counting, reset and display 0:00 if the timer is in count up mode, or the target time if the timer is in countdown mode.  This will happen regardless of if the application is running in the foreground or .background - However if it is running in the background the user will not see the changes to the timer screen until they bring the application back into the foreground.
The application also engages a screen lock out when that option is set in the user settings.  
The application also disables the devices auto-lock feature if that setting is set in the user settings.

*/

import android.os.Bundle
import android.view.WindowManager
import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import com.example.yetanothertimer.ui.theme.YetAnotherTimerTheme
import com.example.yetanothertimer.data.SupportedLanguages
import com.example.yetanothertimer.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

class MainActivity : AppCompatActivity() {
    private val vm: TimerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply persisted app locale before composing UI so resources load in the chosen language
        runBlocking {
            try {
                val repo = com.example.yetanothertimer.data.SettingsRepository(this@MainActivity)
                val tag = repo.languageTagFlow.first()
                val locales = LocaleListCompat.forLanguageTags(tag)
                AppCompatDelegate.setApplicationLocales(locales)
            } catch (_: Exception) { /* ignore and proceed */ }
        }
        setContent {
            YetAnotherTimerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    val state by vm.state.collectAsState()
                    // Locale is applied before setContent and on selection; no reactive reset here to avoid loops
                    LaunchedEffect(state.keepScreenOn) {
                        if (state.keepScreenOn) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                    // Use modern edge-to-edge API to style system bars without deprecated setters
                    LaunchedEffect(Unit) {
                        try {
                            this@MainActivity.enableEdgeToEdge(
                                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
                                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
                            )
                        } catch (_: Exception) { }
                    }
                    TimerScreen(vm)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(vm: TimerViewModel) {
    val state by vm.state.collectAsState()
    val showSettings = rememberSaveable { mutableStateOf(false) }
    val showHelp = rememberSaveable { mutableStateOf(false) }
    val showShare = rememberSaveable { mutableStateOf(false) }
    val showLanguageMenu = rememberSaveable { mutableStateOf(false) }
    // Confirmation prompts removed; apply changes immediately
    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        // Root container to stack timer content and FAB row overlay
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Main timer content with tap/double-tap gestures
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.touchLockEnabled, state.isMoving) {
                        detectTapGestures(
                            onTap = { 
                                // Only handle tap if touch lock is disabled or no recent movement
                                if (!state.touchLockEnabled || !state.isMoving) {
                                    vm.toggleStartPause() 
                                }
                            },
                            onDoubleTap = { 
                                // Only handle double tap if touch lock is disabled or no recent movement
                                if (!state.touchLockEnabled || !state.isMoving) {
                                    vm.doubleTapToResetOnly() 
                                }
                            }
                        )
                    }
            ) {
                val minPx = kotlin.math.min(maxWidth.value, maxHeight.value)
                val sizeSp = (minPx * 0.22f).sp
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Timer display column to allow padlock above timer
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Padlock overlay when touch lock is enabled and moving
                        if (state.touchLockEnabled && state.isMoving) {
                            val lockIconSize = (minPx * 0.08f).dp
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Touch locked due to movement",
                                tint = Color.Gray,
                                modifier = Modifier.size(lockIconSize)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Timer text
                        Text(
                            text = state.display,
                            color = Color.White,
                            fontSize = sizeSp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom-aligned FAB row with symmetric edge padding
            val edgePadding = 16.dp
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = edgePadding, vertical = edgePadding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left section
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (state.helpIconVisible) {
                        val isLocked = state.touchLockEnabled && state.isMoving
                        FloatingActionButton(
                            onClick = { 
                                if (!isLocked) {
                                    showHelp.value = true 
                                }
                            },
                            containerColor = if (isLocked) Color.Gray else Color.White,
                            contentColor = if (isLocked) Color.DarkGray else Color.Black
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Help,
                                contentDescription = stringResource(id = R.string.desc_help_icon)
                            )
                        }
                    } else {
                        // Keep layout stable
                        Spacer(modifier = Modifier.size(56.dp))
                    }
                }

                // Center: Language button with dropdown (conditionally visible)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (state.languageIconVisible) {
                        val isLocked = state.touchLockEnabled && state.isMoving
                        FloatingActionButton(
                            onClick = { 
                                if (!isLocked) {
                                    showLanguageMenu.value = !showLanguageMenu.value 
                                }
                            },
                            containerColor = if (isLocked) Color.Gray else Color.White,
                            contentColor = if (isLocked) Color.DarkGray else Color.Black
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.language_icon),
                                contentDescription = stringResource(id = R.string.desc_language_icon)
                            )
                        }
                    } else {
                        // Keep layout stable
                        Spacer(modifier = Modifier.size(56.dp))
                    }
                    val languages = remember { com.example.yetanothertimer.data.SupportedLanguages.all }
                    val scope = rememberCoroutineScope()
                    val isLocked = state.touchLockEnabled && state.isMoving
                    DropdownMenu(
                        expanded = showLanguageMenu.value && !isLocked,
                        onDismissRequest = { showLanguageMenu.value = false }
                    ) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val activity = context as Activity
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.autonym) },
                                onClick = {
                                    showLanguageMenu.value = false
                                    scope.launch {
                                        // Persist first to avoid reading stale value on immediate recreate
                                        val repo = com.example.yetanothertimer.data.SettingsRepository(context)
                                        repo.setLanguageTag(lang.tag)
                                        // Immediately apply and recreate to refresh Compose strings
                                        val desired = LocaleListCompat.forLanguageTags(lang.tag)
                                        AppCompatDelegate.setApplicationLocales(desired)
                                        activity.recreate()
                                    }
                                }
                            )
                        }
                    }
                }

                // Right section
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    val isLocked = state.touchLockEnabled && state.isMoving
                    FloatingActionButton(
                        onClick = { 
                            if (!isLocked) {
                                showSettings.value = true 
                            }
                        },
                        containerColor = if (isLocked) Color.Gray else Color.White,
                        contentColor = if (isLocked) Color.DarkGray else Color.Black
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(id = R.string.desc_settings_icon)
                        )
                    }
                }
            }
        }
    }
    if (showSettings.value) {
        val currentDuration = if (state.isCountUp) {
            vm.getCurrentCountUpDuration()
        } else {
            vm.getCurrentCountDownDuration()
        }
        
        SettingsDialog(
            initialMinutes = currentDuration.minutes,
            initialSeconds = currentDuration.seconds,
            initialChimeEnabled = state.chimeEnabled,
            initialKeepScreenOn = state.keepScreenOn,
            initialHelpIconVisible = state.helpIconVisible,
            initialLanguageIconVisible = state.languageIconVisible,
            initialIsCountUp = state.isCountUp,
            initialLanguageTag = state.languageTag,
            initialTouchLockEnabled = state.touchLockEnabled,
            viewModel = vm,
            onDismiss = {
                // Intentionally left blank to prevent accidental dismiss via outside tap/back.
                // Settings dialog will only close via explicit Save or Cancel buttons below.
            },
            onCancel = {
                showSettings.value = false
            },
            onSave = { m, s ->
                // For prompts, use the current mode's values
                val newStart = (m * 60) + s
                // Apply new values immediately to the active timer without confirmation
                // Countdown: if new start is lower than current remaining, clamp down to new value and continue
                // Count up: if new limit is lower than current elapsed, finish immediately (reset to 0:00 and stop)
                if (!state.isCountUp) {
                    val sessionInProgress = state.isRunning || (state.remainingSeconds < state.totalSeconds)
                    if (sessionInProgress && state.remainingSeconds > newStart) {
                        vm.lowerActiveCountdownTo(newStart)
                    }
                } else {
                    val sessionInProgress = state.isRunning || (state.remainingSeconds > 0)
                    if (sessionInProgress && state.remainingSeconds > newStart) {
                        // Immediately finish as if limit reached: show limit briefly, chime (if on), then reset to 0:00
                        vm.finishCountUpAtLimit(newStart)
                    }
                }
                showSettings.value = false
            },
            onToggleChime = { enabled -> vm.setChimeEnabled(enabled) },
            onToggleKeepScreenOn = { enabled -> vm.setKeepScreenOn(enabled) },
            onToggleHelpIcon = { visible -> vm.setHelpIconVisible(visible) },
            onToggleLanguageIcon = { visible -> vm.setLanguageIconVisible(visible) },
            onToggleCountUp = { enabled -> vm.setCountUpEnabled(enabled) },
            onToggleTouchLock = { enabled -> vm.setTouchLockEnabled(enabled) },
            onSetLanguageTag = { tag -> vm.setLanguageTag(tag) }
        )
    }

    if (showHelp.value) {
        // Derive layout direction from current language tag
        val isRtl = SupportedLanguages.isRtl(state.languageTag)
        val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        AlertDialog(
            onDismissRequest = {
                showHelp.value = false
            },
            confirmButton = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                showShare.value = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                        ) { Text(stringResource(id = R.string.btn_share)) }

                        Button(
                            onClick = {
                                showHelp.value = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                        ) { Text(stringResource(id = R.string.btn_close)) }
                    }
                }
            },
            title = {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                    Text(
                        stringResource(id = R.string.title_help),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            },
            text = {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(id = R.string.help_line_tap), color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                            Text(stringResource(id = R.string.help_line_double_tap), color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                            Text(stringResource(id = R.string.help_line_options), color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)

                            val context = androidx.compose.ui.platform.LocalContext.current
                            val rawVersion = remember(context.packageName) { context.versionNameOrEmpty() }
                            val trimmedVersion = rawVersion
                                .split('.')
                                .dropLastWhile { it == "0" }
                                .joinToString(".")
                                .ifBlank { rawVersion }

                            val footer = stringResource(
                                id = R.string.help_footer_format,
                                stringResource(id = R.string.license_word),
                                trimmedVersion
                            )
                            Text(footer, color = Color.White, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        }
                    }
                }
            },
            containerColor = Color.Black,
            textContentColor = Color.White
        )
    }

    if (showShare.value) {
        ShareQrDialog(
            languageTag = state.languageTag,
            onDismiss = { showShare.value = false }
        )
    }

    // Confirmation prompts removed: behavior is now immediate based on rules above
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    initialMinutes: Int,
    initialSeconds: Int,
    initialChimeEnabled: Boolean,
    initialKeepScreenOn: Boolean,
    initialHelpIconVisible: Boolean,
    initialIsCountUp: Boolean,
    initialLanguageIconVisible: Boolean,
    initialLanguageTag: String,
    initialTouchLockEnabled: Boolean,
    viewModel: TimerViewModel, // Add view model parameter to get current values
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onSave: (Int, Int) -> Unit,
    onToggleChime: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleHelpIcon: (Boolean) -> Unit,
    onToggleCountUp: (Boolean) -> Unit,
    onToggleLanguageIcon: (Boolean) -> Unit,
    onToggleTouchLock: (Boolean) -> Unit,
    onSetLanguageTag: (String) -> Unit
) {
    var minutesText by rememberSaveable { mutableStateOf(initialMinutes.toString()) }
    var secondsText by rememberSaveable { mutableStateOf(initialSeconds.toString()) }
    var minutesCleared by remember { mutableStateOf(false) }
    var secondsCleared by remember { mutableStateOf(false) }
    var chimeEnabled by rememberSaveable { mutableStateOf(initialChimeEnabled) }
    var keepScreenOn by rememberSaveable { mutableStateOf(initialKeepScreenOn) }
    var helpIconVisible by rememberSaveable { mutableStateOf(initialHelpIconVisible) }
    var isCountUp by rememberSaveable { mutableStateOf(initialIsCountUp) }
    var languageIconVisible by rememberSaveable { mutableStateOf(initialLanguageIconVisible) }
    var touchLockEnabled by rememberSaveable { mutableStateOf(initialTouchLockEnabled) }
    var languageTag by rememberSaveable { mutableStateOf(initialLanguageTag.ifBlank { "en" }) }
    
    // Store temporary values for both count up and count down modes
    // Initialize with the actual stored values for each mode
    val initialCountUpDuration = viewModel.getCurrentCountUpDuration()
    val initialCountDownDuration = viewModel.getCurrentCountDownDuration()
    
    var tempCountUpMinutes by rememberSaveable { mutableStateOf(initialCountUpDuration.minutes.toString()) }
    var tempCountUpSeconds by rememberSaveable { mutableStateOf(initialCountUpDuration.seconds.toString()) }
    var tempCountDownMinutes by rememberSaveable { mutableStateOf(initialCountDownDuration.minutes.toString()) }
    var tempCountDownSeconds by rememberSaveable { mutableStateOf(initialCountDownDuration.seconds.toString()) }
    // Track whether each mode has been edited during this dialog session
    var editedCountUp by rememberSaveable { mutableStateOf(false) }
    var editedCountDown by rememberSaveable { mutableStateOf(false) }
    
    // Initialize the displayed values based on the current mode
    LaunchedEffect(Unit) {
        if (initialIsCountUp) {
            minutesText = tempCountUpMinutes
            secondsText = tempCountUpSeconds
        } else {
            minutesText = tempCountDownMinutes
            secondsText = tempCountDownSeconds
        }
    }
    
    // Update displayed values when mode changes, preserving unsaved edits.
    LaunchedEffect(isCountUp) {
        if (isCountUp) {
            // Save current values to count down temp storage
            tempCountDownMinutes = minutesText
            tempCountDownSeconds = secondsText
            // Load count up values: if not edited this session, refresh from store
            if (!editedCountUp) {
                val fresh = viewModel.getCurrentCountUpDuration()
                tempCountUpMinutes = fresh.minutes.toString()
                tempCountUpSeconds = fresh.seconds.toString()
            }
            minutesText = tempCountUpMinutes
            secondsText = tempCountUpSeconds
        } else {
            // Save current values to count up temp storage
            tempCountUpMinutes = minutesText
            tempCountUpSeconds = secondsText
            // Load count down values: if not edited this session, refresh from store
            if (!editedCountDown) {
                val fresh = viewModel.getCurrentCountDownDuration()
                tempCountDownMinutes = fresh.minutes.toString()
                tempCountDownSeconds = fresh.seconds.toString()
            }
            minutesText = tempCountDownMinutes
            secondsText = tempCountDownSeconds
        }
        // Reset the cleared flags when switching modes
        minutesCleared = false
        secondsCleared = false
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                    ) {
                        Text(stringResource(id = R.string.btn_cancel))
                    }

                    Button(
                        onClick = {
                            // Save both count up and count down durations from temporary storage
                            val countUpM = tempCountUpMinutes.filter { char -> char.isDigit() }.toIntOrNull()?.coerceAtLeast(0) ?: 0
                            val countUpS = tempCountUpSeconds.filter { char -> char.isDigit() }.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            val countDownM = tempCountDownMinutes.filter { char -> char.isDigit() }.toIntOrNull()?.coerceAtLeast(0) ?: 0
                            val countDownS = tempCountDownSeconds.filter { char -> char.isDigit() }.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            
                            viewModel.setBothDurations(countUpM, countUpS, countDownM, countDownS)
                            
                            // Apply toggle changes only on Save
                            onToggleChime(chimeEnabled)
                            onToggleKeepScreenOn(keepScreenOn)
                            onToggleHelpIcon(helpIconVisible)
                            onToggleLanguageIcon(languageIconVisible)
                            onToggleCountUp(isCountUp)
                            onToggleTouchLock(touchLockEnabled)
                            onSetLanguageTag(languageTag)
                            
                            // For the prompt logic, use the current mode's values
                            val currentM = if (isCountUp) countUpM else countDownM
                            val currentS = if (isCountUp) countUpS else countDownS
                            onSave(currentM, currentS)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                    ) {
                        Text(stringResource(id = R.string.btn_save))
                    }
                }
            }
        },
        title = {
            val isRtl = SupportedLanguages.isRtl(languageTag)
            val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                Text(
                    stringResource(id = R.string.title_options),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        },
        text = {
            val isRtl = SupportedLanguages.isRtl(languageTag)
            val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                // Language selection moved to the new bottom language button; dropdown removed from Options
                // Count up/down toggle row: now ABOVE Minutes/Seconds and above Chime
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            isCountUp = !isCountUp
                        }
                ) {
                    // Keep icon a consistent size so following rows can align to the text start
                    val iconSize = 24.dp
                    // Icon: up arrow for count up (gray), down arrow for count down (green)
                    val iconTint = if (isCountUp) Color.Gray else Color.Green
                    Icon(
                        imageVector = if (isCountUp) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = if (isCountUp) stringResource(id = R.string.desc_count_up) else stringResource(id = R.string.desc_count_down),
                        tint = iconTint,
                        modifier = Modifier.size(iconSize)
                    )
                    Text(
                        text = if (isCountUp) stringResource(id = R.string.label_count_up) else stringResource(id = R.string.label_count_down),
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp).fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
                // Inputs row: Minutes (left) and Seconds (right)
                Row(
                    // Align the start of the Minutes box with the start of the label text above
                    // by padding start equal to icon width + the 8.dp text start padding.
                    modifier = Modifier
                        .padding(start = 24.dp + 8.dp)
                        // Make the pair of inputs 20% wider (0.8 -> 0.96 of parent width)
                        .fillMaxWidth(0.96f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { v ->
                            val filtered = v.filter { char -> char.isDigit() }.take(3)
                            minutesText = filtered
                            // Also update the appropriate temporary storage
                            if (isCountUp) {
                                tempCountUpMinutes = filtered
                                editedCountUp = true
                            } else {
                                tempCountDownMinutes = filtered
                                editedCountDown = true
                            }
                        },
                        label = { Text(stringResource(id = R.string.field_minutes), color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White,
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            disabledContainerColor = Color.Black,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { state ->
                                if (state.isFocused && !minutesCleared) {
                                    minutesText = ""
                                    minutesCleared = true
                                }
                            }
                    )

                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { v ->
                            val filtered = v.filter { char -> char.isDigit() }.take(2)
                            secondsText = filtered
                            // Also update the appropriate temporary storage
                            if (isCountUp) {
                                tempCountUpSeconds = filtered
                                editedCountUp = true
                            } else {
                                tempCountDownSeconds = filtered
                                editedCountDown = true
                            }
                        },
                        label = { Text(stringResource(id = R.string.field_seconds), color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White,
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            disabledContainerColor = Color.Black,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { state ->
                                if (state.isFocused && !secondsCleared) {
                                    secondsText = ""
                                    secondsCleared = true
                                }
                            }
                    )
                }
                // Add a little extra breathing room between the time inputs and the Chime row
                // Approximately 1/4 of a typical body text line height (~24sp), so ~6dp
                Spacer(modifier = Modifier.height(6.dp))
                // Chime toggle row: tap row (icon or text) to toggle (local only until Save)
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            chimeEnabled = !chimeEnabled
                        }
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = if (chimeEnabled) stringResource(id = R.string.desc_disable_chime) else stringResource(id = R.string.desc_enable_chime),
                        tint = if (chimeEnabled) Color.Green else Color.Gray
                    )
                    Text(
                        text = if (chimeEnabled) stringResource(id = R.string.label_chime_on) else stringResource(id = R.string.label_chime_off),
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp).fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                // Touch lock toggle row: tap row (icon or text) to toggle (local only until Save)
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            touchLockEnabled = !touchLockEnabled
                        }
                ) {
                    Icon(
                        imageVector = if (touchLockEnabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = if (touchLockEnabled) stringResource(id = R.string.desc_disable_touch_lock) else stringResource(id = R.string.desc_enable_touch_lock),
                        tint = if (touchLockEnabled) Color.Green else Color.Gray
                    )
                    Text(
                        text = if (touchLockEnabled) stringResource(id = R.string.label_touch_lock_on) else stringResource(id = R.string.label_touch_lock_off),
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp).fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                // Screensaver toggle row: tap row (icon or text) to toggle (local only until Save)
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            keepScreenOn = !keepScreenOn
                        }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Smartphone,
                        contentDescription = if (keepScreenOn) stringResource(id = R.string.desc_keep_screen_on_on) else stringResource(id = R.string.desc_keep_screen_on_off),
                        tint = if (keepScreenOn) Color.Green else Color.Gray
                    )
                    Text(
                        text = if (keepScreenOn) stringResource(id = R.string.label_keep_screen_on_on) else stringResource(id = R.string.label_keep_screen_on_off),
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp).fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                // Help icon visibility toggle row: tap row (icon or text) to toggle (local only until Save)
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            helpIconVisible = !helpIconVisible
                        }
                ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = if (helpIconVisible) stringResource(id = R.string.desc_hide_help_icon) else stringResource(id = R.string.desc_show_help_icon),
                        tint = if (helpIconVisible) Color.Green else Color.Gray
                    )
                    Text(
                        text = if (helpIconVisible) stringResource(id = R.string.label_show_help_icon) else stringResource(id = R.string.label_hide_help_icon),
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp).fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                // Language icon visibility toggle row: tap row (icon or text) to toggle (local only until Save)
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            languageIconVisible = !languageIconVisible
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.language_icon),
                        contentDescription = if (languageIconVisible) stringResource(id = R.string.desc_hide_language_icon) else stringResource(id = R.string.desc_show_language_icon),
                        tint = if (languageIconVisible) Color.Green else Color.Gray
                    )
                    Text(
                        text = if (languageIconVisible) stringResource(id = R.string.label_show_language_icon) else stringResource(id = R.string.label_hide_language_icon),
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp).fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
            }
        },
        containerColor = Color.Black,
        textContentColor = Color.White
    )
}

// Language dropdown removed; selection is now through the bottom language button.

private fun Context.versionNameOrEmpty(): String {
    val pm = packageManager
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0).versionName
        }
    }.getOrNull().orEmpty()
}

@Composable
private fun ShareQrDialog(languageTag: String, onDismiss: () -> Unit) {
    val languageCode = remember(languageTag) {
        languageTag.substringBefore('-').ifBlank { "en" }
    }
    val shareUrl = remember(languageCode) {
        "https://play.google.com/store/apps/details?id=io.github.roblatour.yetanothertimer&hl=$languageCode"
    }
    val qrBitmap = remember(shareUrl) { generateQrCodeBitmap(shareUrl) }
    val isRtl = remember(languageTag) { SupportedLanguages.isRtl(languageTag) }
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
            ) {
                Text(stringResource(id = R.string.btn_close))
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.title_share_qr),
                color = Color.White
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                qrBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = stringResource(id = R.string.desc_share_qr),
                        modifier = Modifier.size(220.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.share_qr_description),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = shareUrl,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        containerColor = Color.Black,
        textContentColor = Color.White
        )
        }
}

private fun generateQrCodeBitmap(content: String, targetSize: Int = 512): ImageBitmap? {
    return runCatching {
        val writer = QRCodeWriter()
        val hints = mapOf(EncodeHintType.MARGIN to 0)
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, targetSize, targetSize, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val black = android.graphics.Color.BLACK
        val white = android.graphics.Color.WHITE
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) black else white)
            }
        }
        bitmap.asImageBitmap()
    }.getOrNull()
}