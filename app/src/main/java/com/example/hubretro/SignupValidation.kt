package com.example.hubretro

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// ═══════════════════════════════════════════════════════════════════════════════
// Sign-up validation rules + availability checks (username / email)
// ═══════════════════════════════════════════════════════════════════════════════

object SignupRules {
    const val USERNAME_MIN = 3
    const val USERNAME_MAX = 20
    const val PASSWORD_MIN = 8
    private val usernameRegex = Regex("^[A-Za-z0-9_.]+$")
    private val specialRegex = Regex("[^A-Za-z0-9]")

    /** The handle every account gets, e.g. "Logodzip" → "@logodzip". Used for uniqueness. */
    fun handleFor(username: String) = "@" + username.trim().lowercase().replace(" ", "")

    /** null = valid, otherwise a short error message. */
    fun usernameError(username: String): String? {
        val u = username.trim()
        return when {
            u.isEmpty() -> "Pick a username"
            u.length < USERNAME_MIN -> "At least $USERNAME_MIN characters"
            u.length > USERNAME_MAX -> "Max $USERNAME_MAX characters"
            u.contains(' ') -> "No spaces allowed"
            !usernameRegex.matches(u) -> "Only letters, numbers, _ and ."
            !Moderation.isAllowedName(u) -> "That username isn't allowed"
            else -> null
        }
    }

    fun emailError(email: String): String? {
        val e = email.trim()
        return when {
            e.isEmpty() -> "Enter your email"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches() -> "That doesn't look like an email"
            else -> null
        }
    }

    data class PasswordCheck(
        val startsWithCapital: Boolean,
        val longEnough: Boolean,
        val hasNumber: Boolean,
        val hasSpecial: Boolean
    ) {
        val allPassed get() = startsWithCapital && longEnough && hasNumber && hasSpecial
    }

    fun checkPassword(password: String) = PasswordCheck(
        startsWithCapital = password.firstOrNull()?.isUpperCase() == true,
        longEnough = password.length >= PASSWORD_MIN,
        hasNumber = password.any { it.isDigit() },
        hasSpecial = specialRegex.containsMatchIn(password)
    )
}

enum class Availability { UNKNOWN, CHECKING, AVAILABLE, TAKEN }

object SignupAvailability {
    private val db get() = FirebaseFirestore.getInstance()

    /**
     * Checks if another account already uses this username (case-insensitive, via the @handle).
     * Returns UNKNOWN if the lookup isn't allowed (e.g. Firestore rules block signed-out reads) —
     * the final check at account creation still protects against duplicates.
     */
    suspend fun username(username: String): Availability = try {
        val handle = SignupRules.handleFor(username)
        val byHandle = db.collection("users").whereEqualTo("userHandle", handle).limit(1).get().await()
        if (!byHandle.isEmpty) Availability.TAKEN
        else {
            val byName = db.collection("users").whereEqualTo("username", username.trim()).limit(1).get().await()
            if (byName.isEmpty) Availability.AVAILABLE else Availability.TAKEN
        }
    } catch (e: Exception) { Availability.UNKNOWN }

    /** Checks the users collection for this email (typed and lowercase). */
    suspend fun email(email: String): Availability = try {
        val e = email.trim()
        val exact = db.collection("users").whereEqualTo("email", e).limit(1).get().await()
        if (!exact.isEmpty) Availability.TAKEN
        else {
            val lower = db.collection("users").whereEqualTo("email", e.lowercase()).limit(1).get().await()
            if (lower.isEmpty) Availability.AVAILABLE else Availability.TAKEN
        }
    } catch (e: Exception) { Availability.UNKNOWN }
}
