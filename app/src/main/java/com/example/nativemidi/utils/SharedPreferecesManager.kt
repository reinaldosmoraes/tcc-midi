package com.example.nativemidi.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferecesManager {

    companion object {
        val SHARED_PREFERENCES_NAME = "SHARED_PREFERENCES_NAME"
        val ACCENT_TRESHOLD = "ACCENT_TRESHOLD"
        val ACCENT_TRESHOLD_DEFAULT_VALUE = 20

        fun setAccentTreshold(context: Context, value: Int) {
            val preferences: SharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, 0)
            val editor = preferences.edit()
            editor.putInt(ACCENT_TRESHOLD, value)
            editor.commit()
        }

        fun getAccentTreshold(context: Context): Int {
            val preferences: SharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, 0)
            return preferences.getInt(ACCENT_TRESHOLD, ACCENT_TRESHOLD_DEFAULT_VALUE)
        }
    }

}