package com.example.hubretro

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

    // Google Sign-In launcher
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
            } catch (e: ApiException) { /* handle silently */ }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            authViewModel.resetAuthState()
            onLoginSuccess()
        }
    }

    // Background — same as your app's retro background
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.my_retro_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark overlay so the form is readable
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

            // Pixel art robot icon as header decoration
            Image(
                painter = painterResource(id = R.drawable.robot),
                contentDescription = "RetroHub Robot",
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title — same style as your HOME / ALBUMS titles
            Text(
                text = "SIGN IN",
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = VaporwavePink.copy(alpha = 0.8f),
                        offset = Offset(x = 4f, y = 4f),
                        blurRadius = 8f
                    )
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Welcome back, retro explorer",
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form card — same card style as your NewsItemCard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RetroDarkPurple.copy(alpha = 0.85f))
                    .border(
                        BorderStroke(1.dp, VaporwavePink.copy(alpha = 0.6f)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // Email field
                RetroInputField(
                    value = email,
                    onValueChange = { email = it },
                    label = "EMAIL",
                    keyboardType = KeyboardType.Email
                )

                // Password field
                RetroInputField(
                    value = password,
                    onValueChange = { password = it },
                    label = "PASSWORD",
                    isPassword = true
                )

                // Error message
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

                // Sign In button — same style as your FeatureNavigationCard buttons
                Button(
                    onClick = { authViewModel.signInWithEmail(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaporwavePink,
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
                            text = "SIGN IN",
                            fontFamily = RetroFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Divider with OR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = RetroTextOffWhite.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "  OR  ",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = RetroTextOffWhite.copy(alpha = 0.25f)
                    )
                }

                // Google Sign-In button
                OutlinedButton(
                    onClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, VaporwaveBlue.copy(alpha = 0.8f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RetroTextOffWhite
                    ),
                    enabled = authState !is AuthState.Loading
                ) {
                    Text(
                        text = "CONTINUE WITH GOOGLE",
                        fontFamily = RetroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = VaporwaveBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigate to Create Account
            val annotatedText = buildAnnotatedString {
                withStyle(SpanStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )) {
                    append("New to RetroHub?  ")
                }
                withStyle(SpanStyle(
                    fontFamily = RetroFontFamily,
                    color = VaporwaveCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )) {
                    append("CREATE ACCOUNT")
                }
            }

            TextButton(onClick = onNavigateToCreateAccount) {
                Text(text = annotatedText)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Reusable retro-styled input field
@Composable
fun RetroInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = VaporwavePink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = VaporwavePink.copy(alpha = 0.5f),
                    offset = Offset(1f, 1f),
                    blurRadius = 2f
                )
            ),
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
                fontFamily = RetroFontFamily,
                fontSize = 13.sp,
                color = RetroTextOffWhite
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VaporwavePink,
                unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                focusedTextColor = RetroTextOffWhite,
                unfocusedTextColor = RetroTextOffWhite,
                cursorColor = VaporwavePink,
                focusedContainerColor = Color(0xFF12122A),
                unfocusedContainerColor = Color(0xFF12122A)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}