package com.example.hubretro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ✅ Yellow header banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookYellow)
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 64.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "RETROHUB",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 48.sp,
                        letterSpacing = 3.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Join the RetroHub community!",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = ScrapbookDark.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ✅ Form card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                ScrapbookCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = ScrapbookCardWhite,
                    cornerRadius = 16.dp,
                    shadowOffset = 5.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "CREATE ACCOUNT",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 28.sp,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ScrapbookAuthField(
                            value = username,
                            onValueChange = { username = it },
                            label = "USERNAME"
                        )

                        ScrapbookAuthField(
                            value = email,
                            onValueChange = { email = it },
                            label = "EMAIL",
                            keyboardType = KeyboardType.Email
                        )

                        ScrapbookAuthField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordMismatch = false
                            },
                            label = "PASSWORD",
                            isPassword = true
                        )

                        ScrapbookAuthField(
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ScrapbookRed.copy(alpha = 0.1f))
                                    .border(1.dp, ScrapbookRed, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Passwords do not match!",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ScrapbookRed,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Firebase error
                        if (authState is AuthState.Error) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ScrapbookRed.copy(alpha = 0.1f))
                                    .border(1.dp, ScrapbookRed, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = (authState as AuthState.Error).message,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ScrapbookRed,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Create Account button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (authState is AuthState.Loading)
                                        ScrapbookDark.copy(alpha = 0.4f)
                                    else ScrapbookDark
                                )
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                                .clickable(enabled = authState !is AuthState.Loading) {
                                    if (password != confirmPassword) {
                                        passwordMismatch = true
                                    } else {
                                        authViewModel.createAccountWithEmail(
                                            email, password, username
                                        )
                                    }
                                }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (authState is AuthState.Loading) {
                                CircularProgressIndicator(
                                    color = ScrapbookYellow,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "CREATE ACCOUNT",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookYellow,
                                    fontSize = 20.sp,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigate to Login
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToLogin() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                val annotatedText = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 14.sp
                        )
                    ) { append("Already have an account?  ") }
                    withStyle(
                        SpanStyle(
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 16.sp
                        )
                    ) { append("SIGN IN") }
                }
                Text(text = annotatedText, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}