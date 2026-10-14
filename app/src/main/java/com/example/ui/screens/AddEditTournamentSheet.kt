package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyHelper
import com.example.data.model.Session
import com.example.data.model.Tournament
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val POPULAR_PLATFORMS = listOf(
    "PokerBaazi",
    "Spartan Poker",
    "Adda52",
    "PokerStars India",
    "Natural8 India",
    "CoinPoker",
    "Junglee Poker",
    "Custom"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTournamentSheet(
    existingTournament: Tournament? = null,
    availableSessions: List<Session> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Tournament) -> Unit,
    onDelete: ((Tournament) -> Unit)? = null
) {
    var name by remember { mutableStateOf(existingTournament?.name ?: "") }
    var platform by remember { mutableStateOf(existingTournament?.platform ?: "PokerBaazi") }
    var customPlatform by remember { mutableStateOf("") }
    var isCustomPlatform by remember { mutableStateOf(existingTournament?.platform !in POPULAR_PLATFORMS && existingTournament?.platform != null) }

    var format by remember { mutableStateOf(existingTournament?.format ?: TournamentFormat.MTT.displayName) }
    var initialBuyInText by remember { mutableStateOf(if (existingTournament != null) CurrencyHelper.paiseToInputString(existingTournament.initialBuyInPaise) else "") }
    var entryFeeText by remember { mutableStateOf(if (existingTournament != null) CurrencyHelper.paiseToInputString(existingTournament.entryFeePaise) else "") }
    var reEntriesCountText by remember { mutableStateOf(existingTournament?.reEntriesCount?.takeIf { it > 0 }?.toString() ?: "") }
    var reEntryCostText by remember { mutableStateOf(if (existingTournament != null) CurrencyHelper.paiseToInputString(existingTournament.reEntryCostPaise) else "") }
    var addOnsCountText by remember { mutableStateOf(existingTournament?.addOnsCount?.takeIf { it > 0 }?.toString() ?: "") }
    var addOnCostText by remember { mutableStateOf(if (existingTournament != null) CurrencyHelper.paiseToInputString(existingTournament.addOnCostPaise) else "") }
    var additionalFeesText by remember { mutableStateOf(if (existingTournament != null) CurrencyHelper.paiseToInputString(existingTournament.additionalFeesPaise) else "") }
    var cashesText by remember { mutableStateOf(if (existingTournament != null) CurrencyHelper.paiseToInputString(existingTournament.cashesPaise) else "") }
    var refundText by remember { mutableStateOf(if (existingTournament != null) CurrencyHelper.paiseToInputString(existingTournament.refundPaise) else "") }
    var finishPosText by remember { mutableStateOf(existingTournament?.finishingPosition?.toString() ?: "") }
    var fieldSizeText by remember { mutableStateOf(existingTournament?.fieldSize?.toString() ?: "") }
    var status by remember { mutableStateOf(existingTournament?.status ?: TournamentStatus.COMPLETED) }
    var notes by remember { mutableStateOf(existingTournament?.notes ?: "") }
    var selectedSessionId by remember { mutableStateOf(existingTournament?.sessionId) }
    val dateEpochMs by remember { mutableLongStateOf(existingTournament?.dateEpochMs ?: System.currentTimeMillis()) }

    var showValidationErrors by remember { mutableStateOf(false) }

    // Live calculation values
    val initialBuyInPaise = remember(initialBuyInText) { CurrencyHelper.parseInrToPaise(initialBuyInText) }
    val entryFeePaise = remember(entryFeeText) { CurrencyHelper.parseInrToPaise(entryFeeText) }
    val reEntriesCount = remember(reEntriesCountText) { reEntriesCountText.toIntOrNull() ?: 0 }
    val reEntryCostPaise = remember(reEntryCostText, initialBuyInPaise) {
        val parsed = CurrencyHelper.parseInrToPaise(reEntryCostText)
        if (parsed > 0L) parsed else if (reEntriesCount > 0) initialBuyInPaise else 0L
    }
    val addOnsCount = remember(addOnsCountText) { addOnsCountText.toIntOrNull() ?: 0 }
    val addOnCostPaise = remember(addOnCostText) { CurrencyHelper.parseInrToPaise(addOnCostText) }
    val additionalFeesPaise = remember(additionalFeesText) { CurrencyHelper.parseInrToPaise(additionalFeesText) }
    val cashesPaise = remember(cashesText) { CurrencyHelper.parseInrToPaise(cashesText) }
    val refundPaise = remember(refundText) { CurrencyHelper.parseInrToPaise(refundText) }

    val liveTotalInvestment = remember(initialBuyInPaise, reEntriesCount, reEntryCostPaise, addOnsCount, addOnCostPaise, entryFeePaise, additionalFeesPaise) {
        initialBuyInPaise + (reEntriesCount * reEntryCostPaise) + (addOnsCount * addOnCostPaise) + entryFeePaise + additionalFeesPaise
    }

    val liveNetProfit = remember(cashesPaise, refundPaise, liveTotalInvestment) {
        cashesPaise + refundPaise - liveTotalInvestment
    }

    val liveRoi = remember(liveTotalInvestment, liveNetProfit) {
        if (liveTotalInvestment > 0L) {
            (liveNetProfit.toDouble() / liveTotalInvestment.toDouble()) * 100.0
        } else null
    }

    val isNameValid = name.isNotBlank()
    val isBuyInValid = initialBuyInPaise > 0L || status == TournamentStatus.CANCELLED
    val finalPlatform = if (isCustomPlatform) customPlatform.trim().ifBlank { "Custom" } else platform

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingTournament == null) "Log Tournament" else "Edit Tournament",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("tournament_form_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (existingTournament != null && onDelete != null) {
                        IconButton(
                            onClick = { onDelete(existingTournament) },
                            modifier = Modifier.testTag("tournament_form_delete_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedLoss)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (!isNameValid || !isBuyInValid) {
                                showValidationErrors = true
                            } else {
                                val t = Tournament(
                                    id = existingTournament?.id ?: 0L,
                                    name = name.trim(),
                                    dateEpochMs = dateEpochMs,
                                    platform = finalPlatform,
                                    initialBuyInPaise = initialBuyInPaise,
                                    format = format,
                                    entryFeePaise = entryFeePaise,
                                    reEntriesCount = reEntriesCount,
                                    reEntryCostPaise = reEntryCostPaise,
                                    addOnsCount = addOnsCount,
                                    addOnCostPaise = addOnCostPaise,
                                    additionalFeesPaise = additionalFeesPaise,
                                    cashesPaise = cashesPaise,
                                    refundPaise = refundPaise,
                                    finishingPosition = finishPosText.toIntOrNull(),
                                    fieldSize = fieldSizeText.toIntOrNull(),
                                    status = status,
                                    notes = notes.trim(),
                                    sessionId = selectedSessionId
                                )
                                onSave(t)
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("tournament_form_save_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldProfit,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (existingTournament == null) "Save Tournament" else "Update", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // --- Live Financial Outcome Card ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("live_calculation_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, if (liveNetProfit >= 0) EmeraldProfit.copy(alpha = 0.5f) else RedLoss.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LIVE TOURNAMENT OUTCOME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Net Profit / Loss",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = CurrencyHelper.formatInr(liveNetProfit, showSign = true),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (liveNetProfit >= 0) EmeraldProfit else RedLoss
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Estimated ROI",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = CurrencyHelper.formatRoi(liveRoi),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if ((liveRoi ?: 0.0) >= 0.0) EmeraldProfit else RedLoss
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Investment: ${CurrencyHelper.formatInr(liveTotalInvestment)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = "Gross Cashes: ${CurrencyHelper.formatInr(cashesPaise)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // --- Section 1: Required Information ---
            Text(
                text = "REQUIRED DETAILS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tournament Name *") },
                placeholder = { Text("e.g. Daily Main 10L GTD, Mega Stack") },
                isError = showValidationErrors && !isNameValid,
                supportingText = if (showValidationErrors && !isNameValid) {
                    { Text("Tournament name is required", color = RedLoss) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tournament_input_name"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = DarkCardBorder
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Platform Selection
            Text(text = "Online Poker Platform *", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(POPULAR_PLATFORMS) { plat ->
                    val selected = if (plat == "Custom") isCustomPlatform else (!isCustomPlatform && platform == plat)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (plat == "Custom") {
                                isCustomPlatform = true
                            } else {
                                isCustomPlatform = false
                                platform = plat
                            }
                        },
                        label = { Text(plat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = DarkCardBorder,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (isCustomPlatform) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customPlatform,
                    onValueChange = { customPlatform = it },
                    label = { Text("Custom Platform Name") },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Format Selection
            Text(text = "Tournament Format", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TournamentFormat.entries) { fmt ->
                    val selected = format == fmt.displayName
                    FilterChip(
                        selected = selected,
                        onClick = { format = fmt.displayName },
                        label = { Text(fmt.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.secondary,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = DarkCardBorder,
                            selectedBorderColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Initial Buy-In (₹)
            OutlinedTextField(
                value = initialBuyInText,
                onValueChange = { initialBuyInText = it },
                label = { Text("Initial Buy-in (₹) *") },
                placeholder = { Text("e.g. 550, 1100, 2750") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = showValidationErrors && !isBuyInValid,
                supportingText = if (showValidationErrors && !isBuyInValid) {
                    { Text("Initial buy-in is required", color = RedLoss) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tournament_input_buyin"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = DarkCardBorder
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Section 2: Payout & Results ---
            Text(
                text = "RESULTS & PAYOUT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = cashesText,
                onValueChange = { cashesText = it },
                label = { Text("Total Cashes / Payout Received (₹)") },
                placeholder = { Text("0 if uncashed, or payout amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tournament_input_cashes"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = EmeraldProfit,
                    unfocusedBorderColor = DarkCardBorder
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = finishPosText,
                    onValueChange = { finishPosText = it },
                    label = { Text("Finish Position") },
                    placeholder = { Text("e.g. 1, 14") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    value = fieldSizeText,
                    onValueChange = { fieldSizeText = it },
                    label = { Text("Field Size (Players)") },
                    placeholder = { Text("e.g. 250") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Status selection
            Text(text = "Tournament Status", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TournamentStatus.entries.forEach { st ->
                    val selected = status == st
                    FilterChip(
                        selected = selected,
                        onClick = { status = st },
                        label = { Text(st.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (st == TournamentStatus.COMPLETED) EmeraldProfit.copy(alpha = 0.2f) else RedLoss.copy(alpha = 0.2f),
                            selectedLabelColor = if (st == TournamentStatus.COMPLETED) EmeraldProfit else RedLoss,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = DarkCardBorder,
                            selectedBorderColor = if (st == TournamentStatus.COMPLETED) EmeraldProfit else RedLoss
                        )
                    )
                }
            }

            if (status != TournamentStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = refundText,
                    onValueChange = { refundText = it },
                    label = { Text("Refund Amount (₹)") },
                    placeholder = { Text("Full or partial refund amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

            Spacer(modifier = Modifier.height(16.dp))

            // --- Section 3: Optional Re-entries, Add-ons & Fees ---
            Text(
                text = "RE-ENTRIES & ADDITIONAL COSTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = reEntriesCountText,
                    onValueChange = { reEntriesCountText = it },
                    label = { Text("Re-entries Count") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    value = reEntryCostText,
                    onValueChange = { reEntryCostText = it },
                    label = { Text("Cost per Re-entry (₹)") },
                    placeholder = { Text(if (initialBuyInText.isNotBlank()) initialBuyInText else "₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = addOnsCountText,
                    onValueChange = { addOnsCountText = it },
                    label = { Text("Add-ons Count") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    value = addOnCostText,
                    onValueChange = { addOnCostText = it },
                    label = { Text("Cost per Add-on (₹)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = entryFeeText,
                    onValueChange = { entryFeeText = it },
                    label = { Text("Separate Fee (₹)") },
                    placeholder = { Text("If not in buy-in") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    value = additionalFeesText,
                    onValueChange = { additionalFeesText = it },
                    label = { Text("Other Fees (₹)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

            // Session Link
            if (availableSessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LINK TO SESSION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedSessionId == null,
                            onClick = { selectedSessionId = null },
                            label = { Text("None") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                    items(availableSessions) { sess ->
                        val selected = selectedSessionId == sess.id
                        FilterChip(
                            selected = selected,
                            onClick = { selectedSessionId = if (selected) null else sess.id },
                            label = { Text(sess.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes & Hand History Insights") },
                placeholder = { Text("e.g. Lost with KK vs AA deep, solid ICM play on bubble") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = DarkCardBorder
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
