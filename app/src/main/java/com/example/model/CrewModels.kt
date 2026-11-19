package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class StatusCategory(val label: String, val emoji: String) {
    GENERAL("Update", "✨"),
    HOSPITAL("Hospital", "🏥"),
    WORK("Job / Work", "💼"),
    STUDY("Studying", "📚"),
    CHILL("Free / Chill", "☕"),
    TRAVEL("On the Go", "🚗");

    companion object {
        fun fromRaw(raw: String?): StatusCategory =
            entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

enum class AvailabilityStatus(val label: String) {
    AVAILABLE("Available"),
    BUSY("Busy"),
    NEED_SUPPORT("Need Support"),
    OFFLINE("Offline");

    companion object {
        fun fromRaw(raw: String?): AvailabilityStatus =
            entries.firstOrNull { it.name == raw } ?: AVAILABLE
    }
}

enum class MediaType(val label: String) {
    PHOTO("Photo"),
    VIDEO("Video");

    companion object {
        fun fromRaw(raw: String?): MediaType =
            entries.firstOrNull { it.name == raw } ?: PHOTO
    }
}

enum class GameType(val label: String, val subtitle: String) {
    TIC_TAC_TOE("Tic-Tac-Toe", "Classic 3×3 quick duel"),
    CONNECT_FOUR("4-in-a-Row", "Drop discs & connect 4"),
    RPS_DUEL("RPS Showdown", "Best of 3 secret picks");

    companion object {
        fun fromRaw(raw: String?): GameType =
            entries.firstOrNull { it.name == raw } ?: TIC_TAC_TOE
    }
}

enum class GameStatus(val label: String) {
    WAITING("Waiting for friend"),
    PLAYING("Playing live"),
    FINISHED("Finished");

    companion object {
        fun fromRaw(raw: String?): GameStatus =
            entries.firstOrNull { it.name == raw } ?: WAITING
    }
}

enum class RpsMove(val label: String, val emoji: String) {
    NONE("", ""),
    ROCK("Rock", "🪨"),
    PAPER("Paper", "📄"),
    SCISSORS("Scissors", "✂️");

    companion object {
        fun fromRaw(raw: String?): RpsMove =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val birthdayMonth: Long = 0L,
    val birthdayDay: Long = 0L,
    val avatarIndex: Long = 0L,
    val activeGroupId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val creatorUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val groupId: String = "",
    val memberUids: List<String> = emptyList(),
    val displayName: String = "",
    val avatarIndex: Long = 0L,
    val birthdayMonth: Long = 0L,
    val birthdayDay: Long = 0L,
    val statusCategory: String = StatusCategory.GENERAL.name,
    val statusText: String = "",
    val statusPlace: String = "",
    val availability: String = AvailabilityStatus.AVAILABLE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): StatusCategory = StatusCategory.fromRaw(statusCategory)
    fun availabilityEnum(): AvailabilityStatus = AvailabilityStatus.fromRaw(availability)
}

@Keep
@IgnoreExtraProperties
data class BirthdayWish(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val targetUserId: String = "",
    val targetName: String = "",
    val senderUserId: String = "",
    val senderName: String = "",
    val message: String = "",
    val giftEmoji: String = "🎂",
    val year: Long = 2026L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StatusPost(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val avatarIndex: Long = 0L,
    val category: String = StatusCategory.GENERAL.name,
    val message: String = "",
    val placeName: String = "",
    val availability: String = AvailabilityStatus.AVAILABLE.name,
    val supportUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): StatusCategory = StatusCategory.fromRaw(category)
    fun availabilityEnum(): AvailabilityStatus = AvailabilityStatus.fromRaw(availability)
}

@Keep
@IgnoreExtraProperties
data class GroupMemory(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val mediaType: String = MediaType.PHOTO.name,
    val caption: String = "",
    val tag: String = "",
    val mediaBase64: String = "",
    val thumbnailBase64: String = "",
    val externalUrl: String = "",
    val likedByUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun mediaTypeEnum(): MediaType = MediaType.fromRaw(mediaType)
}

@Keep
@IgnoreExtraProperties
data class GameRoom(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val gameType: String = GameType.TIC_TAC_TOE.name,
    val status: String = GameStatus.WAITING.name,
    val hostUid: String = "",
    val hostName: String = "",
    val guestUid: String = "",
    val guestName: String = "",
    val currentTurnUid: String = "",
    val winnerUid: String = "",
    val board: List<String> = emptyList(),
    val rpsRound: Long = 1L,
    val rpsHostMove: String = "",
    val rpsGuestMove: String = "",
    val rpsHostScore: Long = 0L,
    val rpsGuestScore: Long = 0L,
    val rpsSummary: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun gameTypeEnum(): GameType = GameType.fromRaw(gameType)
    fun statusEnum(): GameStatus = GameStatus.fromRaw(status)
}
