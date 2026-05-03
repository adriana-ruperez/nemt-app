package com.mobapps.nemt.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.mobapps.nemt.R
import com.mobapps.nemt.data.UserProfileRepository

private val AuthBackground = Color(0xFFF3F4F7)
private val AuthSurface = Color(0xFFFFFFFF)
private val AuthBorder = Color(0xFFE4E7EE)
private val AuthPrimary = Color(0xFF111318)
private val AuthSecondary = Color(0xFF7A7F8C)
private val AuthBlue = Color(0xFF2F8FFF)
private val AuthErrorBackground = Color(0xFFFFECEC)
private val AuthErrorText = Color(0xFFB13C3C)
private val AuthSuccessBackground = Color(0xFFE7F6EC)
private val AuthSuccessText = Color(0xFF2D7A50)

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    AuthContainer(
        verticalArrangement = Arrangement.Center
    ) {
        AuthLogo()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "NEMT",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AuthPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Login or register",
            color = AuthSecondary,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryAuthButton(
            text = "Login",
            onClick = onLoginClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        SecondaryAuthButton(
            text = "Register",
            onClick = onRegisterClick
        )
    }
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onOpenRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNeedsVerification: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    AuthContainer(onBack = onBack) {
        AuthLogo()

        Spacer(modifier = Modifier.height(24.dp))

        AuthHeader(
            title = "Login",
            subtitle = "Email and password"
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthTextField(
            value = email,
            onValueChange = {
                email = it.trim()
                message = null
            },
            label = "Email",
            leadingIcon = Icons.Outlined.AlternateEmail,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(12.dp))

        PasswordTextField(
            value = password,
            onValueChange = {
                password = it
                message = null
            },
            label = "Password",
            isVisible = isPasswordVisible,
            onToggleVisibility = { isPasswordVisible = !isPasswordVisible }
        )

        message?.let {
            Spacer(modifier = Modifier.height(14.dp))
            InlineMessage(
                message = it,
                isError = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryAuthButton(
            text = "Login",
            isLoading = isLoading,
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    message = "Complete all fields."
                    return@PrimaryAuthButton
                }

                isLoading = true
                message = null

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false

                        if (!task.isSuccessful) {
                            message = task.exception?.localizedMessage ?: "Login failed."
                            return@addOnCompleteListener
                        }

                        val user = auth.currentUser
                        if (user == null) {
                            message = "Session unavailable."
                            return@addOnCompleteListener
                        }

                        UserProfileRepository.ensureProfile(user) {
                            if (user.isEmailVerified) {
                                onLoginSuccess()
                            } else {
                                onNeedsVerification()
                            }
                        }
                    }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        AuthFooterLink(
            prompt = "No account?",
            action = "Register",
            onClick = onOpenRegister
        )
    }
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onOpenLogin: () -> Unit,
    onRegistrationPendingVerification: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isConfirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(true) }

    AuthContainer(onBack = onBack) {
        AuthLogo()

        Spacer(modifier = Modifier.height(24.dp))

        AuthHeader(
            title = "Register",
            subtitle = "Create a new account"
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthTextField(
            value = email,
            onValueChange = {
                email = it.trim()
                message = null
            },
            label = "Email",
            leadingIcon = Icons.Outlined.AlternateEmail,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(12.dp))

        PasswordTextField(
            value = password,
            onValueChange = {
                password = it
                message = null
            },
            label = "Password",
            isVisible = isPasswordVisible,
            onToggleVisibility = { isPasswordVisible = !isPasswordVisible }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PasswordTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                message = null
            },
            label = "Confirm password",
            isVisible = isConfirmPasswordVisible,
            onToggleVisibility = { isConfirmPasswordVisible = !isConfirmPasswordVisible }
        )

        message?.let {
            Spacer(modifier = Modifier.height(14.dp))
            InlineMessage(
                message = it,
                isError = isError
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryAuthButton(
            text = "Register",
            isLoading = isLoading,
            onClick = {
                when {
                    email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                        isError = true
                        message = "Complete all fields."
                        return@PrimaryAuthButton
                    }

                    password.length < 6 -> {
                        isError = true
                        message = "Minimum 6 characters."
                        return@PrimaryAuthButton
                    }

                    password != confirmPassword -> {
                        isError = true
                        message = "Passwords do not match."
                        return@PrimaryAuthButton
                    }
                }

                isLoading = true
                message = null

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            isLoading = false
                            isError = true
                            message = task.exception?.localizedMessage ?: "Register failed."
                            return@addOnCompleteListener
                        }

                        val user = auth.currentUser
                        if (user == null) {
                            isLoading = false
                            isError = true
                            message = "Profile setup failed."
                            return@addOnCompleteListener
                        }

                        UserProfileRepository.createDefaultProfile(user) { profileResult ->
                            if (profileResult.isFailure) {
                                user.delete().addOnCompleteListener {
                                    isLoading = false
                                    isError = true
                                    message = profileResult.exceptionOrNull()?.localizedMessage
                                        ?: "Profile setup failed."
                                }
                                return@createDefaultProfile
                            }

                            user.sendEmailVerification().addOnCompleteListener {
                                isLoading = false
                                isError = false
                                message = "Verification email sent."
                                onRegistrationPendingVerification()
                            }
                        }
                    }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        AuthFooterLink(
            prompt = "Already registered?",
            action = "Login",
            onClick = onOpenLogin
        )
    }
}

@Composable
fun VerifyEmailScreen(
    email: String,
    onVerified: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    var isResending by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }

    AuthContainer {
        AuthLogo()

        Spacer(modifier = Modifier.height(24.dp))

        AuthHeader(
            title = "Verify email",
            subtitle = email.ifBlank { "Check your inbox" }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthInfoCard(
            text = "Open the link in your email, then come back here."
        )

        message?.let {
            Spacer(modifier = Modifier.height(14.dp))
            InlineMessage(
                message = it,
                isError = isError
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryAuthButton(
            text = "I've verified my email",
            isLoading = isRefreshing,
            onClick = {
                val user = auth.currentUser
                if (user == null) {
                    isError = true
                    message = "Session expired."
                    return@PrimaryAuthButton
                }

                isRefreshing = true
                message = null
                user.reload().addOnCompleteListener { task ->
                    isRefreshing = false

                    if (!task.isSuccessful) {
                        isError = true
                        message = task.exception?.localizedMessage ?: "Could not refresh."
                        return@addOnCompleteListener
                    }

                    if (auth.currentUser?.isEmailVerified == true) {
                        onVerified()
                    } else {
                        isError = true
                        message = "Email not verified yet."
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SecondaryAuthButton(
            text = "Resend email",
            isLoading = isResending,
            onClick = {
                val user = auth.currentUser
                if (user == null) {
                    isError = true
                    message = "Session expired."
                    return@SecondaryAuthButton
                }

                isResending = true
                message = null
                user.sendEmailVerification().addOnCompleteListener { task ->
                    isResending = false
                    isError = !task.isSuccessful
                    message = if (task.isSuccessful) {
                        "Email sent."
                    } else {
                        task.exception?.localizedMessage ?: "Could not resend."
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = onLogout) {
            Text(
                text = "Use another account",
                color = AuthSecondary
            )
        }
    }
}

@Composable
private fun AuthContainer(
    onBack: (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AuthBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuthSurface)
                        .border(1.dp, AuthBorder, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = AuthPrimary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = verticalArrangement,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AuthLogo() {
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(AuthSurface)
            .border(1.dp, AuthBorder, RoundedCornerShape(30.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_sinfondo),
            contentDescription = "NEMT logo",
            modifier = Modifier.size(76.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun AuthHeader(
    title: String,
    subtitle: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = AuthPrimary
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = subtitle,
        color = AuthSecondary,
        fontSize = 14.sp
    )
}

@Composable
private fun AuthInfoCard(
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AuthSurface)
            .border(1.dp, AuthBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.MarkEmailUnread,
            contentDescription = null,
            tint = AuthBlue,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            color = AuthPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null
            )
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AuthSurface,
            unfocusedContainerColor = AuthSurface,
            focusedBorderColor = AuthBlue,
            unfocusedBorderColor = AuthBorder,
            focusedTextColor = AuthPrimary,
            unfocusedTextColor = AuthPrimary,
            focusedLabelColor = AuthBlue,
            unfocusedLabelColor = AuthSecondary,
            focusedLeadingIconColor = AuthBlue,
            unfocusedLeadingIconColor = AuthSecondary,
            focusedTrailingIconColor = AuthBlue,
            unfocusedTrailingIconColor = AuthSecondary
        )
    )
}

@Composable
private fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        leadingIcon = Icons.Outlined.Lock,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (isVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (isVisible) "Hide password" else "Show password"
                )
            }
        }
    )
}

@Composable
private fun PrimaryAuthButton(
    text: String,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthBlue,
            contentColor = Color.White,
            disabledContainerColor = AuthBlue.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SecondaryAuthButton(
    text: String,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthSurface,
            contentColor = AuthPrimary,
            disabledContainerColor = AuthSurface.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = AuthBlue
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AuthFooterLink(
    prompt: String,
    action: String,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = prompt,
            color = AuthSecondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = action,
            color = AuthBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

@Composable
private fun InlineMessage(
    message: String,
    isError: Boolean
) {
    val background = if (isError) AuthErrorBackground else AuthSuccessBackground
    val textColor = if (isError) AuthErrorText else AuthSuccessText

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 14.sp
        )
    }
}
