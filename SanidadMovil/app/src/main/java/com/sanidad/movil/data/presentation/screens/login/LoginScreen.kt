package com.sanidad.movil.presentation.screens.login

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Paleta inspirada en tu CSS
private val HealthGreen = Color(0xFF10B981)
private val HealthDark = Color(0xFF065F46)
private val HealthLight = Color(0xFFECFDF5)
private val ClinicalBlue = Color(0xFF3B82F6)
private val SoftGray = Color(0xFFF3F4F6)
private val ErrorRed = Color(0xFFEF4444)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Fondo gradiente suave
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFF0FDF4), Color(0xFFE0F2F1))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Tarjeta de login
        Card(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(16.dp)
                .fillMaxWidth()
                .then(
                    if (isLandscape) Modifier.padding(horizontal = 32.dp)
                    else Modifier
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                // Podemos simular el border sutil con un modifier aparte, pero aquí no es necesario
            )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icono de cruz médica verde
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(HealthGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Encabezado
                Text(
                    text = "Portal Farmacéutico",
                    style = MaterialTheme.typography.headlineSmall,
                    color = HealthDark,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gestión de Inventario y Ventas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Mensaje de error
                AnimatedVisibility(visible = uiState.error != null) {
                    uiState.error?.let { error ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(2.dp, ErrorRed)
                        ) {
                            Text(
                                text = error,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Campos de entrada
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        viewModel.clearError()
                    },
                    label = { Text("Usuario / Matrícula") },
                    placeholder = { Text("Ingrese su usuario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HealthGreen,
                        focusedLabelColor = HealthDark,
                        unfocusedBorderColor = SoftGray,
                        cursorColor = HealthGreen
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        viewModel.clearError()
                    },
                    label = { Text("Contraseña") },
                    placeholder = { Text("••••••••") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HealthGreen,
                        focusedLabelColor = HealthDark,
                        unfocusedBorderColor = SoftGray,
                        cursorColor = HealthGreen
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botón de acceso
                Button(
                    onClick = { viewModel.login(username, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthGreen,
                        contentColor = Color.White,
                        disabledContainerColor = HealthGreen.copy(alpha = 0.6f)
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificando...")
                    } else {
                        Text(
                            "Acceder al Sistema",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Soporte
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "¿Problemas con el acceso? ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "Contactar a Soporte IT",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClinicalBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Navegación automática al éxito
                LaunchedEffect(uiState.isSuccess) {
                    if (uiState.isSuccess) {
                        onLoginSuccess()
                    }
                }
            }
        }
    }
}