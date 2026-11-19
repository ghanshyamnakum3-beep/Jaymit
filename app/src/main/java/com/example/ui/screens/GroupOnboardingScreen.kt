package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.BirthdayUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupOnboardingScreen(
    displayName: String,
    isBusy: Boolean,
    onCreateGroup: (groupName: String, bMonth: Long, bDay: Long) -> Unit,
    onJoinGroup: (code: String, bMonth: Long, bDay: Long) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Create, 1 = Join
    var groupName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var setBirthdayNow by remember { mutableStateOf(true) }
    var birthMonth by remember { mutableIntStateOf(6) }
    var birthDay by remember { mutableIntStateOf(15) }

    val maxDays = BirthdayUtils.maxDaysInMonth(birthMonth)
    if (birthDay > maxDays) birthDay = maxDays

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hey $displayName!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Start a private friends crew or enter an invite code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("onboarding_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Default.Celebration, contentDescription = null) },
                    modifier = Modifier.testTag("onboarding_create_tab")
                ) {
                    Text("Create Crew")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                    modifier = Modifier.testTag("onboarding_join_tab")
                ) {
                    Text("Join with Code")
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedTab == 0) {
                        Text(
                            text = "Name Your Friends Group",
                            style = MaterialTheme.typography.titleLarge
                        )
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { if (it.length <= 50) groupName = it },
                            label = { Text("Crew Name") },
                            placeholder = { Text("e.g. College Legends, Sunset Crew") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_group_name_input")
                        )
                    } else {
                        Text(
                            text = "Enter Friend's Invite Code",
                            style = MaterialTheme.typography.titleLarge
                        )
                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = { if (it.length <= 12) inviteCode = it.uppercase() },
                            label = { Text("Invite Code") },
                            placeholder = { Text("e.g. CREW7X") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_group_code_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Cake,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Share my birthday", style = MaterialTheme.typography.titleSmall)
                        }
                        FilterChip(
                            selected = setBirthdayNow,
                            onClick = { setBirthdayNow = !setBirthdayNow },
                            label = { Text(if (setBirthdayNow) "Yes" else "Skip") },
                            modifier = Modifier.testTag("onboarding_birthday_toggle")
                        )
                    }

                    if (setBirthdayNow) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (1..12).forEach { m ->
                                FilterChip(
                                    selected = birthMonth == m,
                                    onClick = { birthMonth = m },
                                    label = { Text(BirthdayUtils.monthName(m)) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { birthDay = if (birthDay <= 1) maxDays else birthDay - 1 },
                                modifier = Modifier.testTag("onboarding_day_minus")
                            ) { Text("-") }
                            Text(
                                text = "${BirthdayUtils.fullMonthName(birthMonth)} $birthDay",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            OutlinedButton(
                                onClick = { birthDay = if (birthDay >= maxDays) 1 else birthDay + 1 },
                                modifier = Modifier.testTag("onboarding_day_plus")
                            ) { Text("+") }
                        }
                    }

                    val canSubmit = if (selectedTab == 0) groupName.isNotBlank() else inviteCode.trim().length >= 4
                    Button(
                        onClick = {
                            val m = if (setBirthdayNow) birthMonth.toLong() else 0L
                            val d = if (setBirthdayNow) birthDay.toLong() else 0L
                            if (selectedTab == 0) {
                                onCreateGroup(groupName, m, d)
                            } else {
                                onJoinGroup(inviteCode, m, d)
                            }
                        },
                        enabled = !isBusy && canSubmit,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("onboarding_submit_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (selectedTab == 0) "Create Crew & Get Invite Code" else "Join Crew",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
