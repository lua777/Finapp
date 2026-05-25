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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DriverRecord
import com.example.viewmodel.DriverViewModel
import com.example.viewmodel.PeriodSummary
import com.example.viewmodel.PeriodView
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
import com.example.data.GoogleDriveHelper
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

// Creative Direction: Bento Grid Design Theme (Dynamic Dark/Light modern, cozy shadows)
private val BentoBg: Color @Composable get() = MaterialTheme.colorScheme.background
private val BentoTextDark: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val BentoTextSlate: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val BentoPrimaryBlue: Color @Composable get() = MaterialTheme.colorScheme.primary
private val BentoBorder: Color @Composable get() = MaterialTheme.colorScheme.outline
private val BentoSurface: Color @Composable get() = MaterialTheme.colorScheme.surface

private val BentoOrangeBg = Color(0xFFFFEDD5)
private val BentoOrangeText = Color(0xFFEA580C)

private val BentoGreenBg = Color(0xFFDCFCE7)
private val BentoGreenText = Color(0xFF16A34A)

private val BentoRedBg = Color(0xFFFEE2E2)
private val BentoRedText = Color(0xFFDC2626)

private val BentoPurpleBg = Color(0xFFF3E8FF)
private val BentoPurpleText = Color(0xFF7C3AED)

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
    val monthlyGoal by viewModel.monthlyRevenueGoal.collectAsStateWithLifecycle()
    val vOwnership by viewModel.vehicleOwnership.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Painel (Bento grid), 1 = Ajustes (Configurações), 2 = Perfil
    var showAddDialog by remember { mutableStateOf(false) }
    var prefilledStopwatchHours by remember { mutableStateOf<Double?>(null) }

    val context = LocalContext.current
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val email = account.email
                    if (email != null) {
                        viewModel.onGoogleSignInSuccess(
                            email = email,
                            displayName = account.displayName,
                            photoUrl = account.photoUrl?.toString()
                        )
                        Toast.makeText(context, "Conectado ao Google: $email", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Falha na conexão Google: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (activeTab == 0) Icons.Default.DirectionsCar 
                                          else if (activeTab == 1) Icons.Default.Settings 
                                          else Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = BentoPrimaryBlue,
                            modifier = Modifier.size(32.dp).padding(end = 8.dp)
                        )
                        Text(
                            text = if (activeTab == 0) "Fluxo Driver" 
                                   else if (activeTab == 1) "Ajustes de Conta" 
                                   else "Perfil do Motorista",
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
        bottomBar = {
            NavigationBar(
                containerColor = BentoSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Painel"
                        )
                    },
                    label = { Text("Painel", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPrimaryBlue,
                        selectedTextColor = BentoPrimaryBlue,
                        indicatorColor = BentoPrimaryBlue.copy(alpha = 0.12f),
                        unselectedIconColor = BentoTextSlate,
                        unselectedTextColor = BentoTextSlate
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes"
                        )
                    },
                    label = { Text("Ajustes", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPrimaryBlue,
                        selectedTextColor = BentoPrimaryBlue,
                        indicatorColor = BentoPrimaryBlue.copy(alpha = 0.12f),
                        unselectedIconColor = BentoTextSlate,
                        unselectedTextColor = BentoTextSlate
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Perfil"
                        )
                    },
                    label = { Text("Perfil", fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPrimaryBlue,
                        selectedTextColor = BentoPrimaryBlue,
                        indicatorColor = BentoPrimaryBlue.copy(alpha = 0.12f),
                        unselectedIconColor = BentoTextSlate,
                        unselectedTextColor = BentoTextSlate
                    )
                )
            }
        },
        floatingActionButton = {
            if (activeTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = BentoPrimaryBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar Lançamento") },
                    text = { Text("Lançar Dia", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("add_record_fab")
                )
            }
        },
        containerColor = BentoBg
    ) { innerPadding ->
        Crossfade(
            targetState = activeTab,
            label = "ScreenTransition",
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BentoBg)
        ) { tab ->
            when (tab) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
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

                            // Interactive Monthly Revenue Target Gauge if configured
                            if (monthlyGoal > 0.0) {
                                item {
                                    MonthlyGoalProgressBar(viewModel = viewModel, goal = monthlyGoal)
                                }
                            }

                            // KPIs metrics block
                            item {
                                DashboardKPIs(summary = summary, vOwnership = vOwnership)
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
                1 -> {
                    // Custom Bento settings/adjustment deck
                    SettingsTab(
                        viewModel = viewModel,
                        googleSignInClient = googleSignInClient,
                        onSignInTrigger = { signInLauncher.launch(googleSignInClient.signInIntent) }
                    )
                }
                2 -> {
                    // Beautiful custom Driver Profile Tab with advanced stats
                    ProfileTab(
                        viewModel = viewModel,
                        googleSignInClient = googleSignInClient,
                        onSignInTrigger = { signInLauncher.launch(googleSignInClient.signInIntent) },
                        onNavigateToSettings = { activeTab = 1 }
                    )
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
fun DashboardKPIs(
    summary: PeriodSummary,
    vOwnership: Int = 1
) {
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
                            text = "Faturamento / Hora",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextSlate
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f/h", summary.grossHourlyRate),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = BentoTextDark,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "Líq (Lucro): R$ %.2f/h", summary.netHourlyRate),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoGreenText
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f/min", summary.netMinutelyRate),
                            fontSize = 9.sp,
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
                            text = "Rendimento / Km (Bruto)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextSlate
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %.2f/km", summary.grossRevenuePerKm),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = BentoTextDark,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "Líq (Lucro): R$ %.2f/km", summary.profitPerKm),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoGreenText
                        )
                        Text(
                            text = String.format(Locale("pt", "BR"), "Custo: R$ %.2f/km", summary.costPerKm),
                            fontSize = 9.sp,
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
                                .background(BentoPurpleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = BentoPurpleText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text("Abastecimento / Recarga", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoTextDark)
                    }
                    Text(
                        text = String.format(Locale("pt", "BR"), "- R$ %.2f", summary.totalFuel),
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
                        Text(if (vOwnership == 1) "Aluguel do Veículo" else "Custos de Posse (Veículo)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoTextDark)
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
            val fuelRatio = if (total > 0) (summary.totalFuel / total).toFloat().coerceIn(0f..1f) else 0f
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
                    if (fuelRatio > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(fuelRatio.coerceAtLeast(0.001f))
                                .background(BentoPurpleText) // Purple Fuel
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
                LegendItem(color = BentoPurpleText, label = "Abastecimento")
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val safeDismiss = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    // Fetch the last registered rent to auto-fill for nice user accessibility
    val lastRent by viewModel.lastRentValue.collectAsStateWithLifecycle()
    val vehicleType by viewModel.vehicleType.collectAsStateWithLifecycle()
    val vOwnership by viewModel.vehicleOwnership.collectAsStateWithLifecycle()
    val vRentCost by viewModel.vehicleRentCost.collectAsStateWithLifecycle()
    val vRentWeeklyCost by viewModel.vehicleRentWeeklyCost.collectAsStateWithLifecycle()
    val vKmAllowance by viewModel.vehicleKmAllowance.collectAsStateWithLifecycle()
    val vExtraKmCost by viewModel.vehicleExtraKmCost.collectAsStateWithLifecycle()
    val vOwnCost by viewModel.vehicleOwnCost.collectAsStateWithLifecycle()

    val fuelLabel = when (vehicleType) {
        0 -> "Abastecimento (Combustão) (R$)"
        1 -> "Combustível / Recarga (R$)"
        2 -> "Recarga de Energia (R$)"
        else -> "Abastecimento/Recarga (R$)"
    }

    val parsedDouble = { input: String ->
        input.replace(",", ".").toDoubleOrNull() ?: 0.0
    }

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
    
    val kmVal = parsedDouble(kmStr)
    val calculatedSuggestedCost = remember(vOwnership, vRentWeeklyCost, vKmAllowance, vExtraKmCost, vOwnCost, kmVal) {
        if (vOwnership == 1) {
            val dailyRentBase = vRentWeeklyCost / 7.0
            val dailyAllowance = if (vKmAllowance > 0.0) vKmAllowance / 7.0 else 0.0
            val excessKm = if (vKmAllowance > 0.0) maxOf(0.0, kmVal - dailyAllowance) else 0.0
            val extraKmCostValue = excessKm * vExtraKmCost
            dailyRentBase + extraKmCostValue
        } else {
            vOwnCost
        }
    }

    var isRentManuallyEdited by remember { mutableStateOf(false) }

    // If and only if addition is triggered, auto-fill configured daily ownership cost or lastRent
    var rentStr by remember(recordToEdit) {
        mutableStateOf(
            recordToEdit?.expenseRent?.toString()?.replace(".", ",") 
                ?: if (calculatedSuggestedCost > 0.0) String.format(Locale.US, "%.2f", calculatedSuggestedCost).replace(".", ",")
                else if (lastRent > 0) lastRent.toString().replace(".", ",") else ""
        )
    }

    // Auto-update rent state if not manually modified
    LaunchedEffect(calculatedSuggestedCost) {
        if (!isRentManuallyEdited && !isEdit) {
            rentStr = String.format(Locale.US, "%.2f", calculatedSuggestedCost).replace(".", ",")
        }
    }

    var fuelStr by remember { mutableStateOf(recordToEdit?.expenseFuel?.toString()?.replace(".", ",") ?: "") }
    var miscStr by remember { mutableStateOf(recordToEdit?.expenseMisc?.toString()?.replace(".", ",") ?: "") }
    var descStr by remember { mutableStateOf(recordToEdit?.description ?: "") }

    Dialog(
        onDismissRequest = safeDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
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
                    IconButton(onClick = safeDismiss) {
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
                        focusManager.clearFocus()
                        keyboardController?.hide()
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
                            text = if (vOwnership == 1) "Aluguel Veículo (R$)" else "Posse / Custos (R$)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoTextSlate,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = rentStr,
                            onValueChange = { 
                                rentStr = it 
                                isRentManuallyEdited = true
                            },
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
                        if (vOwnership == 1 && vRentWeeklyCost > 0.0) {
                            val baseDay = vRentWeeklyCost / 7.0
                            val dailyAllowance = if (vKmAllowance > 0.0) vKmAllowance / 7.0 else 0.0
                            val exceeded = if (vKmAllowance > 0.0) maxOf(0.0, kmVal - dailyAllowance) else 0.0
                            val excessCost = exceeded * vExtraKmCost
                            Text(
                                text = "Sugerido: R$ ${String.format(Locale.US, "%.2f", calculatedSuggestedCost)} (R$ ${String.format(Locale.US, "%.2f", baseDay)} base + R$ ${String.format(Locale.US, "%.2f", excessCost)} extra)",
                                fontSize = 10.sp,
                                color = BentoPrimaryBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable {
                                        rentStr = String.format(Locale.US, "%.2f", calculatedSuggestedCost).replace(".", ",")
                                        isRentManuallyEdited = false
                                    }
                            )
                        } else if (vOwnership == 0 && vOwnCost > 0.0) {
                            Text(
                                text = "Sugerido (Posses + Amort.): R$ ${String.format(Locale.US, "%.2f", vOwnCost)}",
                                fontSize = 10.sp,
                                color = BentoPrimaryBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable {
                                        rentStr = String.format(Locale.US, "%.2f", vOwnCost).replace(".", ",")
                                        isRentManuallyEdited = false
                                    }
                            )
                        }
                    }
                }

                // Fuel & Misc expenses input row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fuelLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoTextSlate,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = fuelStr,
                            onValueChange = { fuelStr = it },
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
                            modifier = Modifier.fillMaxWidth().testTag("fuel_input")
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Miscelâneas (R$)",
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
                                safeDismiss()
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
                            val fuel = fuelStr.toSafeDouble()
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
                                expenseFuel = fuel,
                                description = descStr
                            )
                            safeDismiss()
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

@Composable
fun MonthlyGoalProgressBar(
    viewModel: DriverViewModel,
    goal: Double,
    modifier: Modifier = Modifier
) {
    val progressRevenue by viewModel.monthlyRevenueProgress.collectAsStateWithLifecycle()
    val percentage = if (goal > 0) (progressRevenue / goal).toFloat() else 0f
    val displayPercentage = (percentage * 100).coerceIn(0f, 100f)
    val missingRevenue = (goal - progressRevenue).coerceAtLeast(0.0)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BentoPurpleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = BentoPurpleText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "META DE FATURAMENTO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = BentoTextSlate,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Progresso Mensal",
                            style = MaterialTheme.typography.bodySmall,
                            color = BentoTextSlate
                        )
                    }
                }
                
                Text(
                    text = String.format(Locale("pt", "BR"), "%.0f%%", displayPercentage),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = BentoPurpleText
                )
            }

            // Linear Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { percentage.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = BentoPurpleText,
                    trackColor = BentoPurpleBg
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", progressRevenue),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextDark
                    )
                    Text(
                        text = String.format(Locale("pt", "BR"), "Meta: R$ %.2f", goal),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextSlate
                    )
                }
            }

            if (missingRevenue > 0.0) {
                Text(
                    text = String.format(Locale("pt", "BR"), "Faltam apenas R$ %.2f para atingir o seu objetivo do mês!", missingRevenue),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BentoPurpleText
                )
            } else {
                Text(
                    text = "Parabéns! Você alcançou a sua meta mensal de faturamento! 🚀🏆",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoGreenText
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    viewModel: DriverViewModel,
    googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient,
    onSignInTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val closureDay by viewModel.weeklyClosureDay.collectAsStateWithLifecycle()
    val monthlyGoal by viewModel.monthlyRevenueGoal.collectAsStateWithLifecycle()
    val driveFolder by viewModel.driveFolderName.collectAsStateWithLifecycle()
    val formatReport by viewModel.reportFormat.collectAsStateWithLifecycle()
    val summary by viewModel.periodSummary.collectAsStateWithLifecycle()
    val records by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val vType by viewModel.vehicleType.collectAsStateWithLifecycle()
    val vConsumption by viewModel.vehicleConsumption.collectAsStateWithLifecycle()
    val fPriceEstimate by viewModel.fuelPriceEstimate.collectAsStateWithLifecycle()
    val vOwnership by viewModel.vehicleOwnership.collectAsStateWithLifecycle()
    val vRentCost by viewModel.vehicleRentCost.collectAsStateWithLifecycle()
    val vRentWeeklyCost by viewModel.vehicleRentWeeklyCost.collectAsStateWithLifecycle()
    val vKmAllowance by viewModel.vehicleKmAllowance.collectAsStateWithLifecycle()
    val vExtraKmCost by viewModel.vehicleExtraKmCost.collectAsStateWithLifecycle()
    val vOwnCost by viewModel.vehicleOwnCost.collectAsStateWithLifecycle()

    val googleEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()
    val googleName by viewModel.googleUserName.collectAsStateWithLifecycle()
    val googlePhoto by viewModel.googleUserPhotoUrl.collectAsStateWithLifecycle()
    val googleIsSyncing by viewModel.googleIsSyncing.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var goalInput by remember(monthlyGoal) { 
        mutableStateOf(if (monthlyGoal > 0.0) String.format(Locale.US, "%.2f", monthlyGoal) else "") 
    }
    var folderInput by remember(driveFolder) { mutableStateOf(driveFolder) }

    var consumptionInput by remember(vConsumption) { 
        mutableStateOf(String.format(Locale.US, "%.1f", vConsumption).replace(".", ",")) 
    }
    var priceInput by remember(fPriceEstimate) { 
        mutableStateOf(String.format(Locale.US, "%.2f", fPriceEstimate).replace(".", ",")) 
    }
    var rentCostInput by remember(vRentCost) { 
        mutableStateOf(String.format(Locale.US, "%.2f", vRentCost).replace(".", ",")) 
    }
    var rentWeeklyCostInput by remember(vRentWeeklyCost) { 
        mutableStateOf(String.format(Locale.US, "%.2f", vRentWeeklyCost).replace(".", ",")) 
    }
    var kmAllowanceInput by remember(vKmAllowance) { 
        mutableStateOf(String.format(Locale.US, "%.1f", vKmAllowance).replace(".", ",")) 
    }
    var extraKmCostInput by remember(vExtraKmCost) { 
        mutableStateOf(String.format(Locale.US, "%.2f", vExtraKmCost).replace(".", ",")) 
    }
    var ownCostInput by remember(vOwnCost) { 
        mutableStateOf(String.format(Locale.US, "%.2f", vOwnCost).replace(".", ",")) 
    }

    val parsedDouble = { input: String ->
        input.replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App settings introduction card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BentoPrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = BentoPrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ajustes e Configurações",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextDark
                        )
                        Text(
                            text = "Personalize sua experiência de motorista e gerencie seus relatórios.",
                            fontSize = 12.sp,
                            color = BentoTextSlate
                        )
                    }
                }
            }
        }

        // Section 1: Visual Theme (Light / Dark)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "APARÊNCIA DO APLICATIVO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = BentoTextSlate,
                        letterSpacing = 1.sp
                    )

                    // Three columns segmented buttons for theme choice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Sistema", 1 to "Claro", 2 to "Escuro").forEach { (modeVal, label) ->
                            val isSelected = themeMode == modeVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BentoPrimaryBlue else BentoBg)
                                    .clickable { viewModel.setAppThemeMode(modeVal) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else BentoTextDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Faturamento Goal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "OBJETIVO MENSAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = BentoTextSlate,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Defina uma meta de faturamento bruto para acompanhar seu desempenho.",
                        fontSize = 12.sp,
                        color = BentoTextSlate
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = goalInput,
                            onValueChange = { goalInput = it },
                            label = { Text("Meta Mensal (R$)") },
                            placeholder = { Text("Ex: 5000.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryBlue,
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark
                            )
                        )

                        Button(
                            onClick = {
                                val value = goalInput.toDoubleOrNull() ?: 0.0
                                viewModel.setMonthlyRevenueGoal(value)
                                focusManager.clearFocus()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Veículo & Eficiência
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "VEÍCULO & EFICIÊNCIA DE CUSTOS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = BentoTextSlate,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Acompanhe seus custos de combustível ou energia de forma inteligente de acordo com o tipo do seu veículo.",
                        fontSize = 12.sp,
                        color = BentoTextSlate
                    )

                    // Vehicle Type selector
                    Text(
                        text = "Tipo de motorização:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Combustão", 1 to "Híbrido", 2 to "Elétrico").forEach { (typeVal, label) ->
                            val isSelected = vType == typeVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BentoPrimaryBlue else BentoBg)
                                    .clickable { viewModel.setVehicleType(typeVal) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else BentoTextDark
                                )
                            }
                        }
                    }

                    // Autonomy/Consumption input & unit cost
                    val consumptionLabel = if (vType == 2) "Eficiência (Km/kWh)" else "Consumo Médio (Km/L)"
                    val priceLabel = if (vType == 2) "Preço Energia (R$/kWh)" else "Preço Combustível (R$/L)"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = consumptionInput,
                            onValueChange = { consumptionInput = it },
                            label = { Text(consumptionLabel, fontSize = 11.sp) },
                            placeholder = { Text(if (vType == 2) "Ex: 6,0" else "Ex: 11,5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryBlue,
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark
                            )
                        )

                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            label = { Text(priceLabel, fontSize = 11.sp) },
                            placeholder = { Text(if (vType == 2) "Ex: 0,90" else "Ex: 5,60") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryBlue,
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark
                            )
                        )
                    }

                    HorizontalDivider(color = BentoBorder.copy(alpha = 0.3f))

                    Text(
                        text = "Regime de Posse / Propriedade:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Próprio", 1 to "Alugado").forEach { (ownVal, label) ->
                            val isSelected = vOwnership == ownVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BentoPrimaryBlue else BentoBg)
                                    .clickable { viewModel.setVehicleOwnership(ownVal) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else BentoTextDark
                                )
                            }
                        }
                    }

                    if (vOwnership == 0) {
                        OutlinedTextField(
                            value = ownCostInput,
                            onValueChange = { ownCostInput = it },
                            label = { Text("Gastos Diários Estimados (Seguro, IPVA, Manut.)", fontSize = 11.sp) },
                            placeholder = { Text("Ex: 15,00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryBlue,
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark
                            )
                        )
                        Text(
                            text = "Estes custos presumidos de posse (IPVA, seguro, manutenção geral e amortização de desgaste) serão pré-carregados ao lançar atividades diárias para calcular seu faturamento líquido real.",
                            fontSize = 11.sp,
                            color = BentoTextSlate
                        )
                    } else {
                        OutlinedTextField(
                            value = rentWeeklyCostInput,
                            onValueChange = { rentWeeklyCostInput = it },
                            label = { Text("Valor do Aluguel Semanal (R$)", fontSize = 11.sp) },
                            placeholder = { Text("Ex: 420,00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryBlue,
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark
                            )
                        )
                        OutlinedTextField(
                            value = kmAllowanceInput,
                            onValueChange = { kmAllowanceInput = it },
                            label = { Text("Franquia Semanal de KM (KM)", fontSize = 11.sp) },
                            placeholder = { Text("Ex: 1000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryBlue,
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark
                            )
                        )
                        OutlinedTextField(
                            value = extraKmCostInput,
                            onValueChange = { extraKmCostInput = it },
                            label = { Text("Custo Extra por KM Excedido (R$)", fontSize = 11.sp) },
                            placeholder = { Text("Ex: 0,50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoPrimaryBlue,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryBlue,
                                focusedTextColor = BentoTextDark,
                                unfocusedTextColor = BentoTextDark
                            )
                        )
                        Text(
                            text = "O aluguel semanal será rateado em diárias (Valor Semanal / 7). Quando você lançar a atividade diária, o aplicativo também vai sugerir acréscimos caso você exceda a franquia de KM diária proporcional.",
                            fontSize = 11.sp,
                            color = BentoTextSlate
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val cons = parsedDouble(consumptionInput)
                                val prc = parsedDouble(priceInput)
                                if (cons > 0) viewModel.setVehicleConsumption(cons)
                                if (prc > 0) viewModel.setFuelPriceEstimate(prc)

                                val rentWeeklyC = parsedDouble(rentWeeklyCostInput)
                                val kmAllowC = parsedDouble(kmAllowanceInput)
                                val extraKmC = parsedDouble(extraKmCostInput)
                                val rentC = rentWeeklyC / 7.0 // keep daily Rent cost synched as well
                                val ownC = parsedDouble(ownCostInput)

                                viewModel.setVehicleRentCost(rentC)
                                viewModel.setVehicleRentWeeklyCost(rentWeeklyC)
                                viewModel.setVehicleKmAllowance(kmAllowC)
                                viewModel.setVehicleExtraKmCost(extraKmC)
                                viewModel.setVehicleOwnCost(ownC)

                                focusManager.clearFocus()
                                Toast.makeText(context, "Configurações de veículo salvas com sucesso!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimaryBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Salvar Veículo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = BentoBorder.copy(alpha = 0.5f))

                    // Running Cost Estimator results
                    if (vConsumption > 0) {
                        val costPerKm = fPriceEstimate / vConsumption
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BentoPrimaryBlue.copy(alpha = 0.05f))
                                .border(BorderStroke(1.dp, BentoPrimaryBlue.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = BentoPrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "ESTIMADOR DE INSTANCIA DE CUSTO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoPrimaryBlue
                                    )
                                }
                                
                                Text(
                                    text = String.format(Locale("pt", "BR"), "Custo Estimado por Km Rodado: R$ %.3f / Km", costPerKm),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BentoTextDark
                                )

                                Text(
                                    text = "Isso significa que para cada 100 Km rodados, você investirá aproximadamente R$ ${String.format(Locale("pt", "BR"), "%.2f", costPerKm * 100)} em combustível/energia.",
                                    fontSize = 11.sp,
                                    color = BentoTextSlate
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Weekly Closure Day selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DIA DE INÍCIO DA SEMANA / CICLO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = BentoTextSlate,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Selecione o dia do fechamento para iniciar a soma semanal dos lucros de forma personalizada.",
                        fontSize = 12.sp,
                        color = BentoTextSlate
                    )

                    // Grid or Row of days
                    val days = listOf(
                        Calendar.MONDAY to "Seg",
                        Calendar.TUESDAY to "Ter",
                        Calendar.WEDNESDAY to "Qua",
                        Calendar.THURSDAY to "Qui",
                        Calendar.FRIDAY to "Sex",
                        Calendar.SATURDAY to "Sáb",
                        Calendar.SUNDAY to "Dom"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        days.forEach { (dayVal, label) ->
                            val isSelected = closureDay == dayVal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) BentoPrimaryBlue else BentoBg)
                                    .clickable { viewModel.setWeeklyClosureDay(dayVal) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else BentoTextDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Google Drive and Integration Share weekly report
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTEGRAÇÃO & GOOGLE DRIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = BentoTextSlate,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (googleEmail != null) BentoGreenBg else BentoOrangeBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (googleEmail != null) "Conectado" else "Modo Local",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (googleEmail != null) BentoGreenText else BentoOrangeText
                            )
                        }
                    }

                    Text(
                        text = "Conecte sua conta do Google de forma totalmente segura para automatizar a sincronização direta de fechamentos semanais (.csv ou .txt) em pastas personalizadas do seu Google Drive.",
                        fontSize = 12.sp,
                        color = BentoTextSlate
                    )

                    // Google Login / Context Connection Panel
                    if (googleEmail != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BentoBg)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BentoGreenBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = BentoGreenText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = googleName ?: "Usuário Conectado",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextDark
                                    )
                                    Text(
                                        text = googleEmail ?: "",
                                        fontSize = 11.sp,
                                        color = BentoTextSlate
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        viewModel.onGoogleSignOut()
                                        Toast.makeText(context, "Conta Google desconectada.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Sair",
                                    tint = BentoRedText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { onSignInTrigger() },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoBg),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BentoBorder),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = BentoPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Entrar com uma Conta Google",
                                fontWeight = FontWeight.Bold,
                                color = BentoTextDark,
                                fontSize = 13.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = folderInput,
                        onValueChange = { 
                            folderInput = it
                            viewModel.setDriveFolderName(it)
                        },
                        label = { Text("Nome da Pasta no Google Drive") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPrimaryBlue,
                            unfocusedBorderColor = BentoBorder,
                            focusedLabelColor = BentoPrimaryBlue,
                            focusedTextColor = BentoTextDark,
                            unfocusedTextColor = BentoTextDark
                        )
                    )

                    // Format Toggle Checkbox/Segmented control
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Formato do Relatório Semanal:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextDark
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("CSV" to "Planilha CSV", "TEXTO" to "Resumo em Texto").forEach { (fmtVal, label) ->
                                val isSelected = formatReport == fmtVal
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) BentoPrimaryBlue else BentoBg)
                                        .clickable { viewModel.setReportFormat(fmtVal) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else BentoTextDark
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action buttons
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (googleEmail != null) {
                            Button(
                                onClick = {
                                    if (records.isEmpty()) {
                                        Toast.makeText(context, "Sem registros na semana ativa para exportar!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.googleIsSyncing.value = true
                                        scope.launch {
                                            try {
                                                val email = googleEmail ?: ""
                                                val token = GoogleDriveHelper.getAccessToken(context, email)
                                                if (token == null) {
                                                    Toast.makeText(context, "Erro ao obter autorização do Google Drive.", Toast.LENGTH_SHORT).show()
                                                    viewModel.googleIsSyncing.value = false
                                                    return@launch
                                                }

                                                val fileName: String
                                                val mimeType: String
                                                val content: String

                                                if (formatReport == "CSV") {
                                                    fileName = "Relatorio_Semanal_FluxoDriver_${summary.periodLabel.replace(" ", "_").replace("/", "-")}.csv"
                                                    mimeType = "text/csv"
                                                    val csvHeader = "Data,Horas Trabalhadas,Distancia_Km,Faturamento_R$,Alimentacao_R$,Aluguel_Midia_R$,Outros_R$,Combustivel_R$,Despesas_Totais_R$,Lucro_Liquido_R$\n"
                                                    val csvRows = records.joinToString("\n") { r ->
                                                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(r.dateMillis))
                                                        val totalExp = r.expenseMeal + r.expenseRent + r.expenseMisc + r.expenseFuel
                                                        String.format(
                                                            Locale.US,
                                                            "%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                                                            dateStr, r.hoursWorked, r.kmDriven, r.revenue, r.expenseMeal, r.expenseRent, r.expenseMisc, r.expenseFuel, totalExp, (r.revenue - totalExp)
                                                        )
                                                    }
                                                    content = csvHeader + csvRows
                                                } else {
                                                    fileName = "Relatorio_Semanal_FluxoDriver_${summary.periodLabel.replace(" ", "_").replace("/", "-")}.txt"
                                                    mimeType = "text/plain"
                                                    content = """
                                                        === RELATÓRIO DE FECHAMENTO SEMANAL ===
                                                        Período Ativo: ${summary.periodLabel}
                                                        Destino da Pasta: $driveFolder
                                                        --------------------------------------------------
                                                        FATURAMENTO BRUTO: R$ %.2f
                                                        DESPESAS TOTAIS:   R$ %.2f
                                                        LUCRO LÍQUIDO:     R$ %.2f
                                                        
                                                        DETALHAMENTO FINANCEIRO:
                                                        - Combustível:         R$ %.2f
                                                        - Alimentação:         R$ %.2f
                                                        - Aluguel/Mídia:       R$ %.2f
                                                        - Outras / Variáveis:  R$ %.2f
                                                        
                                                        MÉTRICAS DE EFICIÊNCIA:
                                                        - Horas Conduzidas:  %.2f h
                                                        - Distância Rodada:  %.2f Km
                                                        - R$/Hora (Bruto):   R$ %.2f/h
                                                        - R$/Hora (Líquido): R$ %.2f/h
                                                        - R$/Km (Bruto):     R$ %.2f/km
                                                        - R$/Km (Líquido):   R$ %.2f/km
                                                        
                                                        Relatório exportado e sincronizado com o Google Drive de forma segura.
                                                    """.trimIndent().let { template ->
                                                        String.format(
                                                            Locale("pt", "BR"), template,
                                                            summary.totalRevenue, summary.totalExpenses, summary.netProfit,
                                                            summary.totalFuel, summary.totalMeal, summary.totalRent, summary.totalMisc,
                                                            summary.totalHours, summary.totalKm,
                                                            summary.grossHourlyRate, summary.netHourlyRate,
                                                            summary.grossRevenuePerKm, summary.profitPerKm
                                                        )
                                                    }
                                                }

                                                val success = GoogleDriveHelper.uploadFileToDrive(
                                                    token = token,
                                                    folderName = driveFolder,
                                                    fileName = fileName,
                                                    mimeType = mimeType,
                                                    content = content
                                                )

                                                if (success) {
                                                    Toast.makeText(context, "Sincronizado na pasta '$driveFolder' do seu Google Drive!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    GoogleDriveHelper.invalidateToken(context, token)
                                                    Toast.makeText(context, "Erro ao gravar. Credenciais reiniciadas, tente novamente.", Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Falha na sincronização: ${e.message}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                viewModel.googleIsSyncing.value = false
                                            }
                                        }
                                    }
                                },
                                enabled = !googleIsSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimaryBlue),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                if (googleIsSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sincronizando...", fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sincronizar no Google Drive", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (records.isEmpty()) {
                                    Toast.makeText(context, "Sem registros na semana ativa para exportar!", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (formatReport == "CSV") {
                                        val csvHeader = "Data,Horas Trabalhadas,Distancia_Km,Faturamento_R$,Alimentacao_R$,Aluguel_Midia_R$,Outros_R$,Combustivel_R$,Despesas_Totais_R$,Lucro_Liquido_R$\n"
                                        val csvRows = records.joinToString("\n") { r ->
                                            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(r.dateMillis))
                                            val totalExp = r.expenseMeal + r.expenseRent + r.expenseMisc + r.expenseFuel
                                            String.format(
                                                Locale.US,
                                                "%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                                                dateStr, r.hoursWorked, r.kmDriven, r.revenue, r.expenseMeal, r.expenseRent, r.expenseMisc, r.expenseFuel, totalExp, (r.revenue - totalExp)
                                            )
                                        }
                                        val csvContent = csvHeader + csvRows

                                        try {
                                            val reportsDir = java.io.File(context.cacheDir, "reports")
                                            if (!reportsDir.exists()) {
                                                reportsDir.mkdirs()
                                            }
                                            val file = java.io.File(reportsDir, "Relatorio_Semanal_FluxoDriver.csv")
                                            file.writeText(csvContent)
                                            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )

                                            val sendIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                                type = "text/csv"
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Relatório Semanal - ${summary.periodLabel}")
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Salvar Planilha na Pasta: $driveFolder")
                                            context.startActivity(shareIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao exportar arquivo CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        val textContent = """
                                            === RELATÓRIO DE FECHAMENTO SEMANAL ===
                                            Período Ativo: ${summary.periodLabel}
                                            Destino da Pasta: $driveFolder
                                            --------------------------------------------------
                                            FATURAMENTO BRUTO: R$ %.2f
                                            DESPESAS TOTAIS:   R$ %.2f
                                            LUCRO LÍQUIDO:     R$ %.2f
                                            
                                            DETALHAMENTO FINANCEIRO:
                                            - Combustível:         R$ %.2f
                                            - Alimentação:         R$ %.2f
                                            - Aluguel/Mídia:       R$ %.2f
                                            - Outras / Variáveis:  R$ %.2f
                                            
                                            MÉTRICAS DE EFICIÊNCIA:
                                            - Horas Conduzidas:  %.2f h
                                            - Distância Rodada:  %.2f Km
                                            - R$/Hora (Bruto):   R$ %.2f/h
                                            - R$/Hora (Líquido): R$ %.2f/h
                                            - R$/Km (Bruto):     R$ %.2f/km
                                            - R$/Km (Líquido):   R$ %.2f/km
                                            
                                            Relatório exportado e sincronizado com o Google Drive de forma segura.
                                        """.trimIndent().let { template ->
                                            String.format(
                                                Locale("pt", "BR"), template,
                                                summary.totalRevenue, summary.totalExpenses, summary.netProfit,
                                                summary.totalFuel, summary.totalMeal, summary.totalRent, summary.totalMisc,
                                                summary.totalHours, summary.totalKm,
                                                summary.grossHourlyRate, summary.netHourlyRate,
                                                summary.grossRevenuePerKm, summary.profitPerKm
                                            )
                                        }

                                        try {
                                            val reportsDir = java.io.File(context.cacheDir, "reports")
                                            if (!reportsDir.exists()) {
                                                reportsDir.mkdirs()
                                            }
                                            val file = java.io.File(reportsDir, "Relatorio_Semanal_FluxoDriver.txt")
                                            file.writeText(textContent)
                                            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )

                                            val sendIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Relatório Semanal - ${summary.periodLabel}")
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Salvar Relatório na Pasta: $driveFolder")
                                            context.startActivity(shareIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao exportar arquivo TXT: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoGreenText),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compartilhar Relatório Local", 
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTab(
    viewModel: DriverViewModel,
    googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient,
    onSignInTrigger: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val googleEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()
    val googleName by viewModel.googleUserName.collectAsStateWithLifecycle()
    val googlePhoto by viewModel.googleUserPhotoUrl.collectAsStateWithLifecycle()
    val driveFolder by viewModel.driveFolderName.collectAsStateWithLifecycle()
    
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Stats computations
    val totalDays = allRecords.size
    val totalRevenue = allRecords.sumOf { it.revenue }
    val totalHours = allRecords.sumOf { it.hoursWorked }
    val totalKm = allRecords.sumOf { it.kmDriven }
    val totalFuel = allRecords.sumOf { it.expenseFuel }
    val totalRent = allRecords.sumOf { it.expenseRent }
    val totalMeal = allRecords.sumOf { it.expenseMeal }
    val totalMisc = allRecords.sumOf { it.expenseMisc }
    val totalExpenses = totalFuel + totalRent + totalMeal + totalMisc
    val netProfit = totalRevenue - totalExpenses
    val avgHourlyRate = if (totalHours > 0.0) netProfit / totalHours else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper section: User Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (googleEmail != null) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(BentoPrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (googlePhoto != null) {
                            AsyncImage(
                                model = googlePhoto,
                                contentDescription = "Foto de Perfil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(androidx.compose.foundation.shape.CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            // fall back letter avatar
                            Text(
                                text = (googleName ?: googleEmail ?: "U").take(1).uppercase(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimaryBlue
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = googleName ?: "Usuário Google",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = BentoTextDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = googleEmail ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BentoTextSlate,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BentoGreenBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Conta Google Conectada",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoGreenText
                            )
                        }
                    }
                } else {
                    // Logged out display
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(BentoTextSlate.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = BentoTextSlate,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Piloto Anônimo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = BentoTextDark
                        )
                        Text(
                            text = "Sua conta não está integrada ao Google",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BentoTextSlate
                        )
                    }

                    Button(
                        onClick = { onSignInTrigger() },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimaryBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar Login com o Google", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Stats Header
        Text(
            text = "Minhas Estatísticas Globais",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BentoTextDark,
            modifier = Modifier.padding(start = 4.dp)
        )

        // Bento grid style layout for general summary stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BentoGreenText, modifier = Modifier.size(16.dp))
                        Text("Lucro Líquido", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BentoTextSlate)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", netProfit),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (netProfit >= 0) BentoGreenText else BentoRedText
                    )
                    Text("Total acumulado", fontSize = 9.sp, color = BentoTextSlate)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = BentoPrimaryBlue, modifier = Modifier.size(16.dp))
                        Text("Ganho por Hora", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BentoTextSlate)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f/h", avgHourlyRate),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = BentoPrimaryBlue
                    )
                    Text("Média por hora real", fontSize = 9.sp, color = BentoTextSlate)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = BentoPrimaryBlue, modifier = Modifier.size(16.dp))
                        Text("Dias Trabalhados", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BentoTextSlate)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$totalDays dias",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = BentoTextDark
                    )
                    Text("Lançamentos no app", fontSize = 9.sp, color = BentoTextSlate)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = BentoPrimaryBlue, modifier = Modifier.size(16.dp))
                        Text("KM Rodados", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BentoTextSlate)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format(Locale("pt", "BR"), "%.0f km", totalKm),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = BentoTextDark
                    )
                    Text("Distância percorrida", fontSize = 9.sp, color = BentoTextSlate)
                }
            }
        }

        // Expanded financial breakdown block
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DETALHAMENTO HISTÓRICO",
                    style = MaterialTheme.typography.labelSmall,
                    color = BentoTextSlate,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Faturamento Bruto", color = BentoTextDark, fontSize = 13.sp)
                    Text(String.format(Locale("pt", "BR"), "R$ %.2f", totalRevenue), color = BentoGreenText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                HorizontalDivider(color = BentoBorder.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gasto c/ Combustível", color = BentoTextDark, fontSize = 13.sp)
                    Text(String.format(Locale("pt", "BR"), "- R$ %.2f", totalFuel), color = BentoRedText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Custos do Veículo", color = BentoTextDark, fontSize = 13.sp)
                    Text(String.format(Locale("pt", "BR"), "- R$ %.2f", totalRent), color = BentoRedText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Alimentação", color = BentoTextDark, fontSize = 13.sp)
                    Text(String.format(Locale("pt", "BR"), "- R$ %.2f", totalMeal), color = BentoRedText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Outras Despesas", color = BentoTextDark, fontSize = 13.sp)
                    Text(String.format(Locale("pt", "BR"), "- R$ %.2f", totalMisc), color = BentoRedText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                HorizontalDivider(color = BentoBorder.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lucro Líquido Real", color = BentoTextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %.2f", netProfit),
                        color = if (netProfit >= 0) BentoGreenText else BentoRedText,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Google Drive Status section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            border = BorderStroke(1.dp, BentoBorder.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = BentoPrimaryBlue)
                    Text(
                        text = "INTEGRAÇÃO GOOGLE DRIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoTextSlate,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = if (googleEmail != null) {
                        "Sincronização ativa na pasta '$driveFolder' da sua conta Google Drive. Seus fechamentos semanais são carregados automaticamente em tempo real."
                    } else {
                        "Os seus dados estão salvos de forma 100% segura apenas no seu dispositivo local. Integrando com o Google Drive você pode gerar relatórios na nuvem a qualquer hora."
                    },
                    fontSize = 12.sp,
                    color = BentoTextSlate
                )

                if (googleEmail != null) {
                    Button(
                        onClick = onNavigateToSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = BentoBg),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BentoBorder),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = BentoPrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gerenciar Pasta de Backup", color = BentoTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sign Out at the very bottom
        if (googleEmail != null) {
            OutlinedButton(
                onClick = {
                    googleSignInClient.signOut().addOnCompleteListener {
                        viewModel.onGoogleSignOut()
                        Toast.makeText(context, "Conta Google desconectada.", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BentoRedText.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoRedText),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Deslogar Conta Google", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}
