package com.kiosktouchscreendpr.cosmic.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kiosktouchscreendpr.cosmic.app.Route
import com.kiosktouchscreendpr.cosmic.ui.theme.CosmicTheme

@Composable
fun AuthViewRoot(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel<AuthViewModel>()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            navController.navigate(Route.AppSettings) {
                popUpTo<Route.AppAuth> { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            Toast.makeText(context, state.errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    AuthView(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun AuthView(
    state: AuthState,
    onEvent: (AuthEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Enter Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(24.dp))

        PasswordBoxes(
            otpCode = state.password,
            otpLength = 6
        )

        Spacer(Modifier.height(16.dp))

        NumberPad(
            onNumberClick = { number ->
                if (state.password.length < 6) {
                    onEvent(AuthEvent.OnPasswordChange(state.password + number))
                }
            },
            onDeleteClick = {
                if (state.password.isNotEmpty()) {
                    onEvent(AuthEvent.OnPasswordChange(state.password.dropLast(1)))
                }
            },
            onSubmit = {
                onEvent(AuthEvent.OnSubmit)
            }
        )

    }
}


@Composable
fun NumberPad(
    onDeleteClick: () -> Unit = {},
    onNumberClick: (String) -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    val numbers = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf("⌫", "0", "OK")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        numbers.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                row.forEach { number ->
                    when (number) {
                        "⌫" -> BackspaceKey(onDeleteClick)
                        "" -> EmptyKey()
                        "OK" -> Submit(onSubmit)
                        else -> NumberKey(number, onNumberClick)
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordBoxes(
    otpCode: String,
    otpLength: Int = 6,
    obscured: Boolean = true
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 32.dp)
    ) {
        for (i in 0 until otpLength) {
            PasswordDigitBox(
                value = otpCode.getOrNull(i)?.toString() ?: "",
                isFilled = i < otpCode.length,
                isFocused = i == otpCode.length,
                isObscured = obscured
            )
        }
    }
}

@Composable
fun PasswordDigitBox(
    value: String,
    isFilled: Boolean,
    isFocused: Boolean,
    isObscured: Boolean = false
) {
    val borderColor = when {
        isFocused -> MaterialTheme.colorScheme.primary
        isFilled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline
    }

    val displayText = if (isObscured && value.isNotEmpty()) "●" else value

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 40.dp, height = 48.dp)
        ) {
            Text(
                text = displayText,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .width(40.dp)
                .height(2.dp)
                .background(borderColor)
        )
    }
}


@Composable
fun NumberKey(number: String, onClick: (String) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .clickable { onClick(number) }
    ) {
        Text(
            text = number,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun BackspaceKey(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Default.Backspace,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun Submit(onClick: () -> Unit){
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = "OK",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

}


@Composable
fun EmptyKey() {
    Box(
        modifier = Modifier.size(64.dp)
    )
}


@Preview(showBackground = true)
@Composable
private fun Preview() {
    CosmicTheme {
        AuthView(
            state = AuthState(),
            onEvent = {}
        )
    }
}