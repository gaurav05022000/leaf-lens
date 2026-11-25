package com.example.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.example.R
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.data.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val auth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = LoginUiState.Error("Email and password cannot be empty")
            return
        }
        val currentAuth = try { FirebaseAuth.getInstance() } catch(e: Exception) { null }
        if (currentAuth == null || currentAuth.app.options.projectId == "MY_FIREBASE_PROJECT_ID") {
            _uiState.value = LoginUiState.Error("Firebase is not configured. Please add your credentials in Settings -> Secrets.")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val currentAuthInstance = currentAuth ?: return@launch
                currentAuthInstance.signInWithEmailAndPassword(email, pass).await()
                currentAuthInstance.currentUser?.uid?.let { uid ->
                    SubscriptionManager.syncUser(uid)
                    com.example.util.AnalyticsHelper.setUserId(uid)
                    com.example.util.AnalyticsHelper.logEvent("login_email")
                }
                ScanHistoryManager.syncWithFirestore()
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                val msg = when (e) {
                    is FirebaseAuthInvalidUserException -> "User not found. Would you like to sign up?"
                    is FirebaseAuthInvalidCredentialsException -> "Invalid credentials."
                    else -> e.localizedMessage ?: "Login failed"
                }
                _uiState.value = LoginUiState.Error(msg)
            }
        }
    }

    fun loginWithGoogle(context: Context) {
        val currentAuth = try { FirebaseAuth.getInstance() } catch(e: Exception) { null }
        if (currentAuth == null || currentAuth.app.options.projectId == "MY_FIREBASE_PROJECT_ID") {
            _uiState.value = LoginUiState.Error("Firebase is not configured. Please add your credentials in Settings -> Secrets.")
            return
        }

        val webClientId = context.getString(R.string.default_web_client_id)
        if (webClientId == "YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com" || webClientId.isBlank()) {
            _uiState.value = LoginUiState.Error("Please configure default_web_client_id in strings.xml and add SHA-1 in Firebase Console for Google Sign-In.")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        currentAuth.signInWithCredential(firebaseCredential).await()
                        currentAuth.currentUser?.uid?.let { uid ->
                            SubscriptionManager.syncUser(uid)
                            com.example.util.AnalyticsHelper.setUserId(uid)
                            com.example.util.AnalyticsHelper.logEvent("login_google")
                        }
                        ScanHistoryManager.syncWithFirestore()
                        _uiState.value = LoginUiState.Success
                    } catch (e: GoogleIdTokenParsingException) {
                        _uiState.value = LoginUiState.Error("Invalid Google ID token format.")
                    }
                } else {
                    _uiState.value = LoginUiState.Error("Unexpected credential type encountered.")
                }
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                _uiState.value = LoginUiState.Error("No Google account is available. Add one on this device or sign in with email.")
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                _uiState.value = LoginUiState.Idle
            } catch (e: GetCredentialException) {
                Log.w("LoginViewModel", "Google sign-in failed", e)
                _uiState.value = LoginUiState.Error("Google sign-in could not be completed. Try again or sign in with email.")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Firebase Sign-In failed.")
            }
        }
    }

    fun signUp(name: String, email: String, pass: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = LoginUiState.Error("Name, email and password cannot be empty")
            return
        }
        val currentAuth = try { FirebaseAuth.getInstance() } catch(e: Exception) { null }
        if (currentAuth == null || currentAuth.app.options.projectId == "MY_FIREBASE_PROJECT_ID") {
            _uiState.value = LoginUiState.Error("Firebase is not configured. Please add your credentials in Settings -> Secrets.")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val currentAuthInstance = currentAuth ?: return@launch
                currentAuthInstance.createUserWithEmailAndPassword(email, pass).await()

                val user = currentAuthInstance.currentUser
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user?.updateProfile(profileUpdates)?.await()

                currentAuthInstance.currentUser?.uid?.let { uid ->
                    SubscriptionManager.syncUser(uid)
                    com.example.util.AnalyticsHelper.setUserId(uid)
                    com.example.util.AnalyticsHelper.logEvent("sign_up")
                }
                ScanHistoryManager.syncWithFirestore()
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Sign up failed")
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your email to reset password")
            return
        }
        val currentAuth = try { FirebaseAuth.getInstance() } catch(e: Exception) { null }
        if (currentAuth == null || currentAuth.app.options.projectId == "MY_FIREBASE_PROJECT_ID") {
            _uiState.value = LoginUiState.Error("Firebase is not configured. Please add your credentials in Settings -> Secrets.")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                currentAuth.sendPasswordResetEmail(email).await()
                _uiState.value = LoginUiState.Message("Password reset email sent. Please check your inbox.")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Failed to send reset email")
            }
        }
    }

    fun deleteAccount(context: Context, onComplete: (Boolean, String?) -> Unit) {
        val currentAuth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        val user = currentAuth?.currentUser
        if (user == null) {
            onComplete(false, "No active user logged in")
            return
        }

        val uid = user.uid
        viewModelScope.launch {
            try {
                // Check recent authentication BEFORE removing any user data.
                val token = user.getIdToken(true).await()
                val authTime = (token.claims["auth_time"] as? Number)?.toLong() ?: 0L
                val ageSeconds = System.currentTimeMillis() / 1000 - authTime
                if (ageSeconds !in 0..300) {
                    onComplete(false, "Please sign out and sign in again, then delete your account.")
                    return@launch
                }
                val db = FirebaseFirestore.getInstance()
                val storage = FirebaseStorage.getInstance()
                // Failures are surfaced; keep the Auth account so cleanup can be retried.
                com.example.data.AccountDataCleaner.deleteCloudData(db, storage.reference, uid)
                context.deleteDatabase("flora_scan_database")
                PlantRepository.getInstance(context).clearLocalData()
                ScanHistoryManager.clearLocalData()
                SubscriptionManager.resetUser()
                val safeUid = uid.replace(Regex("[^A-Za-z0-9_-]"), "_")
                for (name in listOf("local_plants_$safeUid", "scan_history_$safeUid")) {
                    context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
                }
                val subscriptionPrefs = context.getSharedPreferences("leaflens_subscription_prefs", Context.MODE_PRIVATE)
                subscriptionPrefs.edit()
                    .remove("free_scans_count_$safeUid")
                    .remove("free_scans_date_$safeUid")
                    .remove("trial_start_time_$safeUid").commit()
                // The path is a fixed private directory plus a sanitized Firebase UID.
                java.io.File(context.filesDir, "scans/$safeUid").deleteRecursively()
                // Delete authentication last so failed cleanup remains retryable.
                user.delete().await()
                com.example.util.AnalyticsHelper.setUserId(null)
                onComplete(true, null)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Account deletion error", e)
                onComplete(false, e.localizedMessage ?: "Failed to delete account. Please re-authenticate and try again.")
            }
        }
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data class Message(val message: String) : LoginUiState()
}
