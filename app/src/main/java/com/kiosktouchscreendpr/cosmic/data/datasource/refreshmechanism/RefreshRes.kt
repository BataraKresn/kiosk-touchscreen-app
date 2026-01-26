package com.kiosktouchscreendpr.cosmic.data.datasource.refreshmechanism

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
sealed interface RefreshRes {
    data class Triggered(val token: String) : RefreshRes
    data class Error(val message: String) : RefreshRes
}