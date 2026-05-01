package com.example.hubretro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
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
    val bio: String = "Retro enthusiast 🎮",
    val email: String = "",
    val profilePictureUrl: String = "",
    val bannerUrl: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val setupComplete: Boolean = false,
    val topGames: List<Map<String, Any>> = emptyList(),
    val topSoundtracks: List<Map<String, Any>> = emptyList(),
    val location: String = "",
    val website: String = "",
    val createdAt: Long = 0L,
    val psnUsername: String = "",
    val xboxUsername: String = "",
    val steamUsername: String = "",
    val nintendoUsername: String = ""
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

    private val _followingList = MutableStateFlow<List<UserProfileData>>(emptyList())
    val followingList: StateFlow<List<UserProfileData>> = _followingList.asStateFlow()

    private val _followersList = MutableStateFlow<List<UserProfileData>>(emptyList())
    val followersList: StateFlow<List<UserProfileData>> = _followersList.asStateFlow()

    private val _followingUids = MutableStateFlow<Set<String>>(emptySet())
    val followingUids: StateFlow<Set<String>> = _followingUids.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserProfileData>>(emptyList())
    val allUsers: StateFlow<List<UserProfileData>> = _allUsers.asStateFlow()

    init {
        auth.currentUser?.let {
            fetchUserProfile(it.uid)
            fetchFollowingList(it.uid)
            fetchFollowersList(it.uid)
            fetchFollowingUids(it.uid)
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _currentUser.value = result.user
                result.user?.let {
                    fetchUserProfile(it.uid)
                    fetchFollowingList(it.uid)
                    fetchFollowersList(it.uid)
                    fetchFollowingUids(it.uid)
                }
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
                        email = email,
                        followersCount = 0,
                        followingCount = 0,
                        setupComplete = false,
                        createdAt = System.currentTimeMillis()
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
                            email = user.email ?: "",
                            followersCount = 0,
                            followingCount = 0,
                            setupComplete = false,
                            createdAt = System.currentTimeMillis()
                        )
                        docRef.set(profile).await()
                        _userProfile.value = profile
                    } else {
                        _userProfile.value = doc.toObject(UserProfileData::class.java)
                    }
                    _currentUser.value = user
                    fetchFollowingList(user.uid)
                    fetchFollowersList(user.uid)
                    fetchFollowingUids(user.uid)
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
                val data = doc.data ?: return@launch
                val profile = UserProfileData(
                    uid = data["uid"] as? String ?: "",
                    username = data["username"] as? String ?: "",
                    userHandle = data["userHandle"] as? String ?: "",
                    bio = data["bio"] as? String ?: "Retro enthusiast 🎮",
                    email = data["email"] as? String ?: "",
                    profilePictureUrl = data["profilePictureUrl"] as? String ?: "",
                    bannerUrl = data["bannerUrl"] as? String ?: "",
                    followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0,
                    followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0,
                    setupComplete = data["setupComplete"] as? Boolean ?: false,
                    topGames = (data["topGames"] as? List<*>)
                        ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                    topSoundtracks = (data["topSoundtracks"] as? List<*>)
                        ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                    location = data["location"] as? String ?: "",
                    website = data["website"] as? String ?: "",
                    createdAt = data["createdAt"] as? Long ?: 0L,
                    psnUsername = data["psnUsername"] as? String ?: "",
                    xboxUsername = data["xboxUsername"] as? String ?: "",
                    steamUsername = data["steamUsername"] as? String ?: "",
                    nintendoUsername = data["nintendoUsername"] as? String ?: ""
                )
                _userProfile.value = profile
            } catch (e: Exception) {
                // silently fail
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _userProfile.value = null
        _followingList.value = emptyList()
        _followersList.value = emptyList()
        _followingUids.value = emptySet()
        _authState.value = AuthState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun followUser(targetUid: String) {
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid == targetUid) return
        viewModelScope.launch {
            try {
                val batch = firestore.batch()
                val followDoc = firestore
                    .collection("follows")
                    .document("${currentUid}_${targetUid}")
                batch.set(followDoc, mapOf(
                    "followerId" to currentUid,
                    "followingId" to targetUid
                ))
                val currentUserDoc = firestore.collection("users").document(currentUid)
                batch.update(currentUserDoc, "followingCount", FieldValue.increment(1))
                val targetUserDoc = firestore.collection("users").document(targetUid)
                batch.update(targetUserDoc, "followersCount", FieldValue.increment(1))
                batch.commit().await()
                _followingUids.value = _followingUids.value + targetUid
                fetchFollowingList(currentUid)
                fetchUserProfile(currentUid)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to follow user")
            }
        }
    }

    fun unfollowUser(targetUid: String) {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val batch = firestore.batch()
                val followDoc = firestore
                    .collection("follows")
                    .document("${currentUid}_${targetUid}")
                batch.delete(followDoc)
                val currentUserDoc = firestore.collection("users").document(currentUid)
                batch.update(currentUserDoc, "followingCount", FieldValue.increment(-1))
                val targetUserDoc = firestore.collection("users").document(targetUid)
                batch.update(targetUserDoc, "followersCount", FieldValue.increment(-1))
                batch.commit().await()
                _followingUids.value = _followingUids.value - targetUid
                fetchFollowingList(currentUid)
                fetchUserProfile(currentUid)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to unfollow user")
            }
        }
    }

    fun isFollowing(targetUid: String): Boolean {
        return _followingUids.value.contains(targetUid)
    }

    private fun fetchFollowingUids(uid: String) {
        viewModelScope.launch {
            try {
                val docs = firestore.collection("follows")
                    .whereEqualTo("followerId", uid)
                    .get()
                    .await()
                _followingUids.value = docs.documents
                    .mapNotNull { it.getString("followingId") }
                    .toSet()
            } catch (e: Exception) { }
        }
    }

    fun fetchFollowingList(uid: String) {
        viewModelScope.launch {
            try {
                val docs = firestore.collection("follows")
                    .whereEqualTo("followerId", uid)
                    .get()
                    .await()
                val followingUids = docs.documents.mapNotNull { it.getString("followingId") }
                if (followingUids.isEmpty()) {
                    _followingList.value = emptyList()
                    return@launch
                }
                val profiles = followingUids.mapNotNull { targetUid ->
                    try {
                        val doc = firestore.collection("users")
                            .document(targetUid)
                            .get()
                            .await()
                        val data = doc.data ?: return@mapNotNull null
                        UserProfileData(
                            uid = doc.id,
                            username = data["username"] as? String ?: "",
                            userHandle = data["userHandle"] as? String ?: "",
                            bio = data["bio"] as? String ?: "",
                            email = data["email"] as? String ?: "",
                            profilePictureUrl = data["profilePictureUrl"] as? String ?: "",
                            bannerUrl = data["bannerUrl"] as? String ?: "",
                            followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0,
                            followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0,
                            setupComplete = data["setupComplete"] as? Boolean ?: false,
                            topGames = (data["topGames"] as? List<*>)
                                ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                            topSoundtracks = (data["topSoundtracks"] as? List<*>)
                                ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                            location = data["location"] as? String ?: "",
                            website = data["website"] as? String ?: "",
                            createdAt = data["createdAt"] as? Long ?: 0L,
                            psnUsername = data["psnUsername"] as? String ?: "",
                            xboxUsername = data["xboxUsername"] as? String ?: "",
                            steamUsername = data["steamUsername"] as? String ?: "",
                            nintendoUsername = data["nintendoUsername"] as? String ?: ""
                        )
                    } catch (e: Exception) { null }
                }
                _followingList.value = profiles
            } catch (e: Exception) { }
        }
    }

    fun fetchFollowersList(uid: String) {
        viewModelScope.launch {
            try {
                val docs = firestore.collection("follows")
                    .whereEqualTo("followingId", uid)
                    .get()
                    .await()
                val followerUids = docs.documents.mapNotNull { it.getString("followerId") }
                if (followerUids.isEmpty()) {
                    _followersList.value = emptyList()
                    return@launch
                }
                val profiles = followerUids.mapNotNull { followerUid ->
                    try {
                        val doc = firestore.collection("users")
                            .document(followerUid)
                            .get()
                            .await()
                        val data = doc.data ?: return@mapNotNull null
                        UserProfileData(
                            uid = doc.id,
                            username = data["username"] as? String ?: "",
                            userHandle = data["userHandle"] as? String ?: "",
                            bio = data["bio"] as? String ?: "",
                            email = data["email"] as? String ?: "",
                            profilePictureUrl = data["profilePictureUrl"] as? String ?: "",
                            bannerUrl = data["bannerUrl"] as? String ?: "",
                            followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0,
                            followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0,
                            setupComplete = data["setupComplete"] as? Boolean ?: false,
                            topGames = (data["topGames"] as? List<*>)
                                ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                            topSoundtracks = (data["topSoundtracks"] as? List<*>)
                                ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                            location = data["location"] as? String ?: "",
                            website = data["website"] as? String ?: "",
                            createdAt = data["createdAt"] as? Long ?: 0L,
                            psnUsername = data["psnUsername"] as? String ?: "",
                            xboxUsername = data["xboxUsername"] as? String ?: "",
                            steamUsername = data["steamUsername"] as? String ?: "",
                            nintendoUsername = data["nintendoUsername"] as? String ?: ""
                        )
                    } catch (e: Exception) { null }
                }
                _followersList.value = profiles
            } catch (e: Exception) { }
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        return try {
            val docs = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()
            docs.isEmpty
        } catch (e: Exception) {
            true
        }
    }

    fun completeProfileSetup(setupData: ProfileSetupData) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "username" to setupData.username,
                    "userHandle" to "@${setupData.username}",
                    "setupComplete" to true
                )
                if (setupData.selectedGames.isNotEmpty()) {
                    val gamesData = setupData.selectedGames.map { game ->
                        mapOf(
                            "id" to game.id,
                            "name" to game.name,
                            "coverUrl" to (game.coverUrl ?: ""),
                            "releaseYear" to (game.releaseYear ?: 0)
                        )
                    }
                    updates["topGames"] = gamesData
                }
                if (setupData.selectedSoundtracks.isNotEmpty()) {
                    val soundtracksData = setupData.selectedSoundtracks.map { st ->
                        mapOf(
                            "id" to st.id,
                            "name" to st.name,
                            "coverUrl" to (st.coverUrl ?: ""),
                            "gameName" to (st.gameName ?: "")
                        )
                    }
                    updates["topSoundtracks"] = soundtracksData
                }
                firestore.collection("users").document(uid).update(updates).await()
                fetchUserProfile(uid)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Setup failed")
            }
        }
    }

    fun fetchAllUsers() {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val docs = firestore.collection("users").get().await()
                _allUsers.value = docs.documents
                    .mapNotNull { it.toObject(UserProfileData::class.java) }
                    .filter { it.uid != currentUid }
            } catch (e: Exception) { }
        }
    }
}