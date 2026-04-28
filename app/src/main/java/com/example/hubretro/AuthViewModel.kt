package com.example.hubretro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfileData(
    val uid: String = "",
    val username: String = "",
    val userHandle: String = "",
    val bio: String = "",
    val email: String = "",
    val profilePictureUrl: String = "",
    val bannerUrl: String = ""
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfileData?>(null)
    val userProfile: StateFlow<UserProfileData?> = _userProfile.asStateFlow()

    init {
        auth.currentUser?.let { fetchUserProfile(it.uid) }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _currentUser.value = result.user
                result.user?.let { fetchUserProfile(it.uid) }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun createAccountWithEmail(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.let { user ->
                    val profile = UserProfileData(
                        uid = user.uid,
                        username = username,
                        userHandle = "@${username.lowercase().replace(" ", "")}",
                        bio = "Retro enthusiast 🎮",
                        email = email
                    )
                    firestore.collection("users").document(user.uid).set(profile).await()
                    _userProfile.value = profile
                    _currentUser.value = user
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Account creation failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                result.user?.let { user ->
                    val docRef = firestore.collection("users").document(user.uid)
                    val doc = docRef.get().await()
                    if (!doc.exists()) {
                        val profile = UserProfileData(
                            uid = user.uid,
                            username = user.displayName ?: "Retro User",
                            userHandle = "@${(user.displayName ?: "user").lowercase().replace(" ", "")}",
                            bio = "Retro enthusiast 🎮",
                            email = user.email ?: ""
                        )
                        docRef.set(profile).await()
                        _userProfile.value = profile
                    } else {
                        _userProfile.value = doc.toObject(UserProfileData::class.java)
                    }
                    _currentUser.value = user
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
            }
        }
    }

    fun updateUserProfile(updatedProfile: UserProfileData) {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                firestore.collection("users").document(uid).set(updatedProfile).await()
                _userProfile.value = updatedProfile
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Profile update failed")
            }
        }
    }

    fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                _userProfile.value = doc.toObject(UserProfileData::class.java)
            } catch (e: Exception) {
                // silently fail
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _userProfile.value = null
        _authState.value = AuthState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}