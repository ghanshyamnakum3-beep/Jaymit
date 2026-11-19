package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AvailabilityStatus
import com.example.model.FriendGroup
import com.example.ui.theme.AvatarPalette
import com.example.ui.theme.HospitalRose
import com.example.ui.theme.MarigoldTertiary
import com.example.ui.theme.SageMint
import com.example.util.BirthdayUtils

@Composable
fun CrewAvatar(
    name: String,
    avatarIndex: Long,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    availability: AvailabilityStatus? = null
) {
    val baseColor = AvatarPalette[(avatarIndex.toInt().coerceAtLeast(0)) % AvatarPalette.size]
    val initials = remember(name) {
        name.trim().split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "F" }
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(baseColor, baseColor.copy(alpha = 0.78f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        if (availability != null) {
            val dotColor = when (availability) {
                AvailabilityStatus.AVAILABLE -> SageMint
                AvailabilityStatus.BUSY -> MarigoldTertiary
                AvailabilityStatus.NEED_SUPPORT -> HospitalRose
                AvailabilityStatus.OFFLINE -> MaterialTheme.colorScheme.outline
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun GroupManagementDialog(
    activeGroup: FriendGroup,
    allGroups: List<FriendGroup>,
    onSelectGroup: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onJoinGroup: (String) -> Unit,
    onCopyFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableIntStateOf(0) } // 0 = list/invite, 1 = create, 2 = join
    var newGroupName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (mode) {
                    1 -> "Create New Crew"
                    2 -> "Join Another Crew"
                    else -> "Your Friends Crews"
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (mode == 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Invite Friends to ${activeGroup.name}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = activeGroup.inviteCode,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            copyToClipboard(context, activeGroup.inviteCode)
                                            onCopyFeedback("Invite code ${activeGroup.inviteCode} copied!")
                                        },
                                        modifier = Modifier.testTag("copy_invite_code_button")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy invite code")
                                    }
                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Join our friends group \"${activeGroup.name}\" on CrewCircle! Use invite code: ${activeGroup.inviteCode}"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Invite Code"))
                                        },
                                        modifier = Modifier.testTag("share_invite_code_button")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share invite code")
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Switch Crew",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    allGroups.forEach { grp ->
                        val selected = grp.id == activeGroup.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectGroup(grp.id)
                                    onDismiss()
                                }
                                .testTag("switch_group_${grp.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = grp.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${grp.memberUids.size} members • Code ${grp.inviteCode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Active group",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { mode = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialog_create_group_tab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New")
                        }
                        OutlinedButton(
                            onClick = { mode = 2 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialog_join_group_tab")
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Join")
                        }
                    }
                } else if (mode == 1) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { if (it.length <= 50) newGroupName = it },
                        label = { Text("Friends Group Name") },
                        placeholder = { Text("e.g. Weekend Squad") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_new_group_name_input")
                    )
                } else {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { if (it.length <= 12) joinCode = it.uppercase() },
                        label = { Text("6-Character Invite Code") },
                        placeholder = { Text("e.g. CREW7X") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_join_code_input")
                    )
                }
            }
        },
        confirmButton = {
            when (mode) {
                1 -> Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            onCreateGroup(newGroupName)
                            onDismiss()
                        }
                    },
                    enabled = newGroupName.isNotBlank(),
                    modifier = Modifier.testTag("dialog_create_group_confirm")
                ) { Text("Create") }

                2 -> Button(
                    onClick = {
                        if (joinCode.trim().length >= 4) {
                            onJoinGroup(joinCode)
                            onDismiss()
                        }
                    },
                    enabled = joinCode.trim().length >= 4,
                    modifier = Modifier.testTag("dialog_join_group_confirm")
                ) { Text("Join") }

                else -> TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dialog_group_done")
                ) { Text("Done") }
            }
        },
        dismissButton = {
            if (mode != 0) {
                TextButton(onClick = { mode = 0 }) { Text("Back") }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileBirthdayDialog(
    initialName: String,
    initialAvatarIndex: Long,
    initialMonth: Long,
    initialDay: Long,
    onSave: (String, Long, Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var avatarIdx by remember { mutableLongStateOf(initialAvatarIndex) }
    var selectedMonth by remember { mutableIntStateOf(if (initialMonth in 1..12) initialMonth.toInt() else 1) }
    var selectedDay by remember { mutableIntStateOf(if (initialDay in 1..31) initialDay.toInt() else 1) }
    var includeBirthday by remember { mutableStateOf(initialMonth in 1..12 && initialDay in 1..31) }

    val maxDays = BirthdayUtils.maxDaysInMonth(selectedMonth)
    if (selectedDay > maxDays) selectedDay = maxDays

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My Profile & Birthday", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                Text("Avatar Color", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarPalette.forEachIndexed { index, color ->
                        val isSelected = avatarIdx.toInt() == index
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { avatarIdx = index.toLong() }
                                .testTag("avatar_color_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected color",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Set Birthday Reminder", style = MaterialTheme.typography.labelLarge)
                    FilterChip(
                        selected = includeBirthday,
                        onClick = { includeBirthday = !includeBirthday },
                        label = { Text(if (includeBirthday) "Enabled" else "Off") },
                        modifier = Modifier.testTag("toggle_birthday_chip")
                    )
                }

                if (includeBirthday) {
                    Text("Birth Month", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..12).forEach { m ->
                            FilterChip(
                                selected = selectedMonth == m,
                                onClick = { selectedMonth = m },
                                label = { Text(BirthdayUtils.monthName(m)) }
                            )
                        }
                    }

                    Text("Day (${BirthdayUtils.fullMonthName(selectedMonth)} $selectedDay)", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedDay = if (selectedDay <= 1) maxDays else selectedDay - 1 },
                            modifier = Modifier.testTag("birthday_day_minus")
                        ) { Text("-") }
                        Text(
                            text = selectedDay.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = { selectedDay = if (selectedDay >= maxDays) 1 else selectedDay + 1 },
                            modifier = Modifier.testTag("birthday_day_plus")
                        ) { Text("+") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = if (includeBirthday) selectedMonth.toLong() else 0L
                    val d = if (includeBirthday) selectedDay.toLong() else 0L
                    onSave(name.trim().ifBlank { initialName }, m, d, avatarIdx)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_profile_birthday_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", text))
}
