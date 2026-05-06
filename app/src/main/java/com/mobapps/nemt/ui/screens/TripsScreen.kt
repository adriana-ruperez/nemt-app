package com.mobapps.nemt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.mobapps.nemt.data.TripRecord
import com.mobapps.nemt.data.TripStatus
import com.mobapps.nemt.data.toUiTab
import com.mobapps.nemt.data.TripsFirestoreRepository
import com.mobapps.nemt.data.toRideCardStatusLabel
import com.mobapps.nemt.notifications.NemtNotificationType
import com.mobapps.nemt.notifications.NemtNotifications
import com.mobapps.nemt.ui.components.RideCard
import kotlinx.coroutines.awaitCancellation

private val BackgroundColor = Color(0xFFF3F4F7)
private val CardColor = Color(0xFFFFFFFF)
private val BorderSubtle = Color(0xFFE7E8EE)
private val TextPrimary = Color(0xFF111318)
private val TextSecondary = Color(0xFF7A7F8C)
private val BrandBlue = Color(0xFF2F8FFF)
private val BrandRed = Color(0xFFE14B4B)
private val TextOnBlue = Color(0xFFFFFFFF)
private val SwipeCardColor = Color(0xFFFFFFFF)

@Composable
fun TripsScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val riderUid = auth.currentUser?.uid
    val context = LocalContext.current
    val firestoreTrips by TripsFirestoreRepository.trips.collectAsState()

    LaunchedEffect(riderUid) {
        if (riderUid == null) {
            TripsFirestoreRepository.stopListening()
            return@LaunchedEffect
        }
        TripsFirestoreRepository.startListening(riderUid)
        try {
            awaitCancellation()
        } finally {
            TripsFirestoreRepository.stopListening()
        }
    }

    var selectedTab by remember { mutableStateOf(TripStatus.UPCOMING) }
    var tripToManage by remember { mutableStateOf<TripRecord?>(null) }
    var tripPendingCancellation by remember { mutableStateOf<TripRecord?>(null) }
    var isCancellingTrip by remember { mutableStateOf(false) }
    var cancellationError by remember { mutableStateOf<String?>(null) }
    var editDateTime by remember { mutableStateOf("") }
    var editFrom by remember { mutableStateOf("") }
    var editTo by remember { mutableStateOf("") }

    val visibleTrips = remember(firestoreTrips, selectedTab) {
        firestoreTrips.filter { it.lifecycleStatus.toUiTab() == selectedTab }
    }

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

            if (riderUid == null) {
                Text(
                    text = "Sign in to view and manage your trips.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 10.dp)
                )
            } else {
                RidesList(
                    selected = selectedTab,
                    trips = visibleTrips,
                    onCancelTrip = { trip ->
                        cancellationError = null
                        tripPendingCancellation = trip
                    },
                    onManageTrip = { trip ->
                        tripToManage = trip
                        editDateTime = trip.dateTimeDisplay
                        editFrom = trip.originTitle
                        editTo = trip.destinationTitle
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (tripPendingCancellation != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isCancellingTrip) tripPendingCancellation = null
            },
            title = { Text("Cancel ride") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to cancel this ride? This action is irreversible and you will need to rebook the ride.")
                    cancellationError?.let { error ->
                        Text(
                            text = error,
                            color = BrandRed,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trip = tripPendingCancellation ?: return@TextButton
                        isCancellingTrip = true
                        TripsFirestoreRepository.cancelTrip(trip.id) { result ->
                            isCancellingTrip = false
                            result.onSuccess {
                                tripPendingCancellation = null
                                selectedTab = TripStatus.CANCELLED
                            }.onFailure {
                                cancellationError = it.localizedMessage
                                    ?: "Could not cancel this ride."
                            }
                        }
                    },
                    enabled = !isCancellingTrip
                ) {
                    if (isCancellingTrip) {
                        CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirm")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { tripPendingCancellation = null },
                    enabled = !isCancellingTrip
                ) {
                    Text("Keep")
                }
            }
        )
    }

    if (tripToManage != null && riderUid != null) {
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
                            TripsFirestoreRepository.updateTripFields(
                                tripId = trip.id,
                                newDateTimeDisplay = editDateTime.trim(),
                                newFrom = editFrom.trim(),
                                newTo = editTo.trim()
                            ) { result ->
                                result.onSuccess {
                                    NemtNotifications.notifyNow(
                                        context = context,
                                        type = NemtNotificationType.TRIP_UPDATED,
                                        title = "Trip updated",
                                        body = "Your ride details were updated successfully."
                                    )
                                }
                            }
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
    trips: List<TripRecord>,
    onCancelTrip: (TripRecord) -> Unit,
    onManageTrip: (TripRecord) -> Unit,
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
            TripStatus.UPCOMING -> "Upcoming trips"
            TripStatus.COMPLETED -> "Completed trips"
            TripStatus.CANCELLED -> "Cancelled trips"
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
            val isCancellable = selected == TripStatus.UPCOMING
            if (isCancellable) {
                SwipeToCancelRideCard(
                    trip = trip,
                    onCancelTrip = { onCancelTrip(trip) }
                )
            } else {
                RideCard(
                    status = trip.lifecycleStatus.toRideCardStatusLabel(),
                    dateTime = trip.dateTimeDisplay,
                    from = trip.originTitle,
                    to = trip.destinationTitle,
                    patientName = trip.riderDisplayName,
                    vehicle = trip.vehicleLabel,
                    actionLabel = null,
                    onActionClick = null,
                    secondaryActionLabel = null,
                    onSecondaryActionClick = null
                )
            }
        }
    }
}

@Composable
private fun SwipeToCancelRideCard(
    trip: TripRecord,
    onCancelTrip: () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = 124.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val revealThreshold = actionWidthPx * 0.45f
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var isRevealed by remember { mutableStateOf(false) }
    var foregroundHeightPx by remember { mutableIntStateOf(0) }
    val offsetDp = with(density) { offsetPx.toDp() }
    val foregroundHeightDp = with(density) {
        if (foregroundHeightPx > 0) foregroundHeightPx.toDp() else 122.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(foregroundHeightDp)
                .background(BrandRed, RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancelTrip,
                    modifier = Modifier.width(actionWidth)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cancel ride",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W600
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = offsetDp)
                .onSizeChanged { foregroundHeightPx = it.height }
                .pointerInput(isRevealed, actionWidthPx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetPx = (offsetPx + dragAmount).coerceIn(-actionWidthPx, 0f)
                        },
                        onDragEnd = {
                            val shouldReveal = offsetPx <= -revealThreshold
                            isRevealed = shouldReveal
                            offsetPx = if (shouldReveal) -actionWidthPx else 0f
                        }
                    )
                }
                .clickable(enabled = isRevealed) {
                    isRevealed = false
                    offsetPx = 0f
                }
        ) {
            SwipeRideCardContent(
                status = trip.lifecycleStatus.toRideCardStatusLabel(),
                dateTime = trip.dateTimeDisplay,
                from = trip.originTitle,
                to = trip.destinationTitle,
                patientName = trip.riderDisplayName,
                vehicle = trip.vehicleLabel
            )
        }
    }
}

@Composable
private fun SwipeRideCardContent(
    status: String,
    dateTime: String,
    from: String,
    to: String,
    patientName: String,
    vehicle: String?
) {
    val statusColor = when (status) {
        "In progress", "Accepted", "Assigned", "Arrived" -> BrandBlue
        "Completed" -> Color(0xFF30D158)
        "Cancelled" -> BrandRed
        else -> TextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SwipeCardColor, RoundedCornerShape(18.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(statusColor, RoundedCornerShape(999.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W600,
                    color = statusColor
                )
            }
            Text(
                text = dateTime,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(BrandBlue, RoundedCornerShape(999.dp))
                )
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(20.dp)
                        .padding(vertical = 3.dp)
                        .background(BorderSubtle)
                )
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(BrandRed, RoundedCornerShape(999.dp))
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = from,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = to,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            vehicle?.takeIf { it.isNotBlank() }?.let { vehicleLabel ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.width(16.dp).height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = vehicleLabel,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
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
