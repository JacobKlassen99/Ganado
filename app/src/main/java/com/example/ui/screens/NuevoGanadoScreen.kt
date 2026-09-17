package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.FemaleAccent
import com.example.ui.theme.MaleAccent
import com.example.ui.viewmodel.GanadoViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoGanadoScreen(
    viewModel: GanadoViewModel,
    onNavigateBack: () -> Unit,
    onGanadoSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val allGanado by viewModel.allGanado.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    var numeroGanado by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("Hembra") } // "Macho" o "Hembra"
    var fechaNacimiento by remember { mutableStateOf(today.format(formatter)) }
    var madre by remember { mutableStateOf<String?>(null) }
    var padre by remember { mutableStateOf<String?>(null) }
    var estado by remember { mutableStateOf("Activo") }
    var pesoInicial by remember { mutableStateOf("") }
    var observacion by remember { mutableStateOf("") }

    var numeroError by remember { mutableStateOf<String?>(null) }
    var pesoError by remember { mutableStateOf<String?>(null) }

    var expandedMadre by remember { mutableStateOf(false) }
    var expandedPadre by remember { mutableStateOf(false) }

    // Filtrar estrictamente solo hembras para Madre y solo machos para Padre
    val femaleCattle = remember(allGanado) {
        allGanado.filter { it.sexo.equals("Hembra", ignoreCase = true) }
    }
    val maleCattle = remember(allGanado) {
        allGanado.filter { it.sexo.equals("Macho", ignoreCase = true) }
    }

    val estadoList = listOf("Activo", "En engorde", "Gestante", "Vendido", "Fallecido")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nuevo Ganado",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("nuevo_ganado_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nº Ganado
            Column {
                OutlinedTextField(
                    value = numeroGanado,
                    onValueChange = {
                        numeroGanado = it
                        numeroError = null
                    },
                    label = { Text("Nº Ganado *") },
                    placeholder = { Text("Ej: VACA-103, TORO-02, 452") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nuevo_numero_input"),
                    shape = RoundedCornerShape(12.dp),
                    isError = numeroError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
                if (numeroError != null) {
                    Text(
                        text = numeroError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }

            // Sexo: Macho / Hembra
            Column {
                Text(
                    text = "Sexo *",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hembra
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { sexo = "Hembra" }
                            .testTag("nuevo_sexo_hembra"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (sexo == "Hembra") FemaleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (sexo == "Hembra") androidx.compose.foundation.BorderStroke(2.dp, FemaleAccent) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Female,
                                contentDescription = null,
                                tint = if (sexo == "Hembra") FemaleAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hembra",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (sexo == "Hembra") FontWeight.Bold else FontWeight.Normal,
                                    color = if (sexo == "Hembra") FemaleAccent else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    // Macho
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { sexo = "Macho" }
                            .testTag("nuevo_sexo_macho"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (sexo == "Macho") MaleAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (sexo == "Macho") androidx.compose.foundation.BorderStroke(2.dp, MaleAccent) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Male,
                                contentDescription = null,
                                tint = if (sexo == "Macho") MaleAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Macho",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (sexo == "Macho") FontWeight.Bold else FontWeight.Normal,
                                    color = if (sexo == "Macho") MaleAccent else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // Fecha de nacimiento
            OutlinedTextField(
                value = fechaNacimiento,
                onValueChange = { fechaNacimiento = it },
                label = { Text("Fecha de Nacimiento *") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = {
                        val parsed = try {
                            LocalDate.parse(fechaNacimiento, formatter)
                        } catch (e: Exception) {
                            today
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                                fechaNacimiento = selected.format(formatter)
                            },
                            parsed.year,
                            parsed.monthValue - 1,
                            parsed.dayOfMonth
                        ).show()
                    }) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nuevo_fecha_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Madre (SOLO HEMBRAS)
            ExposedDropdownMenuBox(
                expanded = expandedMadre,
                onExpandedChange = { expandedMadre = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (madre != null) "Nº $madre" else "Ninguna / Desconocida",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Madre (solo hembras)") },
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("nuevo_madre_dropdown"),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expandedMadre,
                    onDismissRequest = { expandedMadre = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ninguna / Desconocida") },
                        onClick = {
                            madre = null
                            expandedMadre = false
                        }
                    )
                    femaleCattle.forEach { female ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Female,
                                        contentDescription = null,
                                        tint = FemaleAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Nº ${female.numeroGanado} (${female.estado})")
                                }
                            },
                            onClick = {
                                madre = female.numeroGanado
                                expandedMadre = false
                            }
                        )
                    }
                }
            }

            // Padre (SOLO MACHOS)
            ExposedDropdownMenuBox(
                expanded = expandedPadre,
                onExpandedChange = { expandedPadre = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (padre != null) "Nº $padre" else "Ninguno / Desconocido",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Padre (solo machos)") },
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("nuevo_padre_dropdown"),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expandedPadre,
                    onDismissRequest = { expandedPadre = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ninguno / Desconocido") },
                        onClick = {
                            padre = null
                            expandedPadre = false
                        }
                    )
                    maleCattle.forEach { male ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Male,
                                        contentDescription = null,
                                        tint = MaleAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Nº ${male.numeroGanado} (${male.estado})")
                                }
                            },
                            onClick = {
                                padre = male.numeroGanado
                                expandedPadre = false
                            }
                        )
                    }
                }
            }

            // Estado
            Column {
                Text(
                    text = "Estado *",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    estadoList.take(3).forEach { est ->
                        FilterChip(
                            selected = estado == est,
                            onClick = { estado = est },
                            label = { Text(est) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("nuevo_estado_$est")
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    estadoList.drop(3).forEach { est ->
                        FilterChip(
                            selected = estado == est,
                            onClick = { estado = est },
                            label = { Text(est) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("nuevo_estado_$est")
                        )
                    }
                }
            }

            // Peso inicial
            Column {
                OutlinedTextField(
                    value = pesoInicial,
                    onValueChange = {
                        pesoInicial = it
                        pesoError = null
                    },
                    label = { Text("Peso inicial (kg)") },
                    placeholder = { Text("Ej: 40.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nuevo_peso_inicial_input"),
                    shape = RoundedCornerShape(12.dp),
                    isError = pesoError != null
                )
                if (pesoError != null) {
                    Text(
                        text = pesoError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }

            // Observación
            OutlinedTextField(
                value = observacion,
                onValueChange = { observacion = it },
                label = { Text("Observación") },
                placeholder = { Text("Notas adicionales, raza, color, características...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nuevo_observacion_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Guardar button
            Button(
                onClick = {
                    val cleanNumero = numeroGanado.trim()
                    if (cleanNumero.isBlank()) {
                        numeroError = "El Nº Ganado es obligatorio"
                        return@Button
                    }
                    if (allGanado.any { it.numeroGanado.equals(cleanNumero, ignoreCase = true) }) {
                        numeroError = "Ya existe un animal registrado con el Nº $cleanNumero"
                        return@Button
                    }

                    val pesoVal = if (pesoInicial.isNotBlank()) {
                        val parsed = pesoInicial.toDoubleOrNull()
                        if (parsed == null || parsed <= 0) {
                            pesoError = "Ingrese un peso inicial válido"
                            return@Button
                        }
                        parsed
                    } else null

                    viewModel.addGanado(
                        numeroGanado = cleanNumero,
                        sexo = sexo,
                        fechaNacimiento = fechaNacimiento,
                        madre = madre,
                        padre = padre,
                        estado = estado,
                        pesoInicial = pesoVal,
                        observacion = observacion.trim()
                    )

                    onGanadoSaved(cleanNumero.uppercase())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("guardar_ganado_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Registrar Ganado",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
