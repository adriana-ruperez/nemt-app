package com.mobapps.nemt.data

data class UserProfile(
    val uid: String = "",
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val mobilitySupport: String? = null,
    val medicalPreferences: String? = null,
    val notificationPreferences: String? = null,
    val accessibilityNeeds: String? = null
) {
    fun displayName(fallbackEmail: String): String {
        val first = firstName.orEmpty().trim()
        val last = lastName.orEmpty().trim()
        val combined = listOf(first, last)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return combined.ifBlank {
            fallbackEmail.substringBefore("@")
                .replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
                .ifBlank { "Account" }
        }
    }
}
