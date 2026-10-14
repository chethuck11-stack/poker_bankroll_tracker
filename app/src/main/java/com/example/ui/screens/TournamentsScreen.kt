package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CurrencyHelper
import com.example.data.model.Tournament
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus
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
import com.example.ui.viewmodel.ProfitFilter
import com.example.ui.viewmodel.TournamentSort
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentsScreen(
    viewModel: PokerViewModel,
    onAddTournament: () -> Unit,
    onSelectTournament: (Tournament) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTournaments by viewModel.filteredTournaments.collectAsStateWithLifecycle()
    val allTournaments by viewModel.allTournaments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatformFilter.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.selectedFormatFilter.collectAsStateWithLifecycle()
    val selectedProfit by viewModel.selectedProfitFilter.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSortOrder.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    val distinctPlatforms = remember(allTournaments) {
        allTournaments.map { it.platform }.distinct().filter { it.isNotBlank() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tournaments",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${filteredTournaments.size} of ${allTournaments.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("tournaments_sort_button")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = TextPrimary)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(DarkSurfaceVariant)
                        ) {
                            TournamentSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            sort.displayName,
                                            color = if (selectedSort == sort) EmeraldProfit else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectedSortOrder.value = sort
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTournament,
                containerColor = EmeraldProfit,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("tournaments_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Tournament")
            }
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search by name, platform, notes...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("tournaments_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = DarkCardBorder
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Horizontal Filters Row: Profit (All, Winning, Losing), Platform, Format
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Profit filter chips
                items(ProfitFilter.entries) { filter ->
                    val selected = selectedProfit == filter
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectedProfitFilter.value = filter },
                        label = { Text(filter.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (filter == ProfitFilter.WINNING) EmeraldProfit.copy(alpha = 0.2f) else if (filter == ProfitFilter.LOSING) RedLoss.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = if (filter == ProfitFilter.WINNING) EmeraldProfit else if (filter == ProfitFilter.LOSING) RedLoss else MaterialTheme.colorScheme.primary,
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

                // Platform filter
                items(distinctPlatforms) { plat ->
                    val selected = selectedPlatform == plat
                    FilterChip(
                        selected = selected,
                        onClick = {
                            viewModel.selectedPlatformFilter.value = if (selected) null else plat
                        },
                        label = { Text(plat, fontSize = 11.sp) },
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

                // Format filter
                items(TournamentFormat.entries) { fmt ->
                    val selected = selectedFormat == fmt.displayName
                    FilterChip(
                        selected = selected,
                        onClick = {
                            viewModel.selectedFormatFilter.value = if (selected) null else fmt.displayName
                        },
                        label = { Text(fmt.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = DarkCardBorder,
                            selectedBorderColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            }

            // Tournaments List
            if (filteredTournaments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (allTournaments.isEmpty()) "No tournaments recorded yet" else "No matching tournaments found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (allTournaments.isEmpty()) "Tap + to log your first tournament" else "Try clearing your filters or search terms",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("tournaments_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTournaments, key = { it.id }) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            onClick = { onSelectTournament(tournament) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentCard(
    tournament: Tournament,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
    val dateString = dateFormat.format(Date(tournament.dateEpochMs))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tournament_card_${tournament.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Name & Profit Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tournament.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                ProfitPill(netProfitPaise = tournament.netProfitPaise)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Platform, Format, Status (if not completed), Finish Pos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = tournament.platform,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = tournament.format,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (tournament.status != TournamentStatus.COMPLETED) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RedLoss.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = tournament.status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = RedLoss,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (tournament.finishingPosition != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoldAccent.copy(alpha = 0.15f)
                    ) {
                        val posStr = if (tournament.fieldSize != null) {
                            "#${tournament.finishingPosition} / ${tournament.fieldSize}"
                        } else {
                            "#${tournament.finishingPosition}"
                        }
                        Text(
                            text = posStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (tournament.reEntriesCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceVariant
                    ) {
                        Text(
                            text = "${tournament.reEntriesCount} Re-entry",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Total Investment", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = CurrencyHelper.formatInr(tournament.totalInvestmentPaise),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Gross Cashes", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = CurrencyHelper.formatInr(tournament.cashesPaise),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (tournament.cashesPaise > 0L) EmeraldProfit else TextMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "ROI", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = CurrencyHelper.formatRoi(tournament.roiPercentage),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if ((tournament.roiPercentage ?: 0.0) >= 0.0) EmeraldProfit else RedLoss
                    )
                }
            }

            if (tournament.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tournament.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 2
                )
            }
        }
    }
}
