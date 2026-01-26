package com.kiosktouchscreendpr.cosmic.di

import android.app.AlarmManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.PowerManager
import com.kiosktouchscreendpr.cosmic.core.apprequest.AppRequest
import com.kiosktouchscreendpr.cosmic.core.apprequest.AppRequestImpl
import com.kiosktouchscreendpr.cosmic.core.utils.Preference
import com.kiosktouchscreendpr.cosmic.core.utils.PreferenceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun providePowerManager(@ApplicationContext context: Context): PowerManager {
        return context.getSystemService(PowerManager::class.java)
    }

    @Provides
    @Singleton
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Provides
    @Singleton
    fun provideDevicePolicyManager(@ApplicationContext context: Context): DevicePolicyManager {
        return context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    @Provides
    @Singleton
    fun providesAlarmManager(@ApplicationContext context: Context): AlarmManager {
        return context.getSystemService(AlarmManager::class.java)
    }

    @Provides
    @Singleton
    fun providesConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun providePreference(sharedPreferences: SharedPreferences): Preference {
        return PreferenceImpl(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBinder {

    @Binds
    @Singleton
    abstract fun bindsAppRequest(
        impl: AppRequestImpl
    ): AppRequest

}