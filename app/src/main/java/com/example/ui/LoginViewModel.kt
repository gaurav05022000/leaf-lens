package com.example.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.example.R
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val auth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null // Fallback if Firebase not configured
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
                    if (com.revenuecat.purchases.Purchases.isConfigured) {
                        try {
                            com.revenuecat.purchases.Purchases.sharedInstance.logIn(uid, object : com.revenuecat.purchases.interfaces.LogInCallback {
                                override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo, created: Boolean) {}
                                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                                    Log.e("RevenueCat", "Login failed: ${error.message}")
                                }
                            })
                        } catch(e: Exception) {
                            Log.e("RevenueCat", "Login exception", e)
                        }
                    }
                }
                PointsManager.syncFromFirestore()
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
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

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
                            if (com.revenuecat.purchases.Purchases.isConfigured) {
                                try {
                                    com.revenuecat.purchases.Purchases.sharedInstance.logIn(uid, object : com.revenuecat.purchases.interfaces.LogInCallback {
                                        override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo, created: Boolean) {}
                                        override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                                            Log.e("RevenueCat", "Login failed: ${error.message}")
                                        }
                                    })
                                } catch(e: Exception) {
                                    Log.e("RevenueCat", "Login exception", e)
                                }
                            }
                        }
                        PointsManager.syncFromFirestore()
                        ScanHistoryManager.syncWithFirestore()
                        _uiState.value = LoginUiState.Success
                    } catch (e: GoogleIdTokenParsingException) {
                        _uiState.value = LoginUiState.Error("Invalid Google ID token format.")
                    }
                } else {
                    _uiState.value = LoginUiState.Error("Unexpected credential type encountered.")
                }
            } catch (e: GetCredentialException) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Google Sign-In failed or was cancelled.")
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
                    if (com.revenuecat.purchases.Purchases.isConfigured) {
                        try {
                            com.revenuecat.purchases.Purchases.sharedInstance.logIn(uid, object : com.revenuecat.purchases.interfaces.LogInCallback {
                                override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo, created: Boolean) {}
                                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                                    Log.e("RevenueCat", "Login failed", Exception(error.message))
                                }
                            })
                        } catch(e: Exception) {
                            Log.e("RevenueCat", "Login exception", e)
                        }
                    }
                }
                PointsManager.syncFromFirestore()
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
        if (currentAuth == null) {
            _uiState.value = LoginUiState.Error("Firebase is not configured")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                currentAuth.sendPasswordResetEmail(email).await()
                _uiState.value = LoginUiState.Error("Password reset email sent. Please check your inbox.")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Failed to send reset email")
            }
        }
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
