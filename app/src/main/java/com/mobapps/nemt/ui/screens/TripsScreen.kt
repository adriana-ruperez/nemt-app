package com.mobapps.nemt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobapps.nemt.data.TripStatus
import com.mobapps.nemt.data.TripsRepository
import com.mobapps.nemt.ui.components.RideCard

private val BackgroundColor = Color(0xFFF3F4F7)
private val CardColor = Color(0xFFFFFFFF)
private val BorderSubtle = Color(0xFFE7E8EE)
private val TextPrimary = Color(0xFF111318)
private val TextSecondary = Color(0xFF7A7F8C)
private val BrandBlue = Color(0xFF2F8FFF)
private val TextOnBlue = Color(0xFFFFFFFF)

@Composable
fun TripsScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    LaunchedEffect(Unit) {
        TripsRepository.ensureSeedData()
    }
    var selectedTab by remember { mutableStateOf(TripStatus.UPCOMING) }
    var tripToManage by remember { mutableStateOf<com.mobapps.nemt.data.TripItem?>(null) }
    var editDateTime by remember { mutableStateOf("") }
    var editFrom by remember { mutableStateOf("") }
    var editTo by remember { mutableStateOf("") }
    val allTrips = TripsRepository.trips
    val visibleTrips = allTrips.filter { it.status == selectedTab }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 0.dp)
        ) {
            FilterRow(
                selected = selectedTab,
                onSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RidesList(
                selected = selectedTab,
                trips = visibleTrips,
                onCancelTrip = { tripId ->
                    TripsRepository.cancelTrip(tripId)
                    selectedTab = TripStatus.CANCELLED
                },
                onManageTrip = { trip ->
                    tripToManage = trip
                    editDateTime = trip.dateTime
                    editFrom = trip.from
                    editTo = trip.to
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (tripToManage != null) {
        AlertDialog(
            onDismissRequest = { tripToManage = null },
            title = { Text("Manage trip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editDateTime,
                        onValueChange = { editDateTime = it },
                        label = { Text("Date & time") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editFrom,
                        onValueChange = { editFrom = it },
                        label = { Text("Pickup location") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editTo,
                        onValueChange = { editTo = it },
                        label = { Text("Destination") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trip = tripToManage ?: return@TextButton
                        if (editDateTime.isNotBlank() && editFrom.isNotBlank() && editTo.isNotBlank()) {
                            TripsRepository.updateUpcomingTrip(
                                id = trip.id,
                                newDateTime = editDateTime.trim(),
                                newFrom = editFrom.trim(),
                                newTo = editTo.trim()
                            )
                        }
                        tripToManage = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToManage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FilterRow(
    selected: TripStatus,
    onSelected: (TripStatus) -> Unit
) {
    Row {
        FilterChip(
            label = "Upcoming",
            isActive = selected == TripStatus.UPCOMING,
            onClick = { onSelected(TripStatus.UPCOMING) }
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        FilterChip(
            label = "Completed",
            isActive = selected == TripStatus.COMPLETED,
            onClick = { onSelected(TripStatus.COMPLETED) }
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        FilterChip(
            label = "Cancelled",
            isActive = selected == TripStatus.CANCELLED,
            onClick = { onSelected(TripStatus.CANCELLED) }
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val background = if (isActive) BrandBlue else CardColor
    val border = if (isActive) BrandBlue else BorderSubtle
    val textColor = if (isActive) TextOnBlue else TextSecondary

    Row(
        modifier = Modifier
            .height(32.dp)
            .background(background, RoundedCornerShape(999.dp))
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            color = textColor,
            modifier = Modifier.padding(vertical = 7.dp)
        )
    }
}

@Composable
private fun RidesList(
    selected: TripStatus,
    trips: List<com.mobapps.nemt.data.TripItem>,
    onCancelTrip: (String) -> Unit,
    onManageTrip: (com.mobapps.nemt.data.TripItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        val title = when (selected) {
            TripStatus.UPCOMING -> "Upcoming trips (Spain)"
            TripStatus.COMPLETED -> "Completed trips (Spain)"
            TripStatus.CANCELLED -> "Cancelled trips (Spain)"
        }
        SectionHeader(title = title)
        Spacer(modifier = Modifier.height(8.dp))

        if (trips.isEmpty()) {
            Text(
                text = "No trips in this section yet.",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 10.dp)
            )
            return@Column
        }

        trips.forEach { trip ->
            val statusText = when (trip.status) {
                TripStatus.UPCOMING -> "Scheduled"
                TripStatus.COMPLETED -> "Completed"
                TripStatus.CANCELLED -> "Cancelled"
            }
            RideCard(
                status = statusText,
                dateTime = trip.dateTime,
                from = trip.from,
                to = trip.to,
                patientName = trip.patientName,
                vehicle = trip.vehicle,
                actionLabel = if (trip.status == TripStatus.UPCOMING) "Cancel trip" else null,
                onActionClick = if (trip.status == TripStatus.UPCOMING) {
                    { onCancelTrip(trip.id) }
                } else {
                    null
                },
                secondaryActionLabel = if (trip.status == TripStatus.UPCOMING) "Manage" else null,
                onSecondaryActionClick = if (trip.status == TripStatus.UPCOMING) {
                    { onManageTrip(trip) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.W600,
        color = TextSecondary
    )
}