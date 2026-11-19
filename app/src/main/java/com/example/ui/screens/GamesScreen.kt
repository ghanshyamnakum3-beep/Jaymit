package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GameRoom
import com.example.model.GameStatus
import com.example.model.GameType
import com.example.model.RpsMove
import com.example.model.UiState
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MarigoldTertiary
import com.example.ui.theme.PlumSecondary
import com.example.ui.theme.SageMint
import com.example.util.BirthdayUtils

@Composable
fun GamesScreen(
    currentUserId: String,
    gamesState: UiState<List<GameRoom>>,
    activeGameRoomId: String?,
    onOpenRoom: (String?) -> Unit,
    onCreateRoom: (GameType) -> Unit,
    onJoinRoom: (GameRoom) -> Unit,
    onTicTacToeMove: (GameRoom, Int) -> Unit,
    onConnectFourMove: (GameRoom, Int) -> Unit,
    onRpsMove: (GameRoom, RpsMove) -> Unit,
    onDeleteRoom: (String) -> Unit
) {
    val rooms = (gamesState as? UiState.Success)?.data ?: emptyList()
    val activeRoom = activeGameRoomId?.let { id -> rooms.firstOrNull { it.id == id } }

    if (activeRoom != null) {
        BackHandler { onOpenRoom(null) }
        ActiveGameArena(
            room = activeRoom,
            currentUserId = currentUserId,
            onBack = { onOpenRoom(null) },
            onJoin = { onJoinRoom(activeRoom) },
            onTicTacToeMove = { idx -> onTicTacToeMove(activeRoom, idx) },
            onConnectFourMove = { col -> onConnectFourMove(activeRoom, col) },
            onRpsMove = { move -> onRpsMove(activeRoom, move) }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("games_lobby_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_games_banner),
                    contentDescription = "Live multiplayer games banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xD91E1532), Color(0x883E2F5B), Color(0x22000000))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Crew Live Arcade",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Challenge friends in real time — every move syncs live!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        item {
            Text(
                text = "Start a Live Match",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GameLauncherCard(
                    title = "Tic-Tac-Toe",
                    subtitle = "3×3 Quick",
                    icon = Icons.Default.Grid3x3,
                    accent = CoralPrimary,
                    onClick = { onCreateRoom(GameType.TIC_TAC_TOE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_ttt_button")
                )
                GameLauncherCard(
                    title = "4-in-a-Row",
                    subtitle = "Connect 4",
                    icon = Icons.Default.ViewModule,
                    accent = MarigoldTertiary,
                    onClick = { onCreateRoom(GameType.CONNECT_FOUR) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_c4_button")
                )
                GameLauncherCard(
                    title = "RPS Duel",
                    subtitle = "Best of 3",
                    icon = Icons.Default.Casino,
                    accent = PlumSecondary,
                    onClick = { onCreateRoom(GameType.RPS_DUEL) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_rps_button")
                )
            }
        }

        item {
            Text(
                text = "Group Match Rooms (${rooms.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (gamesState is UiState.Loading) {
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
        } else if (gamesState is UiState.Error) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = gamesState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else if (rooms.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                        Text("No active game rooms", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Tap any game above to open a room — friends in your group can join instantly!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(rooms, key = { it.id }) { room ->
                GameRoomCard(
                    room = room,
                    currentUserId = currentUserId,
                    onOpen = { onOpenRoom(room.id) },
                    onJoin = { onJoinRoom(room) },
                    onDelete = { onDeleteRoom(room.id) }
                )
            }
        }
    }
}

@Composable
private fun GameLauncherCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GameRoomCard(
    room: GameRoom,
    currentUserId: String,
    onOpen: () -> Unit,
    onJoin: () -> Unit,
    onDelete: () -> Unit
) {
    val status = room.statusEnum()
    val canJoin = status == GameStatus.WAITING && room.hostUid != currentUserId
    val isParticipant = currentUserId == room.hostUid || currentUserId == room.guestUid

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_room_card_${room.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = room.gameTypeEnum().label,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = when (status) {
                            GameStatus.WAITING -> MaterialTheme.colorScheme.tertiaryContainer
                            GameStatus.PLAYING -> SageMint.copy(alpha = 0.18f)
                            GameStatus.FINISHED -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (status) {
                                GameStatus.PLAYING -> SageMint
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = if (room.guestName.isNotBlank()) {
                        "${room.hostName} vs ${room.guestName}"
                    } else {
                        "Hosted by ${room.hostName} • Waiting for opponent"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = BirthdayUtils.formatRelativeTime(room.updatedAt ?: room.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canJoin) {
                    Button(
                        onClick = onJoin,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("join_game_${room.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Join")
                    }
                } else {
                    FilledTonalButton(
                        onClick = onOpen,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("open_game_${room.id}")
                    ) {
                        Text(if (isParticipant) "Play" else "Watch")
                    }
                }
                if (room.hostUid == currentUserId) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_game_${room.id}")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Close room")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveGameArena(
    room: GameRoom,
    currentUserId: String,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    onTicTacToeMove: (Int) -> Unit,
    onConnectFourMove: (Int) -> Unit,
    onRpsMove: (RpsMove) -> Unit
) {
    val status = room.statusEnum()
    val isHost = currentUserId == room.hostUid
    val isGuest = currentUserId == room.guestUid
    val isMyTurn = room.currentTurnUid == currentUserId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("game_arena_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to lobby")
            }
            Text(
                text = room.gameTypeEnum().label,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(48.dp))
        }

        // Players & Match Status Banner
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${room.hostName}  vs  ${room.guestName.ifBlank { "Waiting…" }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val bannerText = when {
                    status == GameStatus.WAITING && isHost -> "Share this room with your crew — waiting for a friend to join!"
                    status == GameStatus.WAITING -> "Seat open! Tap Join Match to play live."
                    status == GameStatus.FINISHED -> when (room.winnerUid) {
                        "DRAW" -> "It's a Draw! Well played!"
                        room.hostUid -> "${room.hostName} Wins! 🏆"
                        room.guestUid -> "${room.guestName} Wins! 🏆"
                        else -> "Match Finished!"
                    }
                    room.gameTypeEnum() == GameType.RPS_DUEL -> "Round ${room.rpsRound} • Pick secretly!"
                    isMyTurn -> "Your Turn!"
                    else -> "Waiting for ${if (room.currentTurnUid == room.hostUid) room.hostName else room.guestName}…"
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when {
                        status == GameStatus.FINISHED -> MaterialTheme.colorScheme.tertiaryContainer
                        isMyTurn && status == GameStatus.PLAYING -> SageMint.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (status == GameStatus.FINISHED) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = bannerText,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (status == GameStatus.WAITING && !isHost && !isGuest) {
                    Button(
                        onClick = onJoin,
                        modifier = Modifier.testTag("arena_join_match_button")
                    ) {
                        Text("Join Match")
                    }
                }
            }
        }

        when (room.gameTypeEnum()) {
            GameType.TIC_TAC_TOE -> TicTacToeBoard(
                board = room.board,
                canPlay = status == GameStatus.PLAYING && isMyTurn && (isHost || isGuest),
                onCellClick = onTicTacToeMove
            )
            GameType.CONNECT_FOUR -> ConnectFourBoard(
                board = room.board,
                canPlay = status == GameStatus.PLAYING && isMyTurn && (isHost || isGuest),
                onColumnClick = onConnectFourMove
            )
            GameType.RPS_DUEL -> RpsArena(
                room = room,
                currentUserId = currentUserId,
                onPickMove = onRpsMove
            )
        }
    }
}

@Composable
private fun TicTacToeBoard(
    board: List<String>,
    canPlay: Boolean,
    onCellClick: (Int) -> Unit
) {
    val cells = if (board.size == 9) board else List(9) { "" }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (col in 0..2) {
                    val idx = row * 3 + col
                    val value = cells[idx]
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = canPlay && value.isEmpty()) { onCellClick(idx) }
                            .testTag("ttt_cell_$idx")
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (value == "X") CoralPrimary else PlumSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectFourBoard(
    board: List<String>,
    canPlay: Boolean,
    onColumnClick: (Int) -> Unit
) {
    val cells = if (board.size == 42) board else List(42) { "" }
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PlumSecondary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (r in 0 until 6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (c in 0 until 7) {
                        val value = cells[r * 7 + c]
                        val colOpen = cells[c].isEmpty()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    when (value) {
                                        "R" -> CoralPrimary
                                        "Y" -> MarigoldTertiary
                                        else -> Color.White.copy(alpha = 0.2f)
                                    }
                                )
                                .clickable(enabled = canPlay && colOpen) { onColumnClick(c) }
                                .testTag("c4_cell_${r}_$c")
                        )
                    }
                }
            }
        }
    }
    if (canPlay) {
        Text(
            text = "Tap any column to drop your disc!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RpsArena(
    room: GameRoom,
    currentUserId: String,
    onPickMove: (RpsMove) -> Unit
) {
    val isHost = currentUserId == room.hostUid
    val isGuest = currentUserId == room.guestUid
    val myMoveRaw = if (isHost) room.rpsHostMove else if (isGuest) room.rpsGuestMove else ""
    val opponentPicked = if (isHost) room.rpsGuestMove.isNotEmpty() else room.rpsHostMove.isNotEmpty()
    val status = room.statusEnum()

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(room.hostName, style = MaterialTheme.typography.labelLarge)
                    Text("${room.rpsHostScore}", style = MaterialTheme.typography.displaySmall, color = CoralPrimary)
                }
                Text("VS", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterVertically))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(room.guestName.ifBlank { "Friend" }, style = MaterialTheme.typography.labelLarge)
                    Text("${room.rpsGuestScore}", style = MaterialTheme.typography.displaySmall, color = PlumSecondary)
                }
            }

            if (room.rpsSummary.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = room.rpsSummary,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (status == GameStatus.PLAYING && (isHost || isGuest)) {
                if (myMoveRaw.isNotEmpty()) {
                    val chosen = RpsMove.fromRaw(myMoveRaw)
                    Text(
                        text = "You locked in ${chosen.emoji} ${chosen.label}. ${if (opponentPicked) "Revealing…" else "Waiting for opponent…"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(RpsMove.ROCK, RpsMove.PAPER, RpsMove.SCISSORS).forEach { move ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                                    .clickable { onPickMove(move) }
                                    .testTag("rps_pick_${move.name}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(move.emoji, style = MaterialTheme.typography.headlineMedium)
                                    Text(move.label, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
