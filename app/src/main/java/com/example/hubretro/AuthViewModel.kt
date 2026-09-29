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
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage

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
    val nintendoUsername: String = "",
    val twitchUsername: String = "",
    val youtubeUsername: String = "",
    val habboUsername: String = "",  // ✅ NEW
    val habboRegion: String = "habbo.com"  // ✅ NEW

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

    var activityViewModel: ActivityViewModel? = null

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfileData?>(null)
    val userProfile: StateFlow<UserProfileData?> = _userProfile.asStateFlow()

    /** false while a signed-in user's profile is still loading → app shows the warp/loading screen, never the main page. */
    private val _profileLoaded = MutableStateFlow(auth.currentUser == null)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

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

    // ─── Habbo Avatar URL Helper ──────────────────────────────────────────────

    fun habboAvatarUrl(username: String): String {
        if (username.isBlank()) return ""
        return "https://www.habbo.com/habbo-imaging/avatarimage?user=${username.trim()}&action=std&direction=2&head_direction=2&size=l&gesture=sml"
    }

    // ─── Update Habbo Username ────────────────────────────────────────────────

    fun updateHabboUsername(habboUsername: String, habboRegion: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .set(
                        mapOf(
                            "habboUsername" to habboUsername.trim(),
                            "habboRegion" to habboRegion  // ✅
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                fetchUserProfile(uid)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to update Habbo username")
            }
        }
    }

    // ─── Auth Functions ───────────────────────────────────────────────────────

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _profileLoaded.value = false
                WarpTransitionBus.show("WELCOME BACK!", "Loading your save file…")
                _currentUser.value = result.user
                result.user?.let {
                    fetchUserProfile(it.uid)
                    fetchFollowingList(it.uid)
                    fetchFollowersList(it.uid)
                    fetchFollowingUids(it.uid)
                    activityViewModel?.refreshForUser()
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
                val cleanEmail = email.trim().lowercase()
                val cleanUsername = username.trim()
                // Validate again here so nothing invalid reaches Firebase
                SignupRules.usernameError(cleanUsername)?.let { throw IllegalArgumentException(it) }
                SignupRules.emailError(cleanEmail)?.let { throw IllegalArgumentException(it) }
                if (!SignupRules.checkPassword(password).allPassed) {
                    throw IllegalArgumentException("Password doesn't meet the requirements")
                }
                val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                result.user?.let { user ->
                    // Final username check now that we're signed in (rules usually allow reads here).
                    // If someone already has it, undo the new auth account so nothing half-created remains.
                    if (SignupAvailability.username(cleanUsername) == Availability.TAKEN) {
                        try { user.delete().await() } catch (_: Exception) { }
                        auth.signOut()
                        throw IllegalStateException("USERNAME_TAKEN")
                    }
                    val profile = UserProfileData(
                        uid = user.uid,
                        username = cleanUsername,
                        userHandle = SignupRules.handleFor(cleanUsername),
                        bio = "Retro enthusiast 🎮",
                        email = cleanEmail,
                        followersCount = 0,
                        followingCount = 0,
                        setupComplete = false,
                        createdAt = System.currentTimeMillis(),
                        habboUsername = ""  // ✅ default empty
                    )
                    firestore.collection("users").document(user.uid).set(profile).await()
                    // Animated hand-off: account created → profile setup
                    WarpTransitionBus.show("ACCOUNT CREATED!", "Welcome, $cleanUsername · Next: build your player card")
                    _userProfile.value = profile
                    _profileLoaded.value = true
                    _currentUser.value = user
                    activityViewModel?.logJoinedActivity()
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(friendlySignupError(e))
            }
        }
    }

    private fun friendlySignupError(e: Exception): String = when {
        e.message == "USERNAME_TAKEN" -> "That username is already taken. Try another one."
        e is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "An account with this email already exists. Try signing in."
        e is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "That password is too weak."
        e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "That email address isn't valid."
        e is IllegalArgumentException -> e.message ?: "Please check your details."
        else -> e.message ?: "Account creation failed"
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
                            createdAt = System.currentTimeMillis(),
                            habboUsername = ""  // ✅ default empty

                        )
                        docRef.set(profile).await()
                        _userProfile.value = profile
                        activityViewModel?.logJoinedActivity()
                    } else {
                        val data = doc.data ?: return@let
                        _userProfile.value = data.toUserProfileData(user.uid)
                    }
                    WarpTransitionBus.show(
                        if (!doc.exists()) "ACCOUNT CREATED!" else "WELCOME BACK!",
                        if (!doc.exists()) "Next: build your player card" else "Loading your save file…"
                    )
                    _profileLoaded.value = true
                    _currentUser.value = user
                    fetchFollowingList(user.uid)
                    fetchFollowersList(user.uid)
                    fetchFollowingUids(user.uid)
                    activityViewModel?.refreshForUser()
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

    // ─── Firestore Map → UserProfileData ─────────────────────────────────────

    private fun Map<String, Any>.toUserProfileData(uid: String): UserProfileData {
        return UserProfileData(
            uid = this["uid"] as? String ?: uid,
            username = this["username"] as? String ?: "",
            userHandle = this["userHandle"] as? String ?: "",
            bio = this["bio"] as? String ?: "Retro enthusiast 🎮",
            email = this["email"] as? String ?: "",
            profilePictureUrl = this["profilePictureUrl"] as? String ?: "",
            bannerUrl = this["bannerUrl"] as? String ?: "",
            followersCount = (this["followersCount"] as? Long)?.toInt() ?: 0,
            followingCount = (this["followingCount"] as? Long)?.toInt() ?: 0,
            setupComplete = this["setupComplete"] as? Boolean ?: false,
            topGames = (this["topGames"] as? List<*>)
                ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
            topSoundtracks = (this["topSoundtracks"] as? List<*>)
                ?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
            location = this["location"] as? String ?: "",
            website = this["website"] as? String ?: "",
            createdAt = this["createdAt"] as? Long ?: 0L,
            psnUsername = this["psnUsername"] as? String ?: "",
            xboxUsername = this["xboxUsername"] as? String ?: "",
            steamUsername = this["steamUsername"] as? String ?: "",
            nintendoUsername = this["nintendoUsername"] as? String ?: "",
            twitchUsername = this["twitchUsername"] as? String ?: "",
            youtubeUsername = this["youtubeUsername"] as? String ?: "",
            habboUsername = this["habboUsername"] as? String ?: "",  // ✅ NEW
            habboRegion = this["habboRegion"] as? String ?: "habbo.com"  // ✅ NEW

        )
    }

    // ─── Fetch Profile ────────────────────────────────────────────────────────

    fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                val data = doc.data
                if (data != null) {
                    _userProfile.value = data.toUserProfileData(uid)
                } else {
                    // Signed in but no profile document (e.g. the app closed mid sign-up).
                    // Recreate a blank one so the player lands in profile setup, not a half-broken main page.
                    val user = auth.currentUser
                    if (user != null && user.uid == uid) {
                        val repaired = UserProfileData(
                            uid = uid,
                            username = user.displayName ?: "",
                            email = user.email ?: "",
                            setupComplete = false,
                            createdAt = System.currentTimeMillis()
                        )
                        firestore.collection("users").document(uid).set(repaired, com.google.firebase.firestore.SetOptions.merge()).await()
                        _userProfile.value = repaired
                    }
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "fetchUserProfile failed: ${e.message}")
            } finally {
                _profileLoaded.value = true
            }
        }
    }

    // ─── Sign Out ─────────────────────────────────────────────────────────────

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _userProfile.value = null
        _profileLoaded.value = true
        _followingList.value = emptyList()
        _followersList.value = emptyList()
        _followingUids.value = emptySet()
        _authState.value = AuthState.Idle
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    // ─── Follow / Unfollow ────────────────────────────────────────────────────

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
                try {
                    val targetDoc = firestore.collection("users")
                        .document(targetUid).get().await()
                    val targetUsername = targetDoc.getString("username") ?: ""
                    val targetHandle = targetDoc.getString("userHandle") ?: ""
                    activityViewModel?.logFollowActivity(targetUsername, targetHandle)
                } catch (e: Exception) { }
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
                    .get().await()
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
                    .get().await()
                val followingUids = docs.documents.mapNotNull { it.getString("followingId") }
                if (followingUids.isEmpty()) {
                    _followingList.value = emptyList()
                    return@launch
                }
                val profiles = followingUids.mapNotNull { targetUid ->
                    try {
                        val doc = firestore.collection("users")
                            .document(targetUid).get().await()
                        val data = doc.data ?: return@mapNotNull null
                        data.toUserProfileData(doc.id)
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
                    .get().await()
                val followerUids = docs.documents.mapNotNull { it.getString("followerId") }
                if (followerUids.isEmpty()) {
                    _followersList.value = emptyList()
                    return@launch
                }
                val profiles = followerUids.mapNotNull { followerUid ->
                    try {
                        val doc = firestore.collection("users")
                            .document(followerUid).get().await()
                        val data = doc.data ?: return@mapNotNull null
                        data.toUserProfileData(doc.id)
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
                .get().await()
            docs.isEmpty
        } catch (e: Exception) { true }
    }

    // ─── Upload Profile Picture ───────────────────────────────────────────────

    fun uploadProfilePicture(uri: Uri, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: run { onComplete(false); return }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                // Read + shrink the photo off the main thread (big camera photos used to freeze/crash here)
                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { readScaledJpeg(context, uri) }
                    ?: run { Log.e("Upload", "Cannot read image: $uri"); onComplete(false); return@launch }
                Log.d("Upload", "Profile pic — ${bytes.size} bytes read")
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("profile_images/$uid/profile_picture.jpg")
                storageRef.putBytes(bytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Log.d("Upload", "Profile pic URL: $downloadUrl")
                firestore.collection("users").document(uid)
                    .update("profilePictureUrl", downloadUrl).await()
                fetchUserProfile(uid)
                onComplete(true)
            } catch (e: Throwable) {
                Log.e("Upload", "Profile upload failed: ${e.message}", e)
                _authState.value = AuthState.Error("Upload failed: ${e.message}")
                onComplete(false)
            }
        }
    }

    // ─── Upload Banner Picture ────────────────────────────────────────────────

    fun uploadBannerPicture(uri: Uri, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: run { onComplete(false); return }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { readScaledJpeg(context, uri) }
                    ?: run { Log.e("Upload", "Cannot read banner: $uri"); onComplete(false); return@launch }
                Log.d("Upload", "Banner — ${bytes.size} bytes read")
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("profile_images/$uid/banner.jpg")
                storageRef.putBytes(bytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Log.d("Upload", "Banner URL: $downloadUrl")
                firestore.collection("users").document(uid)
                    .update("bannerUrl", downloadUrl).await()
                fetchUserProfile(uid)
                onComplete(true)
            } catch (e: Throwable) {
                Log.e("Upload", "Banner upload failed: ${e.message}", e)
                _authState.value = AuthState.Error("Upload failed: ${e.message}")
                onComplete(false)
            }
        }
    }

    // ─── Complete Profile Setup ───────────────────────────────────────────────

    fun completeProfileSetup(setupData: ProfileSetupData) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "username" to setupData.username,
                    "userHandle" to SignupRules.handleFor(setupData.username),
                    "setupComplete" to true,
                    "psnUsername" to setupData.psnUsername,
                    "xboxUsername" to setupData.xboxUsername,
                    "steamUsername" to setupData.steamUsername,
                    "nintendoUsername" to setupData.nintendoUsername,
                    "habboUsername" to setupData.habboUsername.trim(),
                    "habboRegion"   to setupData.habboRegion.ifBlank { "habbo.com" }
                )
                if (setupData.selectedGames.isNotEmpty()) {
                    updates["topGames"] = setupData.selectedGames.map { game ->
                        mapOf(
                            "id" to game.id,
                            "name" to game.name,
                            "coverUrl" to (game.coverUrl ?: ""),
                            "releaseYear" to (game.releaseYear ?: 0),
                            "platform" to (game.platforms.firstOrNull() ?: "")
                        )
                    }
                }
                if (setupData.selectedSoundtracks.isNotEmpty()) {
                    updates["topSoundtracks"] = setupData.selectedSoundtracks.map { st ->
                        mapOf(
                            "id" to st.id,
                            "name" to st.name,
                            "coverUrl" to (st.coverUrl ?: ""),
                            "gameName" to (st.gameName ?: "")
                        )
                    }
                }
                // Upload the photos picked in step 2 FIRST, then save everything in one write,
                // so the profile opens with its picture + banner already there.
                setupData.profilePictureUri?.let { uri ->
                    uploadImage(uid, uri, "profile_picture.jpg")?.let { updates["profilePictureUrl"] = it }
                }
                setupData.bannerUri?.let { uri ->
                    uploadImage(uid, uri, "banner.jpg")?.let { updates["bannerUrl"] = it }
                }
                firestore.collection("users").document(uid).update(updates).await()
                WarpTransitionBus.show("ENTERING RETROHUB", "Profile saved · Press START, ${setupData.username.ifBlank { "player" }}!")
                fetchUserProfile(uid)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Setup failed")
            }
        }
    }

    // ─── Fetch All Users ──────────────────────────────────────────────────────

    fun fetchAllUsers() {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val docs = firestore.collection("users").get().await()
                _allUsers.value = docs.documents
                    .mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        data.toUserProfileData(doc.id)
                    }
                    .filter { it.uid != currentUid }
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "fetchAllUsers failed", e)
        }
        }
    }

    /** Decodes the picked image, scales it to max 1600px and re-encodes as JPEG (≈200–600 KB). */
    private fun readScaledJpeg(context: android.content.Context, uri: Uri): ByteArray? = try {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
        bmp?.let {
            val out = java.io.ByteArrayOutputStream()
            it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            it.recycle()
            out.toByteArray()
        }
    } catch (t: Throwable) { null }

    /** Uploads one image to Storage (profile_images/{uid}/{name}) and returns its download URL, or null on failure. */
    private suspend fun uploadImage(uid: String, uri: Uri, name: String): String? = try {
        val context = getApplication<Application>().applicationContext
        val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { readScaledJpeg(context, uri) }
        if (bytes == null) {
            Log.e("Upload", "Could not read $name from $uri")
            null
        } else {
            val ref = FirebaseStorage.getInstance().reference.child("profile_images/$uid/$name")
            ref.putBytes(bytes, com.google.firebase.storage.StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
            ref.downloadUrl.await().toString().also { Log.d("Upload", "$name uploaded: $it") }
        }
    } catch (e: Exception) {
        // Most common cause: Firebase Storage rules don't allow writes to profile_images/{uid}
        Log.e("Upload", "Upload of $name failed: ${e.message}", e)
        null
    }
}
