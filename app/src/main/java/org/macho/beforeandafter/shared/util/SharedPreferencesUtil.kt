package org.macho.beforeandafter.shared.util

import android.content.Context
import android.preference.PreferenceManager

object SharedPreferencesUtil {
    enum class Key(val string: String) {
        LATEST_WEIGHT("latestWeight"),
        LATEST_RATE("latestRate"),
        LATEST_WATCH_REWARDED_AD("latest_watch_rewarded_ad"),
        TIME_OF_LATEST_RECORD("time_of_latest_record")
    }
    fun getBoolean(context: Context, key: Key): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key.string, false)
    }
    fun setBoolean(context: Context, key: Key, value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key.string, value).apply()
    }

    fun getFloat(context: Context, key: Key): Float {
        return PreferenceManager.getDefaultSharedPreferences(context).getFloat(key.string, 0.0f)
    }

    fun setFloat(context: Context, key: Key, value: Float) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat(key.string, value).apply()
    }

    fun getLong(context: Context, key: Key): Long {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(key.string, 0)
    }

    fun setLong(context: Context, key: Key, value: Long) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(key.string, value).apply()
    }
}