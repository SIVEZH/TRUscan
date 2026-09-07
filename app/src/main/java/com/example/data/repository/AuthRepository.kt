package com.example.data.repository

import com.example.data.local.SessionManager
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class AuthRepository(private val userDao: UserDao, private val sessionManager: SessionManager) {

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    suspend fun seedDatabase() {
        withContext(Dispatchers.IO) {
            if (userDao.getUserCount() == 0) {
                userDao.insertUser(
                    UserEntity(
                        userId = "demo-user-id",
                        name = "Test User",
                        email = "user@test.com",
                        phone = "1234567890",
                        passwordHash = hashPassword("Test@123"),
                        role = "USER",
                        authorityCategory = null
                    )
                )
                userDao.insertUser(
                    UserEntity(
                        userId = "demo-food-auth-id",
                        name = "Food Authority",
                        email = "food@test.com",
                        phone = null,
                        passwordHash = hashPassword("Test@123"),
                        role = "AUTHORITY",
                        authorityCategory = "FOOD"
                    )
                )
                userDao.insertUser(
                    UserEntity(
                        userId = "demo-cosmetics-auth-id",
                        name = "Cosmetics Authority",
                        email = "cosmetics@test.com",
                        phone = null,
                        passwordHash = hashPassword("Test@123"),
                        role = "AUTHORITY",
                        authorityCategory = "COSMETICS"
                    )
                )
            }
        }
    }

    suspend fun login(loginId: String, passwordRaw: String): Result<UserEntity> {
        return withContext(Dispatchers.IO) {
            val normalizedId = loginId.trim().lowercase()
            val user = userDao.getUserByEmail(normalizedId) ?: userDao.getUserByPhone(normalizedId)
            
            if (user != null && user.passwordHash == hashPassword(passwordRaw)) {
                sessionManager.saveUserId(user.userId)
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid email or password."))
            }
        }
    }

    suspend fun register(name: String, email: String, passwordRaw: String, role: String = "USER"): Result<UserEntity> {
        return withContext(Dispatchers.IO) {
            val normalizedEmail = email.trim().lowercase()
            val existingUser = userDao.getUserByEmail(normalizedEmail)
            if (existingUser != null) {
                return@withContext Result.failure(Exception("Email is already registered."))
            }
            
            val newUser = UserEntity(
                userId = UUID.randomUUID().toString(),
                name = name.trim(),
                email = normalizedEmail,
                phone = null,
                passwordHash = hashPassword(passwordRaw),
                role = role,
                authorityCategory = null
            )
            
            userDao.insertUser(newUser)
            sessionManager.saveUserId(newUser.userId)
            Result.success(newUser)
        }
    }
    
    suspend fun logout() {
        withContext(Dispatchers.IO) {
            sessionManager.clearSession()
        }
    }
    
    suspend fun getCurrentUser(): UserEntity? {
        return withContext(Dispatchers.IO) {
            val userId = sessionManager.getUserId()
            if (userId != null) {
                userDao.getUserById(userId)
            } else null
        }
    }
    
    suspend fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
    }
}
