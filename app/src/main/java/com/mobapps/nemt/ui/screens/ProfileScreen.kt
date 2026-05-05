package com.mobapps.nemt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PanToolAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.mobapps.nemt.data.UserProfile
import com.mobapps.nemt.data.UserProfileRepository

private val BackgroundColor = Color(0xFFF3F4F7)
private val CardColor = Color(0xFFFFFFFF)
private val CardHighlight = Color(0xFFF8F8FB)
private val BorderSubtle = Color(0xFFE7E8EE)
private val TextPrimary = Color(0xFF111318)
private val TextSecondary = Color(0xFF7A7F8C)
private val BrandBlue = Color(0xFF2F8FFF)
private val SuccessBackground = Color(0xFFE7F6EC)
private val SuccessText = Color(0xFF2D7A50)
private val ErrorBackground = Color(0xFFFFECEC)
private val ErrorText = Color(0xFFB13C3C)
private val GlassWhite = Color(0xDEFFFFFF)
private val GlassBorder = Color(0xCCFFFFFF)
private val GlassShadow = Color(0x66BFD8FF)

private enum class ProfileField(val title: String) {
    FullName("Full name"),
    Phone("Phone"),
    MobilitySupport("Mobility support"),
    MedicalPreferences("Medical preferences"),
    NotificationPreferences("Notifications"),
    AccessibilityNeeds("Accessibility")
}

private val mobilityOptions = listOf(
    "Wheelchair assistance",
    "Walker support",
    "Stretcher transport",
    "Door-to-door escort",
    "Oxygen equipment space",
    "Bariatric transport",
    "Visual assistance",
    "No special support"
)

@Composable
fun ProfileScreen(
    authEmail: String,
    onLogout: () -> Unit,
    onProfileUpdated: (UserProfile) -> Unit,
    onOpenHelpSupport: () -> Unit,
    onOpenTermsPrivacy: () -> Unit,
    contentPadding: PaddingValues
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val fallbackProfile = remember(currentUser?.uid, authEmail) {
        currentUser?.let { UserProfileRepository.defaultProfileFor(it) }
            ?: UserProfile(email = authEmail)
    }

    var profile by remember(currentUser?.uid) { mutableStateOf(fallbackProfile) }
    var isLoading by remember(currentUser?.uid) { mutableStateOf(true) }
    var isSaving by remember(currentUser?.uid) { mutableStateOf(false) }
    var message by remember(currentUser?.uid) { mutableStateOf<String?>(null) }
    var isError by remember(currentUser?.uid) { mutableStateOf(false) }
    var editingField by remember(currentUser?.uid) { mutableStateOf<ProfileField?>(null) }
    var firstNameDraft by remember(currentUser?.uid) { mutableStateOf("") }
    var lastNameDraft by remember(currentUser?.uid) { mutableStateOf("") }
    var editingValue by remember(currentUser?.uid) { mutableStateOf("") }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            isLoading = false
            isError = true
            message = "No active session."
            return@LaunchedEffect
        }

        isLoading = true
        UserProfileRepository.loadProfile(currentUser) { result ->
            isLoading = false
            result.onSuccess { loadedProfile ->
                profile = loadedProfile
                onProfileUpdated(loadedProfile)
            }.onFailure { exception ->
                isError = true
                message = exception.localizedMessage ?: "Could not load profile."
            }
        }
    }

    fun persistProfile(updatedProfile: UserProfile, onSuccess: () -> Unit = {}) {
        if (currentUser == null) {
            isError = true
            message = "No active session."
            return
        }

        isSaving = true
        message = null
        UserProfileRepository.updateProfile(updatedProfile.copy(uid = currentUser.uid)) { result ->
            isSaving = false
            result.onSuccess {
                profile = it
                isError = false
                message = "Profile updated."
                onProfileUpdated(it)
                onSuccess()
            }.onFailure { exception ->
                isError = true
                message = exception.localizedMessage ?: "Could not save profile."
            }
        }
    }

    fun openEditor(field: ProfileField) {
        editingField = field
        when (field) {
            ProfileField.FullName -> {
                firstNameDraft = profile.firstName.orEmpty()
                lastNameDraft = profile.lastName.orEmpty()
            }
            ProfileField.Phone -> editingValue = profile.phone.orEmpty()
            ProfileField.MobilitySupport -> editingValue = profile.mobilitySupport.orEmpty()
            ProfileField.MedicalPreferences -> editingValue = profile.medicalPreferences.orEmpty()
            ProfileField.NotificationPreferences -> editingValue = profile.notificationPreferences.orEmpty()
            ProfileField.AccessibilityNeeds -> editingValue = profile.accessibilityNeeds.orEmpty()
        }
    }

    editingField?.let { field ->
        when (field) {
            ProfileField.FullName -> {
                GlassNameDialog(
                    firstName = firstNameDraft,
                    lastName = lastNameDraft,
                    isSaving = isSaving,
                    onFirstNameChange = { firstNameDraft = it },
                    onLastNameChange = { lastNameDraft = it },
                    onDismiss = {
                        if (!isSaving) editingField = null
                    },
                    onSave = {
                        persistProfile(
                            profile.copy(
                                firstName = firstNameDraft.trim().ifBlank { null },
                                lastName = lastNameDraft.trim().ifBlank { null }
                            )
                        ) {
                            editingField = null
                        }
                    }
                )
            }
            ProfileField.MobilitySupport -> {
                GlassMobilityDialog(
                    selected = editingValue,
                    isSaving = isSaving,
                    onSelect = { editingValue = it },
                    onDismiss = {
                        if (!isSaving) editingField = null
                    },
                    onSave = {
                        persistProfile(
                            profile.copy(mobilitySupport = editingValue.ifBlank { null })
                        ) {
                            editingField = null
                        }
                    }
                )
            }
            else -> {
                GlassTextEditDialog(
                    title = field.title,
                    value = editingValue,
                    isSaving = isSaving,
                    onValueChange = { editingValue = it },
                    onDismiss = {
                        if (!isSaving) editingField = null
                    },
                    onSave = {
                        val updatedProfile = when (field) {
                            ProfileField.Phone -> profile.copy(phone = editingValue.trim().ifBlank { null })
                            ProfileField.MedicalPreferences -> profile.copy(medicalPreferences = editingValue.trim().ifBlank { null })
                            ProfileField.NotificationPreferences -> profile.copy(notificationPreferences = editingValue.trim().ifBlank { null })
                            ProfileField.AccessibilityNeeds -> profile.copy(accessibilityNeeds = editingValue.trim().ifBlank { null })
                            else -> profile
                        }
                        persistProfile(updatedProfile) {
                            editingField = null
                        }
                    }
                )
            }
        }
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
                .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 0.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            HeaderSection(
                profile = profile,
                fallbackEmail = authEmail
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandBlue)
                }
            } else {
                ProfileContent(
                    modifier = Modifier.weight(1f),
                    profile = profile,
                    fallbackEmail = authEmail,
                    message = message,
                    isError = isError,
                    onEditField = ::openEditor,
                    onOpenHelpSupport = onOpenHelpSupport,
                    onOpenTermsPrivacy = onOpenTermsPrivacy,
                    onLogoutClick = onLogout
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    profile: UserProfile,
    fallbackEmail: String
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
            Icon(
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
                text = profile.displayName(fallbackEmail),
                fontSize = 18.sp,
                fontWeight = FontWeight.W700,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = profile.email ?: fallbackEmail.ifBlank { "No email available" },
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ProfileContent(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    fallbackEmail: String,
    message: String?,
    isError: Boolean,
    onEditField: (ProfileField) -> Unit,
    onOpenHelpSupport: () -> Unit,
    onOpenTermsPrivacy: () -> Unit,
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
                value = profile.displayName(fallbackEmail),
                editable = true,
                onClick = { onEditField(ProfileField.FullName) }
            )
            DividerLine()
            InfoRow(
                label = "Email",
                value = profile.email ?: fallbackEmail.ifBlank { "Not available" },
                editable = false
            )
            DividerLine()
            InfoRow(
                label = "Phone",
                value = profile.phone ?: "Not set",
                editable = true,
                onClick = { onEditField(ProfileField.Phone) }
            )
            DividerLine()
            InfoRow(
                label = "Mobility support",
                value = profile.mobilitySupport ?: "Not set",
                editable = true,
                onClick = { onEditField(ProfileField.MobilitySupport) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle(title = "Preferences")
        Spacer(modifier = Modifier.height(8.dp))

        CardContainer {
            SettingsTile(
                icon = Icons.Outlined.FavoriteBorder,
                label = "Medical preferences",
                subtitle = profile.medicalPreferences ?: "Allergies, equipment, notes",
                onClick = { onEditField(ProfileField.MedicalPreferences) }
            )
            DividerLine()
            SettingsTile(
                icon = Icons.Outlined.NotificationsNone,
                label = "Notifications",
                subtitle = profile.notificationPreferences ?: "Ride reminders & updates",
                onClick = { onEditField(ProfileField.NotificationPreferences) }
            )
            DividerLine()
            SettingsTile(
                icon = Icons.Outlined.PanToolAlt,
                label = "Accessibility",
                subtitle = profile.accessibilityNeeds ?: "Language, font size, contrast",
                onClick = { onEditField(ProfileField.AccessibilityNeeds) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle(title = "Support & legal")
        Spacer(modifier = Modifier.height(8.dp))

        CardContainer {
            SettingsTile(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                label = "Help & support",
                subtitle = "Contact methods and assistance hours",
                onClick = onOpenHelpSupport
            )
            DividerLine()
            SettingsTile(
                icon = Icons.Outlined.Description,
                label = "Terms & privacy",
                subtitle = "Read policies",
                onClick = onOpenTermsPrivacy
            )
        }

        if (message != null) {
            Spacer(modifier = Modifier.height(20.dp))
            StatusMessage(
                message = message,
                isError = isError
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
    value: String,
    editable: Boolean,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = editable) { onClick() }
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
            color = if (editable) TextPrimary else TextSecondary,
            modifier = Modifier.weight(2f)
        )

        if (editable) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
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
            Icon(
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

        Icon(
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
private fun StatusMessage(
    message: String,
    isError: Boolean
) {
    val background = if (isError) ErrorBackground else SuccessBackground
    val textColor = if (isError) ErrorText else SuccessText

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun GlassNameDialog(
    firstName: String,
    lastName: String,
    isSaving: Boolean,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    GlassDialogFrame(
        title = "Edit name",
        subtitle = "Update first and last name",
        isSaving = isSaving,
        onDismiss = onDismiss,
        onSave = onSave
    ) {
        GlassField(
            label = "First name",
            value = firstName,
            onValueChange = onFirstNameChange
        )
        Spacer(modifier = Modifier.height(12.dp))
        GlassField(
            label = "Last name",
            value = lastName,
            onValueChange = onLastNameChange
        )
    }
}

@Composable
private fun GlassTextEditDialog(
    title: String,
    value: String,
    isSaving: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    GlassDialogFrame(
        title = title,
        subtitle = "Save changes to your profile",
        isSaving = isSaving,
        onDismiss = onDismiss,
        onSave = onSave
    ) {
        GlassField(
            label = title,
            value = value,
            onValueChange = onValueChange,
            minLines = if (title == "Phone") 1 else 3
        )
    }
}

@Composable
private fun GlassMobilityDialog(
    selected: String,
    isSaving: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    GlassDialogFrame(
        title = "Mobility support",
        subtitle = "Choose the assistance that best matches the rider",
        isSaving = isSaving,
        onDismiss = onDismiss,
        onSave = onSave
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            mobilityOptions.forEach { option ->
                val isSelected = selected == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.55f))
                        .border(
                            1.dp,
                            if (isSelected) BrandBlue.copy(alpha = 0.6f) else GlassBorder.copy(alpha = 0.45f),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassDialogFrame(
    title: String,
    subtitle: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassWhite,
                            Color(0xCFF4F8FF)
                        )
                    )
                )
                .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(GlassShadow, Color.Transparent)
                        )
                    )
            )

            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                content()
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                            .border(1.dp, GlassBorder.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                            .clickable(enabled = !isSaving) { onDismiss() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(BrandBlue)
                            .clickable(enabled = !isSaving) { onSave() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Save",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = minLines == 1,
        minLines = minLines,
        label = { Text(label) },
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.72f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.58f),
            focusedBorderColor = BrandBlue.copy(alpha = 0.65f),
            unfocusedBorderColor = GlassBorder.copy(alpha = 0.7f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = BrandBlue,
            unfocusedLabelColor = TextSecondary
        )
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
