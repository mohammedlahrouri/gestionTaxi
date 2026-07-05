package com.moham.taxi.data.online

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes

class GoogleDriveAuthManager(private val context: Context) {
    private val appDataScope = Scope(DriveScopes.DRIVE_APPDATA)

    fun getSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(appDataScope)
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    fun getCredential(): GoogleAccountCredential? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }
    }

    fun signOut(activity: Activity, onComplete: () -> Unit) {
        val client = getSignInClient()
        client.revokeAccess().addOnCompleteListener(activity) {
            client.signOut().addOnCompleteListener(activity) { onComplete() }
        }
    }
}
