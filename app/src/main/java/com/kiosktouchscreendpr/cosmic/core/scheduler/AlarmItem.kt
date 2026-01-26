package com.kiosktouchscreendpr.cosmic.core.scheduler

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */

data class AlarmItem(
    val hour: Int,
    val minute: Int,
    val type: AlarmType,
)

enum class AlarmType {
    LOCK,
    WAKE,
}