package com.mobapps.nemt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PanToolAlt
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ColumnScope

private val BackgroundColor = Color(0xFFF3F4F7)
private val CardColor = Color(0xFFFFFFFF)
private val CardHighlight = Color(0xFFF8F8FB)
private val BorderSubtle = Color(0xFFE7E8EE)
private val TextPrimary = Color(0xFF111318)
private val TextSecondary = Color(0xFF7A7F8C)
private val BrandBlue = Color(0xFF2F8FFF)

@Composable
fun ProfileScreen(
    userEmail: String,
    onLogout: () -> Unit,
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
                .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 0.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            HeaderSection(
                userEmail = userEmail
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileContent(
                modifier = Modifier.weight(1f),
                userEmail = userEmail,
                onLogoutClick = onLogout
            )
        }
    }
}

@Composable
private fun HeaderSection(
    userEmail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(CardColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = userEmail.substringBefore("@")
                    .replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    }
                    .ifBlank { "Account" },
                fontSize = 18.sp,
                fontWeight = FontWeight.W700,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = userEmail.ifBlank { "No email available" },
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ProfileContent(
    modifier: Modifier = Modifier,
    userEmail: String,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        SectionTitle(title = "Personal information")
        Spacer(modifier = Modifier.height(8.dp))

        CardContainer {
            InfoRow(
                label = "Full name",
                value = "John Doe"
            )
            DividerLine()
            InfoRow(
                label = "Email",
                value = userEmail.ifBlank { "Not available" }
            )
            DividerLine()
            InfoRow(
                label = "Mobility support",
                value = "Wheelchair assistance"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle(title = "Preferences")
        Spacer(modifier = Modifier.height(8.dp))

        CardContainer {
            SettingsTile(
                icon = Icons.Outlined.FavoriteBorder,
                label = "Medical preferences",
                subtitle = "Allergies, equipment, notes"
            )
            DividerLine()
            SettingsTile(
                icon = Icons.Outlined.NotificationsNone,
                label = "Notifications",
                subtitle = "Ride reminders & updates"
            )
            DividerLine()
            SettingsTile(
                icon = Icons.Outlined.PanToolAlt,
                label = "Accessibility",
                subtitle = "Language, font size, contrast"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle(title = "Support & legal")
        Spacer(modifier = Modifier.height(8.dp))

        CardContainer {
            SettingsTile(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                label = "Help & support",
                subtitle = "Contact our team"
            )
            DividerLine()
            SettingsTile(
                icon = Icons.Outlined.Description,
                label = "Terms & privacy",
                subtitle = "Read policies"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LogoutButton(
            onClick = onLogoutClick
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.W600,
        color = TextSecondary,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CardContainer(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor, RoundedCornerShape(18.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
    ) {
        content()
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = value,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun SettingsTile(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(CardHighlight, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                color = TextPrimary
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        androidx.compose.material3.Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(1.dp)
            .background(BorderSubtle)
    )
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent, RoundedCornerShape(16.dp))
            .border(1.dp, BrandBlue, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Log out",
            color = BrandBlue,
            fontSize = 15.sp,
            fontWeight = FontWeight.W600
        )
    }
}
