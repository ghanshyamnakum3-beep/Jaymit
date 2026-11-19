package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AvailabilityStatus
import com.example.model.GroupMember
import com.example.model.StatusCategory
import com.example.model.StatusPost
import com.example.model.UiState
import com.example.ui.components.CrewAvatar
import com.example.ui.theme.HospitalRose
import com.example.ui.theme.MarigoldTertiary
import com.example.ui.theme.SageMint
import com.example.ui.theme.StudyViolet
import com.example.ui.theme.TravelTeal
import com.example.ui.theme.WorkCobalt
import com.example.util.BirthdayUtils

@Composable
fun StatusScreen(
    currentUserId: String,
    membersState: UiState<List<GroupMember>>,
    statusesState: UiState<List<StatusPost>>,
    onPostStatus: (StatusCategory, String, String, AvailabilityStatus) -> Unit,
    onToggleSupport: (StatusPost) -> Unit,
    onDeleteStatus: (String) -> Unit
) {
    var showComposer by remember { mutableStateOf(false) }
    var presetCategory by remember { mutableStateOf(StatusCategory.GENERAL) }

    val members = (membersState as? UiState.Success)?.data ?: emptyList()
    val posts = (statusesState as? UiState.Success)?.data ?: emptyList()
    val myMember = members.firstOrNull { it.userId == currentUserId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("status_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MyStatusBannerCard(
                myMember = myMember,
                onOpenComposer = { cat ->
                    presetCategory = cat
                    showComposer = true
                }
            )
        }

        if (members.isNotEmpty()) {
            item {
                Text(
                    text = "Where Everyone Is Right Now",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(members, key = { it.id }) { member ->
                        LiveMemberChipCard(
                            member = member,
                            isMe = member.userId == currentUserId
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Live Status Timeline (${posts.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (statusesState is UiState.Loading) {
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
        } else if (statusesState is UiState.Error) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = statusesState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else if (posts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("No life updates posted yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Let your friends know if you're at the hospital, starting a job, studying, or free to hang out!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(posts, key = { it.id }) { post ->
                StatusTimelineCard(
                    post = post,
                    currentUserId = currentUserId,
                    onToggleSupport = { onToggleSupport(post) },
                    onDelete = { onDeleteStatus(post.id) }
                )
            }
        }
    }

    if (showComposer) {
        UpdateStatusDialog(
            initialCategory = presetCategory,
            initialPlace = myMember?.statusPlace.orEmpty(),
            onPublish = { cat, msg, place, avail ->
                onPostStatus(cat, msg, place, avail)
                showComposer = false
            },
            onDismiss = { showComposer = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyStatusBannerCard(
    myMember: GroupMember?,
    onOpenComposer: (StatusCategory) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Live Status",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = myMember?.statusText?.ifBlank { "Tap below to share where you are & what you're up to" }
                            ?: "Share where you are & what you're doing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = { onOpenComposer(myMember?.categoryEnum() ?: StatusCategory.GENERAL) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("open_update_status_button")
                ) {
                    Icon(Icons.Default.EditLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Update")
                }
            }

            Text(
                text = "Quick presets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = false,
                        onClick = { onOpenComposer(cat) },
                        label = { Text("${cat.emoji} ${cat.label}") },
                        modifier = Modifier.testTag("preset_status_${cat.name}")
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveMemberChipCard(
    member: GroupMember,
    isMe: Boolean
) {
    val cat = member.categoryEnum()
    val accent = categoryAccentColor(cat)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.width(188.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CrewAvatar(
                    name = member.displayName,
                    avatarIndex = member.avatarIndex,
                    size = 40.dp,
                    availability = member.availabilityEnum()
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMe) "You" else member.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${cat.emoji} ${cat.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = member.statusText.ifBlank { "No update yet" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (member.statusPlace.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = member.statusPlace,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusTimelineCard(
    post: StatusPost,
    currentUserId: String,
    onToggleSupport: () -> Unit,
    onDelete: () -> Unit
) {
    val cat = post.categoryEnum()
    val avail = post.availabilityEnum()
    val accent = categoryAccentColor(cat)
    val supported = currentUserId in post.supportUids

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_post_card_${post.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CrewAvatar(
                    name = post.authorName,
                    avatarIndex = post.avatarIndex,
                    size = 42.dp,
                    availability = avail
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${avail.label} • ${BirthdayUtils.formatRelativeTime(post.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${cat.emoji} ${cat.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = post.message,
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (post.placeName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = post.placeName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleSupport,
                        modifier = Modifier.testTag("support_status_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (supported) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Send care",
                            tint = if (supported) HospitalRose else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${post.supportUids.size} Care",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (post.userId == currentUserId) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_status_${post.id}")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete status",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateStatusDialog(
    initialCategory: StatusCategory,
    initialPlace: String,
    onPublish: (StatusCategory, String, String, AvailabilityStatus) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(initialCategory) }
    var availability by remember {
        mutableStateOf(
            if (initialCategory == StatusCategory.HOSPITAL) AvailabilityStatus.NEED_SUPPORT
            else AvailabilityStatus.AVAILABLE
        )
    }
    var message by remember {
        mutableStateOf(
            when (initialCategory) {
                StatusCategory.HOSPITAL -> "I'm at the hospital right now"
                StatusCategory.WORK -> "Working on my shift / job"
                StatusCategory.STUDY -> "Studying and focused"
                StatusCategory.CHILL -> "Free right now, let's hang out!"
                StatusCategory.TRAVEL -> "Currently traveling"
                StatusCategory.GENERAL -> ""
            }
        )
    }
    var placeName by remember { mutableStateOf(initialPlace) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Your Live Status", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text("${cat.emoji} ${cat.label}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 240) message = it },
                    label = { Text("What are you doing?") },
                    placeholder = { Text("e.g. I'm in City Hospital, or doing my job at…") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_message_input")
                )

                OutlinedTextField(
                    value = placeName,
                    onValueChange = { if (it.length <= 80) placeName = it },
                    label = { Text("Place / Workplace / Hospital (Optional)") },
                    placeholder = { Text("e.g. St. Mary Hospital, Tech Park") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("status_place_input")
                )

                Text("Availability", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AvailabilityStatus.entries.forEach { avail ->
                        FilterChip(
                            selected = availability == avail,
                            onClick = { availability = avail },
                            label = { Text(avail.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(category, message, placeName, availability) },
                enabled = message.isNotBlank(),
                modifier = Modifier.testTag("confirm_post_status_button")
            ) {
                Text("Post Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun categoryAccentColor(cat: StatusCategory): Color = when (cat) {
    StatusCategory.HOSPITAL -> HospitalRose
    StatusCategory.WORK -> WorkCobalt
    StatusCategory.STUDY -> StudyViolet
    StatusCategory.CHILL -> SageMint
    StatusCategory.TRAVEL -> TravelTeal
    StatusCategory.GENERAL -> MaterialTheme.colorScheme.primary
}
