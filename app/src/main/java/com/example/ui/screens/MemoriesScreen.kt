package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.model.GroupMemory
import com.example.model.MediaType
import com.example.model.UiState
import com.example.util.BirthdayUtils
import com.example.util.MediaHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MemoriesScreen(
    currentUserId: String,
    memoriesState: UiState<List<GroupMemory>>,
    onPostMemory: (
        mediaType: MediaType,
        caption: String,
        tag: String,
        mediaBase64: String,
        thumbnailBase64: String,
        externalUrl: String
    ) -> Unit,
    onToggleLike: (GroupMemory) -> Unit,
    onDeleteMemory: (String) -> Unit
) {
    var filter by remember { mutableStateOf<MediaType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var viewingMemory by remember { mutableStateOf<GroupMemory?>(null) }

    val memories = (memoriesState as? UiState.Success)?.data ?: emptyList()
    val filtered = remember(memories, filter) {
        if (filter == null) memories else memories.filter { it.mediaTypeEnum() == filter }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("memories_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Crew Memories",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Photos & video clips saved together forever.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filter == null,
                            onClick = { filter = null },
                            label = { Text("All (${memories.size})") },
                            modifier = Modifier.testTag("filter_memories_all")
                        )
                        FilterChip(
                            selected = filter == MediaType.PHOTO,
                            onClick = { filter = MediaType.PHOTO },
                            leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Photos") },
                            modifier = Modifier.testTag("filter_memories_photos")
                        )
                        FilterChip(
                            selected = filter == MediaType.VIDEO,
                            onClick = { filter = MediaType.VIDEO },
                            leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Videos") },
                            modifier = Modifier.testTag("filter_memories_videos")
                        )
                    }
                }
            }

            if (memoriesState is UiState.Loading) {
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
            } else if (memoriesState is UiState.Error) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = memoriesState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_memories_empty),
                                contentDescription = "Memories album illustration",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                            Text(
                                text = "Start your shared memory vault",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Tap + Add Memory to share photos or video moments from hangouts, birthdays, and trips.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { memory ->
                    MemoryCard(
                        memory = memory,
                        currentUserId = currentUserId,
                        onClick = { viewingMemory = memory },
                        onToggleLike = { onToggleLike(memory) },
                        onDelete = { onDeleteMemory(memory.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("add_memory_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add memory")
                Text("Add Memory", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            onPost = { type, caption, tag, mediaB64, thumbB64, url ->
                onPostMemory(type, caption, tag, mediaB64, thumbB64, url)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    viewingMemory?.let { memory ->
        MemoryViewerDialog(
            memory = memory,
            onDismiss = { viewingMemory = null }
        )
    }
}

@Composable
private fun MemoryCard(
    memory: GroupMemory,
    currentUserId: String,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onDelete: () -> Unit
) {
    val isVideo = memory.mediaTypeEnum() == MediaType.VIDEO
    val isLiked = currentUserId in memory.likedByUids
    val bitmap = remember(memory.id, memory.mediaBase64, memory.thumbnailBase64) {
        val b64 = if (isVideo) memory.thumbnailBase64 else memory.mediaBase64
        MediaHelper.decodeBase64ToImageBitmap(b64)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("memory_card_${memory.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when {
                    bitmap != null -> {
                        Image(
                            bitmap = bitmap,
                            contentDescription = memory.caption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    !isVideo && memory.externalUrl.isNotBlank() -> {
                        AsyncImage(
                            model = memory.externalUrl,
                            contentDescription = memory.caption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.62f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                if (memory.tag.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.62f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = memory.tag,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = memory.caption,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${memory.authorName} • ${BirthdayUtils.formatRelativeTime(memory.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleLike,
                            modifier = Modifier.testTag("like_memory_${memory.id}")
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like memory",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = memory.likedByUids.size.toString(),
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (memory.userId == currentUserId) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.testTag("delete_memory_${memory.id}")
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete memory",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMemoryDialog(
    onPost: (
        mediaType: MediaType,
        caption: String,
        tag: String,
        mediaBase64: String,
        thumbnailBase64: String,
        externalUrl: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(MediaType.PHOTO) }
    var caption by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("Hangout") }
    var externalUrl by remember { mutableStateOf("") }
    var mediaBase64 by remember { mutableStateOf("") }
    var thumbnailBase64 by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var pickerNotice by remember { mutableStateOf<String?>(null) }

    val tags = listOf("Hangout", "Birthday", "Road Trip", "Food", "Throwback")

    val visualMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            processing = true
            pickerNotice = null
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    MediaHelper.processPickedMedia(context, uri)
                }
                processing = false
                result.onSuccess { processed ->
                    selectedType = if (processed.isVideo) MediaType.VIDEO else MediaType.PHOTO
                    mediaBase64 = processed.mediaBase64
                    thumbnailBase64 = processed.thumbnailBase64
                    pickerNotice = when {
                        processed.isVideo && processed.clipTooLargeForCloud ->
                            "Video thumbnail ready! Video is over 450KB — paste an optional video link below to stream full length, or share with thumbnail."
                        processed.isVideo ->
                            "Short video clip ready for instant group playback!"
                        else ->
                            "Photo ready to share with the crew!"
                    }
                }.onFailure { err ->
                    pickerNotice = err.localizedMessage ?: "Failed to load selected media"
                }
            }
        }
    }

    val previewBitmap = remember(mediaBase64, thumbnailBase64, selectedType) {
        val b64 = if (selectedType == MediaType.VIDEO) thumbnailBase64 else mediaBase64
        MediaHelper.decodeBase64ToImageBitmap(b64)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share a Memory", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            visualMediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_photo_button")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Photo")
                    }
                    OutlinedButton(
                        onClick = {
                            visualMediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_video_button")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Video")
                    }
                }

                if (processing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Preparing memory…", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Selected memory preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                if (pickerNotice != null) {
                    Text(
                        text = pickerNotice!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 240) caption = it },
                    label = { Text("Caption") },
                    placeholder = { Text("What happened in this moment?") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_caption_input")
                )

                Text("Tag", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { t ->
                        FilterChip(
                            selected = tag == t,
                            onClick = { tag = t },
                            label = { Text(t) }
                        )
                    }
                }

                OutlinedTextField(
                    value = externalUrl,
                    onValueChange = { if (it.length <= 500) externalUrl = it },
                    label = { Text("Optional Photo / Video URL") },
                    placeholder = { Text("https://… (for HD videos)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_url_input")
                )

                if (externalUrl.isNotBlank() && mediaBase64.isBlank() && thumbnailBase64.isBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == MediaType.PHOTO,
                            onClick = { selectedType = MediaType.PHOTO },
                            label = { Text("Photo Link") }
                        )
                        FilterChip(
                            selected = selectedType == MediaType.VIDEO,
                            onClick = { selectedType = MediaType.VIDEO },
                            label = { Text("Video Link") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val hasMedia = mediaBase64.isNotBlank() || thumbnailBase64.isNotBlank() || externalUrl.isNotBlank()
            Button(
                onClick = {
                    onPost(selectedType, caption, tag, mediaBase64, thumbnailBase64, externalUrl)
                },
                enabled = !processing && caption.isNotBlank() && hasMedia,
                modifier = Modifier.testTag("confirm_post_memory_button")
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MemoryViewerDialog(
    memory: GroupMemory,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isVideo = memory.mediaTypeEnum() == MediaType.VIDEO
    val photoBitmap = remember(memory.id, memory.mediaBase64, memory.thumbnailBase64) {
        val b64 = if (isVideo) memory.thumbnailBase64 else memory.mediaBase64
        MediaHelper.decodeBase64ToImageBitmap(b64)
    }
    val playableVideoUri = remember(memory.id, memory.mediaBase64, memory.externalUrl) {
        when {
            isVideo && memory.mediaBase64.isNotBlank() ->
                MediaHelper.writeBase64VideoToTempFile(context, memory.id, memory.mediaBase64)
            isVideo && memory.externalUrl.endsWith(".mp4", ignoreCase = true) ->
                Uri.parse(memory.externalUrl)
            else -> null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)
        Surface(
            color = Color.Black,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        playableVideoUri != null -> {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        val controller = MediaController(ctx)
                                        controller.setAnchorView(this)
                                        setMediaController(controller)
                                        setVideoURI(playableVideoUri)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                            )
                        }
                        photoBitmap != null -> {
                            Image(
                                bitmap = photoBitmap,
                                contentDescription = memory.caption,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        memory.externalUrl.isNotBlank() -> {
                            AsyncImage(
                                model = memory.externalUrl,
                                contentDescription = memory.caption,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .testTag("close_memory_viewer_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close viewer", tint = Color.White)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = memory.caption,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Shared by ${memory.authorName} • ${BirthdayUtils.formatRelativeTime(memory.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    if (isVideo && memory.externalUrl.isNotBlank() && playableVideoUri == null) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(memory.externalUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.testTag("open_external_video_button")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Video Link")
                        }
                    }
                }
            }
        }
    }
}
