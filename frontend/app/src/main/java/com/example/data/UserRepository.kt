package com.example.data

import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allLogs: Flow<List<VerificationLogEntity>> = userDao.getAllLogs()

    suspend fun getUserById(userId: String): UserEntity? = userDao.getUserById(userId)

    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)

    suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)

    suspend fun deleteUserById(userId: String) = userDao.deleteUserById(userId)

    suspend fun insertLog(log: VerificationLogEntity) = userDao.insertLog(log)

    suspend fun clearLogs() = userDao.clearLogs()
}
