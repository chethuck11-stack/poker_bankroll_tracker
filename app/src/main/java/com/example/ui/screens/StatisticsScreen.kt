package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CurrencyHelper
import com.example.data.repository.GroupProfitStat
import com.example.ui.components.StatCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: PokerViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.lifetimeStats.collectAsStateWithLifecycle()
    val peakAndDrawdown by viewModel.peakAndDrawdown.collectAsStateWithLifecycle()
    val platformStats by viewModel.platformStats.collectAsStateWithLifecycle()
    val formatStats by viewModel.formatStats.collectAsStateWithLifecycle()
    val buyInTierStats by viewModel.buyInTierStats.collectAsStateWithLifecycle()
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Poker Analytics",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Sample size: ${stats.completedTournaments} completed tournaments",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("statistics_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Overall Overview Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "LIFETIME TOURNAMENT SUMMARY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Total Poker Profit", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    text = CurrencyHelper.formatInr(stats.netProfitPaise, showSign = true),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (stats.netProfitPaise >= 0) EmeraldProfit else RedLoss
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Overall ROI", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    text = CurrencyHelper.formatRoi(stats.roiPercentage),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if ((stats.roiPercentage ?: 0.0) >= 0.0) EmeraldProfit else RedLoss
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Invested: ${CurrencyHelper.formatInr(stats.totalInvestmentPaise)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = "Cashes: ${CurrencyHelper.formatInr(stats.totalCashesPaise)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // --- Key Metric Grid ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "ITM Rate",
                            value = CurrencyHelper.formatItm(stats.itmPercentage),
                            subtitle = "${stats.winningCount + stats.breakEvenCount} cashes / ${stats.completedTournaments} events",
                            valueColor = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Avg Buy-In",
                            value = CurrencyHelper.formatInrCompact(stats.averageBuyInPaise),
                            subtitle = "Per completed event",
                            valueColor = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Avg Profit / Event",
                            value = CurrencyHelper.formatInr(stats.averageProfitPaise, showSign = true),
                            subtitle = "Expected value / tourney",
                            valueColor = if (stats.averageProfitPaise >= 0) EmeraldProfit else RedLoss,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Win Rate",
                            value = if (stats.completedTournaments > 0) "${((stats.winningCount.toDouble() / stats.completedTournaments.toDouble()) * 100).toInt()}%" else "0%",
                            subtitle = "${stats.winningCount} Won / ${stats.losingCount} Lost",
                            valueColor = EmeraldProfit,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Best Cash Result",
                            value = CurrencyHelper.formatInr(stats.bestCashPaise),
                            subtitle = "Largest single payout",
                            valueColor = EmeraldProfit,
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Largest Tourney Loss",
                            value = CurrencyHelper.formatInr(stats.largestLossPaise),
                            subtitle = "Single event loss",
                            valueColor = RedLoss,
                            icon = Icons.Default.TrendingDown,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- Peak & Drawdown Card ---
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
                            Text(
                                text = "BANKROLL VOLATILITY & RISK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Icon(Icons.Default.Shield, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Peak Bankroll", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    text = CurrencyHelper.formatInr(peakAndDrawdown.peakBankrollPaise),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldProfit
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Max Drawdown", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    text = "-${CurrencyHelper.formatInr(peakAndDrawdown.maxDrawdownPaise)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RedLoss
                                )
                                if (peakAndDrawdown.maxDrawdownPercent != null) {
                                    Text(
                                        text = "${String.format(java.util.Locale.US, "%.1f", peakAndDrawdown.maxDrawdownPercent)}% from peak",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Breakdown by Platform ---
            item {
                BreakdownSection(
                    title = "Profit by Poker Platform",
                    stats = platformStats
                )
            }

            // --- Breakdown by Format ---
            item {
                BreakdownSection(
                    title = "Profit by Tournament Format",
                    stats = formatStats
                )
            }

            // --- Breakdown by Buy-in Range ---
            item {
                BreakdownSection(
                    title = "Profit by Buy-in Range",
                    stats = buyInTierStats
                )
            }

            // --- Educational Disclaimer ---
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Variance notice: Past poker results and ROI do not predict or guarantee future winnings. Maintain strict bankroll management (e.g. 50-100 buy-ins for MTTs).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BreakdownSection(
    title: String,
    stats: List<GroupProfitStat>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (stats.isEmpty()) {
                Text(
                    text = "No completed tournaments available for this breakdown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.forEach { stat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stat.key,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${stat.tournamentsCount} tournaments • Invest: ${CurrencyHelper.formatInrCompact(stat.totalInvestmentPaise)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyHelper.formatInr(stat.netProfitPaise, showSign = true),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stat.netProfitPaise >= 0) EmeraldProfit else RedLoss
                                )
                                Text(
                                    text = "ROI: ${CurrencyHelper.formatRoi(stat.roiPercentage)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if ((stat.roiPercentage ?: 0.0) >= 0.0) EmeraldProfit else RedLoss
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
