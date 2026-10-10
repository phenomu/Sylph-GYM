package com.phenom.gym.data.remote

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Wraps the Android Credential Manager + Google ID token flow.
 * Returns a Google ID token (JWT) that we send to /api/mobile/auth/google/verify.
 *
 * Strategy:
 *  1. Try GetGoogleIdOption (finds existing Google accounts on device).
 *  2. If NoCredentialException, fall back to GetSignInWithGoogleOption
 *     which always shows the Google account picker (including "Add account").
 */
class GoogleSignInHelper(
    context: Context,
    private val serverClientId: String
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun getIdToken(activityContext: Context): Result<String> = runCatching {
        // Step 1: show Google account picker — list ALL Google accounts on device
        // filterByAuthorizedAccounts(false) → show all accounts, not just previously authorized
        // autoSelectEnabled(false) → ALWAYS show picker, user chooses account
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val cred = try {
            credentialManager.getCredential(activityContext, request).credential
        } catch (e: NoCredentialException) {
            // Step 2: fallback — show Google account picker (incl. "Add account")
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()
            val fallbackReq = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            credentialManager.getCredential(activityContext, fallbackReq).credential
        }

        val googleId = GoogleIdTokenCredential.createFrom(cred.data)
        googleId.idToken
    }.recoverCatching { e ->
        if (e is GetCredentialException) {
            throw ApiException(0, "Google sign-in gagal: ${e.message}")
        }
        throw e
    }
}
