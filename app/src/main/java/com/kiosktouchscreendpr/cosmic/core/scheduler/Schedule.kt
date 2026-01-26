package com.kiosktouchscreendpr.cosmic.core.scheduler

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
interface Schedule {
    fun schedule(item: AlarmItem)
    fun cancel(item: AlarmItem)
}