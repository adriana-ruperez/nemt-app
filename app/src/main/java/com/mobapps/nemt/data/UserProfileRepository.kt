package com.mobapps.nemt.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object UserProfileRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val usersCollection by lazy { firestore.collection("users") }

    fun createDefaultProfile(
        user: FirebaseUser,
        onComplete: (Result<UserProfile>) -> Unit
    ) {
        val profile = defaultProfileFor(user)

        usersCollection.document(user.uid)
            .set(profile)
            .addOnSuccessListener {
                onComplete(Result.success(profile))
            }
            .addOnFailureListener { exception ->
                onComplete(Result.failure(exception))
            }
    }

    fun ensureProfile(
        user: FirebaseUser,
        onComplete: (Result<UserProfile>) -> Unit
    ) {
        usersCollection.document(user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                        ?.copy(uid = user.uid)
                        ?: defaultProfileFor(user)
                    onComplete(Result.success(profile))
                } else {
                    createDefaultProfile(user, onComplete)
                }
            }
            .addOnFailureListener { exception ->
                onComplete(Result.failure(exception))
            }
    }

    fun loadProfile(
        user: FirebaseUser,
        onComplete: (Result<UserProfile>) -> Unit
    ) {
        ensureProfile(user, onComplete)
    }

    fun updateProfile(
        profile: UserProfile,
        onComplete: (Result<UserProfile>) -> Unit
    ) {
        usersCollection.document(profile.uid)
            .set(profile)
            .addOnSuccessListener {
                onComplete(Result.success(profile))
            }
            .addOnFailureListener { exception ->
                onComplete(Result.failure(exception))
            }
    }

    fun defaultProfileFor(user: FirebaseUser): UserProfile {
        val email = user.email.orEmpty()
        val firstName = email.substringBefore("@")
            .trim()
            .ifBlank { "User" }
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }

        return UserProfile(
            uid = user.uid,
            firstName = firstName,
            lastName = null,
            email = email.ifBlank { null },
            phone = null,
            mobilitySupport = null,
            medicalPreferences = null,
            notificationPreferences = null,
            accessibilityNeeds = null
        )
    }
}
