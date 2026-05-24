package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DriverRecord
import com.example.viewmodel.DriverViewModel
import com.example.viewmodel.PeriodSummary
import com.example.viewmodel.PeriodView
import java.text.SimpleDateFormat
import java.util.*

// Creative Direction: Bento Grid Design Theme (Light modern, pure whites, slate 900 text, cozy shadows)
private val BentoBg = Color(0xFFF7F9FF)
private val BentoTextDark = Color(0xFF0F172A)
private val BentoTextSlate = Color(0xFF64748B)
private val BentoPrimaryBlue = Color(0xFF2563EB)
private val BentoBorder = Color(0xFFE2E8F0)
private val BentoSurface = Color(0xFFFFFFFF)

private val BentoOrangeBg = Color(0xFFFFEDD5)
private val BentoOrangeText = Color(0xFFEA580C)

private val BentoGreenBg = Color(0xFFDCFCE7)
private val BentoGreenText = Color(0xFF16A34A)

private val BentoRedBg = Color(0xFFFEE2E2)
private val BentoRedText = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(
    viewModel: DriverViewModel,
    modifier: Modifier = Modifier
) {
    val currentPeriod by viewModel.currentPeriod.collectAsStateWithLifecycle()
    val referenceMillis by viewModel.referenceDateMillis.collectAsStateWithLifecycle()
    val summary by viewModel.periodSummary.collectAsStateWithLifecycle()
    val records by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val editingRecord by viewModel.editingRecord.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var prefilledStopwatchHours by remember { mutableStateOf<Double?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Driver",
                            tint = BentoPrimaryBlue,
                            modifier = Modifier.size(32.dp).padding(end = 8.dp)
                        )
                        Text(
                            text = "Driver Finanças",
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoTextDark,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoBg,
                    titleContentColor = BentoTextDark
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BentoPrimaryBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar Lançamento") },
                text = { Text("Lançar Dia", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("add_record_fab")
            )
        },
        containerColor = BentoBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BentoBg)
        ) {
            // Period selector tabs: Diário, Semanal, Mensal (Bento Segmented Control style)
            PeriodTabs(
                selectedPeriod = currentPeriod,
                onPeriodSelected = { viewModel.setPeriodType(it) }
            )

            // Date navigation controls
            DateNavigator(
                label = summary.periodLabel,
                onPrevious = { viewModel.shiftPeriod(-1) },
                onNext = { viewModel.shiftPeriod(1) },
                onToday = { viewModel.setReferenceDate(System.currentTimeMillis()) }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Work stopwatch tracking card
                item {
                    WorkStopwatchCard(
                        viewModel = viewModel,
                        onLaunchWithTime = { hours ->
                            prefilledStopwatchHours = hours
                            showAddDialog = true
                        }
                    )
                }

                // KPIs metrics block
                item {
                    DashboardKPIs(summary = summary)
                }

                // Balance indicator visual bar (Faturamento x Despesas)
                item {
                    BalanceVisualBar(summary = summary)
                }

                // Table label & Action Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tabela de Produtividade",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextDark
                        )
                        if (records.isNotEmpty()) {
                            Text(
                                text = "${records.size} registro(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoTextSlate
                            )
                        }
                    }
                }

                // Financial table matching user requested explicit columns
                item {
                    if (records.isEmpty()) {
                        EmptyStateCard()
                    } else {
                        FinancialTable(
                            records = records,
                            onRecordClick = { viewModel.selectRecordForEdit(it) }
                        )
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showAddDialog) {
        AddEditRecordDialog(
            viewModel = viewModel,
            recordToEdit = null,
            prefilledHours = prefilledStopwatchHours,
            onDismiss = {
                showAddDialog = false
                prefilledStopwatchHours = null
            }
        )
    }

    if (editingRecord != null) {
        AddEditRecordDialog(
            viewModel = viewModel,
            recordToEdit = editingRecord,
            prefilledHours = null,
            onDismiss = { viewModel.selectRecordForEdit(null) }
        )
    }
}

@Composable
fun PeriodTabs(
    selectedPeriod: PeriodView,
    onPeriodSelected: (PeriodView) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PeriodView.values().forEach { period ->
                val isSelected = selectedPeriod == period
                val label = when(period) {
                    PeriodView.DAILY -> "Diário"
                    PeriodView.WEEKLY -> "Semanal"
                    PeriodView.MONTHLY -> "Mensal"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onPeriodSelected(period) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BentoPrimaryBlue else BentoTextSlate
                    )
                }
            }
        }
    }
}

@Composable
fun DateNavigator(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.testTag("nav_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Anterior",
                    tint = BentoTextDark
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).clickable { onToday() }
            ) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Clique p/ ir para Hoje",
                    fontSize = 11.sp,
                    color = BentoPrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.testTag("nav_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Próximo",
                    tint = BentoTextDark
                )
            }
        }
    }
}

@Composable
fun DashboardKPIs(summary: PeriodSummary) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Bento Net Profit card (Glorious Gradient Blue backdrop, white labels)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BentoPrimaryBlue),
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BentoPrimaryBlue, Color(0xFF1D4ED8))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LUCRO LÍQUIDO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 1.2.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f", summary.netProfit),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-1).sp
                        )
                        
                        // Efficiency feedback chip based on hours worked
                        val ratingFactor = if (summary.totalHours > 0) summary.netProfit / summary.totalHours else 0.0
                        val chipText = when {
                            ratingFactor >= 40.0 -> "Excelente"
                            ratingFactor >= 25.0 -> "Bom Ritmo"
                            ratingFactor > 0.0 -> "Rendimento Regular"
                            else -> "Inativo"
                        }
                        if (summary.netProfit != 0.0) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = chipText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Faturamento Bruto", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
                            Text(
                                text = String.format(Locale("pt", "BR"), "R$ %.2f", summary.totalRevenue),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Despesas Totais", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
                            Text(
                                text = String.format(Locale("pt", "BR"), "R$ %.2f", summary.totalExpenses),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Secondary Bento Row of metrics (Orange Ganho/Hora, Green Ganho/Km)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rate metrics card: per hour/minute (Orange Bento Design)
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BentoOrangeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = BentoOrangeText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ganho / Hora",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextSlate
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f/h", summary.netHourlyRate),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = BentoTextDark,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f/min", summary.netMinutelyRate),
                            fontSize = 10.sp,
                            color = BentoTextSlate
                        )
                    }
                }
            }

            // Kilometric indicators: profit and expense per km (Green Bento Design)
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BentoGreenBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = BentoGreenText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Rendimento / Km",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextSlate
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f/km", summary.profitPerKm),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = BentoTextDark,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "Custo: R$ %.2f/km", summary.costPerKm),
                            fontSize = 10.sp,
                            color = BentoTextSlate
                        )
                    }
                }
            }
        }

        // Distance & Hours summary list card (Clean Bento block list)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "RESUMO DA OPERAÇÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = BentoTextSlate,
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE0E7FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text("Horas Trabalhadas", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoTextDark)
                    }
                    Text(
                        text = String.format(Locale("pt", "BR"), "%.1f h", summary.totalHours),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = BentoTextDark
                    )
                }

                HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(BentoGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = BentoGreenText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text("Km Rodados", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoTextDark)
                    }
                    Text(
                        text = String.format(Locale("pt", "BR"), "%.1f km", summary.totalKm),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = BentoTextDark
                    )
                }

                HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(BentoRedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = BentoRedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text("Alimentação & Outros", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoTextDark)
                    }
                    Text(
                        text = String.format(Locale("pt", "BR"), "- R$ %.2f", summary.totalMeal + summary.totalMisc),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = BentoRedText
                    )
                }

                HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF3F4F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CarRental,
                                contentDescription = null,
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text("Aluguel do Veículo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoTextDark)
                    }
                    Text(
                        text = String.format(Locale("pt", "BR"), "- R$ %.2f", summary.totalRent),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = BentoRedText
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceVisualBar(summary: PeriodSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISTRIBUIÇÃO DA RECEITA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoTextSlate,
                    letterSpacing = 1.sp
                )
                
                val expensePct = if (summary.totalRevenue > 0) (summary.totalExpenses / summary.totalRevenue) * 100 else 0.0
                val profitPct = if (summary.totalRevenue > 0) (summary.netProfit / summary.totalRevenue) * 100 else 100.0
                Text(
                    text = String.format(Locale("pt", "BR"), "Lucro: %.0f%% | Custos: %.0f%%", profitPct.coerceIn(0.0..100.0), expensePct.coerceIn(0.0..100.0)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoGreenText
                )
            }

            // Elegant horizontal multi-color progress bar representing finances
            val total = summary.totalRevenue
            val mealRatio = if (total > 0) (summary.totalMeal / total).toFloat().coerceIn(0f..1f) else 0f
            val rentRatio = if (total > 0) (summary.totalRent / total).toFloat().coerceIn(0f..1f) else 0f
            val miscRatio = if (total > 0) (summary.totalMisc / total).toFloat().coerceIn(0f..1f) else 0f
            val netRatio = if (total > 0) (summary.netProfit / total).toFloat().coerceIn(0f..1f) else 1f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (netRatio > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(netRatio.coerceAtLeast(0.001f))
                                .background(BentoGreenText)
                        )
                    }
                    if (mealRatio > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(mealRatio.coerceAtLeast(0.001f))
                                .background(BentoOrangeText) // Amber Food
                        )
                    }
                    if (rentRatio > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(rentRatio.coerceAtLeast(0.001f))
                                .background(BentoPrimaryBlue) // Blue Rent
                        )
                    }
                    if (miscRatio > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(miscRatio.coerceAtLeast(0.001f))
                                .background(BentoRedText) // Red Misc
                        )
                    }
                }
            }

            // Legend indicators
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendItem(color = BentoGreenText, label = "Lucro Líquido")
                LegendItem(color = BentoOrangeText, label = "Alimentação")
                LegendItem(color = BentoPrimaryBlue, label = "Aluguel")
                LegendItem(color = BentoRedText, label = "Miscelâneas")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BentoTextSlate)
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = BentoTextSlate,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Nenhum lançamento no período",
                fontWeight = FontWeight.Bold,
                color = BentoTextDark,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Toque no botão 'Lançar Dia' no canto inferior direito para adicionar seções de trabalho, km rodados e faturamento.",
                color = BentoTextSlate,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// Spreadsheet Horizontal Ledger Table matching user "organizar em tabelas" request
@Composable
fun FinancialTable(
    records: List<DriverRecord>,
    onRecordClick: (DriverRecord) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        // Horizontally scrolling layout to accommodate clean analytical columns
        val horizontalScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
                .padding(vertical = 8.dp)
        ) {
            // Table Header Row
            Row(
                modifier = Modifier
                    .background(BentoSurface)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeaderCell("Data", width = 90.dp)
                TableHeaderCell("Horas", width = 70.dp)
                TableHeaderCell("Km Rodado", width = 85.dp)
                TableHeaderCell("Faturado", width = 95.dp)
                TableHeaderCell("Despesas", width = 90.dp)
                TableHeaderCell("Lucro Líq.", width = 95.dp)
                TableHeaderCell("Ganho/Hora", width = 95.dp)
                TableHeaderCell("Notas", width = 120.dp)
            }

            HorizontalDivider(color = BentoBorder, thickness = 1.5.dp)

            // Table Rows
            records.forEach { record ->
                Row(
                    modifier = Modifier
                        .clickable { onRecordClick(record) }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(record.getFormattedDate(), width = 90.dp, color = BentoTextDark, fontWeight = FontWeight.Bold)
                    TableCell(String.format(Locale("pt", "BR"), "%.1fh", record.hoursWorked), width = 70.dp)
                    TableCell(String.format(Locale("pt", "BR"), "%.1f km", record.kmDriven), width = 85.dp)
                    TableCell(String.format(Locale("pt", "BR"), "R$ %.2f", record.revenue), width = 95.dp, color = BentoGreenText, fontWeight = FontWeight.SemiBold)
                    TableCell(String.format(Locale("pt", "BR"), "R$ %.2f", record.totalExpenses), width = 90.dp, color = BentoRedText, fontWeight = FontWeight.Medium)
                    TableCell(String.format(Locale("pt", "BR"), "R$ %.2f", record.netProfit), width = 95.dp, color = if (record.netProfit >= 0) BentoGreenText else BentoRedText, fontWeight = FontWeight.Bold)
                    TableCell(String.format(Locale("pt", "BR"), "R$ %.2f/h", record.netHourlyRate), width = 95.dp, color = BentoPrimaryBlue, fontWeight = FontWeight.Bold)
                    TableCell(if (record.description.isEmpty()) "—" else record.description, width = 120.dp, maxLines = 1)
                }
                HorizontalDivider(color = BentoBorder, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        color = BentoTextSlate,
        textAlign = TextAlign.Start
    )
}

@Composable
fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = BentoTextDark,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        fontSize = 13.sp,
        fontWeight = fontWeight,
        color = color,
        textAlign = TextAlign.Start,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecordDialog(
    viewModel: DriverViewModel,
    recordToEdit: DriverRecord?,
    prefilledHours: Double? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEdit = recordToEdit != null

    // Fetch the last registered rent to auto-fill for nice user accessibility
    val lastRent by viewModel.lastRentValue.collectAsStateWithLifecycle()

    // Form inputs state
    var dateMillis by remember { mutableStateOf(recordToEdit?.dateMillis ?: System.currentTimeMillis()) }
    var hoursStr by remember { 
        mutableStateOf(
            recordToEdit?.hoursWorked?.toString()?.replace(".", ",") 
                ?: prefilledHours?.let { String.format(Locale.forLanguageTag("pt-BR"), "%.2f", it) } 
                ?: ""
        )
    }
    var kmStr by remember { mutableStateOf(recordToEdit?.kmDriven?.toString()?.replace(".", ",") ?: "") }
    var revenueStr by remember { mutableStateOf(recordToEdit?.revenue?.toString()?.replace(".", ",") ?: "") }
    var mealStr by remember { mutableStateOf(recordToEdit?.expenseMeal?.toString()?.replace(".", ",") ?: "") }
    // If and only if addition is triggered and lastRent exists, auto-fill the lastRent value to save repetitive work!
    var rentStr by remember {
        mutableStateOf(
            recordToEdit?.expenseRent?.toString()?.replace(".", ",") 
                ?: if (lastRent > 0) lastRent.toString().replace(".", ",") else ""
        )
    }
    var miscStr by remember { mutableStateOf(recordToEdit?.expenseMisc?.toString()?.replace(".", ",") ?: "") }
    var descStr by remember { mutableStateOf(recordToEdit?.description ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("record_dialog_card"),
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header of dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "Editar Lançamento" else "Lançar Atividade",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = BentoTextSlate)
                    }
                }

                HorizontalDivider(color = BentoBorder)

                // Date Picker Field
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Text(
                    text = "DATA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoTextSlate
                )
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = dateMillis
                        }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    set(Calendar.HOUR_OF_DAY, 12)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                dateMillis = selectedCal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("select_date_button"),
                    border = BorderStroke(1.dp, BentoBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoTextDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = BentoPrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = sdf.format(Date(dateMillis)), fontWeight = FontWeight.Bold)
                }

                // Core work telemetry (Hours & KMs) in adjacent row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HORAS TRABALHADAS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoTextSlate,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = hoursStr,
                            onValueChange = { hoursStr = it },
                            placeholder = { Text("Ex: 8,5", color = BentoTextSlate, fontSize = 14.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark,
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("hours_input")
                        )

                        val stopwatchSeconds by viewModel.stopwatchSeconds.collectAsStateWithLifecycle()
                        if (stopwatchSeconds > 0 && !isEdit) {
                            val decimalHours = stopwatchSeconds / 3600.0
                            TextButton(
                                onClick = {
                                    hoursStr = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", decimalHours)
                                },
                                contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp),
                                modifier = Modifier.align(Alignment.End).testTag("import_stopwatch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = BentoPrimaryBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Importar (${String.format(Locale.forLanguageTag("pt-BR"), "%.2f", decimalHours)}h)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimaryBlue
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "KM RODADOS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoTextSlate,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = kmStr,
                            onValueChange = { kmStr = it },
                            placeholder = { Text("Ex: 154,2", color = BentoTextSlate, fontSize = 14.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark,
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("km_input")
                        )
                    }
                }

                // Gross revenue (Faturado)
                Column {
                    Text(
                        text = "VALOR FATURADO ENTRADA (R$)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoTextSlate,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = revenueStr,
                        onValueChange = { revenueStr = it },
                        placeholder = { Text("Ex: 380,50", color = BentoTextSlate, fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Text("R$", color = BentoGreenText, fontWeight = FontWeight.Bold) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BentoTextDark,
                            unfocusedTextColor = BentoTextDark,
                            focusedBorderColor = BentoPrimaryBlue,
                            unfocusedBorderColor = BentoBorder,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("revenue_input")
                    )
                }

                // Expenses Section label
                Text(
                    text = "DESPESAS COMPLEMENTARES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = BentoTextDark,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Meal Expense input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alimentação (R$)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoTextSlate,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = mealStr,
                            onValueChange = { mealStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark,
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("meal_input")
                        )
                    }

                    // Rent expense input
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aluguel Veículo (R$)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoTextSlate,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = rentStr,
                            onValueChange = { rentStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark,
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("rent_input")
                        )
                    }
                }

                // Misc expenses input
                Column {
                    Text(
                        text = "Miscelâneas / Outros Custos (R$)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoTextSlate,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = miscStr,
                        onValueChange = { miscStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BentoTextDark,
                            unfocusedTextColor = BentoTextDark,
                            focusedBorderColor = BentoPrimaryBlue,
                            unfocusedBorderColor = BentoBorder,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("misc_input")
                    )
                }

                // Description/Notes input
                Column {
                    Text(
                        text = "DADOS COMPLEMENTARES / NOTAS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoTextSlate,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = descStr,
                        onValueChange = { descStr = it },
                        placeholder = { Text("Ex: Uber e 99 - Turno Noite", color = BentoTextSlate, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BentoTextDark,
                            unfocusedTextColor = BentoTextDark,
                            focusedBorderColor = BentoPrimaryBlue,
                            unfocusedBorderColor = BentoBorder,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("description_input")
                    )

                    val lunchSeconds by viewModel.lunchStopwatchSeconds.collectAsStateWithLifecycle()
                    if (lunchSeconds > 0 && !isEdit) {
                        val breakMinutes = lunchSeconds / 60
                        val breakHours = lunchSeconds / 3600.0
                        TextButton(
                            onClick = {
                                val breakLabel = if (lunchSeconds < 3600) {
                                    "Almoço: ${breakMinutes}min"
                                } else {
                                    String.format(Locale.forLanguageTag("pt-BR"), "Almoço: %.1fh", breakHours)
                                }
                                descStr = if (descStr.isEmpty()) breakLabel else "$descStr | $breakLabel"
                            },
                            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp),
                            modifier = Modifier.align(Alignment.End).testTag("import_lunch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = BentoGreenText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Importar Almoço (" + (if (lunchSeconds < 3600) "$breakMinutes min" else String.format(Locale.forLanguageTag("pt-BR"), "%.2f h", breakHours)) + ")",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoGreenText
                            )
                        }
                    }
                }

                // Button controls in dynamic arrangement
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isEdit) {
                        Button(
                            onClick = {
                                viewModel.deleteRecordById(recordToEdit!!.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoRedBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("delete_button")
                        ) {
                            Text("Excluir", fontWeight = FontWeight.Bold, color = BentoRedText)
                        }
                    }

                    Button(
                        onClick = {
                            // Safe parsing function converting commas (Brazilian notation) to dots
                            fun String.toSafeDouble(): Double {
                                return this.trim()
                                    .replace(",", ".")
                                    .toDoubleOrNull() ?: 0.0
                            }

                            val hours = hoursStr.toSafeDouble()
                            val kms = kmStr.toSafeDouble()
                            val rev = revenueStr.toSafeDouble()
                            val meal = mealStr.toSafeDouble()
                            val rent = rentStr.toSafeDouble()
                            val misc = miscStr.toSafeDouble()

                            // Basic validation
                            if (hours <= 0) {
                                android.widget.Toast.makeText(context, "Insira um número de horas maior que zero.", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.saveRecord(
                                id = recordToEdit?.id ?: 0L,
                                dateMillis = dateMillis,
                                hoursWorked = hours,
                                kmDriven = kms,
                                revenue = rev,
                                expenseMeal = meal,
                                expenseRent = rent,
                                expenseMisc = misc,
                                description = descStr
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(if (isEdit) 1.5f else 1f).testTag("save_button")
                    ) {
                        Text(if (isEdit) "Salvar" else "Salvar Lançamento", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkStopwatchCard(
    viewModel: DriverViewModel,
    onLaunchWithTime: (Double) -> Unit
) {
    val stopwatchSeconds by viewModel.stopwatchSeconds.collectAsStateWithLifecycle()
    val isRunning by viewModel.isStopwatchRunning.collectAsStateWithLifecycle()

    val lunchStopwatchSeconds by viewModel.lunchStopwatchSeconds.collectAsStateWithLifecycle()
    val isLunchRunning by viewModel.isLunchStopwatchRunning.collectAsStateWithLifecycle()

    val hrs = stopwatchSeconds / 3600
    val mins = (stopwatchSeconds % 3600) / 60
    val secs = stopwatchSeconds % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)

    val lhrs = lunchStopwatchSeconds / 3600
    val lmins = (lunchStopwatchSeconds % 3600) / 60
    val lsecs = lunchStopwatchSeconds % 60
    val formattedLunchTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", lhrs, lmins, lsecs)

    val decimalHours = stopwatchSeconds / 3600.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stopwatch_widget_card"),
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.5.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Stopwatch",
                        tint = BentoPrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Controle de Tempo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextDark
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isRunning) BentoOrangeBg 
                            else if (isLunchRunning) BentoGreenBg 
                            else BentoBorder.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isRunning) "TURNO ATIVO" 
                               else if (isLunchRunning) "ALMOÇO" 
                               else "PAUSADOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isRunning) BentoOrangeText 
                                else if (isLunchRunning) BentoGreenText 
                                else BentoTextSlate
                    )
                }
            }

            // Dual Stopwatch controls side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Work Hours
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isRunning) BentoOrangeBg.copy(alpha = 0.2f) else Color.Transparent)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = if (isRunning) BentoOrangeText else BentoTextSlate,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Ativo (Direção)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextSlate
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formattedTime,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isRunning) BentoOrangeText else BentoTextDark,
                        modifier = Modifier.testTag("stopwatch_time_text")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset Work
                        FilledIconButton(
                            onClick = { viewModel.resetStopwatch() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = BentoRedBg,
                                contentColor = BentoRedText
                            ),
                            modifier = Modifier.size(34.dp).testTag("stopwatch_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Zerar",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Play/Pause Work
                        FilledIconButton(
                            onClick = {
                                if (isRunning) {
                                    viewModel.pauseStopwatch()
                                } else {
                                    viewModel.startStopwatch()
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isRunning) BentoOrangeBg else BentoPrimaryBlue.copy(alpha = 0.1f),
                                contentColor = if (isRunning) BentoOrangeText else BentoPrimaryBlue
                            ),
                            modifier = Modifier.size(38.dp).testTag("stopwatch_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pausar" else "Iniciar",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(80.dp)
                        .background(BentoBorder)
                )

                // Column 2: Lunch break
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isLunchRunning) BentoGreenBg.copy(alpha = 0.2f) else Color.Transparent)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = if (isLunchRunning) BentoGreenText else BentoTextSlate,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Almoço/Pausa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextSlate
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formattedLunchTime,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isLunchRunning) BentoGreenText else BentoTextDark,
                        modifier = Modifier.testTag("lunch_stopwatch_time_text")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset Lunch
                        FilledIconButton(
                            onClick = { viewModel.resetLunchStopwatch() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = BentoRedBg,
                                contentColor = BentoRedText
                            ),
                            modifier = Modifier.size(34.dp).testTag("lunch_stopwatch_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Zerar Almoço",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Play/Pause Lunch
                        FilledIconButton(
                            onClick = {
                                if (isLunchRunning) {
                                    viewModel.pauseLunchStopwatch()
                                } else {
                                    viewModel.startLunchStopwatch()
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isLunchRunning) BentoGreenBg else BentoGreenText.copy(alpha = 0.1f),
                                contentColor = if (isLunchRunning) BentoGreenText else BentoGreenText
                            ),
                            modifier = Modifier.size(38.dp).testTag("lunch_stopwatch_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isLunchRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isLunchRunning) "Pausar Almoço" else "Iniciar Almoço",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Actions / Guide info
            if (stopwatchSeconds > 0 || lunchStopwatchSeconds > 0) {
                HorizontalDivider(color = BentoBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (stopwatchSeconds > 0) {
                            Text(
                                text = String.format(Locale.forLanguageTag("pt-BR"), "Direção: %.2f h", decimalHours),
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoTextSlate,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (lunchStopwatchSeconds > 0) {
                            val breakMinutes = lunchStopwatchSeconds / 60
                            Text(
                                text = "Lunch/Pausa: ${breakMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (stopwatchSeconds > 0) {
                        Button(
                            onClick = { onLaunchWithTime(decimalHours) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BentoPrimaryBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("stopwatch_quick_launch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lançar Atividade",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Gerencie o tempo de direção e paradas para almoço. Ao iniciar um, o outro pausa automaticamente para precisão total!",
                    fontSize = 11.sp,
                    color = BentoTextSlate,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
