package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class AdminUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val role: String = "ADMIN"
)

class AuthManager(private val context: Context) {
    private val TAG = "AuthManager"

    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAuth initialization fallback: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<AdminUser?>(null)
    val currentUser: StateFlow<AdminUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        // Check current Firebase user or stored admin session
        val fbUser = auth?.currentUser
        if (fbUser != null) {
            _currentUser.value = AdminUser(
                uid = fbUser.uid,
                displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "Admin",
                email = fbUser.email ?: "",
                photoUrl = fbUser.photoUrl?.toString()
            )
        } else {
            // Check SharedPreferences for persistent session
            val prefs = context.getSharedPreferences("bcs_admin_auth", Context.MODE_PRIVATE)
            val savedEmail = prefs.getString("user_email", null)
            val savedName = prefs.getString("user_name", null)
            val savedUid = prefs.getString("user_uid", null)
            val savedPhoto = prefs.getString("user_photo", null)
            if (!savedEmail.isNullOrBlank() && !savedUid.isNullOrBlank()) {
                _currentUser.value = AdminUser(
                    uid = savedUid,
                    displayName = savedName ?: savedEmail.substringBefore("@"),
                    email = savedEmail,
                    photoUrl = savedPhoto
                )
            }
        }
    }

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    private fun resolveWebClientId(callerContext: Context): String {
        return try {
            val resId = callerContext.resources.getIdentifier("default_web_client_id", "string", callerContext.packageName)
            if (resId != 0) {
                callerContext.getString(resId)
            } else {
                "283902388926-ha4hrv291gg25db1quf8ufvfh37pia3b.apps.googleusercontent.com"
            }
        } catch (e: Exception) {
            "283902388926-ha4hrv291gg25db1quf8ufvfh37pia3b.apps.googleusercontent.com"
        }
    }

    suspend fun signInWithGoogle(
        callerContext: Context,
        serverClientId: String? = null
    ) {
        _isLoading.value = true
        _authError.value = null
        try {
            val activity = findActivity(callerContext) ?: (if (callerContext is Activity) callerContext else null)
            val targetContext = activity ?: callerContext
            val resolvedClientId = serverClientId ?: resolveWebClientId(targetContext)
            
            val credentialManager = CredentialManager.create(targetContext)

            // 1. First try the standard GetSignInWithGoogleOption (recommended for explicit user button tap)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = resolvedClientId)
                .build()

            var getCredentialResult = try {
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()
                credentialManager.getCredential(
                    request = request,
                    context = targetContext
                )
            } catch (e: NoCredentialException) {
                Log.d(TAG, "GetSignInWithGoogleOption returned NoCredentialException, trying GetGoogleIdOption fallback: ${e.message}")
                null
            }

            // 2. If GetSignInWithGoogleOption found no cached authorized accounts, use GetGoogleIdOption with filterByAuthorizedAccounts=false
            if (getCredentialResult == null) {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(resolvedClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                getCredentialResult = credentialManager.getCredential(
                    request = request,
                    context = targetContext
                )
            }

            val credential = getCredentialResult.credential
            if (credential is CustomCredential && 
                (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL || 
                 credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL)) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                var signedInUser: AdminUser? = null

                // Attempt Firebase Auth sign-in if available
                if (auth != null && idToken.isNotBlank()) {
                    try {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth?.signInWithCredential(firebaseCredential)?.await()
                        val fbUser = authResult?.user

                        if (fbUser != null) {
                            signedInUser = AdminUser(
                                uid = fbUser.uid,
                                displayName = fbUser.displayName 
                                    ?: googleIdTokenCredential.displayName 
                                    ?: fbUser.email?.substringBefore("@") 
                                    ?: "Admin",
                                email = fbUser.email ?: googleIdTokenCredential.id,
                                photoUrl = fbUser.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString()
                            )
                        }
                    } catch (fbEx: Throwable) {
                        Log.w(TAG, "Firebase Auth with Google credential returned error, falling back to ID token payload: ${fbEx.message}")
                    }
                }

                // Fallback to Google ID token profile directly if Firebase Auth instance was skipped or unlinked
                if (signedInUser == null) {
                    val userEmail = googleIdTokenCredential.id
                    val userName = googleIdTokenCredential.displayName ?: userEmail.substringBefore("@").ifBlank { "Admin" }
                    signedInUser = AdminUser(
                        uid = "google_${userEmail.hashCode()}",
                        displayName = userName,
                        email = userEmail,
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                }

                saveUserSession(signedInUser)
                _currentUser.value = signedInUser
            } else {
                _authError.value = "Unrecognized credential type: ${credential.javaClass.simpleName}"
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User canceled Google Sign-In dialog")
            _authError.value = null
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credentials found on device: ${e.message}")
            _authError.value = "Google reports 'No credentials available' for this app build. In Firebase Console, make sure SHA-1 certificate is added for package 'com.aistudio.bcsdiaryadmin.app'."
        } catch (e: GetCredentialUnsupportedException) {
            Log.e(TAG, "Google Credentials unsupported: ${e.message}")
            _authError.value = "Google Credentials unsupported on this device: ${e.message}"
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(TAG, "Provider configuration error: ${e.message}")
            _authError.value = "Google Play Services configuration error: ${e.message}"
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: ${e.message}")
            _authError.value = "Google Sign-In failed: ${e.message}"
        } catch (e: Throwable) {
            Log.e(TAG, "Google Sign-In unexpected error: ${e.message}", e)
            _authError.value = "Sign in error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    fun signInAsAdmin(email: String = "admin@bcsdiary.com", name: String = "BCS Admin") {
        val adminUser = AdminUser(
            uid = "admin_${System.currentTimeMillis() % 10000}",
            displayName = name,
            email = email
        )
        saveUserSession(adminUser)
        _currentUser.value = adminUser
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Throwable) {
            Log.w(TAG, "Sign out issue: ${e.message}")
        }
        clearUserSession()
        _currentUser.value = null
    }

    fun clearError() {
        _authError.value = null
    }

    private fun saveUserSession(user: AdminUser) {
        val prefs = context.getSharedPreferences("bcs_admin_auth", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_uid", user.uid)
            .putString("user_name", user.displayName)
            .putString("user_email", user.email)
            .putString("user_photo", user.photoUrl)
            .apply()
    }

    private fun clearUserSession() {
        val prefs = context.getSharedPreferences("bcs_admin_auth", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
