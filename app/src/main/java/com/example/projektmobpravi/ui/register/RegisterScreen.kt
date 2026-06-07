package com.example.projektmobpravi.ui.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.projektmobpravi.ui.navigation.Screen
import com.example.projektmobpravi.ui.theme.*

@Composable
fun RegisterScreen(navController: NavHostController) {
    val viewModel: RegisterViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(DeepGreen, BrandEnd)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentGold.copy(alpha = 0.12f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter   = fadeIn() + slideInVertically(initialOffsetY = { -40 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CashlyLogo()
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text  = "Kreiraj račun",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextOnDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text  = "Počni pratiti svoju potrošnju",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnDark.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text  = "Novi račun",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextDark
                    )
                    Text(
                        text  = "Ispunite sve podatke za registraciju",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value         = username,
                        onValueChange = { username = it; viewModel.clearError() },
                        label         = { Text("Korisničko ime") },
                        leadingIcon   = { Icon(Icons.Default.Person, null, tint = MintGreen) },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MintGreen,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedLabelColor    = MintGreen
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value           = email,
                        onValueChange   = { email = it; viewModel.clearError() },
                        label           = { Text("Email") },
                        leadingIcon     = { Icon(Icons.Default.Email, null, tint = MintGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(12.dp),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MintGreen,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedLabelColor    = MintGreen
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it; viewModel.clearError() },
                        label         = { Text("Lozinka") },
                        leadingIcon   = { Icon(Icons.Default.Lock, null, tint = MintGreen) },
                        trailingIcon  = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(12.dp),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MintGreen,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedLabelColor    = MintGreen
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value         = confirmPassword,
                        onValueChange = { confirmPassword = it; viewModel.clearError() },
                        label         = { Text("Potvrdi lozinku") },
                        leadingIcon   = { Icon(Icons.Default.Lock, null, tint = MintGreen) },
                        trailingIcon  = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible)
                                        Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(12.dp),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MintGreen,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedLabelColor    = MintGreen
                        ),
                        singleLine = true
                    )

                    uiState.errorMessage?.let { error ->
                        Text(
                            text  = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick  = { viewModel.register(username, email, password, confirmPassword) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled  = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = TextOnDark,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text  = "Registracija",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextOnDark
                            )
                        }
                    }

                    TextButton(
                        onClick  = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text  = "Već imaš račun? Prijavi se",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashlyLogo() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.30f),
                shape = RoundedCornerShape(28.dp)
            )
            .background(Color.White.copy(alpha = 0.16f))
    ) {
        Icon(
            imageVector        = Icons.Default.Savings,
            contentDescription = null,
            tint               = TextOnDark,
            modifier           = Modifier
                .size(50.dp)
                .align(Alignment.Center)
        )
        Box(
            modifier         = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AccentGold)
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp, y = (-10).dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "€",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
    }
}
