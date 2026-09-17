package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GanadoEntity
import com.example.data.repository.GanadoRepository
import com.example.ui.theme.FemaleAccent
import com.example.ui.theme.FemaleAccentContainer
import com.example.ui.theme.MaleAccent
import com.example.ui.theme.MaleAccentContainer

@Composable
fun CattleCard(
    ganado: GanadoEntity,
    ultimoPeso: Double?,
    aumentoTotal: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMacho = ganado.sexo.equals("Macho", ignoreCase = true)
    val genderColor = if (isMacho) MaleAccent else FemaleAccent
    val genderContainer = if (isMacho) MaleAccentContainer else FemaleAccentContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("cattle_card_${ganado.numeroGanado}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Nº Ganado, Sexo Badge, Estado Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Number tag
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Nº ${ganado.numeroGanado}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Gender badge
                    Surface(
                        color = genderContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isMacho) Icons.Default.Male else Icons.Default.Female,
                                contentDescription = ganado.sexo,
                                tint = genderColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ganado.sexo,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = genderColor
                                )
                            )
                        }
                    }
                }

                // Estado badge
                EstadoBadge(estado = ganado.estado)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Age, Weight metrics, Family summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    val edadStr = GanadoRepository.calculateAge(ganado.fechaNacimiento)
                    Text(
                        text = "Edad: $edadStr",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Nac: ${ganado.fechaNacimiento}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Parents snippet if present
                    if (!ganado.madre.isNullOrBlank() || !ganado.padre.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val lineage = buildString {
                            if (!ganado.madre.isNullOrBlank()) append("M: ${ganado.madre}")
                            if (!ganado.padre.isNullOrBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append("P: ${ganado.padre}")
                            }
                        }
                        Text(
                            text = lineage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Weight summary on the right
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ultimoPeso != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Scale,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$ultimoPeso kg",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            if (aumentoTotal != null) {
                                val sign = if (aumentoTotal >= 0) "+" else ""
                                Text(
                                    text = "$sign${String.format("%.1f", aumentoTotal)} kg total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (aumentoTotal >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Sin pesajes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Ver ficha",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EstadoBadge(estado: String) {
    val (bgColor, textColor) = when (estado.trim().lowercase()) {
        "activo" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "gestante" -> Color(0xFFF3E5F5) to Color(0xFF7B1FA2)
        "en engorde" -> Color(0xFFE0F2F1) to Color(0xFF00796B)
        "vendido" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "fallecido" -> Color(0xFFECEFF1) to Color(0xFF546E7A)
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = estado,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}
