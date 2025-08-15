package net.rpcsx

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.rpcsx.utils.GeneralSettings
import java.io.File

data class User(
    val userId: String,
    val userDir: String,
    val username: String
)

object UserValidator {
    private val usernameRegex = Regex("^[A-Za-z0-9_]{3,16}$")

    fun isValidUserId(id: String) = id.length == 8 && id.isDigitsOnly()
    fun asUserKeyOrZero(id: String) = if (isValidUserId(id)) id.toIntOrNull() ?: 0 else 0
    fun validateUsername(username: String) = usernameRegex.matches(username)

    fun normalizeUsername(raw: String, fallback: String): String {
        val cleaned = raw.filter { it.isLetterOrDigit() || it == '_' }.take(16)
        return if (validateUsername(cleaned)) cleaned else fallback.take(16)
    }
}

private fun readTextOrNull(file: File): String? = runCatching {
    if (file.isFile) file.readText(Charsets.UTF_8).trim() else null
}.getOrNull()

private fun toUser(userId: String): User {
    val userDir = File(RPCSX.getHdd0Dir(), "home/$userId")
    val fallback = "User$userId"
    val name = readTextOrNull(File(userDir, "localusername")) ?: fallback
    return User(userId, userDir.path, UserValidator.normalizeUsername(name, fallback))
}

class UserRepository private constructor() {
    private val _users = mutableStateMapOf<Int, User>()
    private val _activeUser = mutableStateOf("")
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ioMutex = Mutex()

    companion object {
        private val instance = UserRepository()
        val users get() = instance._users
        val activeUser get() = instance._activeUser

        fun load() {
            instance.repoScope.launch {
                instance.refreshUsersList()

                val fromSettings = getUserFromSettings()
                val fromEmu = runCatching { RPCSX.instance.getUser() }.getOrNull()
                var resolved = fromEmu?.takeIf(UserValidator::isValidUserId) ?: fromSettings

                if (!users.containsKey(UserValidator.asUserKeyOrZero(resolved))) {
                    resolved = users.keys.minOrNull()?.let { "%08d".format(it) } ?: "00000001"
                }

                withContext(Dispatchers.Main) {
                    if (activeUser.value != resolved) activeUser.value = resolved
                }
            }
        }

        fun getUsername(userId: String) =
            users.values.firstOrNull { it.userId == userId }?.username

        fun getUserFromSettings(): String {
            val stored = GeneralSettings["active_user"] as? String
            return if (UserValidator.isValidUserId(stored ?: "")) stored!! else "00000001"
        }

        fun createUser(username: String) {
            if (!UserValidator.validateUsername(username)) return
            instance.repoScope.launch {
                val nextId = instance.findNextFreeUserId() ?: return@launch
                instance.generateUser(nextId, username)
                instance.refreshUsersList()
            }
        }

        fun removeUser(userId: String) {
            if (!UserValidator.isValidUserId(userId) || activeUser.value == userId) return
            instance.repoScope.launch {
                val key = userId.toInt()
                users[key]?.let { File(it.userDir).deleteRecursively() }
                instance.refreshUsersList()
            }
        }

        fun renameUser(userId: String, username: String) {
            if (!UserValidator.isValidUserId(userId) || !UserValidator.validateUsername(username)) return
            instance.repoScope.launch {
                runCatching {
                    File(RPCSX.getHdd0Dir(), "home/$userId/localusername")
                        .writeText(username, Charsets.UTF_8)
                }
                instance.refreshUsersList()
            }
        }

        fun loginUser(userId: String) {
            if (!UserValidator.isValidUserId(userId)) return
            instance.repoScope.launch {
                runCatching { RPCSX.instance.loginUser(userId) }
                withContext(Dispatchers.Main) { activeUser.value = userId }
                GeneralSettings.setValue("active_user", userId)
                GameRepository.queueRefresh()
            }
        }
    }

    private suspend fun refreshUsersList() {
        ioMutex.withLock {
            val fresh = getUserAccounts()
            withContext(Dispatchers.Main) {
                _users.clear()
                _users.putAll(fresh)
            }
        }
    }

    private fun findNextFreeUserId(): String? {
        val taken = _users.keys.toSortedSet()
        var candidate = 1
        for (k in taken) {
            if (k == candidate) candidate++ else if (k > candidate) break
        }
        return if (candidate < 100_000_000) "%08d".format(candidate) else null
    }

    private fun generateUser(userId: String, userName: String) {
        require(UserValidator.isValidUserId(userId))
        val userDir = File(RPCSX.getHdd0Dir(), "home/$userId").apply { mkdirs() }
        listOf("exdata", "savedata", "trophy").forEach { File(userDir, it).mkdirs() }
        File(userDir, "localusername").writeText(userName, Charsets.UTF_8)
    }

    private fun getUserAccounts(): HashMap<Int, User> {
        val userList = HashMap<Int, User>()
        val dirs = File(RPCSX.getHdd0Dir(), "home").listFiles() ?: return userList
        for (dir in dirs) {
            if (!dir.isDirectory) continue
            val key = UserValidator.asUserKeyOrZero(dir.name)
            if (key == 0) continue
            userList[key] = toUser(dir.name)
        }
        return userList
    }
}
