package com.kiosktouchscreendpr.cosmic.presentation.home

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kiosktouchscreendpr.cosmic.app.Route
import com.kiosktouchscreendpr.cosmic.core.constant.AppConstant

@Composable
fun HomeViewRoot(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel<HomeViewModel>(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    val resetToHomePage = {
        webViewRef.value?.clearHistory()
        webViewRef.value?.loadUrl(state.baseUrl)
    }

    LaunchedEffect(Unit) {
        viewModel.resetEvent.collect {
            resetToHomePage()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.triggerRefresh {
            resetToHomePage()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.observeNetworks {
            resetToHomePage()
        }
    }
    
    // Periodic refresh setiap 1 menit untuk check schedule changes
    LaunchedEffect(Unit) {
        viewModel.startPeriodicRefresh {
            resetToHomePage()
        }
    }

    HomeView(
        state = state,
        webViewRef = webViewRef,
        onUrlChanged = { url ->
            viewModel.onUrlChanged(url)
        },
        onUserInteraction = {
            viewModel.onUserInteraction()
        },
        onHomeClick = {
            resetToHomePage()
        },
        onLogoutClick = {
            navController.navigate(Route.AppAuth) {
                popUpTo<Route.AppHome> {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Suppress("DEPRECATION")
@Composable
fun HomeView(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onUserInteraction: () -> Unit = {},
    onUrlChanged: (String) -> Unit = {},
    webViewRef: MutableState<WebView?> = remember { mutableStateOf(null) }
) {
    val baseUrl = state.baseUrl
    var progress by remember { mutableIntStateOf(0) }
    var tapCount by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf(baseUrl) }

    val interactionModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent()
                onUserInteraction()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(interactionModifier),
    ) {

        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewRef.value = this

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            Log.d("Home", "onPageStarted: $url")
                            currentUrl = url
                            onUrlChanged(url)
                            onUserInteraction()
                        }

                        override fun onPageFinished(
                            view: WebView, url: String
                        ) {
                            super.onPageFinished(view, url)
                            Log.d("Home", "onPageFinished: $url")
                            if (url == baseUrl) {
                                onEvent(HomeEvent.OnInitialUrl(true))
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            val newUrl = request.url.toString()
                            currentUrl = newUrl
                            onUrlChanged(newUrl)
                            return false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true && view != null) {
                                view.loadUrl("file:///android_asset/html/errorpage.html")
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(
                            view: WebView, newProgress: Int
                        ) {
                            super.onProgressChanged(view, newProgress)
                            progress = newProgress
                            Log.d("Home", "onProgressChanged: $progress")
                        }
                    }

                    setOnTouchListener { _, _ ->
                        onUserInteraction()
                        false
                    }

                    setInitialScale(100)
                    settings.apply {
                        builtInZoomControls = false
                        displayZoomControls = false
                        userAgentString = AppConstant.USER_AGENT
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        mediaPlaybackRequiresUserGesture = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    loadUrl(state.baseUrl)
                }
            },
            update = {
                if (state.baseUrl.isNotEmpty()) {
                    it.loadUrl(state.baseUrl)
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                modifier = Modifier
                    .height(50.dp)
                    .width(50.dp)
                    .align(Alignment.TopStart),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                ),
                onClick = {
                    onUserInteraction()
                    tapCount++
                    if (tapCount == 5) {
                        onLogoutClick()
                        tapCount = 0
                    }
                },
                interactionSource = remember { MutableInteractionSource() },
                content = {}
            )

            Button(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp),
                onClick = {
                    onHomeClick()
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black
                ),
            ) {
                Icon(
                    Icons.Rounded.Home,
                    contentDescription = null
                )
            }

            // Hidden refresh button (top-right corner, 5 taps)
            Button(
                modifier = Modifier
                    .height(50.dp)
                    .width(50.dp)
                    .align(Alignment.TopEnd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                ),
                onClick = {
                    tapCount++
                    if (tapCount == 5) {
                        onHomeClick() // Manual refresh
                        tapCount = 0
                    }
                },
                interactionSource = remember { MutableInteractionSource() },
                content = {}
            )
        }
        if (progress in 1..99) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 5.dp,
                        progress = progress / 100f
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading: $progress%", color = Color.White)
                }
            }
        }
    }
}