package com.kiosktouchscreendpr.cosmic.presentation.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.app.Route
import com.kiosktouchscreendpr.cosmic.core.constant.AppConstant
import com.kiosktouchscreendpr.cosmic.presentation.remotecontrol.RemoteControlViewModel
import java.util.Calendar

@Composable
fun SettingsRoot(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remoteControlViewModel: RemoteControlViewModel = hiltViewModel()

    val prefs = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    val relayServerUrl = remember {
        val baseUrl = BuildConfig.WEBVIEW_BASEURL.takeIf { it.isNotBlank() } ?: "https://kiosk.mugshot.dev"
        baseUrl.replace("https://", "wss://").replace("http://", "ws://") + "/remote-control-ws"
    }

    var autoStartRequested by remember { mutableStateOf(false) }
    var shouldNavigateHome by remember { mutableStateOf(false) }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.e("SettingsView", "🎥🎥🎥 Screen capture result: ${result.resultCode} (RESULT_OK=${Activity.RESULT_OK})")
        android.util.Log.e("SettingsView", "📦 Result data: ${result.data}")
        if (result.resultCode == Activity.RESULT_OK) {
            val deviceId = prefs.getString(AppConstant.REMOTE_ID, "") ?: ""
            val deviceToken = prefs.getString(AppConstant.REMOTE_TOKEN, "") ?: ""
            android.util.Log.e("SettingsView", "📱 Got device prefs - ID: $deviceId, Token: $deviceToken")
            if (deviceId.isNotEmpty() && deviceToken.isNotEmpty()) {
                android.util.Log.e("SettingsView", "🚀🚀🚀 Starting remote control with: $relayServerUrl 🚀🚀🚀")
                remoteControlViewModel.startRemoteControl(
                    context = context,
                    deviceId = deviceId,
                    authToken = deviceToken,
                    relayServerUrl = relayServerUrl
                )
                android.util.Log.e("SettingsView", "📹📹📹 Calling onScreenCapturePermissionGranted with resultCode=${result.resultCode}, data=${result.data}")
                remoteControlViewModel.onScreenCapturePermissionGranted(
                    context = context,
                    resultCode = result.resultCode,
                    data = result.data
                )
                android.util.Log.e("SettingsView", "✅ onScreenCapturePermissionGranted called")
                android.util.Log.e("SettingsView", "✅✅✅ PERMISSION GRANTED - Setting shouldNavigateHome=true ✅✅✅")
                shouldNavigateHome = true
            } else {
                android.util.Log.e("SettingsView", "Device not registered - missing ID or Token")
                Toast.makeText(
                    context,
                    "Remote Control belum terdaftar. Coba submit ulang.",
                    Toast.LENGTH_SHORT
                ).show()
                shouldNavigateHome = true
            }
        } else {
            android.util.Log.w("SettingsView", "Screen capture permission denied")
            Toast.makeText(
                context,
                "Izin screen capture dibutuhkan untuk Remote Control.",
                Toast.LENGTH_SHORT
            ).show()
            shouldNavigateHome = true
        }
    }

    var showPowerOffPicker by remember { mutableStateOf(false) }
    var showPowerOnPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        android.util.Log.e("SettingsView", "🔵 LaunchedEffect triggered - state.isSuccess=${state.isSuccess}")
        if (state.isSuccess) {
            if (!autoStartRequested) {
                android.util.Log.e("SettingsView", "✅✅✅ Submit success! Auto-requesting screen capture permission ✅✅✅")
                autoStartRequested = true
                val deviceId = prefs.getString(AppConstant.REMOTE_ID, "") ?: ""
                val deviceToken = prefs.getString(AppConstant.REMOTE_TOKEN, "") ?: ""
                android.util.Log.e("SettingsView", "📱 Stored prefs - ID: $deviceId, Token: $deviceToken")
                if (deviceId.isNotEmpty() && deviceToken.isNotEmpty()) {
                    android.util.Log.e("SettingsView", "🎬 Creating MediaProjectionManager...")
                    val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val intent = manager.createScreenCaptureIntent()
                    android.util.Log.e("SettingsView", "📺 createScreenCaptureIntent() returned: $intent")
                    android.util.Log.e("SettingsView", "🚀🚀🚀 LAUNCHING screenCaptureLauncher NOW 🚀🚀🚀")
                    screenCaptureLauncher.launch(intent)
                    android.util.Log.e("SettingsView", "✅ screenCaptureLauncher.launch() called - WAITING FOR PERMISSION RESULT...")
                    // DO NOT navigate yet - wait for permission result in launcher callback
                } else {
                    android.util.Log.e("SettingsView", "❌ Cannot start remote - prefs empty (ID: '$deviceId', Token: '$deviceToken')")
                    Toast.makeText(
                        context,
                        "Remote Control belum terdaftar. Coba submit ulang.",
                        Toast.LENGTH_SHORT
                    ).show()
                    shouldNavigateHome = true
                }
            } else {
                android.util.Log.e("SettingsView", "⚠️ autoStartRequested already true, skipping screen capture launch")
            }
        }
    }

    // Separate LaunchedEffect for navigation - only navigate when permission result comes back
    LaunchedEffect(shouldNavigateHome) {
        if (shouldNavigateHome) {
            android.util.Log.e("SettingsView", "📱📱📱 Permission result received - Navigating to Home screen...")
            navController.navigate(Route.AppHome) {
                popUpTo(Route.AppSettings) {
                    inclusive = true
                }
            }
            android.util.Log.e("SettingsView", "✅ Navigation to Home completed")
        }
    }

    LaunchedEffect(state.errorMessage) {
        if (!state.errorMessage.isNullOrEmpty()) {
            Toast.makeText(
                context,
                state.errorMessage ?: "",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    SettingsScreen(
        state = state,
        onAction = viewModel::onEvent,
        onShowPowerOffPicker = { showPowerOffPicker = true },
        onShowPowerOnPicker = { showPowerOnPicker = true },
        navController = navController
    )

    DialogManager(
        showPowerOffPicker = showPowerOffPicker,
        showPowerOnPicker = showPowerOnPicker,
        powerOffTime = state.powerOffTime,
        powerOnTime = state.powerOnTime,
        onPowerOffTimeSet = {
            viewModel.onEvent(SettingsEvent.OnPowerOffTimeChanged(it))
            showPowerOffPicker = false
        },
        onPowerOnTimeSet = {
            viewModel.onEvent(SettingsEvent.OnPowerOnTimeChanged(it))
            showPowerOnPicker = false
        },
        onDismissPowerOff = { showPowerOffPicker = false },
        onDismissPowerOn = { showPowerOnPicker = false }
    )
}

@Composable
private fun DialogManager(
    showPowerOffPicker: Boolean,
    showPowerOnPicker: Boolean,
    powerOffTime: Pair<Int, Int>?,
    powerOnTime: Pair<Int, Int>?,
    onPowerOffTimeSet: (Pair<Int, Int>) -> Unit,
    onPowerOnTimeSet: (Pair<Int, Int>) -> Unit,
    onDismissPowerOff: () -> Unit,
    onDismissPowerOn: () -> Unit
) {
    if (showPowerOffPicker) {
        TimePickerDialogWrapper(
            initialTime = powerOffTime,
            onConfirm = onPowerOffTimeSet,
            onDismiss = onDismissPowerOff
        )
    }

    if (showPowerOnPicker) {
        TimePickerDialogWrapper(
            initialTime = powerOnTime,
            onConfirm = onPowerOnTimeSet,
            onDismiss = onDismissPowerOn
        )
    }
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsEvent) -> Unit,
    onShowPowerOffPicker: () -> Unit,
    onShowPowerOnPicker: () -> Unit,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Token Input dengan Generate Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Display Name",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp, max = 200.dp),
                value = state.token,
                onValueChange = { onAction(SettingsEvent.OnTokenChanged(it)) },
                label = { Text("Enter Display Name") },
                placeholder = { Text("Lobby TV") },
                supportingText = {
                    Text(
                        text = "Nama ini digunakan untuk URL: /display/${state.token.ifEmpty { "DISPLAY" }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                shape = MaterialTheme.shapes.medium,
                leadingIcon = {
                    val token = state.token
                    if (token.isEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Lock",
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.clickable {
                                onAction(SettingsEvent.OnTokenChanged(""))
                            }
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                )
            )
            
            // Generate Token Button
            TextButton(
                onClick = { onAction(SettingsEvent.OnRefreshTokens) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(if (state.isLoadingTokens) "Loading display CMS..." else "🔄 Ambil Display dari CMS")
            }

            if (!state.tokenLoadError.isNullOrBlank()) {
                Text(
                    text = state.tokenLoadError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (state.availableTokens.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    state.availableTokens.forEach { tokenItem ->
                        TextButton(
                            onClick = { onAction(SettingsEvent.OnSelectToken(tokenItem)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tokenItem)
                        }
                    }
                }
            }
        }

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 50.dp, max = 200.dp),
            value = state.timeout,
            onValueChange = { onAction(SettingsEvent.OnTimeoutChanged(it)) },
            label = { Text("Enter Timeout") },
            shape = MaterialTheme.shapes.medium,
            leadingIcon = {
                val timeout = state.timeout
                if (timeout.isEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Lock",
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.clickable { onAction(SettingsEvent.OnTimeoutChanged("")) }
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            )
        )

        SchedulePowerControlDisplay(
            powerOffTime = state.powerOffTime,
            powerOnTime = state.powerOnTime,
            onPowerOffTimeClick = onShowPowerOffPicker,
            onPowerOnTimeClick = onShowPowerOnPicker
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    onAction(SettingsEvent.OnSubmit)
                    (context as? Activity)?.startLockTask()
                },
                modifier = Modifier.padding(10.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Text("Submit")
            }

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(top = 10.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Text("Settings")
            }
        }
        
        Text(
            text = "App Version: 1.0.0",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun SchedulePowerControlDisplay(
    powerOffTime: Pair<Int, Int>?,
    powerOnTime: Pair<Int, Int>?,
    onPowerOffTimeClick: () -> Unit,
    onPowerOnTimeClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Power Off", style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onPowerOffTimeClick) {
                Text(
                    text = powerOffTime?.let {
                        String.format("%02d:%02d", it.first, it.second)
                    } ?: "Set Time"
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Power On", style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onPowerOnTimeClick) {
                Text(
                    text = powerOnTime?.let {
                        String.format("%02d:%02d", it.first, it.second)
                    } ?: "Set Time"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogWrapper(
    initialTime: Pair<Int, Int>?,
    onConfirm: (Pair<Int, Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val currentTime = Calendar.getInstance()

    val initialHour = initialTime?.first ?: currentTime.get(Calendar.HOUR_OF_DAY)
    val initialMinute = initialTime?.second ?: currentTime.get(Calendar.MINUTE)

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    TimePickerDialog(
        onDismiss = onDismiss,
        onConfirm = { onConfirm(Pair(timePickerState.hour, timePickerState.minute)) }
    ) {
        TimePicker(state = timePickerState)
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        text = { content() }
    )
}