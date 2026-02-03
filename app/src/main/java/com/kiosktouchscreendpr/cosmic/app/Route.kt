package com.kiosktouchscreendpr.cosmic.app

import kotlinx.serialization.Serializable

sealed class Route {
    @Serializable
    data object AppAuth : Route()

    @Serializable
    data object AppSettings : Route()

    @Serializable
    data object AppHome : Route()

    @Serializable
    data object AppRemoteControl : Route()
}