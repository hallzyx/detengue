package com.hallzyx.detengue.core.prefs

import android.content.Context
import androidx.core.content.edit

/** Preferencias mínimas locales. Nada sale del dispositivo. */
class AppPrefs(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    private companion object {
        const val FILE = "detengue_prefs"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
