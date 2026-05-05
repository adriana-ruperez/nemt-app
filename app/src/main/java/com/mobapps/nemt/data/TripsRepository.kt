package com.mobapps.nemt.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class TripStatus {
    UPCOMING,
    COMPLETED,
    CANCELLED
}

data class TripItem(
    val id: String,
    val status: TripStatus,
    val dateTime: String,
    val from: String,
    val to: String,
    val patientName: String,
    val vehicle: String
)

object TripsRepository {
    private val _trips = mutableStateListOf<TripItem>()
    val trips: SnapshotStateList<TripItem> = _trips

    fun ensureSeedData() {
        if (_trips.isNotEmpty()) return
        _trips.addAll(generateSpainTrips())
    }

    fun cancelTrip(id: String) {
        val idx = _trips.indexOfFirst { it.id == id }
        if (idx == -1) return
        val trip = _trips[idx]
        if (trip.status != TripStatus.UPCOMING) return
        _trips[idx] = trip.copy(status = TripStatus.CANCELLED)
    }

    fun addConfirmedUpcomingTrip(
        from: String,
        to: String,
        patientName: String = "For: You",
        vehicle: String = "Unit A12 · Wheelchair Van",
        dateTime: String = "Today · ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}"
    ) {
        val newTrip = TripItem(
            id = "up_confirmed_${System.currentTimeMillis()}",
            status = TripStatus.UPCOMING,
            dateTime = dateTime,
            from = from,
            to = to,
            patientName = patientName,
            vehicle = vehicle
        )
        _trips.add(0, newTrip)
    }

    fun updateUpcomingTrip(
        id: String,
        newDateTime: String,
        newFrom: String,
        newTo: String
    ) {
        val idx = _trips.indexOfFirst { it.id == id }
        if (idx == -1) return
        val trip = _trips[idx]
        if (trip.status != TripStatus.UPCOMING) return
        _trips[idx] = trip.copy(
            dateTime = newDateTime,
            from = newFrom,
            to = newTo
        )
    }

    private fun generateSpainTrips(): List<TripItem> {
        val random = Random(44)
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
            "Rúa do Vilar 19, Santiago de Compostela",
            "Avenida Diagonal 640, Barcelona",
            "Calle Bailén 15, Bilbao",
            "Avenida de Anaga 9, Santa Cruz de Tenerife",
            "Passeig de Gràcia 88, Barcelona",
            "Calle Alfonso I 32, Zaragoza",
            "Avenida de América 55, Madrid",
            "Avenida de la Aurora 12, Málaga"
        )
        val destinations = listOf(
            "Hospital Universitario La Paz, Madrid",
            "Hospital Clínic de Barcelona, Barcelona",
            "Hospital Virgen del Rocío, Sevilla",
            "Hospital La Fe, Valencia",
            "Hospital Regional de Málaga, Málaga",
            "Hospital San Cecilio, Granada",
            "Hospital Miguel Servet, Zaragoza",
            "Hospital Universitario Central, Asturias",
            "Hospital Universitario de Canarias, Tenerife",
            "Hospital Universitario de Gran Canaria Dr. Negrín",
            "Hospital de Cruces, Bilbao",
            "Hospital Clínico Universitario, Santiago",
            "Hospital Son Espases, Palma",
            "Hospital Universitario de Navarra, Pamplona"
        )
        val vehicles = listOf(
            "Unit 12 · Wheelchair Van",
            "Unit 7 · Accessible Sedan",
            "Unit 3 · Lift-equipped Van",
            "Unit 19 · Mobility Assist",
            "Unit 23 · Oxygen Support Van",
            "Unit 31 · Bariatric Assist"
        )
        val names = listOf(
            "For: Sofia R.",
            "For: Daniel M.",
            "For: Elena C.",
            "For: Pablo G.",
            "For: Lucia A.",
            "For: Mateo V.",
            "For: Carla N.",
            "For: Hugo D."
        )

        val upcomingTimes = listOf(
            "Today · 6:10 PM",
            "Today · 8:20 PM",
            "Tomorrow · 8:45 AM",
            "Tomorrow · 12:15 PM",
            "Thu · 9:40 AM",
            "Thu · 3:10 PM",
            "Fri · 10:20 AM",
            "Fri · 5:50 PM",
            "Sat · 11:35 AM",
            "Sun · 4:05 PM"
        )
        val completedTimes = listOf(
            "Mon · 9:30 AM",
            "Mon · 2:20 PM",
            "Sun · 3:05 PM",
            "Sun · 6:40 PM",
            "Sat · 11:20 AM",
            "Sat · 4:15 PM",
            "Fri · 8:05 AM",
            "Fri · 6:45 PM",
            "Thu · 10:10 AM",
            "Wed · 1:55 PM"
        )
        val cancelledTimes = listOf(
            "Today · 8:00 AM",
            "Today · 11:20 AM",
            "Tue · 12:30 PM",
            "Tue · 7:10 PM",
            "Mon · 5:10 PM",
            "Sun · 9:45 AM",
            "Sat · 2:25 PM",
            "Fri · 3:00 PM"
        )

        fun pickRoute(i: Int): Pair<String, String> {
            val from = pickups[i % pickups.size]
            val to = destinations[(i + random.nextInt(1, 4)) % destinations.size]
            return from to to
        }

        val out = mutableListOf<TripItem>()
        upcomingTimes.forEachIndexed { i, t ->
            val (from, to) = pickRoute(i)
            out += TripItem(
                id = "up_$i",
                status = TripStatus.UPCOMING,
                dateTime = t,
                from = from,
                to = to,
                patientName = names[i % names.size],
                vehicle = vehicles[i % vehicles.size]
            )
        }
        completedTimes.forEachIndexed { i, t ->
            val (from, to) = pickRoute(i + 10)
            out += TripItem(
                id = "done_$i",
                status = TripStatus.COMPLETED,
                dateTime = t,
                from = from,
                to = to,
                patientName = names[(i + 1) % names.size],
                vehicle = vehicles[(i + 2) % vehicles.size]
            )
        }
        cancelledTimes.forEachIndexed { i, t ->
            val (from, to) = pickRoute(i + 20)
            out += TripItem(
                id = "cancel_$i",
                status = TripStatus.CANCELLED,
                dateTime = t,
                from = from,
                to = to,
                patientName = names[(i + 2) % names.size],
                vehicle = vehicles[(i + 3) % vehicles.size]
            )
        }
        return out
    }
}
