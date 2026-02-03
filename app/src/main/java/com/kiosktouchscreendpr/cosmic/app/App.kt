package com.kiosktouchscreendpr.cosmic.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kiosktouchscreendpr.cosmic.app.AppState.Status
import com.kiosktouchscreendpr.cosmic.core.utils.HideSystemBars
import com.kiosktouchscreendpr.cosmic.presentation.auth.AuthViewRoot
import com.kiosktouchscreendpr.cosmic.presentation.home.HomeViewRoot
import com.kiosktouchscreendpr.cosmic.presentation.remotecontrol.RemoteControlScreen
import com.kiosktouchscreendpr.cosmic.presentation.settings.SettingsRoot
import com.kiosktouchscreendpr.cosmic.ui.theme.CosmicTheme

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */

@Composable
fun App() {
    val appViewModel = hiltViewModel<AppViewModel>()
    val state = appViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.value.status) {
        when (state.value.status) {
            Status.DISCONNECTED -> println("HearthBeat disconnected")
            Status.CONNECTING -> println("HearthBeat connecting")
            Status.CONNECTED -> println("HearthBeat connected")
        }
    }

    CosmicTheme {
        val navController = rememberNavController()

        val isLoggedIn = appViewModel.isLoggedIn
        val token = appViewModel.token
        val startDestination = when {
            isLoggedIn.isNullOrEmpty() -> Route.AppAuth
            token.isNullOrEmpty() -> Route.AppSettings
            else -> Route.AppHome
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavHost(
                navController = navController,
                startDestination = startDestination
            )
            HideSystemBars()
        }
    }
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: Route,
) {

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<Route.AppAuth> {
            AuthViewRoot(navController)
        }

        composable<Route.AppSettings> {
            SettingsRoot(navController)
        }

        composable<Route.AppHome> {
            HomeViewRoot(navController)
        }

        composable<Route.AppRemoteControl> {
            RemoteControlScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}