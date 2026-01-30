package com.kiosktouchscreendpr.cosmic.core.utils

import android.util.Log
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
fun getDeviceIP(): String? {
    return try {
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .firstOrNull { isValidIpAddr(it) }
            ?.hostAddress
    } catch (e: Exception) {
        Log.d("getDeviceIp", "Error retrieving IP: $e")
        null
    }
}

fun isValidIpAddr(address: InetAddress): Boolean =
    !address.isLoopbackAddress && address is Inet4Address

fun formatLink(ip: String): String {
    return ip.takeIf { it.isNotBlank() } ?: "localhost"
}