package com.mobapps.nemt.maps

import com.google.android.gms.maps.model.LatLng

data class TripTurn(
    val position: LatLng,
    val maneuver: String?,
    val instruction: String
)

/**
 * Driving route details from Google Directions (or a straight-line fallback).
 */
data class TripDirections(
    val polylinePoints: List<LatLng>,
    val durationSeconds: Int,
    val durationText: String,
    val distanceMeters: Int,
    val distanceText: String,
    /** Next turn / start of route in plain text when available from Directions. */
    val nextStepHint: String?,
    /** A few upcoming maneuver points (turns/roundabouts/etc.) */
    val turns: List<TripTurn>
)
