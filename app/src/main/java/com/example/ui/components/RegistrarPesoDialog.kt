package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RegistrarPesoDialog(
    numeroGanado: String,
    ultimoPeso: Double?,
    primerPeso: Double?,
    onDismiss: () -> Unit,
    onConfirm: (fecha: String, peso: Double, observacion: String) -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    var fecha by remember { mutableStateOf(today.format(formatter)) }
    var pesoInput by remember { mutableStateOf("") }
    var observacion by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pesoDouble = pesoInput.toDoubleOrNull()
    val aumentoAnterior = if (pesoDouble != null && ultimoPeso != null) pesoDouble - ultimoPeso else null
    val aumentoTotal = if (pesoDouble != null && primerPeso != null) pesoDouble - primerPeso else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Registrar Peso",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Ganado Nº $numeroGanado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Fecha selection
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text("Fecha de Pesaje") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fecha_pesaje_input"),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val parsedDate = try {
                                LocalDate.parse(fecha, formatter)
                            } catch (e: Exception) {
                                today
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selected = LocalDate.of(year, month + 1, dayOfMonth)
                                    fecha = selected.format(formatter)
                                },
                                parsedDate.year,
                                parsedDate.monthValue - 1,
                                parsedDate.dayOfMonth
                            ).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Seleccionar fecha"
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Peso input
                OutlinedTextField(
                    value = pesoInput,
                    onValueChange = {
                        pesoInput = it
                        errorMessage = null
                    },
                    label = { Text("Peso (kg) *") },
                    placeholder = { Text("Ej: 420.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("peso_input"),
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live calculations card (Requirement 4: Calculate automatically increase since previous & total increase)
                if (pesoDouble != null && pesoDouble > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Cálculo Automático:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (aumentoAnterior != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (aumentoAnterior >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = if (aumentoAnterior >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Aumento vs anterior ($ultimoPeso kg):",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    val sign = if (aumentoAnterior >= 0) "+" else ""
                                    Text(
                                        text = "$sign${String.format("%.1f", aumentoAnterior)} kg",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (aumentoAnterior >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    )
                                }
                            }

                            if (aumentoTotal != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Aumento total (primer peso: $primerPeso kg):",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    val sign = if (aumentoTotal >= 0) "+" else ""
                                    Text(
                                        text = "$sign${String.format("%.1f", aumentoTotal)} kg",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (aumentoTotal >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    )
                                }
                            }

                            if (ultimoPeso == null) {
                                Text(
                                    text = "Este será el primer peso registrado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Observación input
                OutlinedTextField(
                    value = observacion,
                    onValueChange = { observacion = it },
                    label = { Text("Observación (Opcional)") },
                    placeholder = { Text("Ej: Post-parto, destete, engorde...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("observacion_pesaje_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = pesoInput.toDoubleOrNull()
                    if (p == null || p <= 0) {
                        errorMessage = "Ingrese un peso válido mayor a 0 kg"
                    } else {
                        onConfirm(fecha, p, observacion.trim())
                    }
                },
                modifier = Modifier.testTag("guardar_pesaje_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Guardar Pesaje")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}
