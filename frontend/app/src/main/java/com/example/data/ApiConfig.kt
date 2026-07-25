package com.example.data

import android.content.Context

/**
 * Global API configurations for the Face Recognition and Liveness Verification system.
 */
object ApiConfig {
    const val DEFAULT_BASE_URL: String = "https://face-recognition-production-7a69.up.railway.app/"
    private const val PREFS_NAME = "api_config_prefs"
    private const val KEY_BASE_URL = "base_url"

    private var _overrideUrl: String? = null

    /**
     * Initialize ApiConfig with context to load persistent URL.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL)
        if (!savedUrl.isNullOrBlank()) {
            _overrideUrl = savedUrl
        }
    }

    /**
     * Update base URL and persist in SharedPreferences.
     */
    fun updateBaseUrl(context: Context?, newUrl: String) {
        var url = newUrl.trim()
        if (url.isBlank()) {
            url = DEFAULT_BASE_URL
        }
        _overrideUrl = url
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_BASE_URL, url)
            ?.apply()
    }

    /**
     * The backend API server base URL.
     * Edit this variable to update your backend deployment URL for development or production environments.
     */
    var BASE_URL: String
        get() {
            var url = (_overrideUrl ?: DEFAULT_BASE_URL).trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            if (!url.endsWith("/")) {
                url = "$url/"
            }
            return url
        }
        set(value) {
            _overrideUrl = value
        }
}
