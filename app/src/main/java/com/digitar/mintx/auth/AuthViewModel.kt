package com.digitar.mintx.auth

import android.app.Activity
import android.app.Application
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.digitar.mintx.data.model.User
import com.digitar.mintx.utils.SessionManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    private val sessionManager = SessionManager(application)

    private val _loginState = MutableLiveData<LoginUiState>()
    val loginState: LiveData<LoginUiState> = _loginState

    private val _timerText = MutableLiveData<String>()
    val timerText: LiveData<String> = _timerText

    private val _timerSeconds = MutableLiveData<Long>()
    val timerSeconds: LiveData<Long> = _timerSeconds

    private val _isResendEnabled = MutableLiveData<Boolean>()
    val isResendEnabled: LiveData<Boolean> = _isResendEnabled

    private var countDownTimer: CountDownTimer? = null
    
    // Store temporarily
    var currentPhoneNumber: String? = null

    // Firebase Phone Auth Callbacks
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-retrieval or instant verification
            android.util.Log.d("AuthViewModel", "onVerificationCompleted: Auto-verification success")
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // CRITICAL: Log this to Logcat to see the real error
            android.util.Log.e("AuthViewModel", "Verification Failed", e)

            val errorMessage = when (e) {
                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                    "Invalid Request. Check SHA-1/SHA-256 keys in Firebase Console. (${e.message})"
                }
                is com.google.firebase.FirebaseTooManyRequestsException -> {
                    "SMS Limit Exceeded. Please try again in 1 hour or use a Test Phone Number."
                }
                else -> {
                    e.message ?: "Verification failed. Check internet & Play Integrity."
                }
            }
            _loginState.value = LoginUiState.Error(errorMessage)
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            android.util.Log.d("AuthViewModel", "OTP Sent Successfully. Verification ID: $verificationId")
            repository.verificationId = verificationId
            repository.resendToken = token
            _loginState.value = LoginUiState.OtpSent
            startTimer()
        }
    }

    // 1. Send OTP (Start Login)
    fun sendOtp(phoneNumber: String, activity: Activity) {
        currentPhoneNumber = phoneNumber
        _loginState.value = LoginUiState.Loading
        repository.sendOtp(phoneNumber, activity, callbacks)
    }
    
    // 2. Resend OTP
    fun resendOtp(phoneNumber: String, activity: Activity) {
        currentPhoneNumber = phoneNumber
        _loginState.value = LoginUiState.Loading
        repository.sendOtp(phoneNumber, activity, callbacks) // Repository handles token reuse
    }

    // 3. Verify OTP (User enters manual code)
    fun verifyOtp(code: String) {
        val verificationId = repository.verificationId
        if (verificationId != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithPhoneAuthCredential(credential)
        } else {
             _loginState.value = LoginUiState.Error("Verification ID missing. Please resend OTP.")
        }
    }

    fun handleGoogleSignInResult(task: com.google.android.gms.tasks.Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount>) {
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account)
            } else {
                _loginState.value = LoginUiState.Error("Google Sign-In failed")
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            _loginState.value = LoginUiState.Error("Google Sign-In failed: ${e.message}")
        }
    }

    private fun firebaseAuthWithGoogle(acct: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        _loginState.value = LoginUiState.Loading
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(acct.idToken, null)
        
        viewModelScope.launch {
            repository.signInWithCredential(credential).collect { result ->
                result.onSuccess { uid ->
                    // For Google, we have extra info immediately
                    val name = acct.displayName ?: "User"
                    val email = acct.email ?: ""
                    val photoUrl = acct.photoUrl?.toString() ?: ""
                    
                    checkUserAndNavigate(uid, name, email, photoUrl)
                }.onFailure {
                    _loginState.value = LoginUiState.Error(it.message ?: "Sign In Failed")
                }
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        _loginState.value = LoginUiState.Loading
        viewModelScope.launch {
            repository.signInWithCredential(credential).collect { result ->
                result.onSuccess { uid ->
                    checkUserAndNavigate(uid)
                }.onFailure {
                    _loginState.value = LoginUiState.Error(it.message ?: "Sign In Failed")
                }
            }
        }
    }
    
    private suspend fun checkUserAndNavigate(uid: String, googleName: String? = null, googleEmail: String? = null, googlePhoto: String? = null) {
        val phone = currentPhoneNumber 
        val exists = repository.checkUserExists(uid) // Check by UID
        
        if (phone != null) {
             sessionManager.createLoginSession(phone) // Basic session
        } else if (googleEmail != null) {
             // Google Login Session
             sessionManager.createGoogleSession(googleEmail, googlePhoto ?: "")
        }
        
        if (exists) {
            val user = repository.getUser(uid) // Get by UID
            if (user != null) {
                // Determine if we need to update profile with Google info if it was missing?
                // For now, just complete session.
                val nameToUse = if (user.name.isNotEmpty()) user.name else googleName ?: "User"
                val emailToUse = if (user.email.isNotEmpty()) user.email else googleEmail ?: ""
                val photoToUse = if (user.photoUrl.isNotEmpty()) user.photoUrl else googlePhoto ?: ""
                
                sessionManager.completeProfile(nameToUse, user.age, emailToUse, photoToUse)
                _loginState.value = LoginUiState.LoginSuccess
            } else {
                // User exists in auth but not db?
                 _loginState.value = LoginUiState.NavigateToProfile
            }
        } else {
            // New User
            if (googleName != null) {
                // If Google, we can auto-create the user!
                val newUser = User(
                    firebaseUid = uid,
                    name = googleName,
                    email = googleEmail ?: "",
                    photoUrl = googlePhoto ?: "",
                    createdAt = System.currentTimeMillis()
                )
                
                // Save immediately
                val saveResult = repository.createUser(newUser)
                if (saveResult.isSuccess) {
                    sessionManager.completeProfile(googleName, 0, googleEmail ?: "", googlePhoto ?: "")
                    _loginState.value = LoginUiState.LoginSuccess
                } else {
                    _loginState.value = LoginUiState.Error("Failed to create profile")
                }
            } else {
                _loginState.value = LoginUiState.NavigateToProfile
            }
        }
    }

    // 4. Create Profile
    fun createProfile(name: String, age: String) {
        val ageInt = age.toIntOrNull()
        val phone = currentPhoneNumber ?: sessionManager.getUserMobile()
        
        if (name.isBlank() || ageInt == null || phone == null) {
            _loginState.value = LoginUiState.Error("Please fill all fields")
            return
        }
        
        _loginState.value = LoginUiState.Loading
        val user = User(
            firebaseUid = repository.getCurrentUser()?.uid ?: "",
            phone = phone,
            name = name,
            age = ageInt,
            countryCode = "", // Can parse from phone if key
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            Log.d("CreateUser", "Request started for user: $user")

            val result = repository.createUser(user)

            result.onSuccess {
                Log.d("CreateUser", "User saved successfully in Firebase")

                sessionManager.completeProfile(name, ageInt)
                _loginState.value = LoginUiState.LoginSuccess
            }.onFailure { error ->
                Log.e("CreateUser", "Failed to save user", error)

                _loginState.value =
                    LoginUiState.Error("Failed to save profile: ${error.message}")
            }
        }

    }

    private fun startTimer() {
        _isResendEnabled.value = false
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(90000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                _timerSeconds.value = secondsRemaining
                
                val minutes = secondsRemaining / 60
                val seconds = secondsRemaining % 60
                _timerText.value = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                _timerText.value = "Resend OTP"
                _timerSeconds.value = 0
                _isResendEnabled.value = true
            }
        }.start()
    }

    fun resetLoginState() {
        _loginState.value = LoginUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object OtpSent : LoginUiState()
    object LoginSuccess : LoginUiState()
    object NavigateToProfile : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
