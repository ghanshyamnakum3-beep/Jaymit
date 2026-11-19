package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BirthdayWish
import com.example.model.GroupMember
import com.example.model.UiState
import com.example.ui.components.CrewAvatar
import com.example.util.BirthdayUtils

@Composable
fun BirthdaysScreen(
    currentUserId: String,
    membersState: UiState<List<GroupMember>>,
    wishesState: UiState<List<BirthdayWish>>,
    onEditMyBirthday: () -> Unit,
    onSendWish: (target: GroupMember, message: String, giftEmoji: String) -> Unit,
    onDeleteWish: (String) -> Unit
) {
    var wishTarget by remember { mutableStateOf<GroupMember?>(null) }

    val members = (membersState as? UiState.Success)?.data ?: emptyList()
    val wishes = (wishesState as? UiState.Success)?.data ?: emptyList()

    val sortedMembers = remember(members) {
        members.sortedWith(
            compareBy<GroupMember> {
                BirthdayUtils.daysUntilBirthday(it.birthdayMonth, it.birthdayDay) ?: Int.MAX_VALUE
            }.thenBy { it.displayName.lowercase() }
        )
    }

    val upcomingMember = remember(sortedMembers) {
        sortedMembers.firstOrNull {
            BirthdayUtils.daysUntilBirthday(it.birthdayMonth, it.birthdayDay) != null
        }
    }
    val upcomingDays = upcomingMember?.let {
        BirthdayUtils.daysUntilBirthday(it.birthdayMonth, it.birthdayDay)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("birthdays_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Upcoming Birthday Reminder Hero Banner
        item {
            BirthdayReminderHero(
                upcomingMember = upcomingMember,
                daysAway = upcomingDays,
                onEditMyBirthday = onEditMyBirthday,
                onWishNow = { upcomingMember?.let { wishTarget = it } }
            )
        }

        if (membersState is UiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (membersState is UiState.Error) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = membersState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crew Birthdays (${sortedMembers.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = onEditMyBirthday,
                        modifier = Modifier.testTag("edit_my_birthday_button")
                    ) {
                        Icon(
                            Icons.Default.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("My Birthday")
                    }
                }
            }

            items(sortedMembers, key = { it.id }) { member ->
                val daysAway = BirthdayUtils.daysUntilBirthday(member.birthdayMonth, member.birthdayDay)
                val wishCount = wishes.count { it.targetUserId == member.userId }
                MemberBirthdayCard(
                    member = member,
                    isMe = member.userId == currentUserId,
                    daysAway = daysAway,
                    wishCount = wishCount,
                    onSendWish = { wishTarget = member }
                )
            }
        }

        // Birthday Wishes Wall
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Birthday Wishes Wall (${wishes.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        if (wishes.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No birthday wishes yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap \"Wish\" on any friend above to celebrate their upcoming day!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(wishes, key = { it.id }) { wish ->
                BirthdayWishCard(
                    wish = wish,
                    canDelete = wish.senderUserId == currentUserId,
                    onDelete = { onDeleteWish(wish.id) }
                )
            }
        }
    }

    wishTarget?.let { target ->
        SendWishDialog(
            target = target,
            onSend = { msg, gift ->
                onSendWish(target, msg, gift)
                wishTarget = null
            },
            onDismiss = { wishTarget = null }
        )
    }
}

@Composable
private fun BirthdayReminderHero(
    upcomingMember: GroupMember?,
    daysAway: Int?,
    onEditMyBirthday: () -> Unit,
    onWishNow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .height(178.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_birthday_banner),
            contentDescription = "Birthday celebration banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xD93E2F5B),
                            Color(0x99E85D4A),
                            Color(0x44F5A623)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "BIRTHDAY RADAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (upcomingMember != null && daysAway != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when (daysAway) {
                            0 -> "Happy Birthday ${upcomingMember.displayName}! 🎉"
                            1 -> "${upcomingMember.displayName}'s birthday is TOMORROW!"
                            else -> "${upcomingMember.displayName}'s birthday in $daysAway days"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = BirthdayUtils.formatBirthday(upcomingMember.birthdayMonth, upcomingMember.birthdayDay),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onWishNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF3E2F5B)
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("hero_send_wish_button")
                    ) {
                        Icon(Icons.Default.Cake, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send Birthday Wish", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Never miss a friend's birthday",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Button(
                        onClick = onEditMyBirthday,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF3E2F5B)
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("hero_set_birthday_button")
                    ) {
                        Text("Add Your Birthday")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberBirthdayCard(
    member: GroupMember,
    isMe: Boolean,
    daysAway: Int?,
    wishCount: Int,
    onSendWish: () -> Unit
) {
    val isToday = daysAway == 0
    val isSoon = daysAway != null && daysAway in 1..7

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isToday -> MaterialTheme.colorScheme.tertiaryContainer
                isSoon -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("birthday_member_card_${member.userId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CrewAvatar(
                name = member.displayName,
                avatarIndex = member.avatarIndex,
                size = 48.dp,
                availability = member.availabilityEnum()
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isMe) "${member.displayName} (You)" else member.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (wishCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "$wishCount 🎂",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = BirthdayUtils.formatBirthday(member.birthdayMonth, member.birthdayDay),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (daysAway != null) {
                    Text(
                        text = BirthdayUtils.reminderLabel(daysAway),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday || isSoon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            FilledTonalButton(
                onClick = onSendWish,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("wish_button_${member.userId}")
            ) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Wish")
            }
        }
    }
}

@Composable
private fun BirthdayWishCard(
    wish: BirthdayWish,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wish_card_${wish.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = wish.giftEmoji, style = MaterialTheme.typography.titleLarge)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${wish.senderName} → ${wish.targetName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = BirthdayUtils.formatRelativeTime(wish.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = wish.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_wish_${wish.id}")
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete wish",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SendWishDialog(
    target: GroupMember,
    onSend: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val giftOptions = listOf("🎂", "🎁", "🥳", "💐", "🎈", "🍰")
    var selectedGift by remember { mutableStateOf(giftOptions.first()) }
    var message by remember { mutableStateOf("Happy Birthday ${target.displayName}! Wishing you the best year ahead!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wish ${target.displayName}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pick a birthday gift", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    giftOptions.forEach { emoji ->
                        FilterChip(
                            selected = selectedGift == emoji,
                            onClick = { selectedGift = emoji },
                            label = { Text(emoji, style = MaterialTheme.typography.titleMedium) }
                        )
                    }
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 300) message = it },
                    label = { Text("Your Birthday Message") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wish_message_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (message.isNotBlank()) onSend(message, selectedGift) },
                enabled = message.isNotBlank(),
                modifier = Modifier.testTag("confirm_send_wish_button")
            ) {
                Text("Send $selectedGift")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
