package com.zenmind.kotlin.features.authentication.data.services

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.zenmind.kotlin.R

class GoogleSignInService {
    /** null means the user dismissed Google's account picker. */
    suspend fun getIdToken(context: Context): String? {
        val option = GetSignInWithGoogleOption.Builder(
            context.getString(R.string.google_web_client_id)
        ).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val credential = try {
            CredentialManager.create(context).getCredential(context, request).credential
        } catch (_: GetCredentialCancellationException) {
            return null
        } catch (exception: GetCredentialException) {
            throw IllegalStateException("No se pudo conectar con Google. Inténtalo de nuevo.", exception)
        }

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Google no devolvió una cuenta válida.")
        }

        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (exception: GoogleIdTokenParsingException) {
            throw IllegalStateException("Google no devolvió una cuenta válida.", exception)
        } catch (exception: IllegalArgumentException) {
            throw IllegalStateException("Google no devolvió una cuenta válida.", exception)
        }
    }
}
