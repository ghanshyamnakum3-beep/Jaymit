package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CrewRepository
import com.example.model.AvailabilityStatus
import com.example.model.BirthdayWish
import com.example.model.FriendGroup
import com.example.model.GameRoom
import com.example.model.GameStatus
import com.example.model.GameType
import com.example.model.GroupMember
import com.example.model.GroupMemory
import com.example.model.MediaType
import com.example.model.RpsMove
import com.example.model.StatusCategory
import com.example.model.StatusPost
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.util.BirthdayUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CrewViewModel(
    private val repository: CrewRepository,
    val currentUserId: String,
    val initialDisplayName: String,
    val initialEmail: String?
) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    private val _activeGameRoomId = MutableStateFlow<String?>(null)
    val activeGameRoomId: StateFlow<String?> = _activeGameRoomId.asStateFlow()

    private val _actionLoading = MutableStateFlow(false)
    val actionLoading: StateFlow<Boolean> = _actionLoading.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapAndLog("profile", error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val groupsState: StateFlow<UiState<List<FriendGroup>>> = repository.observeMyGroups(currentUserId)
        .toUiState("groups")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activeGroup: StateFlow<FriendGroup?> = combine(
        groupsState,
        userProfile,
        _selectedGroupId
    ) { groupsUi, profile, manualGroupId ->
        val groups = (groupsUi as? UiState.Success)?.data ?: emptyList()
        val chosenId = manualGroupId ?: profile?.activeGroupId?.takeIf { it.isNotBlank() }
        groups.firstOrNull { it.id == chosenId } ?: groups.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    private val activeGroupKeyFlow: Flow<FriendGroup?> = activeGroup
        .distinctUntilChanged { old, new ->
            old?.id == new?.id && old?.memberUids == new?.memberUids
        }

    val membersState: StateFlow<UiState<List<GroupMember>>> = activeGroupKeyFlow
        .flatMapLatest { group ->
            if (group == null) flowOf(UiState.Success(emptyList()))
            else repository.observeMembers(group.id, currentUserId).toUiState("members")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val wishesState: StateFlow<UiState<List<BirthdayWish>>> = activeGroupKeyFlow
        .flatMapLatest { group ->
            if (group == null) flowOf(UiState.Success(emptyList()))
            else repository.observeWishes(group.id, currentUserId).toUiState("wishes")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val statusesState: StateFlow<UiState<List<StatusPost>>> = activeGroupKeyFlow
        .flatMapLatest { group ->
            if (group == null) flowOf(UiState.Success(emptyList()))
            else repository.observeStatuses(group.id, currentUserId).toUiState("statuses")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val memoriesState: StateFlow<UiState<List<GroupMemory>>> = activeGroupKeyFlow
        .flatMapLatest { group ->
            if (group == null) flowOf(UiState.Success(emptyList()))
            else repository.observeMemories(group.id, currentUserId).toUiState("memories")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val gamesState: StateFlow<UiState<List<GameRoom>>> = activeGroupKeyFlow
        .flatMapLatest { group ->
            if (group == null) flowOf(UiState.Success(emptyList()))
            else repository.observeGames(group.id, currentUserId).toUiState("games")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(initialDisplayName, initialEmail)
        }
        viewModelScope.launch {
            activeGroup.collect { group ->
                if (group != null) {
                    val profile = userProfile.value
                    val name = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
                    val avatar = profile?.avatarIndex ?: 0L
                    val bMonth = profile?.birthdayMonth ?: 0L
                    val bDay = profile?.birthdayDay ?: 0L
                    repository.ensureMemberDocExists(group, name, avatar, bMonth, bDay)
                    repository.syncMemberUidsAcrossGroup(group)
                }
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun selectGroup(groupId: String) {
        _selectedGroupId.value = groupId
        _activeGameRoomId.value = null
        viewModelScope.launch {
            repository.setActiveGroupId(groupId)
        }
    }

    fun openGameRoom(gameId: String?) {
        _activeGameRoomId.value = gameId
    }

    fun createGroup(name: String, birthdayMonth: Long, birthdayDay: Long) {
        viewModelScope.launch {
            _actionLoading.value = true
            val profile = userProfile.value
            val displayName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
            val avatar = profile?.avatarIndex ?: 0L
            val result = repository.createGroup(name, displayName, avatar, birthdayMonth, birthdayDay)
            _actionLoading.value = false
            result.onSuccess { created ->
                _selectedGroupId.value = created.id
                _feedbackMessage.value = "Group \"${created.name}\" created! Invite code: ${created.inviteCode}"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to create group"
            }
        }
    }

    fun joinGroup(inviteCode: String, birthdayMonth: Long, birthdayDay: Long) {
        viewModelScope.launch {
            _actionLoading.value = true
            val profile = userProfile.value
            val displayName = profile?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
            val avatar = profile?.avatarIndex ?: 0L
            val result = repository.joinGroupByCode(inviteCode, displayName, avatar, birthdayMonth, birthdayDay)
            _actionLoading.value = false
            result.onSuccess { joined ->
                _selectedGroupId.value = joined.id
                _feedbackMessage.value = "Joined \"${joined.name}\"!"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not join group"
            }
        }
    }

    fun updateMyBirthdayAndProfile(
        displayName: String,
        birthdayMonth: Long,
        birthdayDay: Long,
        avatarIndex: Long
    ) {
        val groupId = activeGroup.value?.id.orEmpty()
        viewModelScope.launch {
            _actionLoading.value = true
            val result = repository.updateMyBirthdayAndProfile(
                groupId = groupId,
                displayName = displayName,
                birthdayMonth = birthdayMonth,
                birthdayDay = birthdayDay,
                avatarIndex = avatarIndex
            )
            _actionLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Profile & birthday saved!"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun sendBirthdayWish(
        targetMember: GroupMember,
        message: String,
        giftEmoji: String
    ) {
        val group = activeGroup.value ?: return
        val senderName = myDisplayName()
        viewModelScope.launch {
            _actionLoading.value = true
            val result = repository.sendBirthdayWish(
                group = group,
                targetUserId = targetMember.userId,
                targetName = targetMember.displayName,
                senderName = senderName,
                message = message,
                giftEmoji = giftEmoji,
                year = BirthdayUtils.currentYear()
            )
            _actionLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Birthday wish sent to ${targetMember.displayName}!"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to send wish"
            }
        }
    }

    fun deleteWish(wishId: String) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            repository.deleteWish(group.id, wishId).onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to delete wish"
            }
        }
    }

    fun postLiveStatus(
        category: StatusCategory,
        message: String,
        placeName: String,
        availability: AvailabilityStatus
    ) {
        val group = activeGroup.value ?: return
        val avatar = userProfile.value?.avatarIndex ?: 0L
        viewModelScope.launch {
            _actionLoading.value = true
            val result = repository.updateLiveStatus(
                group = group,
                authorName = myDisplayName(),
                avatarIndex = avatar,
                category = category,
                message = message,
                placeName = placeName,
                availability = availability
            )
            _actionLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Live status updated!"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to update status"
            }
        }
    }

    fun toggleStatusSupport(post: StatusPost) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            repository.toggleStatusSupport(group.id, post.id, post.supportUids).onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not send support"
            }
        }
    }

    fun deleteStatusPost(statusId: String) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            repository.deleteStatusPost(group.id, statusId).onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not delete status"
            }
        }
    }

    fun postMemory(
        mediaType: MediaType,
        caption: String,
        tag: String,
        mediaBase64: String,
        thumbnailBase64: String,
        externalUrl: String
    ) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            _actionLoading.value = true
            val result = repository.postMemory(
                group = group,
                authorName = myDisplayName(),
                mediaType = mediaType,
                caption = caption,
                tag = tag,
                mediaBase64 = mediaBase64,
                thumbnailBase64 = thumbnailBase64,
                externalUrl = externalUrl
            )
            _actionLoading.value = false
            result.onSuccess {
                _feedbackMessage.value = "Memory shared with the crew!"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to share memory"
            }
        }
    }

    fun toggleMemoryLike(memory: GroupMemory) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            repository.toggleMemoryLike(group.id, memory.id, memory.likedByUids).onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not update reaction"
            }
        }
    }

    fun deleteMemory(memoryId: String) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            repository.deleteMemory(group.id, memoryId).onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not delete memory"
            }
        }
    }

    fun createGameRoom(gameType: GameType) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            _actionLoading.value = true
            val result = repository.createGameRoom(group, gameType, myDisplayName())
            _actionLoading.value = false
            result.onSuccess { gameId ->
                _activeGameRoomId.value = gameId
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not create game"
            }
        }
    }

    fun joinGameRoom(room: GameRoom) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            _actionLoading.value = true
            val result = repository.joinGameRoom(group.id, room.id, myDisplayName(), room.hostUid)
            _actionLoading.value = false
            result.onSuccess {
                _activeGameRoomId.value = room.id
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not join game"
            }
        }
    }

    fun deleteGameRoom(gameId: String) {
        val group = activeGroup.value ?: return
        viewModelScope.launch {
            if (_activeGameRoomId.value == gameId) _activeGameRoomId.value = null
            repository.deleteGameRoom(group.id, gameId).onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Could not close room"
            }
        }
    }

    fun makeTicTacToeMove(room: GameRoom, cellIndex: Int) {
        if (room.statusEnum() != GameStatus.PLAYING) return
        if (room.currentTurnUid != currentUserId) return
        val board = room.board.toMutableList()
        if (board.size != 9 || board[cellIndex].isNotEmpty()) return

        val symbol = if (currentUserId == room.hostUid) "X" else "O"
        board[cellIndex] = symbol

        val winLines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        val won = winLines.any { (a, b, c) ->
            board[a] == symbol && board[b] == symbol && board[c] == symbol
        }
        val draw = !won && board.none { it.isEmpty() }

        val nextStatus = if (won || draw) GameStatus.FINISHED.name else GameStatus.PLAYING.name
        val winner = when {
            won -> currentUserId
            draw -> "DRAW"
            else -> ""
        }
        val nextTurn = when {
            won || draw -> ""
            currentUserId == room.hostUid -> room.guestUid
            else -> room.hostUid
        }

        viewModelScope.launch {
            repository.updateGameMove(
                groupId = room.groupId,
                gameId = room.id,
                fields = mapOf(
                    "board" to board,
                    "status" to nextStatus,
                    "winnerUid" to winner,
                    "currentTurnUid" to nextTurn
                )
            )
        }
    }

    fun makeConnectFourMove(room: GameRoom, column: Int) {
        if (room.statusEnum() != GameStatus.PLAYING) return
        if (room.currentTurnUid != currentUserId) return
        if (column !in 0..6 || room.board.size != 42) return

        val board = room.board.toMutableList()
        var targetRow = -1
        for (row in 5 downTo 0) {
            if (board[row * 7 + column].isEmpty()) {
                targetRow = row
                break
            }
        }
        if (targetRow == -1) return

        val disc = if (currentUserId == room.hostUid) "R" else "Y"
        board[targetRow * 7 + column] = disc

        val won = checkConnectFourWin(board, disc)
        val draw = !won && board.none { it.isEmpty() }

        val nextStatus = if (won || draw) GameStatus.FINISHED.name else GameStatus.PLAYING.name
        val winner = when {
            won -> currentUserId
            draw -> "DRAW"
            else -> ""
        }
        val nextTurn = when {
            won || draw -> ""
            currentUserId == room.hostUid -> room.guestUid
            else -> room.hostUid
        }

        viewModelScope.launch {
            repository.updateGameMove(
                groupId = room.groupId,
                gameId = room.id,
                fields = mapOf(
                    "board" to board,
                    "status" to nextStatus,
                    "winnerUid" to winner,
                    "currentTurnUid" to nextTurn
                )
            )
        }
    }

    fun makeRpsMove(room: GameRoom, move: RpsMove) {
        if (room.statusEnum() != GameStatus.PLAYING) return
        if (move == RpsMove.NONE) return
        val isHost = currentUserId == room.hostUid
        val isGuest = currentUserId == room.guestUid
        if (!isHost && !isGuest) return

        if (isHost && room.rpsHostMove.isNotEmpty()) return
        if (isGuest && room.rpsGuestMove.isNotEmpty()) return

        val hostMoveStr = if (isHost) move.name else room.rpsHostMove
        val guestMoveStr = if (isGuest) move.name else room.rpsGuestMove

        val updates = mutableMapOf<String, Any>()
        if (hostMoveStr.isNotEmpty() && guestMoveStr.isNotEmpty()) {
            val h = RpsMove.fromRaw(hostMoveStr)
            val g = RpsMove.fromRaw(guestMoveStr)
            val roundOutcome = resolveRpsRound(h, g)
            val newHostScore = room.rpsHostScore + if (roundOutcome > 0) 1L else 0L
            val newGuestScore = room.rpsGuestScore + if (roundOutcome < 0) 1L else 0L
            val summary = when {
                roundOutcome > 0 -> "${room.hostName}'s ${h.emoji} beat ${room.guestName}'s ${g.emoji}!"
                roundOutcome < 0 -> "${room.guestName}'s ${g.emoji} beat ${room.hostName}'s ${h.emoji}!"
                else -> "Tie round! Both picked ${h.emoji}"
            }
            val matchOver = newHostScore >= 2L || newGuestScore >= 2L || room.rpsRound >= 5L
            val nextStatus = if (matchOver) GameStatus.FINISHED.name else GameStatus.PLAYING.name
            val winner = when {
                !matchOver -> ""
                newHostScore > newGuestScore -> room.hostUid
                newGuestScore > newHostScore -> room.guestUid
                else -> "DRAW"
            }
            updates["rpsHostMove"] = if (matchOver) hostMoveStr else ""
            updates["rpsGuestMove"] = if (matchOver) guestMoveStr else ""
            updates["rpsHostScore"] = newHostScore
            updates["rpsGuestScore"] = newGuestScore
            updates["rpsRound"] = if (matchOver) room.rpsRound else (room.rpsRound + 1L).coerceAtMost(10L)
            updates["rpsSummary"] = summary.take(120)
            updates["status"] = nextStatus
            updates["winnerUid"] = winner
        } else {
            if (isHost) {
                updates["rpsHostMove"] = move.name
            } else {
                updates["rpsGuestMove"] = move.name
            }
        }

        viewModelScope.launch {
            repository.updateGameMove(room.groupId, room.id, updates)
        }
    }

    private fun resolveRpsRound(host: RpsMove, guest: RpsMove): Int {
        if (host == guest) return 0
        return when (host) {
            RpsMove.ROCK -> if (guest == RpsMove.SCISSORS) 1 else -1
            RpsMove.PAPER -> if (guest == RpsMove.ROCK) 1 else -1
            RpsMove.SCISSORS -> if (guest == RpsMove.PAPER) 1 else -1
            RpsMove.NONE -> 0
        }
    }

    private fun checkConnectFourWin(board: List<String>, disc: String): Boolean {
        val rows = 6
        val cols = 7
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (board[r * cols + c] != disc) continue
                // Horizontal
                if (c + 3 < cols &&
                    board[r * cols + c + 1] == disc &&
                    board[r * cols + c + 2] == disc &&
                    board[r * cols + c + 3] == disc
                ) return true
                // Vertical
                if (r + 3 < rows &&
                    board[(r + 1) * cols + c] == disc &&
                    board[(r + 2) * cols + c] == disc &&
                    board[(r + 3) * cols + c] == disc
                ) return true
                // Diagonal down-right
                if (r + 3 < rows && c + 3 < cols &&
                    board[(r + 1) * cols + c + 1] == disc &&
                    board[(r + 2) * cols + c + 2] == disc &&
                    board[(r + 3) * cols + c + 3] == disc
                ) return true
                // Diagonal up-right
                if (r - 3 >= 0 && c + 3 < cols &&
                    board[(r - 1) * cols + c + 1] == disc &&
                    board[(r - 2) * cols + c + 2] == disc &&
                    board[(r - 3) * cols + c + 3] == disc
                ) return true
            }
        }
        return false
    }

    fun myDisplayName(): String =
        userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName

    private fun <T> Flow<T>.toUiState(streamName: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapAndLog(streamName, error)
                val rootError = firestoreError ?: error
                emit(UiState.Error(rootError.message ?: "Failed to load $streamName"))
            }

    private fun unwrapAndLog(streamName: String, error: Throwable): FirebaseFirestoreException? {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) {
            throw error
        }
        Log.w(TAG, "Error observing $streamName", firestoreError ?: error)
        return firestoreError
    }

    private companion object {
        const val TAG = "CrewViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
