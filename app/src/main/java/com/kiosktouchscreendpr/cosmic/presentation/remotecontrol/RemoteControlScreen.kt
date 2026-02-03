package com.kiosktouchscreendpr.cosmic.presentation.remotecontrol

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.core.constant.AppConstant
import com.kiosktouchscreendpr.cosmic.core.utils.Preference
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Remote Control Screen
 * Allows device to be controlled remotely from CMS web interface
 * 
 * Features:
 * - Start/Stop remote control session
 * - Display connection status
 * - Show active duration
 * - Connection metrics
 * - Permission management
 * 
 * Backend Integration:
 * - Relay Server: wss://kiosk.mugshot.dev/remote-control-ws
 * - Device authenticates with token from registration
 * - Sends screen capture frames to relay
 * - Receives input commands from CMS viewers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(
    viewModel: RemoteControlViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val remoteControlState by viewModel.remoteControlState.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    
    // Get shared preferences for device credentials
    val preference = remember {
        context.getSharedPreferences("cosmic_prefs", android.content.Context.MODE_PRIVATE)
            .let { prefs ->
                object : Preference {
                    override fun <T> get(key: String, defaultValue: T): T {
                        return when (defaultValue) {
                            is String -> prefs.getString(key, defaultValue) as T
                            is Int -> prefs.getInt(key, defaultValue) as T
                            is Boolean -> prefs.getBoolean(key, defaultValue) as T
                            is Long -> prefs.getLong(key, defaultValue) as T
                            else -> defaultValue
                        }
                    }
                    override fun <T> set(key: String, value: T) {
                        prefs.edit().apply {
                            when (value) {
                                is String -> putString(key, value)
                                is Int -> putInt(key, value)
                                is Boolean -> putBoolean(key, value)
                                is Long -> putLong(key, value)
                            }
                            apply()
                        }
                    }
                }
            }
    }
    
    // Get device credentials
    val deviceToken = preference.get(AppConstant.REMOTE_TOKEN, "")
    val deviceId = preference.get(AppConstant.REMOTE_ID, "")
    
    // Relay server URL (production)
    val relayServerUrl = remember {
        val baseUrl = BuildConfig.WEBVIEW_BASEURL.takeIf { it.isNotBlank() } ?: "https://kiosk.mugshot.dev"
        baseUrl.replace("https://", "wss://").replace("http://", "ws://") + "/remote-control-ws"
    }
    
    // Active duration timer
    var activeDuration by remember { mutableStateOf(0L) }
    var startTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(remoteControlState) {
        if (remoteControlState is RemoteControlState.Active) {
            if (startTime == 0L) {
                startTime = System.currentTimeMillis()
            }
            while (remoteControlState is RemoteControlState.Active) {
                activeDuration = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        } else {
            startTime = 0L
            activeDuration = 0L
        }
    }
    
    // Check if accessibility service is enabled
    val isAccessibilityEnabled = remember(remoteControlState) {
        viewModel.isAccessibilityServiceEnabled(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Remote Control",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            StatusCard(
                remoteControlState = remoteControlState,
                connectionStatus = connectionStatus,
                activeDuration = activeDuration
            )
            
            // Device Info Card
            DeviceInfoCard(
                deviceId = deviceId,
                deviceToken = deviceToken,
                relayServerUrl = relayServerUrl
            )
            
            // Permissions Warning
            if (!isAccessibilityEnabled && remoteControlState !is RemoteControlState.Idle) {
                PermissionWarningCard(
                    onOpenSettings = {
                        viewModel.openAccessibilitySettings(context)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action Buttons
            when (remoteControlState) {
                is RemoteControlState.Idle -> {
                    StartRemoteControlButton(
                        deviceId = deviceId,
                        deviceToken = deviceToken,
                        relayServerUrl = relayServerUrl,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        onStart = { id, token, url ->
                            viewModel.startRemoteControl(
                                context = context,
                                deviceId = id,
                                authToken = token,
                                relayServerUrl = url
                            )
                        },
                        onOpenAccessibilitySettings = {
                            viewModel.openAccessibilitySettings(context)
                        }
                    )
                }
                
                is RemoteControlState.Starting -> {
                    ConnectingIndicator()
                }
                
                is RemoteControlState.Active -> {
                    StopRemoteControlButton(
                        onStop = {
                            viewModel.stopRemoteControl(context)
                        }
                    )
                }
                
                is RemoteControlState.Error -> {
                    ErrorDisplay(
                        message = (remoteControlState as RemoteControlState.Error).message,
                        onRetry = {
                            viewModel.startRemoteControl(
                                context = context,
                                deviceId = deviceId,
                                authToken = deviceToken,
                                relayServerUrl = relayServerUrl
                            )
                        }
                    )
                }
            }
            
            // Help Section
            HelpCard()
        }
    }
}

@Composable
private fun StatusCard(
    remoteControlState: RemoteControlState,
    connectionStatus: ConnectionStatus,
    activeDuration: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (remoteControlState) {
                is RemoteControlState.Active -> MaterialTheme.colorScheme.primaryContainer
                is RemoteControlState.Starting -> MaterialTheme.colorScheme.secondaryContainer
                is RemoteControlState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                StatusIndicator(
                    state = remoteControlState,
                    status = connectionStatus
                )
            }
            
            Divider()
            
            // Status text
            Text(
                text = when (remoteControlState) {
                    is RemoteControlState.Idle -> "🔴 Remote Control Inactive"
                    is RemoteControlState.Starting -> "🟡 Connecting to relay server..."
                    is RemoteControlState.Active -> "🟢 Remote Control Active - Viewers can control this device"
                    is RemoteControlState.Error -> "❌ Error: ${(remoteControlState as RemoteControlState.Error).message}"
                },
                style = MaterialTheme.typography.bodyLarge
            )
            
            // Duration for active state
            if (remoteControlState is RemoteControlState.Active && activeDuration > 0) {
                Text(
                    text = "Active for: ${formatDuration(activeDuration)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    state: RemoteControlState,
    status: ConnectionStatus
) {
    val (color, icon) = when {
        state is RemoteControlState.Active -> Pair(Color.Green, Icons.Default.CheckCircle)
        state is RemoteControlState.Starting -> Pair(Color(0xFFFFA726), Icons.Default.Refresh)
        state is RemoteControlState.Error -> Pair(Color.Red, Icons.Default.Error)
        else -> Pair(Color.Gray, Icons.Default.Circle)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun DeviceInfoCard(
    deviceId: String,
    deviceToken: String,
    relayServerUrl: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Device Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Divider()
            
            InfoRow(label = "Device ID", value = deviceId.ifEmpty { "Not registered" })
            InfoRow(
                label = "Token", 
                value = if (deviceToken.isNotEmpty()) {
                    "${deviceToken.take(8)}...${deviceToken.takeLast(8)}"
                } else {
                    "Not registered"
                }
            )
            InfoRow(label = "Relay Server", value = relayServerUrl)
            
            if (deviceId.isEmpty() || deviceToken.isEmpty()) {
                Text(
                    text = "⚠️ Device not registered. Please go to Settings to register.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PermissionWarningCard(
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Text(
                text = "Accessibility Service must be enabled to receive input commands from viewers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Accessibility Settings")
            }
        }
    }
}

@Composable
private fun StartRemoteControlButton(
    deviceId: String,
    deviceToken: String,
    relayServerUrl: String,
    isAccessibilityEnabled: Boolean,
    onStart: (String, String, String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val isValid = deviceId.isNotEmpty() && deviceToken.isNotEmpty()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                if (isValid) {
                    onStart(deviceId, deviceToken, relayServerUrl)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Remote Control",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        if (!isAccessibilityEnabled) {
            Text(
                text = "Note: Accessibility Service should be enabled for full functionality",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ConnectingIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Connecting to relay server...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Please wait",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun StopRemoteControlButton(
    onStop: () -> Unit
) {
    Button(
        onClick = onStop,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Stop Remote Control",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ErrorDisplay(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Connection Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry Connection")
            }
        }
    }
}

@Composable
private fun HelpCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "How It Works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "1. Tap 'Start Remote Control' to enable remote access",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "2. Device connects to relay server securely",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "3. CMS users can view screen and send input commands",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "4. Tap 'Stop' to disconnect and disable remote access",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "⚠️ Important: Keep the app running for remote control to work",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return when {
        hours > 0 -> String.format("%dh %dm %ds", hours, minutes, secs)
        minutes > 0 -> String.format("%dm %ds", minutes, secs)
        else -> String.format("%ds", secs)
    }
}
