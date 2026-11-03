package com.anidex.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Passes the OAuth callback to the app. MainActivity completes token exchange. */
class AuthCallbackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = Intent(this, com.anidex.MainActivity::class.java).apply {
            data = intent?.data
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(callback)
        finish()
    }
}
