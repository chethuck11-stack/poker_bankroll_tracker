package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BackupData
import com.example.data.model.BackupValidationResult
import com.example.data.model.BankrollTransaction
import com.example.data.model.BankrollTransactionType
import com.example.data.model.CurrencyHelper
import com.example.data.model.Session
import com.example.data.model.SessionWithTournaments
import com.example.ui.components.ProfitPill
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RedLoss
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PokerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankrollScreen(
    viewModel: PokerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Ledger", "Sessions", "Backup & Restore")

    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val sessionsWithTournaments by viewModel.allSessionsWithTournaments.collectAsStateWithLifecycle()
    val currentBankroll by viewModel.currentBankroll.collectAsStateWithLifecycle()

    var showAddTxDialog by remember { mutableStateOf(false) }
    var showInitialBankrollDialog by remember { mutableStateOf(false) }
    var showCreateSessionDialog by remember { mutableStateOf(false) }

    // Dialog state for restoring backup
    var pendingRestoreData by remember { mutableStateOf<BackupData?>(null) }
    var pendingValidationResult by remember { mutableStateOf<BackupValidationResult?>(null) }
    var replaceOnRestore by remember { mutableStateOf(false) }

    var showResetDefaultDialog by remember { mutableStateOf(false) }
    var showWipeCleanDialog by remember { mutableStateOf(false) }

    // Document Picker Launchers for Storage Access Framework
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportJson()
                writeTextToUri(context, uri, json)
                Toast.makeText(context, "Full backup exported successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = readTextFromUri(context, uri)
                if (json != null) {
                    val (validation, data) = viewModel.validateBackupJson(json)
                    if (validation.isValid && data != null) {
                        pendingValidationResult = validation
                        pendingRestoreData = data
                    } else {
                        Toast.makeText(context, validation.errorMessage ?: "Invalid backup file", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Failed to read backup file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val exportTournamentsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val csv = viewModel.exportTournamentsCsv()
                writeTextToUri(context, uri, csv)
                Toast.makeText(context, "Tournaments CSV exported!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportTransactionsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val csv = viewModel.exportTransactionsCsv()
                writeTextToUri(context, uri, csv)
                Toast.makeText(context, "Ledger CSV exported!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportSessionsCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val csv = viewModel.exportSessionsCsv()
                writeTextToUri(context, uri, csv)
                Toast.makeText(context, "Sessions CSV exported!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bankroll & Operations",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Balance: ${CurrencyHelper.formatInr(currentBankroll)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldProfit,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkBackground,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) TextPrimary else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> LedgerTab(
                    transactions = transactions,
                    currentBankroll = currentBankroll,
                    onOpenAddTransaction = { showAddTxDialog = true },
                    onOpenInitialBankroll = { showInitialBankrollDialog = true },
                    onDeleteTransaction = { viewModel.deleteBankrollTransaction(it) }
                )
                1 -> SessionsTab(
                    sessionsWithTournaments = sessionsWithTournaments,
                    onCreateSession = { showCreateSessionDialog = true },
                    onDeleteSession = { viewModel.deleteSession(it) }
                )
                2 -> BackupTab(
                    onExportJson = {
                        val fileName = "poker_bankroll_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.json"
                        exportJsonLauncher.launch(fileName)
                    },
                    onImportJson = {
                        importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    onExportTournamentsCsv = {
                        val fileName = "tournaments_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.csv"
                        exportTournamentsCsvLauncher.launch(fileName)
                    },
                    onExportTransactionsCsv = {
                        val fileName = "bankroll_ledger_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.csv"
                        exportTransactionsCsvLauncher.launch(fileName)
                    },
                    onExportSessionsCsv = {
                        val fileName = "poker_sessions_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.csv"
                        exportSessionsCsvLauncher.launch(fileName)
                    },
                    onResetToDefault = { showResetDefaultDialog = true },
                    onWipeClean = { showWipeCleanDialog = true }
                )
            }
        }
    }

    // --- Dialogs ---
    if (showAddTxDialog) {
        AddTransactionDialog(
            onDismiss = { showAddTxDialog = false },
            onConfirm = { type, amountPaise, desc ->
                viewModel.addBankrollTransaction(type, amountPaise, desc)
                showAddTxDialog = false
            }
        )
    }

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

    if (showCreateSessionDialog) {
        CreateSessionDialog(
            onDismiss = { showCreateSessionDialog = false },
            onConfirm = { session ->
                viewModel.saveSession(session)
                showCreateSessionDialog = false
            }
        )
    }

    // Confirmation & Validation Dialog for Backup Restore
    if (pendingRestoreData != null && pendingValidationResult != null) {
        val validation = pendingValidationResult!!
        val data = pendingRestoreData!!

        AlertDialog(
            onDismissRequest = {
                pendingRestoreData = null
                pendingValidationResult = null
            },
            title = {
                Text("Restore Backup Confirmation", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Backup file validated successfully:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• ${validation.tournamentCount} Tournaments (${validation.duplicateTournamentsCount} existing matches)\n• ${validation.transactionCount} Bankroll transactions\n• ${validation.sessionCount} Sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Choose Restore Mode:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceOnRestore = false }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = !replaceOnRestore, onClick = { replaceOnRestore = false })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Merge Records", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Keeps existing data and appends new records", fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceOnRestore = true }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = replaceOnRestore, onClick = { replaceOnRestore = true })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Replace Everything", fontWeight = FontWeight.SemiBold, color = RedLoss)
                            Text("Clears current records and installs this backup", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(data, replaceOnRestore) {
                            Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_SHORT).show()
                            pendingRestoreData = null
                            pendingValidationResult = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (replaceOnRestore) RedLoss else EmeraldProfit,
                        contentColor = Color.Black
                    )
                ) {
                    Text(if (replaceOnRestore) "Wipe & Restore" else "Merge Data", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingRestoreData = null
                        pendingValidationResult = null
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showResetDefaultDialog) {
        AlertDialog(
            onDismissRequest = { showResetDefaultDialog = false },
            title = {
                Text("Restore Default Template?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    text = "This will clear all current records and restore the initial default starter template (₹5,00,000 opening bankroll and sample tournament). Any currently logged tournaments and transactions will be replaced.\n\nAre you sure you want to proceed?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefaultData {
                            Toast.makeText(context, "Data restored to default template!", Toast.LENGTH_SHORT).show()
                            showResetDefaultDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black)
                ) {
                    Text("Restore Defaults", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDefaultDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showWipeCleanDialog) {
        AlertDialog(
            onDismissRequest = { showWipeCleanDialog = false },
            title = {
                Text("Wipe All Data Clean?", fontWeight = FontWeight.Bold, color = RedLoss)
            },
            text = {
                Text(
                    text = "This will permanently delete all tournaments, bankroll ledger entries, and sessions. The app will be completely empty with ₹0 bankroll, ready for a 100% fresh personal start.\n\nThis action cannot be undone unless you exported a backup file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wipeCleanAllData {
                            Toast.makeText(context, "Database wiped clean! Fresh start ready.", Toast.LENGTH_SHORT).show()
                            showWipeCleanDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedLoss, contentColor = Color.White)
                ) {
                    Text("Yes, Wipe Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeCleanDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun LedgerTab(
    transactions: List<BankrollTransaction>,
    currentBankroll: Long,
    onOpenAddTransaction: () -> Unit,
    onOpenInitialBankroll: () -> Unit,
    onDeleteTransaction: (BankrollTransaction) -> Unit
) {
    // Calculate running balance for each transaction (sorted chronologically)
    val sortedAsc = remember(transactions) { transactions.sortedBy { it.dateEpochMs } }
    val runningBalances = remember(sortedAsc) {
        val map = mutableMapOf<Long, Long>()
        var running = 0L
        for (tx in sortedAsc) {
            running += tx.netImpactPaise
            map[tx.id] = running
        }
        map
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ledger_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Total Bankroll Balance", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(
                                text = CurrencyHelper.formatInr(currentBankroll),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }

                        Button(
                            onClick = onOpenAddTransaction,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_transaction_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Funds", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onOpenInitialBankroll,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                    ) {
                        Text("Configure / Reset Initial Bankroll", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Text(
                text = "TRANSACTION HISTORY & RUNNING BALANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions logged yet.", color = TextMuted)
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
                val running = runningBalances[tx.id] ?: 0L
                val isPositive = tx.netImpactPaise >= 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_item_${tx.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isPositive) EmeraldProfit.copy(alpha = 0.2f) else RedLoss.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = tx.type.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) EmeraldProfit else RedLoss,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (tx.tournamentId != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Auto-Synced", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tx.description.ifBlank { tx.type.displayName },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = dateFormat.format(Date(tx.dateEpochMs)),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyHelper.formatInr(tx.netImpactPaise, showSign = true),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositive) EmeraldProfit else RedLoss
                            )
                            Text(
                                text = "Bal: ${CurrencyHelper.formatInr(running)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )

                            // Allow deleting manual transactions
                            if (tx.tournamentId == null) {
                                IconButton(
                                    onClick = { onDeleteTransaction(tx) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionsTab(
    sessionsWithTournaments: List<SessionWithTournaments>,
    onCreateSession: () -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sessions_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Tournament Sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Group tourneys into single playing grinds", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }

                    Button(
                        onClick = onCreateSession,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("create_session_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (sessionsWithTournaments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sessions created yet. Group multiple tournaments into a session to track aggregate grind results.", color = TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            items(sessionsWithTournaments, key = { it.session.id }) { item ->
                val s = item.session
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
                val dateStr = dateFormat.format(Date(s.dateEpochMs))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_card_${s.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                val timeRange = listOfNotNull(s.startTime, s.endTime).joinToString(" - ")
                                Text(
                                    text = "$dateStr ${if (timeRange.isNotBlank()) "• $timeRange" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfitPill(netProfitPaise = item.netProfitPaise)
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onDeleteSession(s.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${item.tournamentCount} Tournaments", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(text = "Invest: ${CurrencyHelper.formatInr(item.totalInvestmentPaise)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(text = "Cashes: ${CurrencyHelper.formatInr(item.totalCashesPaise)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(text = "ROI: ${CurrencyHelper.formatRoi(item.roiPercentage)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if ((item.roiPercentage ?: 0.0) >= 0.0) EmeraldProfit else RedLoss)
                        }

                        if (s.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = s.notes, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupTab(
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportTournamentsCsv: () -> Unit,
    onExportTransactionsCsv: () -> Unit,
    onExportSessionsCsv: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("backup_tab"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Warning Banner
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GoldAccent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Local Storage Safety Notice", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All tournament and bankroll records are stored locally on your device with zero cloud sync. If you uninstall the app or clear its storage, local records will be deleted. We strongly recommend periodically exporting a JSON backup to your Google Drive or internal storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Full JSON Backup & Restore
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Full Database JSON Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Complete snapshot of all tournaments, bankroll ledger, and sessions.", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportJson,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_json"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onImportJson,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_restore_json"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, DarkCardBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Backup")
                        }
                    }
                }
            }
        }

        // CSV Spreadsheets
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Export Spreadsheets (CSV)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Export your data to standard comma-separated files readable by Excel, Google Sheets, or Numbers.", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onExportTournamentsCsv,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_export_tournaments_csv"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("Export Tournaments CSV", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onExportTransactionsCsv,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_export_transactions_csv"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("Export Bankroll Ledger CSV", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onExportSessionsCsv,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_export_sessions_csv"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("Export Sessions CSV", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// --- Dialogs ---

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (BankrollTransactionType, Long, String) -> Unit
) {
    var type by remember { mutableStateOf(BankrollTransactionType.DEPOSIT) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val amountPaise = remember(amountText) { CurrencyHelper.parseInrToPaise(amountText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bankroll Transaction", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Text("Select Type:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                val allowedTypes = listOf(
                    BankrollTransactionType.DEPOSIT,
                    BankrollTransactionType.WITHDRAWAL,
                    BankrollTransactionType.BONUS_RAKEBACK,
                    BankrollTransactionType.MANUAL_CORRECTION
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allowedTypes.take(2).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allowedTypes.drop(2).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_tx_amount"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Reason / Note") },
                    placeholder = { Text("e.g. Baazi deposit bonus, UPI reload") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountPaise > 0L) {
                        onConfirm(type, amountPaise, description)
                    }
                },
                enabled = amountPaise > 0L,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_tx_save_btn")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun InitialBankrollDialog(
    currentBankroll: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val amountPaise = remember(amountText) { CurrencyHelper.parseInrToPaise(amountText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Initial Bankroll", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Text(
                    text = "Enter your starting poker bankroll. This creates or updates the opening balance transaction in your bankroll ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Starting Bankroll (₹) *") },
                    placeholder = { Text("e.g. 10000, 25000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_initial_bankroll_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountPaise > 0L) {
                        onConfirm(amountPaise)
                    }
                },
                enabled = amountPaise > 0L,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                modifier = Modifier.testTag("dialog_initial_bankroll_save_btn")
            ) {
                Text("Set Initial Bankroll", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun CreateSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (Session) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Tournament Session", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Session Name *") },
                    placeholder = { Text("e.g. Sunday Million Grind, Friday Evening MTTs") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_input_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (optional)") },
                        placeholder = { Text("18:00") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (optional)") },
                        placeholder = { Text("23:30") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Session Notes") },
                    placeholder = { Text("Focus, mindset, notes on field") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            Session(
                                name = name.trim(),
                                dateEpochMs = System.currentTimeMillis(),
                                startTime = startTime.trim().ifBlank { null },
                                endTime = endTime.trim().ifBlank { null },
                                notes = notes.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                modifier = Modifier.testTag("session_save_btn")
            ) {
                Text("Create Session", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = DarkSurface
    )
}

// Helpers for reading/writing via Storage Access Framework
suspend fun writeTextToUri(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        OutputStreamWriter(outputStream).use { writer ->
            writer.write(text)
        }
    }
}

suspend fun readTextFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    } catch (_: Exception) {
        null
    }
}
