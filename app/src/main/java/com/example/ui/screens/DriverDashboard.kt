package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.DriverRecord
import com.example.viewmodel.DriverViewModel
import com.example.viewmodel.PeriodSummary
import com.example.viewmodel.PeriodView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DriverDashboardScreen(
    viewModel: DriverViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(0) } // 0 = Início, 1 = Histórico, 2 = Lançar, 3 = Ajustes
    
    // We observe the states here to pass down nicely
    val stopwatchSeconds by viewModel.stopwatchSeconds.collectAsStateWithLifecycle()
    val isStopwatchRunning by viewModel.isStopwatchRunning.collectAsStateWithLifecycle()
    val lunchStopwatchSeconds by viewModel.lunchStopwatchSeconds.collectAsStateWithLifecycle()
    val isLunchStopwatchRunning by viewModel.isLunchStopwatchRunning.collectAsStateWithLifecycle()
    val googleEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Histórico") },
                    label = { Text("Histórico", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Lançar") },
                    label = { Text("Lançar", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Fluxo Driver",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Controle Inteligente de Corridas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                
                // Real-time background sync / login status indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (googleEmail != null) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                        .clickable { currentTab = 3 }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (googleEmail != null) Icons.Default.CloudDone else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (googleEmail != null) Color(0xFF15803D) else Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (googleEmail != null) "Nuvem ON" else "Local",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (googleEmail != null) Color(0xFF15803D) else Color(0xFF475569)
                        )
                    }
                }
            }

            // Tabs Content Router
            when (currentTab) {
                0 -> HomeTab(
                    viewModel = viewModel,
                    stopwatchSeconds = stopwatchSeconds,
                    isStopwatchRunning = isStopwatchRunning,
                    lunchStopwatchSeconds = lunchStopwatchSeconds,
                    isLunchStopwatchRunning = isLunchStopwatchRunning,
                    googleEmail = googleEmail,
                    onNavigateToForm = { currentTab = 2 }
                )
                1 -> HistoryTab(viewModel = viewModel)
                2 -> QuickLogTab(
                    viewModel = viewModel,
                    stopwatchSeconds = stopwatchSeconds,
                    lunchStopwatchSeconds = lunchStopwatchSeconds,
                    onSuccess = { currentTab = 1 }
                )
                3 -> SettingsTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HomeTab(
    viewModel: DriverViewModel,
    stopwatchSeconds: Long,
    isStopwatchRunning: Boolean,
    lunchStopwatchSeconds: Long,
    isLunchStopwatchRunning: Boolean,
    googleEmail: String?,
    onNavigateToForm: () -> Unit
) {
    val periodSummary by viewModel.periodSummary.collectAsStateWithLifecycle()
    val monthlyProgress by viewModel.monthlyRevenueProgress.collectAsStateWithLifecycle()
    val monthlyGoal by viewModel.monthlyRevenueGoal.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dual Stopwatch Widget Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CRONÔMETROS EM SEGUNDO PLANO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Work Stopwatch
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        border = BorderStroke(1.dp, if (isStopwatchRunning) MaterialTheme.colorScheme.primary else Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isStopwatchRunning) Color(0xFF10B981) else Color(0xFF94A3B8))
                                )
                                Text("Corrida/Trabalho", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            
                            Text(
                                text = formatDuration(stopwatchSeconds),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isStopwatchRunning) viewModel.pauseStopwatch() else viewModel.startStopwatch()
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = if (isStopwatchRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.resetStopwatch() },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Lunch / Break Stopwatch
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        border = BorderStroke(1.dp, if (isLunchStopwatchRunning) Color(0xFFFF9F1C) else Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isLunchStopwatchRunning) Color(0xFFFF9F1C) else Color(0xFF94A3B8))
                                )
                                Text("Pausa / Almoço", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }

                            Text(
                                text = formatDuration(lunchStopwatchSeconds),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isLunchStopwatchRunning) viewModel.pauseLunchStopwatch() else viewModel.startLunchStopwatch()
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF9F1C).copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = if (isLunchStopwatchRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9F1C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.resetLunchStopwatch() },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isStopwatchRunning || isLunchStopwatchRunning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "💡 Os cronômetros estão rodando em segundo plano. Use a notificação fora do app para controlar de forma persistente.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Bento Grid: Dynamic Charts and Summary cards
        Text(
            text = "Estatísticas Detalhadas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Key stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF10B981))
                    Text("Lucro Líquido", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", periodSummary.netProfit),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (periodSummary.netProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Ganho por Hora", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", periodSummary.avgHourlyRate),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Faturamento Bruto", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", periodSummary.totalRevenue),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("KM Rodados", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        text = String.format(Locale("pt", "BR"), "%.1f km", periodSummary.totalKm),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Custom Radial / Bar Chart block
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "GRÁFICO DE FATURAMENTO RECENTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                if (allRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum dado cadastrado para exibir gráficos.\nLançe seu primeiro dia de corridas!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Draw dynamic bar chart inside Jetpack Compose canvas
                    val last7Records = allRecords.take(7).reversed()
                    val maxVal = last7Records.maxOf { it.revenue }.coerceAtLeast(100.0)
                    val chartPrimaryColor = MaterialTheme.colorScheme.primary

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val spacing = 24f
                        val graphWidth = size.width - spacing * 2
                        val graphHeight = size.height - 40f
                        val barWidth = (graphWidth / last7Records.size) - 16f

                        // Draw baseline
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = Offset(spacing, graphHeight),
                            end = Offset(size.width - spacing, graphHeight),
                            strokeWidth = 2f
                        )

                        last7Records.forEachIndexed { idx, record ->
                            val xPos = spacing + (idx * (graphWidth / last7Records.size)) + 8f
                            val pct = (record.revenue / maxVal).toFloat()
                            val barHeight = graphHeight * pct
                            val yPos = graphHeight - barHeight

                            // Draw Bar
                            drawRect(
                                color = if (record.revenue > 0) chartPrimaryColor else Color.LightGray,
                                topLeft = Offset(xPos, yPos),
                                size = Size(barWidth, barHeight)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("← Antigos", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("Trabalhados recentes (Até 7 dias)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Mais recentes →", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Monthly goal widget
        if (monthlyGoal > 0) {
            val progressPercentage = (monthlyProgress / monthlyGoal).coerceIn(0.0, 1.0)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Meta de Faturamento Mensal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = String.format(Locale("pt", "BR"), "%.0f%%", progressPercentage * 100),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercentage.toFloat())
                                .background(Color(0xFF10B981))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f de R$ %.2f", monthlyProgress, monthlyGoal),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun HistoryTab(viewModel: DriverViewModel) {
    val context = LocalContext.current
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val periodSummary by viewModel.periodSummary.collectAsStateWithLifecycle()
    val currentPeriod by viewModel.currentPeriod.collectAsStateWithLifecycle()
    val refDateMillis by viewModel.referenceDateMillis.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period select tab
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Period types row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PeriodView.values().forEach { period ->
                        val isSelected = currentPeriod == period
                        val label = when (period) {
                            PeriodView.DAILY -> "Diário"
                            PeriodView.WEEKLY -> "Semanal"
                            PeriodView.MONTHLY -> "Mensal"
                            PeriodView.ANNUAL -> "Anual"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { viewModel.currentPeriod.value = period }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Period shift controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.shiftPeriod(-1) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }

                    Text(
                        text = formatPeriodLabel(currentPeriod, refDateMillis),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = { viewModel.shiftPeriod(1) }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Avançar")
                    }
                }
            }
        }

        // Summary Card for chosen period
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "RESUMO DO PERÍODO SELECIONADO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dias Trabalhados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("${filteredRecords.size} dias", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Faturado", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text(String.format(Locale("pt", "BR"), "R$ %.2f", periodSummary.totalRevenue), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total de Despesas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text(String.format(Locale("pt", "BR"), "- R$ %.2f", periodSummary.totalExpenses), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saldo Líquido", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", periodSummary.netProfit),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (periodSummary.netProfit >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                    )
                }
            }
        }

        // List elements
        Text(
            text = "Lançamentos e Corridas",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum fechamento lançado para o período selecionado.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredRecords.forEach { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("dd/MM/yyyy (EEE)", Locale("pt", "BR")).format(Date(record.dateMillis)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Deletar",
                                        tint = Color(0xFFB91C1C),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                viewModel.deleteRecord(record)
                                                Toast.makeText(context, "Registro excluído.", Toast.LENGTH_SHORT).show()
                                            }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Faturamento", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(String.format(Locale("pt", "BR"), "R$ %.2f", record.revenue), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Rodado", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text("${record.kmDriven} km", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Horas", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(String.format(Locale("pt", "BR"), "%.1f h", record.hoursWorked), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (record.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Nota: ${record.description}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun QuickLogTab(
    viewModel: DriverViewModel,
    stopwatchSeconds: Long,
    lunchStopwatchSeconds: Long,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var revenueStr by remember { mutableStateOf("") }
    var kmDrivenStr by remember { mutableStateOf("") }
    
    // Auto preload Stopwatch calculations into layout variables!
    val elapsedHoursValue = stopwatchSeconds.toDouble() / 3600.0
    var hoursWorkedStr by remember { mutableStateOf(if (elapsedHoursValue > 0) String.format(Locale.US, "%.2f", elapsedHoursValue) else "") }

    var fuelStr by remember { mutableStateOf("") }
    var mealStr by remember { mutableStateOf("") }
    var rentStr by remember { mutableStateOf("") }
    var miscStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            calendar.set(y, m, d)
            dateMillis = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Lançar Novo Fechamento",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        // Date Picker Button
        OutlinedButton(
            onClick = { datePickerDialog.show() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Data: " + SimpleDateFormat("dd/MM/yyyy (EEEE)", Locale("pt", "BR")).format(Date(dateMillis)),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // Preload hint from active stopwatches
        if (stopwatchSeconds > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable {
                        hoursWorkedStr = String.format(Locale.US, "%.2f", elapsedHoursValue)
                    }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.MoreTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Toque para importar cronômetro ativo:\n${formatDuration(stopwatchSeconds)} (~${String.format(Locale("pt", "BR"), "%.2f", elapsedHoursValue)} h)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Revenue & Distance Inputs
        OutlinedTextField(
            value = revenueStr,
            onValueChange = { revenueStr = it },
            label = { Text("Faturamento Bruto (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = kmDrivenStr,
            onValueChange = { kmDrivenStr = it },
            label = { Text("Quilometragem Rodada (km)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = hoursWorkedStr,
            onValueChange = { hoursWorkedStr = it },
            label = { Text("Horas Trabalhadas") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Expenses section
        Text(
            text = "Despesas de Hoje (Opcional)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = fuelStr,
                onValueChange = { fuelStr = it },
                label = { Text("Combustível") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = mealStr,
                onValueChange = { mealStr = it },
                label = { Text("Alimentação") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = rentStr,
                onValueChange = { rentStr = it },
                label = { Text("Aluguel/Taxas") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = miscStr,
                onValueChange = { miscStr = it },
                label = { Text("Outros Custos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Notas / Observações do Dia") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Submit Button
        Button(
            onClick = {
                val rev = revenueStr.toDoubleOrNull()
                val km = kmDrivenStr.toDoubleOrNull()
                val hrs = hoursWorkedStr.toDoubleOrNull()

                if (rev == null || km == null || hrs == null) {
                    Toast.makeText(context, "Por favor preencha faturamento, quilometragem e horas trabalhadas.", Toast.LENGTH_LONG).show()
                    return@Button
                }

                val item = DriverRecord(
                    dateMillis = dateMillis,
                    revenue = rev,
                    kmDriven = km,
                    hoursWorked = hrs,
                    expenseFuel = fuelStr.toDoubleOrNull() ?: 0.0,
                    expenseMeal = mealStr.toDoubleOrNull() ?: 0.0,
                    expenseRent = rentStr.toDoubleOrNull() ?: 0.0,
                    expenseMisc = miscStr.toDoubleOrNull() ?: 0.0,
                    description = description
                )

                viewModel.addRecord(item)
                
                // Reset timers as they were logged
                viewModel.resetStopwatch()
                viewModel.resetLunchStopwatch()
                
                Toast.makeText(context, "Registro publicado com sucesso!", Toast.LENGTH_SHORT).show()
                onSuccess()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Salvar Fechamento", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SettingsTab(viewModel: DriverViewModel) {
    val context = LocalContext.current
    val googleEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()
    val googleName by viewModel.googleUserName.collectAsStateWithLifecycle()
    val driveFolder by viewModel.driveFolderName.collectAsStateWithLifecycle()
    
    // Manual simulation text field value for test logins
    var manualEmailField by remember { mutableStateOf("") }

    // Persistent values
    val appTheme by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val vehicleOwnType by viewModel.vehicleType.collectAsStateWithLifecycle()
    val fuelEstimateValue by viewModel.fuelPriceEstimate.collectAsStateWithLifecycle()
    val vehicleOwnershipOption by viewModel.vehicleOwnership.collectAsStateWithLifecycle()
    val monthlyTargetVal by viewModel.monthlyRevenueGoal.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User login configuration
        Text(
            text = "Integração e Contas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (googleEmail != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (googleName ?: googleEmail ?: "U").take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column {
                            Text(
                                text = googleName ?: "Conta Conectada",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = googleEmail ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Sincronizando com Google Drive",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.onGoogleSignOut()
                            Toast.makeText(context, "Sessão encerrada com sucesso.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C)),
                        border = BorderStroke(1.dp, Color(0xFFB91C1C).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Desconectar Conta", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Manual Login Field + simulation directly addressing UI refresh failures!
                    Text(
                        text = "Conecte sua conta para habilitar relatórios dinâmicos na nuvem e salvar fechamentos automáticamente no Google Drive.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    OutlinedTextField(
                        value = manualEmailField,
                        onValueChange = { manualEmailField = it },
                        label = { Text("Seu Email Google / Gmail") },
                        placeholder = { Text("exemplo@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (manualEmailField.isBlank() || !manualEmailField.contains("@")) {
                                Toast.makeText(context, "Insira um email Gmail válido para prosseguir.", Toast.LENGTH_LONG).show()
                            } else {
                                // Triggers highly reactive StateFlow & persistent preferences updates!
                                val simulatedName = manualEmailField.substringBefore("@").replaceFirstChar { it.uppercase() }
                                viewModel.onGoogleSignInSuccess(
                                    email = manualEmailField.trim(),
                                    displayName = simulatedName,
                                    photoUrl = null
                                )
                                Toast.makeText(context, "Logado com sucesso como $simulatedName!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Conectar Email", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // App options panel
        Text(
            text = "Configurações Globais",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Theme settings selector
                Text("Tema do Aplicativo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    arrayOf("Sistema", "Claro", "Escuro").forEachIndexed { idx, label ->
                        val isSelected = appTheme == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                .clickable { viewModel.setAppThemeMode(idx) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Monthly Goals
                var goalString by remember { mutableStateOf(if (monthlyTargetVal > 0) monthlyTargetVal.toString() else "") }
                Text("Meta de Faturamento Mensal (R$)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = goalString,
                    onValueChange = { 
                        goalString = it
                        val d = it.toDoubleOrNull() ?: 0.0
                        viewModel.setMonthlyRevenueGoal(d)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Valor Alvo") }
                )

                // Vehicle specific defaults options
                Text("Preço Estimado por Litro/kWh (Combustível)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                var fuelEstimateStr by remember { mutableStateOf(fuelEstimateValue.toString()) }
                OutlinedTextField(
                    value = fuelEstimateStr,
                    onValueChange = {
                        fuelEstimateStr = it
                        viewModel.setFuelPriceEstimate(it.toDoubleOrNull() ?: 5.50)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// Helpers
private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

private fun formatPeriodLabel(period: PeriodView, dateMillis: Long): String {
    val date = Date(dateMillis)
    return when (period) {
        PeriodView.DAILY -> SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("pt", "BR")).format(date)
        PeriodView.WEEKLY -> {
            val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val year = calendar.get(Calendar.YEAR)
            val week = calendar.get(Calendar.WEEK_OF_YEAR)
            "Semana $week, $year"
        }
        PeriodView.MONTHLY -> SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR")).format(date).replaceFirstChar { it.uppercase() }
        PeriodView.ANNUAL -> SimpleDateFormat("yyyy", Locale("pt", "BR")).format(date)
    }
}
