package net.rpcsx

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.rpcsx.utils.GeneralSettings
import java.io.File

data class User(
    val userId: String,
    val userDir: String,
    val username: String
)

/**
 * Normalize a username:
 * - keep only [A-Za-z0-9_]
 * - trim to max 16 chars
 * - ensure length in [3..16], otherwise return fallback
 */
private fun normalizeUsername(raw: String, fallback: String): String {
    val cleaned = raw.filter { it.isLetterOrDigit() || it == '_' }.take(16)
    return if (cleaned.length in 3..16) cleaned else fallback.take(16)
}

/** Safely read file text; returns null on any error. */
private fun readTextOrNull(file: File): String? = runCatching {
    if (file.isFile) file.readText(Charsets.UTF_8).trim() else null
}.getOrNull()

/** Build a User from disk with safe fallback username. */
private fun toUser(userId: String): User {
    val userDir = File(RPCSX.getHdd0Dir()).resolve("home").resolve(userId)
    val fallback = "User$userId"
    val name = readTextOrNull(userDir.resolve("localusername")) ?: fallback
    val username = normalizeUsername(name, fallback)
    return User(userId = userId, userDir = userDir.path, username = username)
}

class UserRepository private constructor() {

    // Compose-observable state
    private val _users = mutableStateMapOf<Int, User>()
    private val _activeUser = mutableStateOf("")

    // Single scope for background IO
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ioMutex = Mutex()

    companion object {
        private val instance = UserRepository()

        val users get() = instance._users
        val activeUser get() = instance._activeUser

        /**
         * Loads/refreshes users and resolves the active user.
         * Can be called from UI; IO is off the main thread.
         */
        fun load() {
            instance.repoScope.launch {
                instance.refreshUsersList()

                val fromSettings = getUserFromSettings()
                val fromEmu = runCatching { RPCSX.instance.getUser() }.getOrNull()
                var resolved = fromEmu?.takeIf { it.isValidUserId() } ?: fromSettings

                // If resolved doesn't exist on disk, fall back to first available or default.
                if (!users.containsKey(resolved.asUserKeyOrZero())) {
                    resolved = users.keys.minOrNull()?.let { "%08d".format(it) } ?: "00000001"
                }

                withContext(Dispatchers.Main) {
                    if (activeUser.value != resolved) activeUser.value = resolved
                }
            }
        }

        fun getUsername(userId: String): String? =
            users.values.firstOrNull { it.userId == userId }?.username

        fun getUserFromSettings(): String {
            val stored = GeneralSettings["active_user"] as? String
            return if (stored.isValidUserId()) stored else "00000001"
        }

        fun createUser(username: String) {
            instance.repoScope.launch {
                val nextId = instance.findNextFreeUserId() ?: return@launch
                if (!validateUsername(username)) return@launch
                instance.generateUser(nextId, username)
                instance.refreshUsersList()
            }
        }

        fun removeUser(userId: String) {
            if (activeUser.value == userId) return
            if (!userId.isValidUserId()) return
            instance.repoScope.launch {
                val key = userId.toInt()
                users[key]?.let { File(it.userDir).deleteRecursively() }
                instance.refreshUsersList()
            }
        }

        fun renameUser(userId: String, username: String) {
            if (!userId.isValidUserId()) return
            if (!validateUsername(username)) return
            instance.repoScope.launch {
                val usernameFile = File(RPCSX.getHdd0Dir())
                    .resolve("home").resolve(userId).resolve("localusername")
                runCatching { usernameFile.writeText(username, Charsets.UTF_8) }
                instance.refreshUsersList()
            }
        }

        fun loginUser(userId: String) {
            if (!userId.isValidUserId()) return
            instance.repoScope.launch {
                runCatching { RPCSX.instance.loginUser(userId) }
                withContext(Dispatchers.Main) { activeUser.value = userId }
                GeneralSettings.setValue("active_user", userId)
                GameRepository.queueRefresh()
            }
        }

        fun validateUsername(text: String): Boolean =
            text.matches(Regex("^[A-Za-z0-9_]{3,16}$"))
    }

    /** Refresh in-memory users by scanning disk (serialized by mutex). */
    private suspend fun refreshUsersList() {
        ioMutex.withLock {
            val fresh = getUserAccounts()
            withContext(Dispatchers.Main) {
                _users.clear()
                _users.putAll(fresh)
            }
        }
    }

    /** Next free 8-digit user id as zero-padded string, or null if exhausted. */
    private fun findNextFreeUserId(): String? {
        val taken = _users.keys.toSortedSet()
        var candidate = 1
        for (k in taken) {
            if (k == candidate) candidate++ else if (k > candidate) break
        }
        if (candidate >= 100_000_000) return null
        return "%08d".format(candidate)
    }

    /** Create minimal user directory structure and persist username. */
    private fun generateUser(userId: String, userName: String) {
        require(userId.isValidUserId())
        val homeDir = File(RPCSX.getHdd0Dir()).resolve("home")
        val userDir = homeDir.resolve(userId)
        homeDir.mkdirs()
        userDir.mkdirs()
        userDir.resolve("exdata").mkdirs()
        userDir.resolve("savedata").mkdirs()
        userDir.resolve("trophy").mkdirs()
        userDir.resolve("localusername").writeText(userName, Charsets.UTF_8)
    }

    /** Scan existing user accounts from disk. */
    private fun getUserAccounts(): HashMap<Int, User> {
        val userList = HashMap<Int, User>()
        val home = File(RPCSX.getHdd0Dir()).resolve("home")
        val dirs = home.listFiles() ?: return userList
        for (dir in dirs) {
            if (!dir.isDirectory) continue
            val name = dir.name
            val key = name.asUserKeyOrZero()
            if (key == 0) continue
            if (!dir.resolve("localusername").isFile) continue
            userList[key] = toUser(name)
        }
        return userList
    }
}

/* ----------------------------- Helpers ------------------------------ */

private fun String.isValidUserId(): Boolean = length == 8 && isDigitsOnly()

private fun String.asUserKeyOrZero(): Int =
    if (isValidUserId()) runCatching { toInt() }.getOrElse { 0 } else 0
