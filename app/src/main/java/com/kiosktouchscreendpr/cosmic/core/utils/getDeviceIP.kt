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
        val tailscaleInterface = NetworkInterface.getNetworkInterfaces().toList()
            .firstOrNull { it.name == "tun0" && it.isUp }

        tailscaleInterface?.inetAddresses?.asSequence()?.firstOrNull { isValidIpAddr(it) }
            ?.hostAddress
            ?.also { return it }

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
    return if (ip.isNotBlank()) {
        "http://$ip:5800/vnc.html?autoconnect=true&show_dot=true&&host=$ip&port=5900"
    } else {
        "http://10.0.2.2:5800/vnc.html?autoconnect=true&show_dot=true&&host=10.0.2.2&port=5900"
    }
}