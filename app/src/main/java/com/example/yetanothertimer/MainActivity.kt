package com.example.yetanothertimer

import android.os.Bundle
import android.view.WindowManager
import android.app.Activity
import android.os.Build
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
                    // Ensure system bars use black background and light icons for contrast
                    LaunchedEffect(Unit) {
                        try {
                            WindowCompat.setDecorFitsSystemWindows(window, true)
                            window.statusBarColor = android.graphics.Color.BLACK
                            window.navigationBarColor = android.graphics.Color.BLACK
                            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                                controller.isAppearanceLightStatusBars = false
                                controller.isAppearanceLightNavigationBars = false
                            }
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
    val showLanguageMenu = rememberSaveable { mutableStateOf(false) }
    val showLowerPrompt = rememberSaveable { mutableStateOf(false) }
    val showRaisePrompt = rememberSaveable { mutableStateOf(false) }
    val pendingLowerTarget = rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingRaiseTarget = rememberSaveable { mutableStateOf<Int?>(null) }
    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        // Root container to stack timer content and FAB row overlay
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Main timer content with tap/double-tap gestures
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { vm.toggleStartPause() },
                            onDoubleTap = { vm.doubleTapToResetOnly() }
                        )
                    }
            ) {
                val minPx = kotlin.math.min(maxWidth.value, maxHeight.value)
                val sizeSp = (minPx * 0.22f).sp
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.display,
                        color = Color.White,
                        fontSize = sizeSp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
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
                        FloatingActionButton(
                            onClick = { showHelp.value = true },
                            containerColor = Color.White,
                            contentColor = Color.Black
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
                        FloatingActionButton(
                            onClick = { showLanguageMenu.value = !showLanguageMenu.value },
                            containerColor = Color.White,
                            contentColor = Color.Black
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
                    DropdownMenu(
                        expanded = showLanguageMenu.value,
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
                    FloatingActionButton(
                        onClick = { showSettings.value = true },
                        containerColor = Color.White,
                        contentColor = Color.Black
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
        SettingsDialog(
            initialMinutes = state.totalSeconds / 60,
            initialSeconds = state.totalSeconds % 60,
            initialChimeEnabled = state.chimeEnabled,
            initialKeepScreenOn = state.keepScreenOn,
            initialHelpIconVisible = state.helpIconVisible,
            initialLanguageIconVisible = state.languageIconVisible,
            initialIsCountUp = state.isCountUp,
            initialLanguageTag = state.languageTag,
            onDismiss = {
                // Intentionally left blank to prevent accidental dismiss via outside tap/back.
                // Settings dialog will only close via explicit Save or Cancel buttons below.
            },
            onCancel = {
                showSettings.value = false
            },
            onSave = { m, s ->
                val newStart = (m * 60) + s
                vm.setStart(m, s)
                // If currently running or paused, and active remaining > new start, prompt to lower
                if (!state.isCountUp) {
                    if ((state.isRunning || state.remainingSeconds > 0) && state.remainingSeconds > newStart) {
                        pendingLowerTarget.value = newStart
                        showLowerPrompt.value = true
                    }
                } else {
                    // Count up: if active remaining < new start, prompt to raise
                    if ((state.isRunning || state.remainingSeconds > 0) && state.remainingSeconds < newStart) {
                        pendingRaiseTarget.value = newStart
                        showRaisePrompt.value = true
                    }
                }
                showSettings.value = false
            },
            onToggleChime = { enabled -> vm.setChimeEnabled(enabled) },
            onToggleKeepScreenOn = { enabled -> vm.setKeepScreenOn(enabled) },
            onToggleHelpIcon = { visible -> vm.setHelpIconVisible(visible) },
            onToggleLanguageIcon = { visible -> vm.setLanguageIconVisible(visible) },
            onToggleCountUp = { enabled -> vm.setCountUpEnabled(enabled) },
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
                Button(
                    onClick = {
                        showHelp.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                ) { Text(stringResource(id = R.string.btn_close)) }
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
                            val footer = stringResource(id = R.string.help_footer_format, stringResource(id = R.string.license_word))
                            Text(footer, color = Color.White, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        }
                    }
                }
            },
            containerColor = Color.Black,
            textContentColor = Color.White
        )
    }

    // Auto-dismiss the lower prompt if the active remaining falls <= target while running (count down)
    LaunchedEffect(state.remainingSeconds, state.isRunning, showLowerPrompt.value, pendingLowerTarget.value) {
        val target = pendingLowerTarget.value
        if (showLowerPrompt.value && target != null) {
            if (state.isRunning && state.remainingSeconds <= target) {
                // Countdown naturally moved below the target; auto-dismiss
                showLowerPrompt.value = false
                pendingLowerTarget.value = null
            }
        }
    }

    // Auto-dismiss the raise prompt if the active remaining rises >= target while running (count up)
    LaunchedEffect(state.remainingSeconds, state.isRunning, showRaisePrompt.value, pendingRaiseTarget.value) {
        val target = pendingRaiseTarget.value
        if (showRaisePrompt.value && target != null) {
            if (state.isRunning && state.remainingSeconds >= target) {
                showRaisePrompt.value = false
                pendingRaiseTarget.value = null
            }
        }
    }

    // Confirmation prompt to lower active countdown to new saved value
    if (showLowerPrompt.value) {
        val target = pendingLowerTarget.value ?: 0
        AlertDialog(
            onDismissRequest = {
                // Treat dismiss as "No"
                showLowerPrompt.value = false
                pendingLowerTarget.value = null
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.lowerActiveCountdownTo(target)
                        showLowerPrompt.value = false
                        pendingLowerTarget.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                ) { Text(stringResource(id = R.string.btn_yes)) }
            },
            dismissButton = {
                Button(
                    onClick = {
                        // Do not change current remaining
                        showLowerPrompt.value = false
                        pendingLowerTarget.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                ) { Text(stringResource(id = R.string.btn_no)) }
            },
            title = { Text(stringResource(id = R.string.title_confirm), color = Color.White) },
            text = {
                Text(stringResource(id = R.string.prompt_lower_to_new), color = Color.White)
            },
            containerColor = Color.Black,
            textContentColor = Color.White
        )
    }

    // Confirmation prompt to raise active count up timer to new saved value
    if (showRaisePrompt.value) {
        val target = pendingRaiseTarget.value ?: 0
        AlertDialog(
            onDismissRequest = {
                showRaisePrompt.value = false
                pendingRaiseTarget.value = null
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.raiseActiveCountUpTo(target)
                        showRaisePrompt.value = false
                        pendingRaiseTarget.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                ) { Text(stringResource(id = R.string.btn_yes)) }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showRaisePrompt.value = false
                        pendingRaiseTarget.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                ) { Text(stringResource(id = R.string.btn_no)) }
            },
            title = { Text(stringResource(id = R.string.title_confirm), color = Color.White) },
            text = {
                Text(stringResource(id = R.string.prompt_raise_to_new), color = Color.White)
            },
            containerColor = Color.Black,
            textContentColor = Color.White
        )
    }
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
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onSave: (Int, Int) -> Unit,
    onToggleChime: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleHelpIcon: (Boolean) -> Unit,
    onToggleCountUp: (Boolean) -> Unit,
    onToggleLanguageIcon: (Boolean) -> Unit,
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
    var languageTag by rememberSaveable { mutableStateOf(initialLanguageTag.ifBlank { "en" }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val m = minutesText.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val s = secondsText.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    // Apply toggle changes only on Save
                    onToggleChime(chimeEnabled)
                    onToggleKeepScreenOn(keepScreenOn)
                    onToggleHelpIcon(helpIconVisible)
                    onToggleLanguageIcon(languageIconVisible)
                    onToggleCountUp(isCountUp)
                    onSetLanguageTag(languageTag)
                    onSave(m, s)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
            ) {
                Text(stringResource(id = R.string.btn_save))
            }
        },
        dismissButton = {
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)) {
                Text(stringResource(id = R.string.btn_cancel))
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
                // Inputs row: Minutes (left) and Seconds (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { v ->
                            minutesText = v.filter { it.isDigit() }.take(3)
                        },
                        label = { Text(stringResource(id = R.string.field_minutes), color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White,
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            disabledContainerColor = Color.Black,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.White,
                            unfocusedIndicatorColor = Color.Gray,
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
                            secondsText = v.filter { it.isDigit() }.take(2)
                        },
                        label = { Text(stringResource(id = R.string.field_seconds), color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White,
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            disabledContainerColor = Color.Black,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.White,
                            unfocusedIndicatorColor = Color.Gray,
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
                    // Count up/down toggle row: directly below Minutes/Seconds and above Chime
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable {
                                isCountUp = !isCountUp
                            }
                    ) {
                        // Icon: up arrow for count up (gray), down arrow for count down (green)
                        val iconTint = if (isCountUp) Color.Gray else Color.Green
                        Icon(
                            imageVector = if (isCountUp) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = if (isCountUp) stringResource(id = R.string.desc_count_up) else stringResource(id = R.string.desc_count_down),
                            tint = iconTint
                        )
                        Text(
                            text = if (isCountUp) stringResource(id = R.string.label_count_up) else stringResource(id = R.string.label_count_down),
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp).fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
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