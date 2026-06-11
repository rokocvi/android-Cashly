package com.example.projektmobpravi.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.projektmobpravi.ui.theme.LocalStrings

@Composable
fun LoginScreen(navController: NavHostController) {
    val viewModel: LoginViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val strings = LocalStrings.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

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
        // Dekorativni krugovi
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-90).dp, y = (-90).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentGold.copy(alpha = 0.12f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 90.dp, y = 90.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentGold.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CashlyLogo()
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text  = "Cashly",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextOnDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text  = strings.loginSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnDark.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

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
                        text  = strings.loginWelcome,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextDark
                    )
                    Text(
                        text  = strings.loginWelcomeSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it; viewModel.clearError() },
                        label         = { Text("Email") },
                        leadingIcon   = {
                            Icon(Icons.Default.Email, null, tint = MintGreen)
                        },
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
                        label         = { Text(strings.passwordLabel) },
                        leadingIcon   = {
                            Icon(Icons.Default.Lock, null, tint = MintGreen)
                        },
                        trailingIcon = {
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

                    uiState.errorMessage?.let { error ->
                        Text(
                            text  = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed
                        )
                    }

                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked         = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors          = CheckboxDefaults.colors(
                                checkedColor   = MintGreen,
                                uncheckedColor = TextMuted
                            )
                        )
                        Text(
                            text  = strings.rememberMe,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDark
                        )
                    }

                    Button(
                        onClick  = { viewModel.login(email, password, rememberMe) },
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
                                text  = strings.loginButton,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextOnDark
                            )
                        }
                    }

                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = TextMuted.copy(alpha = 0.25f))
                        Text(
                            text  = "  ${strings.or_}  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = TextMuted.copy(alpha = 0.25f))
                    }

                    OutlinedButton(
                        onClick  = { navController.navigate(Screen.Register.route) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
                    ) {
                        Text(
                            text  = strings.noAccount,
                            style = MaterialTheme.typography.labelLarge
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
        // Jantarni badge s euro znakom
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
