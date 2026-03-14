package com.mobapps.nemt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobapps.nemt.ui.components.RideCard

private val BackgroundColor = Color(0xFFF3F4F7)
private val CardColor = Color(0xFFFFFFFF)
private val BorderSubtle = Color(0xFFE7E8EE)
private val TextPrimary = Color(0xFF111318)
private val TextSecondary = Color(0xFF7A7F8C)
private val BrandBlue = Color(0xFF2F8FFF)

@Composable
fun TripsScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
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
            FilterRow()

            Spacer(modifier = Modifier.height(16.dp))

            RidesList(
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FilterRow() {
    Row {
        FilterChip(
            label = "Upcoming",
            isActive = true
        )

        Spacer(modifier = Modifier.height(0.dp))
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        FilterChip(
            label = "Completed",
            isActive = false
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        FilterChip(
            label = "Cancelled",
            isActive = false
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    isActive: Boolean
) {
    val background = if (isActive) BrandBlue else CardColor
    val border = if (isActive) BrandBlue else BorderSubtle
    val textColor = if (isActive) TextPrimary else TextSecondary

    Row(
        modifier = Modifier
            .height(32.dp)
            .background(background, RoundedCornerShape(999.dp))
            .border(1.dp, border, RoundedCornerShape(999.dp))
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        SectionHeader(title = "Today")
        Spacer(modifier = Modifier.height(8.dp))

        RideCard(
            status = "In progress",
            dateTime = "Today · 1:15 PM",
            from = "Sunrise Care Home",
            to = "Baptist Hospital",
            patientName = "For: John Doe",
            vehicle = "Unit 5 · Oxigen"
        )

        RideCard(
            status = "Scheduled",
            dateTime = "Today · 17:00 PM",
            from = "123 Main St, Miami, FL",
            to = "Jackson Memorial Hospital",
            patientName = "For: John Doe",
            vehicle = "Unit 12 · Wheelchair"
        )

        Spacer(modifier = Modifier.height(20.dp))

        SectionHeader(title = "This week")
        Spacer(modifier = Modifier.height(8.dp))

        RideCard(
            status = "Scheduled",
            dateTime = "Wed, Feb 26 · 10:30 AM",
            from = "Home",
            to = "Rehab Center North",
            patientName = "For: John Doe",
            vehicle = "Unit 3 · Sedan"
        )

        RideCard(
            status = "Scheduled",
            dateTime = "Fri, Feb 28 · 2:00 PM",
            from = "Care Home West",
            to = "Dialysis Center",
            patientName = "For: John Doe",
            vehicle = "Unit 9 · Wheelchair"
        )

        Spacer(modifier = Modifier.height(20.dp))

        SectionHeader(title = "Past rides")
        Spacer(modifier = Modifier.height(8.dp))

        RideCard(
            status = "Completed",
            dateTime = "Mon, Feb 17 · 11:30 AM",
            from = "Home",
            to = "Clinic Downtown",
            patientName = "For: John Doe",
            vehicle = "Unit 3 · Wheelchair"
        )

        RideCard(
            status = "Completed",
            dateTime = "Sun, Feb 16 · 4:45 PM",
            from = "Sunrise Care Home",
            to = "Central Medical Center",
            patientName = "For: John Doe",
            vehicle = "Unit 2 · Stretcher"
        )
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