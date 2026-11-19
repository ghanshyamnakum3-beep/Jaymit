package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AvailabilityStatus
import com.example.model.BirthdayWish
import com.example.model.FriendGroup
import com.example.model.GameRoom
import com.example.model.GameStatus
import com.example.model.GameType
import com.example.model.GroupMember
import com.example.model.GroupMemory
import com.example.model.MediaType
import com.example.model.StatusCategory
import com.example.model.StatusPost
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class CrewRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
        )
    }

    fun observeMyGroups(userId: String): Flow<List<FriendGroup>> = flow {
        requireUserId()
        emitAll(
            db.collection("groups")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    fun observeMembers(groupId: String, userId: String): Flow<List<GroupMember>> = flow {
        requireUserId()
        emitAll(
            db.collection("groups").document(groupId).collection("members")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
        )
    }

    fun observeWishes(groupId: String, userId: String): Flow<List<BirthdayWish>> = flow {
        requireUserId()
        emitAll(
            db.collection("groups").document(groupId).collection("wishes")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BirthdayWish::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
        )
    }

    fun observeStatuses(groupId: String, userId: String): Flow<List<StatusPost>> = flow {
        requireUserId()
        emitAll(
            db.collection("groups").document(groupId).collection("statuses")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StatusPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
        )
    }

    fun observeMemories(groupId: String, userId: String): Flow<List<GroupMemory>> = flow {
        requireUserId()
        emitAll(
            db.collection("groups").document(groupId).collection("memories")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupMemory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
        )
    }

    fun observeGames(groupId: String, userId: String): Flow<List<GameRoom>> = flow {
        requireUserId()
        emitAll(
            db.collection("groups").document(groupId).collection("games")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GameRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: Long.MAX_VALUE }
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String?
    ): Result<UserProfile> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "users/$uid"
        return try {
            val authUid = requireUserId()
            val ref = db.collection("users").document(authUid)
            val snap = ref.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = authUid, userId = authUid, displayName = displayName)
                Result.success(existing)
            } else {
                val cleanName = displayName.trim().ifBlank { "Friend" }.take(50)
                val avatarIdx = (authUid.hashCode().let { if (it < 0) -it else it } % 8).toLong()
                val payload = mapOf(
                    "userId" to authUid,
                    "displayName" to cleanName,
                    "email" to (email?.trim()?.take(120) ?: ""),
                    "birthdayMonth" to 0L,
                    "birthdayDay" to 0L,
                    "avatarIndex" to avatarIdx,
                    "activeGroupId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.set(payload).await()
                val created = ref.get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = authUid, userId = authUid, displayName = cleanName, avatarIndex = avatarIdx)
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setActiveGroupId(groupId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "users/$uid"
        return try {
            val authUid = requireUserId()
            db.collection("users").document(authUid)
                .update(
                    mapOf(
                        "activeGroupId" to groupId.take(128),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createGroup(
        groupName: String,
        memberDisplayName: String,
        avatarIndex: Long,
        birthdayMonth: Long,
        birthdayDay: Long
    ): Result<FriendGroup> {
        var docPath = "groups"
        return try {
            val uid = requireUserId()
            val cleanGroupName = groupName.trim().take(50)
            require(cleanGroupName.isNotEmpty()) { "Group name cannot be empty" }
            val cleanName = memberDisplayName.trim().ifBlank { "Friend" }.take(50)

            val groupRef = db.collection("groups").document()
            docPath = groupRef.path
            val code = generateInviteCode()
            val codeRef = db.collection("inviteCodes").document(code)

            val batch = db.batch()
            batch.set(
                groupRef,
                mapOf(
                    "name" to cleanGroupName,
                    "inviteCode" to code,
                    "creatorUid" to uid,
                    "memberUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                codeRef,
                mapOf(
                    "groupId" to groupRef.id,
                    "groupName" to cleanGroupName,
                    "creatorUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            val memberRef = groupRef.collection("members").document(uid)
            memberRef.set(
                mapOf(
                    "userId" to uid,
                    "groupId" to groupRef.id,
                    "memberUids" to listOf(uid),
                    "displayName" to cleanName,
                    "avatarIndex" to avatarIndex.coerceIn(0L, 7L),
                    "birthdayMonth" to birthdayMonth.coerceIn(0L, 12L),
                    "birthdayDay" to birthdayDay.coerceIn(0L, 31L),
                    "statusCategory" to StatusCategory.GENERAL.name,
                    "statusText" to "Just started ${cleanGroupName}!",
                    "statusPlace" to "",
                    "availability" to AvailabilityStatus.AVAILABLE.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("users").document(uid).update(
                mapOf(
                    "activeGroupId" to groupRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                FriendGroup(
                    id = groupRef.id,
                    name = cleanGroupName,
                    inviteCode = code,
                    creatorUid = uid,
                    memberUids = listOf(uid)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinGroupByCode(
        rawCode: String,
        memberDisplayName: String,
        avatarIndex: Long,
        birthdayMonth: Long,
        birthdayDay: Long
    ): Result<FriendGroup> {
        val code = rawCode.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
        var docPath = "inviteCodes/$code"
        return try {
            val uid = requireUserId()
            require(code.length in 4..12) { "Enter a valid invite code (4–12 characters)" }
            val cleanName = memberDisplayName.trim().ifBlank { "Friend" }.take(50)

            val codeSnap = db.collection("inviteCodes").document(code).get().await()
            if (!codeSnap.exists()) {
                return Result.failure(IllegalArgumentException("No group found for invite code $code"))
            }
            val groupId = codeSnap.getString("groupId")
                ?: return Result.failure(IllegalStateException("Invite code is missing group ID"))

            val groupRef = db.collection("groups").document(groupId)
            docPath = groupRef.path

            // Add ONLY ourselves to memberUids if not already present
            try {
                groupRef.update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (joinError: Exception) {
                // If already a member, reading the group will succeed; otherwise rethrow
                val checkSnap = groupRef.get().await()
                if (!checkSnap.exists()) throw joinError
            }

            val groupSnap = groupRef.get().await()
            val group = groupSnap.toObject(FriendGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Could not load group"))

            val memberRef = groupRef.collection("members").document(uid)
            val existingMember = memberRef.get().await()
            if (!existingMember.exists()) {
                memberRef.set(
                    mapOf(
                        "userId" to uid,
                        "groupId" to groupId,
                        "memberUids" to group.memberUids,
                        "displayName" to cleanName,
                        "avatarIndex" to avatarIndex.coerceIn(0L, 7L),
                        "birthdayMonth" to birthdayMonth.coerceIn(0L, 12L),
                        "birthdayDay" to birthdayDay.coerceIn(0L, 31L),
                        "statusCategory" to StatusCategory.GENERAL.name,
                        "statusText" to "Joined the crew!",
                        "statusPlace" to "",
                        "availability" to AvailabilityStatus.AVAILABLE.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            db.collection("users").document(uid).update(
                mapOf(
                    "activeGroupId" to groupId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(group)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun ensureMemberDocExists(
        group: FriendGroup,
        displayName: String,
        avatarIndex: Long,
        birthdayMonth: Long,
        birthdayDay: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "groups/${group.id}/members/$uid"
        return try {
            val authUid = requireUserId()
            val ref = db.collection("groups").document(group.id).collection("members").document(authUid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "userId" to authUid,
                        "groupId" to group.id,
                        "memberUids" to group.memberUids,
                        "displayName" to displayName.trim().ifBlank { "Friend" }.take(50),
                        "avatarIndex" to avatarIndex.coerceIn(0L, 7L),
                        "birthdayMonth" to birthdayMonth.coerceIn(0L, 12L),
                        "birthdayDay" to birthdayDay.coerceIn(0L, 31L),
                        "statusCategory" to StatusCategory.GENERAL.name,
                        "statusText" to "Hanging out with the crew",
                        "statusPlace" to "",
                        "availability" to AvailabilityStatus.AVAILABLE.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncMemberUidsAcrossGroup(group: FriendGroup): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        if (group.memberUids.size <= 1) return Result.success(Unit)
        return try {
            val authUid = requireUserId()
            val groupRef = db.collection("groups").document(group.id)
            val targetSet = group.memberUids.toSet()
            val collections = listOf("members", "wishes", "statuses", "memories", "games")
            val batch = db.batch()
            var hasWrites = false
            for (colName in collections) {
                val snap = groupRef.collection(colName)
                    .whereArrayContains("memberUids", authUid)
                    .get()
                    .await()
                for (doc in snap.documents) {
                    val current = (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
                    if (!current.containsAll(targetSet)) {
                        batch.update(doc.reference, "memberUids", group.memberUids)
                        hasWrites = true
                    }
                }
            }
            if (hasWrites) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "groups/${group.id}")
            Result.failure(e)
        }
    }

    suspend fun updateMyBirthdayAndProfile(
        groupId: String,
        displayName: String,
        birthdayMonth: Long,
        birthdayDay: Long,
        avatarIndex: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val memberPath = "groups/$groupId/members/$uid"
        return try {
            val authUid = requireUserId()
            val cleanName = displayName.trim().ifBlank { "Friend" }.take(50)
            val safeMonth = birthdayMonth.coerceIn(0L, 12L)
            val safeDay = birthdayDay.coerceIn(0L, 31L)
            val safeAvatar = avatarIndex.coerceIn(0L, 7L)

            db.collection("users").document(authUid).update(
                mapOf(
                    "displayName" to cleanName,
                    "birthdayMonth" to safeMonth,
                    "birthdayDay" to safeDay,
                    "avatarIndex" to safeAvatar,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (groupId.isNotBlank()) {
                db.collection("groups").document(groupId)
                    .collection("members").document(authUid)
                    .update(
                        mapOf(
                            "displayName" to cleanName,
                            "birthdayMonth" to safeMonth,
                            "birthdayDay" to safeDay,
                            "avatarIndex" to safeAvatar,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, memberPath)
            Result.failure(e)
        }
    }

    suspend fun sendBirthdayWish(
        group: FriendGroup,
        targetUserId: String,
        targetName: String,
        senderName: String,
        message: String,
        giftEmoji: String,
        year: Long
    ): Result<Unit> {
        var docPath = "groups/${group.id}/wishes"
        return try {
            val uid = requireUserId()
            val cleanMsg = message.trim().take(300)
            require(cleanMsg.isNotEmpty()) { "Wish message cannot be empty" }
            val ref = db.collection("groups").document(group.id).collection("wishes").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "groupId" to group.id,
                    "targetUserId" to targetUserId,
                    "targetName" to targetName.trim().ifBlank { "Friend" }.take(50),
                    "senderUserId" to uid,
                    "senderName" to senderName.trim().ifBlank { "Friend" }.take(50),
                    "message" to cleanMsg,
                    "giftEmoji" to giftEmoji.ifBlank { "🎂" }.take(16),
                    "year" to year.coerceIn(2020L, 2100L),
                    "memberUids" to group.memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteWish(groupId: String, wishId: String): Result<Unit> {
        val docPath = "groups/$groupId/wishes/$wishId"
        return try {
            requireUserId()
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateLiveStatus(
        group: FriendGroup,
        authorName: String,
        avatarIndex: Long,
        category: StatusCategory,
        message: String,
        placeName: String,
        availability: AvailabilityStatus
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val memberPath = "groups/${group.id}/members/$uid"
        return try {
            val authUid = requireUserId()
            val cleanMessage = message.trim().take(240)
            require(cleanMessage.isNotEmpty()) { "Status message cannot be empty" }
            val cleanPlace = placeName.trim().take(80)
            val cleanAuthor = authorName.trim().ifBlank { "Friend" }.take(50)

            // 1. Update member's current live status badge
            db.document(memberPath).update(
                mapOf(
                    "statusCategory" to category.name,
                    "statusText" to cleanMessage.take(180),
                    "statusPlace" to cleanPlace,
                    "availability" to availability.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // 2. Append to group status timeline so friends can react/support
            val postRef = db.collection("groups").document(group.id).collection("statuses").document()
            postRef.set(
                mapOf(
                    "groupId" to group.id,
                    "userId" to authUid,
                    "authorName" to cleanAuthor,
                    "avatarIndex" to avatarIndex.coerceIn(0L, 7L),
                    "category" to category.name,
                    "message" to cleanMessage,
                    "placeName" to cleanPlace,
                    "availability" to availability.name,
                    "supportUids" to emptyList<String>(),
                    "memberUids" to group.memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, memberPath)
            Result.failure(e)
        }
    }

    suspend fun toggleStatusSupport(groupId: String, statusId: String, currentSupportUids: List<String>): Result<Unit> {
        val docPath = "groups/$groupId/statuses/$statusId"
        return try {
            val uid = requireUserId()
            val updated = if (uid in currentSupportUids) {
                currentSupportUids - uid
            } else {
                (currentSupportUids + uid).distinct().take(50)
            }
            db.document(docPath).update(
                mapOf(
                    "supportUids" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteStatusPost(groupId: String, statusId: String): Result<Unit> {
        val docPath = "groups/$groupId/statuses/$statusId"
        return try {
            requireUserId()
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun postMemory(
        group: FriendGroup,
        authorName: String,
        mediaType: MediaType,
        caption: String,
        tag: String,
        mediaBase64: String,
        thumbnailBase64: String,
        externalUrl: String
    ): Result<Unit> {
        var docPath = "groups/${group.id}/memories"
        return try {
            val uid = requireUserId()
            val cleanCaption = caption.trim().take(240)
            require(cleanCaption.isNotEmpty()) { "Caption cannot be empty" }
            require(mediaBase64.isNotBlank() || thumbnailBase64.isNotBlank() || externalUrl.isNotBlank()) {
                "Please select a photo, video, or paste a media link"
            }

            val ref = db.collection("groups").document(group.id).collection("memories").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "groupId" to group.id,
                    "userId" to uid,
                    "authorName" to authorName.trim().ifBlank { "Friend" }.take(50),
                    "mediaType" to mediaType.name,
                    "caption" to cleanCaption,
                    "tag" to tag.trim().take(40),
                    "mediaBase64" to mediaBase64.take(650_000),
                    "thumbnailBase64" to thumbnailBase64.take(200_000),
                    "externalUrl" to externalUrl.trim().take(500),
                    "likedByUids" to emptyList<String>(),
                    "memberUids" to group.memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMemoryLike(groupId: String, memoryId: String, currentLikes: List<String>): Result<Unit> {
        val docPath = "groups/$groupId/memories/$memoryId"
        return try {
            val uid = requireUserId()
            val updated = if (uid in currentLikes) {
                currentLikes - uid
            } else {
                (currentLikes + uid).distinct().take(50)
            }
            db.document(docPath).update(
                mapOf(
                    "likedByUids" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(groupId: String, memoryId: String): Result<Unit> {
        val docPath = "groups/$groupId/memories/$memoryId"
        return try {
            requireUserId()
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createGameRoom(
        group: FriendGroup,
        gameType: GameType,
        hostName: String
    ): Result<String> {
        var docPath = "groups/${group.id}/games"
        return try {
            val uid = requireUserId()
            val ref = db.collection("groups").document(group.id).collection("games").document()
            docPath = ref.path
            val initialBoard = when (gameType) {
                GameType.TIC_TAC_TOE -> List(9) { "" }
                GameType.CONNECT_FOUR -> List(42) { "" }
                GameType.RPS_DUEL -> emptyList()
            }
            ref.set(
                mapOf(
                    "groupId" to group.id,
                    "gameType" to gameType.name,
                    "status" to GameStatus.WAITING.name,
                    "hostUid" to uid,
                    "hostName" to hostName.trim().ifBlank { "Friend" }.take(50),
                    "guestUid" to "",
                    "guestName" to "",
                    "currentTurnUid" to uid,
                    "winnerUid" to "",
                    "board" to initialBoard,
                    "rpsRound" to 1L,
                    "rpsHostMove" to "",
                    "rpsGuestMove" to "",
                    "rpsHostScore" to 0L,
                    "rpsGuestScore" to 0L,
                    "rpsSummary" to "",
                    "memberUids" to group.memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinGameRoom(
        groupId: String,
        gameId: String,
        guestName: String,
        hostUid: String
    ): Result<Unit> {
        val docPath = "groups/$groupId/games/$gameId"
        return try {
            val uid = requireUserId()
            db.document(docPath).update(
                mapOf(
                    "guestUid" to uid,
                    "guestName" to guestName.trim().ifBlank { "Friend" }.take(50),
                    "status" to GameStatus.PLAYING.name,
                    "currentTurnUid" to hostUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateGameMove(
        groupId: String,
        gameId: String,
        fields: Map<String, Any>
    ): Result<Unit> {
        val docPath = "groups/$groupId/games/$gameId"
        return try {
            requireUserId()
            val payload = fields.toMutableMap()
            payload["updatedAt"] = FieldValue.serverTimestamp()
            db.document(docPath).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteGameRoom(groupId: String, gameId: String): Result<Unit> {
        val docPath = "groups/$groupId/games/$gameId"
        return try {
            requireUserId()
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }
}
