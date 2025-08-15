package net.rpcsx.ui.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import net.rpcsx.EmulatorState
import net.rpcsx.RPCSX
import net.rpcsx.User
import net.rpcsx.UserRepository
import net.rpcsx.dialogs.AlertDialogQueue

@Composable
private fun UserRow(
    user: User,
    isActive: Boolean,
    onActivate: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(enabled = !isActive) { onActivate() }
    ) {
        RadioButton(
            selected = isActive,
            onClick = onActivate
        )
        Text(
            text = user.username,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
        Text(
            text = user.userId,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    navigateBack: () -> Unit
) {
    // Reactive state from repository
    val usersStateMap = remember { UserRepository.users }
    val activeUserId by remember { UserRepository.activeUser }
    val emulatorState by remember { RPCSX.state }

    // Stable list snapshot when the map changes
    val usersList by remember {
        derivedStateOf {
            usersStateMap.values.sortedBy { it.userId }
        }
    }

    // Simple guard to avoid double switching
    val isSwitchingUser = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UserRepository.load()
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        topBar = {
            TopAppBar(
                title = { Text("Users") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                items(
                    items = usersList,
                    key = { it.userId }
                ) { user ->
                    val isActive = user.userId == activeUserId

                    UserRow(
                        user = user,
                        isActive = isActive,
                        onActivate = {
                            if (isActive || isSwitchingUser.value) return@UserRow
                            isSwitchingUser.value = true

                            val performSwitch = {
                                UserRepository.loginUser(user.userId)
                                isSwitchingUser.value = false
                            }

                            if (emulatorState != EmulatorState.Stopped) {
                                AlertDialogQueue.showDialog(
                                    title = "Stop Emulator?",
                                    message = "To change the user, the emulator must be stopped.\n\nStop the emulator now?",
                                    onConfirm = {
                                        RPCSX.instance.kill()
                                        RPCSX.updateState()
                                        performSwitch()
                                    },
                                    onDismiss = {
                                        isSwitchingUser.value = false
                                    }
                                )
                            } else {
                                performSwitch()
                            }
                        }
                    )
                }

                // Spacer so the last item isn't blocked by nav bar
                item {
                    Box(
                        Modifier.height(
                            (LocalConfiguration.current.screenHeightDp.dp * 0.25f)
                                .coerceAtLeast(24.dp)
                        )
                    )
                }
            }
        }
    }
}
