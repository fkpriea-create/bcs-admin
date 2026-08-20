package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.local.entity.UserEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

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
            if (!savedEmail.isNullOrBlank() && !savedUid.isNullOrBlank()) {
                _currentUser.value = AdminUser(
                    uid = savedUid,
                    displayName = savedName ?: savedEmail.substringBefore("@"),
                    email = savedEmail
                )
            }
        }
    }

    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String = "283902388926-ha4hrv291gg25db1quf8ufvfh37pia3b.apps.googleusercontent.com"
    ) {
        _isLoading.value = true
        _authError.value = null
        try {
            val credentialManager = CredentialManager.create(context)

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                if (auth != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth?.signInWithCredential(firebaseCredential)?.await()
                    val fbUser = authResult?.user

                    if (fbUser != null) {
                        val adminUser = AdminUser(
                            uid = fbUser.uid,
                            displayName = fbUser.displayName ?: googleIdTokenCredential.displayName ?: fbUser.email?.substringBefore("@") ?: "Admin",
                            email = fbUser.email ?: googleIdTokenCredential.id,
                            photoUrl = fbUser.photoUrl?.toString()
                        )
                        saveUserSession(adminUser)
                        _currentUser.value = adminUser
                    } else {
                        // Fallback user from Google Token
                        val adminUser = AdminUser(
                            uid = googleIdTokenCredential.id,
                            displayName = googleIdTokenCredential.displayName ?: "Admin User",
                            email = googleIdTokenCredential.id
                        )
                        saveUserSession(adminUser)
                        _currentUser.value = adminUser
                    }
                } else {
                    val adminUser = AdminUser(
                        uid = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: "Admin User",
                        email = googleIdTokenCredential.id
                    )
                    saveUserSession(adminUser)
                    _currentUser.value = adminUser
                }
            } else {
                _authError.value = "Unrecognized credential type returned from Google Sign-In"
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: ${e.message}")
            _authError.value = "Google Sign-In prompt unavailable or canceled: ${e.localizedMessage}"
        } catch (e: Throwable) {
            Log.e(TAG, "Google Sign-In error: ${e.message}")
            _authError.value = e.message ?: "Sign in failed"
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
            .apply()
    }

    private fun clearUserSession() {
        val prefs = context.getSharedPreferences("bcs_admin_auth", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
