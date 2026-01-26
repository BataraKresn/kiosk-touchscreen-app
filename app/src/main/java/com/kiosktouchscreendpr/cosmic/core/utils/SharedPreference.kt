package com.kiosktouchscreendpr.cosmic.core.utils

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */

interface Preference {
    fun get(key: String, defaultValue: String?): String?
    fun set(key: String, value: String)
    fun remove(key: String)
}

class PreferenceImpl(private val sharedPreferences: SharedPreferences) : Preference {
    override fun get(key: String, defaultValue: String?): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    override fun set(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }

    override fun remove(key: String) {
        sharedPreferences.edit { remove(key) }
    }
}