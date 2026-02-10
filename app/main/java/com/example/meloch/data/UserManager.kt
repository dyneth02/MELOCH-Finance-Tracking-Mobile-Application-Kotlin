package com.example.meloch.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.Patterns
import com.example.meloch.data.model.User

class UserManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val preferencesManager = PreferencesManager(context)

    companion object {
        private const val PREF_NAME = "user_prefs"
        const val KEY_CURRENT_USER = "current_user" // Changed to public for PreferencesManager
        private const val KEY_USERS = "users"
        private const val DEFAULT_EMAIL = "dineth@mail.com"
        private const val DEFAULT_PASSWORD = "dineth12345"
        private const val DEFAULT_USERNAME = "Dineth"
        private const val TAG = "UserManager"
    }

    init {
        // Check if default user exists, if not create it
        if (!isUserExists(DEFAULT_EMAIL)) {
            val defaultUser = User(DEFAULT_USERNAME, DEFAULT_EMAIL, DEFAULT_PASSWORD)
            saveUser(defaultUser)
            Log.d(TAG, "Default user created: $DEFAULT_EMAIL")
        }
    }

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    fun isValidUsername(username: String): Boolean {
        return username.isNotEmpty()
    }

    fun login(email: String, password: String): Boolean {
        val users = getUsers()
        val user = users[email]

        return if (user != null && user.password == password) {
            // Set as current user
            setCurrentUser(email)
            true
        } else {
            false
        }
    }

    fun register(username: String, email: String, password: String): Boolean {
        if (isUserExists(email)) {
            return false
        }

        val newUser = User(username, email, password)
        saveUser(newUser)
        setCurrentUser(email)

        // Initialize new user data with zero values
        initializeNewUserData()

        return true
    }

    private fun initializeNewUserData() {
        // Set zero pocket money
        val cardManager = CardManager(preferencesManager.context)
        cardManager.savePocketMoney(0.0)

        // Clear any cards
        cardManager.saveCards(emptyList())

        // Set zero budget values
        preferencesManager.saveCategoryBudget("Entertainment", 0.0)
        preferencesManager.saveCategoryBudget("Food", 0.0)
        preferencesManager.saveCategoryBudget("Transport", 0.0)
        preferencesManager.saveCategoryBudget("Lifestyle", 0.0)

        // Set zero total budget and budget left
        preferencesManager.setMonthlyBudget(0.0)
        preferencesManager.setBudgetLeft(0.0)

        // Set zero total balance
        preferencesManager.setTotalBalance(0.0)

        // Clear all transactions
        preferencesManager.saveTransactions(emptyList())

        // Reset budget flags
        preferencesManager.setBudgetResetFlag(false)

        Log.d(TAG, "New user data initialized with zero values")
    }

    fun isUserExists(email: String): Boolean {
        return getUsers().containsKey(email)
    }

    fun getCurrentUser(): User? {
        val email = getCurrentUserEmail() ?: return null
        return getUsers()[email]
    }

    fun getCurrentUserEmail(): String? {
        return sharedPreferences.getString(KEY_CURRENT_USER, null)
    }

    private fun setCurrentUser(email: String) {
        sharedPreferences.edit().putString(KEY_CURRENT_USER, email).apply()
    }

    fun logout() {
        sharedPreferences.edit().remove(KEY_CURRENT_USER).apply()
    }

    fun deleteCurrentUser(): Boolean {
        val currentUserEmail = sharedPreferences.getString(KEY_CURRENT_USER, null) ?: return false

        // Get all users
        val users = getUsers().toMutableMap()

        // Remove the current user
        users.remove(currentUserEmail)

        // Save updated users map
        val usersJson = preferencesManager.gson.toJson(users)
        sharedPreferences.edit().putString(KEY_USERS, usersJson).apply()

        // Clear all user-specific data
        preferencesManager.clearAllData()

        // Remove current user reference
        sharedPreferences.edit().remove(KEY_CURRENT_USER).apply()

        Log.d(TAG, "User account deleted: $currentUserEmail")
        return true
    }

    private fun saveUser(user: User) {
        val users = getUsers().toMutableMap()
        users[user.email] = user

        val usersJson = preferencesManager.gson.toJson(users)
        sharedPreferences.edit().putString(KEY_USERS, usersJson).apply()
    }

    private fun getUsers(): Map<String, User> {
        val usersJson = sharedPreferences.getString(KEY_USERS, null)

        return if (usersJson != null) {
            try {
                val type = com.google.gson.reflect.TypeToken.getParameterized(
                    Map::class.java, String::class.java, User::class.java
                ).type
                preferencesManager.gson.fromJson(usersJson, type)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing users JSON", e)
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    private fun resetAppData() {
        // Reset transactions
        preferencesManager.saveTransactions(emptyList())

        // Reset budget values
        preferencesManager.saveCategoryBudget("Entertainment", 0.0)
        preferencesManager.saveCategoryBudget("Food", 0.0)
        preferencesManager.saveCategoryBudget("Transport", 0.0)
        preferencesManager.saveCategoryBudget("Lifestyle", 0.0)

        // Reset total budget and budget left
        preferencesManager.setMonthlyBudget(0.0)
        preferencesManager.setBudgetLeft(0.0)

        // Reset total balance
        preferencesManager.setTotalBalance(0.0)

        Log.d(TAG, "App data reset for new user")
    }
}
