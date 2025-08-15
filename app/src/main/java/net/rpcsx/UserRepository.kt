package net.rpcsx

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.isDigitsOnly
import net.rpcsx.utils.GeneralSettings
import java.io.File

data class User(
    val userId: String,
    val userDir: String,
    val username: String
)

/* ----------------------------- Helpers ------------------------------ */

/** Normalize a username to [A-Za-z0-9_] and max length 16; fallback if <3. */
private fun normalizeUsername(raw: String, fallback: String): String {
    val cleaned = raw.filter { it.isLetterOrDigit() || it == '_' }.take(16)
    return if (cleaned.length in 3..16) cleaned else fallback.take(16)
}

/** Safe file read; returns null on any error or if not a file. */
private fun readTextOrNull(file: File): String? = runCatching {
    if (file.isFile) file.readText(Charsets.UTF_8).trim() else null
}.getOrNull()

/** Build a User object from disk with safe fallbacks. */
private fun toUser(userId: String): User {
    val userDir = File(RPCSX.getHdd0Dir()).resolve("home").resolve(userId)
    val fallback = "User$userId"
    val name = readTextOrNull(userDir.resolve("localusername")) ?: fallback
    val username = normalizeUsername(name, fallback)
    return User(userId, userDir.path, username)
}

private fun String.isValidUserId(): Boolean = length == 8 && isDigitsOnly()

/* ------------------------------------------------------------------- */

class UserRepository {
    private val users = mutableStateMapOf<Int, User>()
    private var activeUser = mutableStateOf("")

    companion object {
        private val instance = UserRepository()

        val users = instance.users
        val activeUser = instance.activeUser

        fun getUsername(userId: String): String? {
            return users.values.firstOrNull { it.userId == userId }?.username
        }

        fun load() {
            updateList()
            instance.activeUser.value = getUserFromSettings()
            // Keep in sync with emulator if it reports a different valid user
            val emuUser = runCatching { RPCSX.instance.getUser() }.getOrNull()
            if (emuUser != null && emuUser.isValidUserId() && instance.activeUser.value != emuUser) {
                instance.activeUser.value = emuUser
            }
        }

        /** Return non-null, validated user id, defaulting to "00000001". */
        fun getUserFromSettings(): String {
            val stored = GeneralSettings["active_user"] as? String
            return if (stored != null && stored.isValidUserId()) stored else "00000001"
        }

        private fun updateList() {
            users.clear()
            users.putAll(getUserAccounts())
        }

        private fun checkUser(directory: String): Int {
            return if (directory.isDigitsOnly() && directory.length == 8) {
                directory.toInt()
            } else {
                0
            }
        }

        private fun generateUser(userId: String, userName: String) {
            require(checkUser(userId) > 0)

            val homeDir = File(RPCSX.getHdd0Dir()).resolve("home")
            val userDir = homeDir.resolve(userId)

            homeDir.mkdirs()
            userDir.mkdirs()
            userDir.resolve("exdata").mkdirs()
            userDir.resolve("savedata").mkdirs()
            userDir.resolve("trophy").mkdirs()
            userDir.resolve("localusername").writeText(userName, Charsets.UTF_8)
        }

        fun createUser(username: String) {
            // find smallest free numeric id starting from 1
            val taken = instance.users.keys.toSortedSet()
            var smallest = 1
            for (k in taken) {
                if (k == smallest) smallest++ else if (k > smallest) break
            }
            if (smallest >= 100_000_000) return

            val nextUserId = "%08d".format(smallest)
            require(checkUser(nextUserId) > 0)

            if (!validateUsername(username)) return

            generateUser(nextUserId, username)
            updateList()
        }

        fun removeUser(userId: String) {
            if (instance.activeUser.value == userId) return

            val key = checkUser(userId)
            if (key == 0) return

            instance.users[key]?.also {
                File(it.userDir).deleteRecursively()
            }

            updateList()
        }

        fun renameUser(userId: String, username: String) {
            val key = checkUser(userId)
            if (key == 0) return
            if (!validateUsername(username)) return

            val usernameFile = File(RPCSX.getHdd0Dir())
                .resolve("home")
                .resolve(userId)
                .resolve("localusername")

            runCatching { usernameFile.writeText(username, Charsets.UTF_8) }
            updateList()
        }

        fun loginUser(userId: String) {
            require(userId.isValidUserId())
            RPCSX.instance.loginUser(userId)
            instance.activeUser.value = userId
            GeneralSettings.setValue("active_user", userId)
            GameRepository.queueRefresh()
        }

        /** Allow underscores, 3..16 chars. */
        fun validateUsername(textToValidate: String): Boolean {
            return textToValidate.matches(Regex("^[A-Za-z0-9_]{3,16}$"))
        }

        private fun getUserAccounts(): HashMap<Int, User> {
            val userList: HashMap<Int, User> = hashMapOf()

            File(RPCSX.getHdd0Dir()).resolve("home").listFiles()?.let {
                for (userDir in it) {
                    if (!userDir.isDirectory) continue

                    val key = checkUser(userDir.name)
                    if (key == 0) continue
                    if (!userDir.resolve("localusername").isFile) continue

                    userList[key] = toUser(userDir.name)
                }
            }

            return userList
        }
    }
}
