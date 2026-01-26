package com.kiosktouchscreendpr.cosmic.presentation.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kiosktouchscreendpr.cosmic.app.Route
import java.util.Calendar

@Composable
fun SettingsRoot(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showPowerOffPicker by remember { mutableStateOf(false) }
    var showPowerOnPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate(Route.AppHome) {
                popUpTo(Route.AppSettings) {
                    inclusive = true
                }
            }
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
        onShowPowerOnPicker = { showPowerOnPicker = true }
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
    context: Context = LocalContext.current
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 50.dp, max = 200.dp),
            value = state.token,
            onValueChange = { onAction(SettingsEvent.OnTokenChanged(it)) },
            label = { Text("Enter Token") },
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