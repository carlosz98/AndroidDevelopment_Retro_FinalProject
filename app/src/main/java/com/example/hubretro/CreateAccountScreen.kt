package com.example.hubretro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hubretro.ui.theme.*

@Composable
fun CreateAccountScreen(
    authViewModel: AuthViewModel = viewModel(),
    onAccountCreated: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordMismatch by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            authViewModel.resetAuthState()
            onAccountCreated()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Same retro background as the rest of the app
        Image(
            painter = painterResource(id = R.drawable.my_retro_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Robot mascot header
            Image(
                painter = painterResource(id = R.drawable.robot),
                contentDescription = "RetroHub Robot",
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "CREATE\nACCOUNT",
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = VaporwavePurple.copy(alpha = 0.8f),
                        offset = Offset(x = 4f, y = 4f),
                        blurRadius = 8f
                    )
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Join the RetroHub community",
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RetroDarkPurple.copy(alpha = 0.85f))
                    .border(
                        BorderStroke(1.dp, VaporwavePurple.copy(alpha = 0.6f)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // Username
                RetroInputField(
                    value = username,
                    onValueChange = { username = it },
                    label = "USERNAME"
                )

                // Email
                RetroInputField(
                    value = email,
                    onValueChange = { email = it },
                    label = "EMAIL",
                    keyboardType = KeyboardType.Email
                )

                // Password
                RetroInputField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordMismatch = false
                    },
                    label = "PASSWORD",
                    isPassword = true
                )

                // Confirm Password
                RetroInputField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        passwordMismatch = false
                    },
                    label = "CONFIRM PASSWORD",
                    isPassword = true
                )

                // Password mismatch warning
                if (passwordMismatch) {
                    Text(
                        text = "Passwords do not match!",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = SynthwaveOrange,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Firebase error message
                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = SynthwaveOrange,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Create Account button
                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            passwordMismatch = true
                        } else {
                            authViewModel.createAccountWithEmail(email, password, username)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaporwavePurple,
                        contentColor = RetroTextOffWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    ),
                    enabled = authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "CREATE ACCOUNT",
                            fontFamily = RetroFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigate back to Login
            val annotatedText = buildAnnotatedString {
                withStyle(SpanStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )) {
                    append("Already have an account?  ")
                }
                withStyle(SpanStyle(
                    fontFamily = RetroFontFamily,
                    color = VaporwavePink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )) {
                    append("SIGN IN")
                }
            }

            TextButton(onClick = onNavigateToLogin) {
                Text(text = annotatedText)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}