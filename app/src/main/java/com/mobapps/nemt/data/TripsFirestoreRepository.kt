package com.mobapps.nemt.data

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Rider-scoped trips in Firestore collection `rides`.
 *
 * Expected Firestore rules (example): only authenticated users can read/write documents
 * where `riderUid == request.auth.uid`.
 */
object TripsFirestoreRepository {

    private const val COLLECTION = "rides"

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val tripsCollection by lazy { firestore.collection(COLLECTION) }

    private val _trips = MutableStateFlow<List<TripRecord>>(emptyList())
    val trips: StateFlow<List<TripRecord>> = _trips.asStateFlow()

    private var listener: ListenerRegistration? = null

    private val demoSeedRequestedForUid: MutableSet<String> =
        ConcurrentHashMap.newKeySet()

    fun startListening(riderUid: String) {
        listener?.remove()
        listener = tripsCollection
            .whereEqualTo("riderUid", riderUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _trips.value = emptyList()
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents.orEmpty()
                if (docs.isEmpty() && demoSeedRequestedForUid.add(riderUid)) {
                    seedDemoTripsIfEmpty(riderUid)
                }
                val list = docs.mapNotNull { it.toTripRecord() }
                _trips.value = list.sortedWith(
                    compareByDescending<TripRecord> { it.scheduledAtMillis }
                        .thenByDescending { it.createdAtMillis }
                )
            }
    }

    fun stopListening() {
        listener?.remove()
        listener = null
    }

    fun createTrip(
        riderUid: String,
        originTitle: String,
        originAddress: String,
        originPlaceId: String?,
        originLatitude: Double?,
        originLongitude: Double?,
        destinationTitle: String,
        destinationAddress: String,
        destinationPlaceId: String?,
        destinationLatitude: Double?,
        destinationLongitude: Double?,
        scheduledAtMillis: Long,
        dateTimeDisplay: String,
        vehicleLabel: String,
        riderDisplayName: String,
        riderEmail: String?,
        onComplete: (Result<String>) -> Unit
    ) {
        val docRef = tripsCollection.document()
        val id = docRef.id
        val data = hashMapOf(
            "id" to id,
            "riderUid" to riderUid,
            "lifecycleStatus" to TripLifecycleStatus.ACCEPTED.name,
            "scheduledAtMillis" to scheduledAtMillis,
            "route" to mapOf(
                "origin" to mapOf(
                    "title" to originTitle,
                    "address" to originAddress,
                    "placeId" to originPlaceId,
                    "location" to mapOf(
                        "latitude" to originLatitude,
                        "longitude" to originLongitude
                    )
                ),
                "destination" to mapOf(
                    "title" to destinationTitle,
                    "address" to destinationAddress,
                    "placeId" to destinationPlaceId,
                    "location" to mapOf(
                        "latitude" to destinationLatitude,
                        "longitude" to destinationLongitude
                    )
                )
            ),
            "schedule" to mapOf(
                "scheduledAtMillis" to scheduledAtMillis,
                "dateTimeDisplay" to dateTimeDisplay
            ),
            "vehicle" to mapOf(
                "label" to vehicleLabel
            ),
            "rider" to mapOf(
                "uid" to riderUid,
                "email" to riderEmail,
                "displayName" to riderDisplayName
            ),
            "timestamps" to mapOf(
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        docRef.set(data)
            .addOnSuccessListener { onComplete(Result.success(id)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun updateTripFields(
        tripId: String,
        newDateTimeDisplay: String,
        newFrom: String,
        newTo: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        tripsCollection.document(tripId)
            .update(
                mapOf(
                    "schedule.dateTimeDisplay" to newDateTimeDisplay,
                    "route.origin.title" to newFrom,
                    "route.destination.title" to newTo,
                    "timestamps.updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun cancelTrip(tripId: String, onComplete: (Result<Unit>) -> Unit) {
        tripsCollection.document(tripId)
            .update(
                mapOf(
                    "lifecycleStatus" to TripLifecycleStatus.CANCELLED.name,
                    "timestamps.updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    /**
     * Optional demo seed when a new rider has no trips (Spain sample data).
     * Writes a batch; listener will refresh.
     */
    private fun seedDemoTripsIfEmpty(riderUid: String) {
        val batch = firestore.batch()
        val templates = demoTripTemplates()
        templates.forEachIndexed { index, template ->
            val ref = tripsCollection.document()
            batch.set(
                ref,
                hashMapOf(
                    "id" to ref.id,
                    "riderUid" to riderUid,
                    "lifecycleStatus" to template.status.name,
                    "scheduledAtMillis" to template.scheduledAtMillis,
                    "route" to mapOf(
                        "origin" to mapOf(
                            "title" to template.from,
                            "address" to template.from,
                            "placeId" to null,
                            "location" to mapOf(
                                "latitude" to null,
                                "longitude" to null
                            )
                        ),
                        "destination" to mapOf(
                            "title" to template.to,
                            "address" to template.to,
                            "placeId" to null,
                            "location" to mapOf(
                                "latitude" to null,
                                "longitude" to null
                            )
                        )
                    ),
                    "schedule" to mapOf(
                        "scheduledAtMillis" to template.scheduledAtMillis,
                        "dateTimeDisplay" to template.dateTimeDisplay
                    ),
                    "vehicle" to mapOf(
                        "label" to template.vehicle
                    ),
                    "rider" to mapOf(
                        "uid" to riderUid,
                        "email" to null,
                        "displayName" to template.patientName
                    ),
                    "requirements" to mapOf(
                        "mobilitySupport" to template.mobility,
                        "accessibilityNeeds" to template.accessibility
                    ),
                    "timestamps" to mapOf(
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            )
        }
        batch.commit()
    }

    private data class DemoTemplate(
        val status: TripLifecycleStatus,
        val from: String,
        val to: String,
        val scheduledAtMillis: Long,
        val dateTimeDisplay: String,
        val vehicle: String,
        val patientName: String,
        val mobility: String?,
        val accessibility: String?
    )

    private fun demoTripTemplates(): List<DemoTemplate> {
        val random = Random(91)
        val base = System.currentTimeMillis()
        val pickups = listOf(
            "Calle de Atocha 27, Madrid",
            "Carrer de Mallorca 201, Barcelona",
            "Avenida de la Constitución 10, Sevilla",
            "Paseo de la Alameda 14, Valencia",
            "Calle Larios 6, Málaga",
            "Gran Vía de Colón 22, Granada",
            "Plaza de España 3, Zaragoza",
            "Calle Uría 8, Oviedo",
            "Calle Triana 45, Las Palmas",
            "Rúa do Vilar 19, Santiago de Compostela"
        )
        val destinations = listOf(
            "Hospital Universitario La Paz, Madrid",
            "Hospital Clínic de Barcelona, Barcelona",
            "Hospital Virgen del Rocío, Sevilla",
            "Hospital La Fe, Valencia",
            "Hospital Regional de Málaga, Málaga",
            "Hospital San Cecilio, Granada",
            "Hospital Miguel Servet, Zaragoza",
            "Hospital de Cruces, Bilbao",
            "Hospital Son Espases, Palma",
            "Hospital Universitario de Navarra, Pamplona"
        )
        val vehicles = listOf(
            "Unit 12 · Wheelchair Van",
            "Unit 7 · Accessible Sedan",
            "Unit 3 · Lift-equipped Van",
            "Unit 19 · Mobility Assist"
        )
        val names = listOf(
            "Sofia R.",
            "Daniel M.",
            "Elena C.",
            "Pablo G."
        )
        fun route(i: Int): Pair<String, String> {
            val from = pickups[i % pickups.size]
            val to = destinations[(i + random.nextInt(1, 4)) % destinations.size]
            return from to to
        }
        val out = mutableListOf<DemoTemplate>()
        repeat(4) { i ->
            val (from, to) = route(i)
            out += DemoTemplate(
                status = TripLifecycleStatus.REQUESTED,
                from = from,
                to = to,
                scheduledAtMillis = base + (i + 1) * 86_400_000L,
                dateTimeDisplay = "Upcoming · Day ${i + 1}",
                vehicle = vehicles[i % vehicles.size],
                patientName = names[i % names.size],
                mobility = "Walker support",
                accessibility = "Large text preferred"
            )
        }
        repeat(3) { i ->
            val (from, to) = route(i + 4)
            out += DemoTemplate(
                status = TripLifecycleStatus.COMPLETED,
                from = from,
                to = to,
                scheduledAtMillis = base - (i + 2) * 86_400_000L,
                dateTimeDisplay = "Completed · ${i + 1}d ago",
                vehicle = vehicles[(i + 1) % vehicles.size],
                patientName = names[(i + 1) % names.size],
                mobility = null,
                accessibility = null
            )
        }
        repeat(3) { i ->
            val (from, to) = route(i + 7)
            out += DemoTemplate(
                status = TripLifecycleStatus.CANCELLED,
                from = from,
                to = to,
                scheduledAtMillis = base - (i + 1) * 43_200_000L,
                dateTimeDisplay = "Cancelled · ${i + 1}d ago",
                vehicle = vehicles[(i + 2) % vehicles.size],
                patientName = names[(i + 2) % names.size],
                mobility = null,
                accessibility = null
            )
        }
        return out
    }

    private fun DocumentSnapshot.toTripRecord(): TripRecord? {
        val tripId = id
        val rider = getString("riderUid") ?: return null
        val status = TripLifecycleStatus.fromString(getString("lifecycleStatus"))
        val route = get("route") as? Map<*, *>
        val origin = route?.get("origin") as? Map<*, *>
        val destination = route?.get("destination") as? Map<*, *>
        val originLocation = origin?.get("location") as? Map<*, *>
        val destinationLocation = destination?.get("location") as? Map<*, *>
        val schedule = get("schedule") as? Map<*, *>
        val vehicle = get("vehicle") as? Map<*, *>
        val riderData = get("rider") as? Map<*, *>
        val requirements = get("requirements") as? Map<*, *>
        val timestamps = get("timestamps") as? Map<*, *>

        val originTitle = origin?.get("title") as? String ?: getString("from").orEmpty()
        val originAddress = origin?.get("address") as? String ?: getString("fromAddress").orEmpty()
        val originPlaceId = origin?.get("placeId") as? String ?: getString("fromPlaceId")
        val originLatitude = (originLocation?.get("latitude") as? Number)?.toDouble()
            ?: getDouble("fromLatitude")
        val originLongitude = (originLocation?.get("longitude") as? Number)?.toDouble()
            ?: getDouble("fromLongitude")
        val destinationTitle = destination?.get("title") as? String ?: getString("to").orEmpty()
        val destinationAddress = destination?.get("address") as? String ?: getString("toAddress").orEmpty()
        val destinationPlaceId = destination?.get("placeId") as? String ?: getString("toPlaceId")
        val destinationLatitude = (destinationLocation?.get("latitude") as? Number)?.toDouble()
            ?: getDouble("toLatitude")
        val destinationLongitude = (destinationLocation?.get("longitude") as? Number)?.toDouble()
            ?: getDouble("toLongitude")
        val scheduled = (schedule?.get("scheduledAtMillis") as? Number)?.toLong()
            ?: getLong("scheduledAtMillis") ?: 0L
        val display = schedule?.get("dateTimeDisplay") as? String
            ?: getString("dateTimeDisplay").orEmpty()
        val vehicleLabel = vehicle?.get("label") as? String ?: getString("vehicle").orEmpty()
        val riderDisplayName = riderData?.get("displayName") as? String
            ?: getString("patientName").orEmpty()
        val riderEmail = riderData?.get("email") as? String ?: getString("riderEmail")
        val mobility = requirements?.get("mobilitySupport") as? String
            ?: getString("mobilitySupportSnapshot")
        val accessibility = requirements?.get("accessibilityNeeds") as? String
            ?: getString("accessibilityNeedsSnapshot")
        val createdMs = ((timestamps?.get("createdAt") as? Timestamp)?.toDate()?.time)
            ?: (get("createdAt") as? Timestamp)?.toDate()?.time
            ?: 0L
        val updatedMs = ((timestamps?.get("updatedAt") as? Timestamp)?.toDate()?.time)
            ?: (get("updatedAt") as? Timestamp)?.toDate()?.time
            ?: createdMs
        return TripRecord(
            id = getString("id") ?: tripId,
            riderUid = rider,
            lifecycleStatus = status,
            originTitle = originTitle,
            originAddress = originAddress,
            originPlaceId = originPlaceId,
            originLatitude = originLatitude,
            originLongitude = originLongitude,
            destinationTitle = destinationTitle,
            destinationAddress = destinationAddress,
            destinationPlaceId = destinationPlaceId,
            destinationLatitude = destinationLatitude,
            destinationLongitude = destinationLongitude,
            scheduledAtMillis = scheduled,
            dateTimeDisplay = display,
            vehicleLabel = vehicleLabel,
            riderDisplayName = riderDisplayName,
            riderEmail = riderEmail,
            mobilitySupport = mobility,
            accessibilityNeeds = accessibility,
            createdAtMillis = createdMs,
            updatedAtMillis = updatedMs
        )
    }

    /** True if pickup and destination are the same place (by title or very close coordinates). */
    fun areStopsEffectivelyDuplicate(
        pickupTitle: String,
        pickupSubtitle: String,
        pickupLatLng: LatLng?,
        destTitle: String,
        destSubtitle: String,
        destLatLng: LatLng?
    ): Boolean {
        val a = "${pickupTitle.trim().lowercase()}|${pickupSubtitle.trim().lowercase()}"
        val b = "${destTitle.trim().lowercase()}|${destSubtitle.trim().lowercase()}"
        if (a.isNotBlank() && a == b) return true
        if (pickupLatLng != null && destLatLng != null) {
            val meters = SphericalUtil.computeDistanceBetween(pickupLatLng, destLatLng)
            if (meters < 80.0) return true
        }
        return false
    }
}
