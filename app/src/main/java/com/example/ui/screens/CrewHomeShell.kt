package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.UiState
import com.example.ui.CrewViewModel
import com.example.ui.components.CrewAvatar
import com.example.ui.components.EditProfileBirthdayDialog
import com.example.ui.components.GroupManagementDialog

enum class CrewTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    BIRTHDAYS(R.string.nav_birthdays, Icons.Filled.Cake, Icons.Outlined.Cake, "tab_birthdays"),
    MEMORIES(R.string.nav_memories, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary, "tab_memories"),
    GAMES(R.string.nav_games, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, "tab_games"),
    STATUS(R.string.nav_status, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "tab_status"),
    PROFILE(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewHomeShell(
    viewModel: CrewViewModel,
    onSignOut: () -> Unit
) {
    val groupsState by viewModel.groupsState.collectAsStateWithLifecycle()
    val activeGroup by viewModel.activeGroup.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val membersState by viewModel.membersState.collectAsStateWithLifecycle()
    val wishesState by viewModel.wishesState.collectAsStateWithLifecycle()
    val statusesState by viewModel.statusesState.collectAsStateWithLifecycle()
    val memoriesState by viewModel.memoriesState.collectAsStateWithLifecycle()
    val gamesState by viewModel.gamesState.collectAsStateWithLifecycle()
    val activeGameRoomId by viewModel.activeGameRoomId.collectAsStateWithLifecycle()
    val actionLoading by viewModel.actionLoading.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(CrewTab.BIRTHDAYS) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    when (val gs = groupsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        is UiState.Error -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = gs.message, color = MaterialTheme.colorScheme.error)
                }
            }
            return
        }
        is UiState.Success -> {
            if (gs.data.isEmpty() || activeGroup == null) {
                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        GroupOnboardingScreen(
                            displayName = viewModel.myDisplayName(),
                            isBusy = actionLoading,
                            onCreateGroup = { name, m, d -> viewModel.createGroup(name, m, d) },
                            onJoinGroup = { code, m, d -> viewModel.joinGroup(code, m, d) },
                            onSignOut = onSignOut
                        )
                    }
                }
                return
            }
        }
    }

    val group = activeGroup!!
    val allGroups = (groupsState as? UiState.Success)?.data ?: emptyList()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showGroupDialog = true }
                                .testTag("group_switcher_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Code ${group.inviteCode} • ${group.memberUids.size} friends",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ExpandMore,
                                    contentDescription = "Manage group",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        val authUser = Firebase.auth.currentUser
                        IconButton(
                            onClick = { currentTab = CrewTab.PROFILE },
                            modifier = Modifier.testTag("open_profile_button")
                        ) {
                            if (authUser?.photoUrl != null) {
                                AsyncImage(
                                    model = authUser.photoUrl,
                                    contentDescription = "Open Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                CrewAvatar(
                                    name = viewModel.myDisplayName(),
                                    avatarIndex = userProfile?.avatarIndex ?: 0L,
                                    size = 34.dp
                                )
                            }
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = stringResource(R.string.sign_out)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        CrewTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(Modifier.weight(1f))
                        CrewTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize()
                    ) {
                        when (currentTab) {
                            CrewTab.BIRTHDAYS -> BirthdaysScreen(
                                currentUserId = viewModel.currentUserId,
                                membersState = membersState,
                                wishesState = wishesState,
                                onEditMyBirthday = { showProfileDialog = true },
                                onSendWish = { target, msg, gift ->
                                    viewModel.sendBirthdayWish(target, msg, gift)
                                },
                                onDeleteWish = { wishId -> viewModel.deleteWish(wishId) }
                            )

                            CrewTab.MEMORIES -> MemoriesScreen(
                                currentUserId = viewModel.currentUserId,
                                memoriesState = memoriesState,
                                onPostMemory = { type, caption, tag, mB64, tB64, url ->
                                    viewModel.postMemory(type, caption, tag, mB64, tB64, url)
                                },
                                onToggleLike = { mem -> viewModel.toggleMemoryLike(mem) },
                                onDeleteMemory = { memId -> viewModel.deleteMemory(memId) }
                            )

                            CrewTab.GAMES -> GamesScreen(
                                currentUserId = viewModel.currentUserId,
                                gamesState = gamesState,
                                activeGameRoomId = activeGameRoomId,
                                onOpenRoom = { roomId -> viewModel.openGameRoom(roomId) },
                                onCreateRoom = { gameType -> viewModel.createGameRoom(gameType) },
                                onJoinRoom = { room -> viewModel.joinGameRoom(room) },
                                onTicTacToeMove = { room, cell -> viewModel.makeTicTacToeMove(room, cell) },
                                onConnectFourMove = { room, col -> viewModel.makeConnectFourMove(room, col) },
                                onRpsMove = { room, move -> viewModel.makeRpsMove(room, move) },
                                onDeleteRoom = { gameId -> viewModel.deleteGameRoom(gameId) }
                            )

                            CrewTab.STATUS -> StatusScreen(
                                currentUserId = viewModel.currentUserId,
                                membersState = membersState,
                                statusesState = statusesState,
                                onPostStatus = { cat, msg, place, avail ->
                                    viewModel.postLiveStatus(cat, msg, place, avail)
                                },
                                onToggleSupport = { post -> viewModel.toggleStatusSupport(post) },
                                onDeleteStatus = { statusId -> viewModel.deleteStatusPost(statusId) }
                            )

                            CrewTab.PROFILE -> ProfileScreen(
                                authUser = Firebase.auth.currentUser,
                                userProfile = userProfile,
                                activeGroup = group,
                                onEditBirthdayAndName = { showProfileDialog = true },
                                onLogout = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGroupDialog) {
        GroupManagementDialog(
            activeGroup = group,
            allGroups = allGroups,
            onSelectGroup = { gid -> viewModel.selectGroup(gid) },
            onCreateGroup = { newName ->
                viewModel.createGroup(
                    name = newName,
                    birthdayMonth = userProfile?.birthdayMonth ?: 0L,
                    birthdayDay = userProfile?.birthdayDay ?: 0L
                )
            },
            onJoinGroup = { code ->
                viewModel.joinGroup(
                    inviteCode = code,
                    birthdayMonth = userProfile?.birthdayMonth ?: 0L,
                    birthdayDay = userProfile?.birthdayDay ?: 0L
                )
            },
            onCopyFeedback = { msg -> viewModel.showFeedback(msg) },
            onDismiss = { showGroupDialog = false }
        )
    }

    if (showProfileDialog) {
        EditProfileBirthdayDialog(
            initialName = viewModel.myDisplayName(),
            initialAvatarIndex = userProfile?.avatarIndex ?: 0L,
            initialMonth = userProfile?.birthdayMonth ?: 0L,
            initialDay = userProfile?.birthdayDay ?: 0L,
            onSave = { name, m, d, avatar ->
                viewModel.updateMyBirthdayAndProfile(name, m, d, avatar)
            },
            onDismiss = { showProfileDialog = false }
        )
    }
}
