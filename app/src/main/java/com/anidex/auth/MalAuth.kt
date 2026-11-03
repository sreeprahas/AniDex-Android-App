package com.anidex.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

/**
 * MAL OAuth 2.0 PKCE helper.
 * Set MAL_CLIENT_ID in local.properties/build config before using real account authorization.
 */
object MalAuth {
    const val CALLBACK = "anidex://oauth"
    private const val AUTH_URL = "https://myanimelist.net/v1/oauth2/authorize"

    fun createVerifier(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun challenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun openLogin(context: Context, clientId: String, verifier: String) {
        val uri = Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("code_challenge", challenge(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("redirect_uri", CALLBACK)
            .build()
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    }
}
