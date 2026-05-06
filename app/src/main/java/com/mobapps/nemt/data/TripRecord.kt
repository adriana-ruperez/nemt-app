package com.mobapps.nemt.data

/**
 * Dispatch lifecycle for a trip stored in Firestore.
 * Tab mapping: Upcoming = REQUESTED through ARRIVED; Completed = COMPLETED; Cancelled = CANCELLED.
 */
enum class TripLifecycleStatus {
    REQUESTED,
    ACCEPTED,
    ASSIGNED,
    EN_ROUTE,
    ARRIVED,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromString(value: String?): TripLifecycleStatus =
            entries.find { it.name == value } ?: REQUESTED
    }
}

/**
 * Firestore-backed trip document (also used in UI after mapping).
 * Field names must match [TripsFirestoreRepository] writes.
 */
data class TripRecord(
    val id: String = "",
    val riderUid: String = "",
    val lifecycleStatus: TripLifecycleStatus = TripLifecycleStatus.REQUESTED,
    val originTitle: String = "",
    val originAddress: String = "",
    val originPlaceId: String? = null,
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    val destinationTitle: String = "",
    val destinationAddress: String = "",
    val destinationPlaceId: String? = null,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    /** Epoch millis for scheduled pickup (sorting / validation). */
    val scheduledAtMillis: Long = 0L,
    /** Human-readable schedule string for cards. */
    val dateTimeDisplay: String = "",
    val vehicleLabel: String = "",
    val riderDisplayName: String = "",
    val riderEmail: String? = null,
    val mobilitySupport: String? = null,
    val accessibilityNeeds: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

fun TripLifecycleStatus.toUiTab(): TripStatus = when (this) {
    TripLifecycleStatus.REQUESTED,
    TripLifecycleStatus.ACCEPTED,
    TripLifecycleStatus.ASSIGNED,
    TripLifecycleStatus.EN_ROUTE,
    TripLifecycleStatus.ARRIVED -> TripStatus.UPCOMING
    TripLifecycleStatus.COMPLETED -> TripStatus.COMPLETED
    TripLifecycleStatus.CANCELLED -> TripStatus.CANCELLED
}

fun TripLifecycleStatus.toRideCardStatusLabel(): String = when (this) {
    TripLifecycleStatus.REQUESTED -> "Requested"
    TripLifecycleStatus.ACCEPTED -> "Accepted"
    TripLifecycleStatus.ASSIGNED -> "Assigned"
    TripLifecycleStatus.EN_ROUTE -> "In progress"
    TripLifecycleStatus.ARRIVED -> "Arrived"
    TripLifecycleStatus.COMPLETED -> "Completed"
    TripLifecycleStatus.CANCELLED -> "Cancelled"
}
