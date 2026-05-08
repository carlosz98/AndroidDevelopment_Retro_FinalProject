package com.example.hubretro

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hubretro.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToCreateAccount: () -> Unit
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("362544702533-jae4a67e1l2lck7j5etpbdg69hj58mne.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { authViewModel.signInWithGoogle(it) }
            } catch (e: ApiException) { }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            authViewModel.resetAuthState()
            onLoginSuccess()
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
                        text = "Welcome back, retro explorer!",
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
                            text = "SIGN IN",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 28.sp,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Email field
                        ScrapbookAuthField(
                            value = email,
                            onValueChange = { email = it },
                            label = "EMAIL",
                            keyboardType = KeyboardType.Email
                        )

                        // Password field
                        ScrapbookAuthField(
                            value = password,
                            onValueChange = { password = it },
                            label = "PASSWORD",
                            isPassword = true
                        )

                        // Error message
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

                        // Sign In button
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
                                    authViewModel.signInWithEmail(email, password)
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
                                    text = "SIGN IN",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookYellow,
                                    fontSize = 20.sp,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // Divider OR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = ScrapbookBorder.copy(alpha = 0.2f)
                            )
                            Text(
                                text = "  OR  ",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 12.sp
                            )
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = ScrapbookBorder.copy(alpha = 0.2f)
                            )
                        }

                        // Google button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ScrapbookPaper)
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                                .clickable(enabled = authState !is AuthState.Loading) {
                                    googleLauncher.launch(googleSignInClient.signInIntent)
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CONTINUE WITH GOOGLE",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigate to Create Account
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToCreateAccount() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                val annotatedText = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 14.sp
                        )
                    ) { append("New to RetroHub?  ") }
                    withStyle(
                        SpanStyle(
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 16.sp
                        )
                    ) { append("CREATE ACCOUNT") }
                }
                Text(text = annotatedText, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ✅ Scrapbook Auth Input Field
@Composable
fun ScrapbookAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPassword) PasswordVisualTransformation()
            else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                color = ScrapbookTextDark
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ScrapbookDark,
                unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                focusedTextColor = ScrapbookTextDark,
                unfocusedTextColor = ScrapbookTextDark,
                cursorColor = ScrapbookDark,
                focusedContainerColor = ScrapbookCardWhite,
                unfocusedContainerColor = ScrapbookCardWhite
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

// Keep RetroInputField for backward compatibility
@Composable
fun RetroInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    ScrapbookAuthField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        isPassword = isPassword,
        keyboardType = keyboardType
    )
}