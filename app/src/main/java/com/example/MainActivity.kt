package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.model.Tournament
import com.example.data.repository.PokerRepository
import com.example.ui.screens.AddEditTournamentSheet
import com.example.ui.screens.BankrollScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InitialBankrollDialog
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TournamentsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PokerViewModel
import com.example.ui.viewmodel.PokerViewModelFactory

enum class AppNavDestination(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Dashboard", Icons.Default.GridView, "nav_dashboard"),
    TOURNAMENTS("Tourneys", Icons.Default.EmojiEvents, "nav_tournaments"),
    ADD("Add", Icons.Default.Add, "nav_add"),
    STATISTICS("Stats", Icons.Default.BarChart, "nav_statistics"),
    BANKROLL("Bankroll", Icons.Default.AccountBalanceWallet, "nav_bankroll");
}

class MainActivity : ComponentActivity() {

    private val viewModel: PokerViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val repository = PokerRepository(database)
        PokerViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PokerBankrollApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PokerBankrollApp(viewModel: PokerViewModel) {
    var currentDestination by remember { mutableStateOf(AppNavDestination.DASHBOARD) }
    var editingTournament by remember { mutableStateOf<Tournament?>(null) }
    var isAddingTournament by remember { mutableStateOf(false) }
    var showInitialBankrollDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val currentBankroll by viewModel.currentBankroll.collectAsStateWithLifecycle()

    // Back handling
    BackHandler(enabled = isAddingTournament || editingTournament != null || currentDestination != AppNavDestination.DASHBOARD) {
        when {
            isAddingTournament -> isAddingTournament = false
            editingTournament != null -> editingTournament = null
            currentDestination != AppNavDestination.DASHBOARD -> currentDestination = AppNavDestination.DASHBOARD
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (!isAddingTournament && editingTournament == null) {
                // Custom Bottom Navigation Bar matching Image 3
                Surface(
                    color = DarkBackground,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bottom_nav_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Dashboard
                        BottomNavTabItem(
                            title = "Dashboard",
                            icon = Icons.Default.GridView,
                            isSelected = currentDestination == AppNavDestination.DASHBOARD,
                            onClick = { currentDestination = AppNavDestination.DASHBOARD },
                            testTag = "nav_dashboard"
                        )

                        // 2. Tourneys
                        BottomNavTabItem(
                            title = "Tourneys",
                            icon = Icons.Default.EmojiEvents,
                            isSelected = currentDestination == AppNavDestination.TOURNAMENTS,
                            onClick = { currentDestination = AppNavDestination.TOURNAMENTS },
                            testTag = "nav_tournaments"
                        )

                        // 3. Center Elevated Add Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { isAddingTournament = true }
                                .padding(horizontal = 8.dp)
                                .testTag("nav_add")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldProfit,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .size(46.dp)
                                    .shadow(elevation = 10.dp, shape = CircleShape, spotColor = EmeraldProfit)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Tournament",
                                        tint = Color.Black,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Add",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldProfit
                            )
                        }

                        // 4. Stats
                        BottomNavTabItem(
                            title = "Stats",
                            icon = Icons.Default.BarChart,
                            isSelected = currentDestination == AppNavDestination.STATISTICS,
                            onClick = { currentDestination = AppNavDestination.STATISTICS },
                            testTag = "nav_statistics"
                        )

                        // 5. Bankroll
                        BottomNavTabItem(
                            title = "Bankroll",
                            icon = Icons.Default.AccountBalanceWallet,
                            isSelected = currentDestination == AppNavDestination.BANKROLL,
                            onClick = { currentDestination = AppNavDestination.BANKROLL },
                            testTag = "nav_bankroll"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isAddingTournament -> {
                    AddEditTournamentSheet(
                        existingTournament = null,
                        availableSessions = allSessions,
                        onDismiss = { isAddingTournament = false },
                        onSave = { tournament ->
                            viewModel.saveTournament(tournament)
                            isAddingTournament = false
                        }
                    )
                }

                editingTournament != null -> {
                    AddEditTournamentSheet(
                        existingTournament = editingTournament,
                        availableSessions = allSessions,
                        onDismiss = { editingTournament = null },
                        onSave = { tournament ->
                            viewModel.saveTournament(tournament)
                            editingTournament = null
                        },
                        onDelete = { tournament ->
                            viewModel.deleteTournament(tournament)
                            editingTournament = null
                        }
                    )
                }

                else -> {
                    when (currentDestination) {
                        AppNavDestination.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToAddTournament = { isAddingTournament = true },
                            onNavigateToTournaments = { currentDestination = AppNavDestination.TOURNAMENTS },
                            onNavigateToBankroll = { currentDestination = AppNavDestination.BANKROLL },
                            onOpenInitialBankroll = { showInitialBankrollDialog = true },
                            onSelectTournament = { editingTournament = it },
                            onOpenInfoDialog = { showAppInfoDialog = true }
                        )

                        AppNavDestination.TOURNAMENTS -> TournamentsScreen(
                            viewModel = viewModel,
                            onAddTournament = { isAddingTournament = true },
                            onSelectTournament = { editingTournament = it }
                        )

                        AppNavDestination.STATISTICS -> StatisticsScreen(
                            viewModel = viewModel
                        )

                        AppNavDestination.BANKROLL -> BankrollScreen(
                            viewModel = viewModel
                        )

                        AppNavDestination.ADD -> {
                            // Handled via isAddingTournament
                        }
                    }
                }
            }
        }
    }

    // Initial Bankroll dialog from Hero Card
    if (showInitialBankrollDialog) {
        InitialBankrollDialog(
            currentBankroll = currentBankroll,
            onDismiss = { showInitialBankrollDialog = false },
            onConfirm = { amountPaise ->
                viewModel.setInitialBankroll(amountPaise)
                showInitialBankrollDialog = false
            }
        )
    }

    // App Information Modal
    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = EmeraldProfit, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Poker Bankroll Tracker", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "100% Offline & Private Poker Tracker",
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldProfit,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Local SQLite database on your device with zero cloud tracking.\n• Exact paise decimal arithmetic for accurate financial records.\n• Supports MTT, SNG, PKO, and satellite tournaments.\n• Full JSON backup and CSV export in the Bankroll tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Cost: ₹0. No subscription, no ads, no internet required.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun BottomNavTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) EmeraldProfit else TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) EmeraldProfit else TextMuted
        )
    }
}
