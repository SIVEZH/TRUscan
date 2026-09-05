package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val userDao: UserDao) {
    suspend fun seedDatabase() {
        withContext(Dispatchers.IO) {
            if (userDao.getUserCount() == 0) {
                userDao.insertUser(
                    UserEntity(
                        id = 1,
                        name = "Test User",
                        email = "user@test.com",
                        phone = "1234567890",
                        passwordHash = "Test@123", // Using plain text for mockup phase
                        role = "USER",
                        authorityCategory = null
                    )
                )
                userDao.insertUser(
                    UserEntity(
                        id = 2,
                        name = "Food Authority",
                        email = "food@test.com",
                        phone = null,
                        passwordHash = "Test@123",
                        role = "AUTHORITY",
                        authorityCategory = "FOOD"
                    )
                )
                userDao.insertUser(
                    UserEntity(
                        id = 3,
                        name = "Cosmetics Authority",
                        email = "cosmetics@test.com",
                        phone = null,
                        passwordHash = "Test@123",
                        role = "AUTHORITY",
                        authorityCategory = "COSMETICS"
                    )
                )
                userDao.insertUser(
                    UserEntity(
                        id = 4,
                        name = "Medicines Authority",
                        email = "medicine@test.com",
                        phone = null,
                        passwordHash = "Test@123",
                        role = "AUTHORITY",
                        authorityCategory = "MEDICINES"
                    )
                )
            }
        }
    }

    suspend fun login(loginId: String, passwordHash: String): Result<UserEntity> {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUserByEmail(loginId) ?: userDao.getUserByPhone(loginId)
            
            if (user != null && user.passwordHash == passwordHash) {
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid credentials"))
            }
        }
    }
}
