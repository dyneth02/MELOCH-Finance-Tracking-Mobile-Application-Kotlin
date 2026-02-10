package com.example.meloch.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.Transaction
import com.example.meloch.data.model.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class PreferencesManager(val context: Context) {
    private val userEmail: String? = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        .getString("current_user", null)

    private val prefsFileName = if (userEmail != null) "${userEmail}_MelochPrefs" else PREFS_NAME

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE)

    val gson = Gson() // Changed from private to public for UserManager access

    init {
        Log.d("PreferencesManager", "Initialized with user: $userEmail, using prefs file: $prefsFileName")
    }

    private val currentUserEmail: String?
        get() {
            val email = sharedPreferences.getString(UserManager.KEY_CURRENT_USER, null)
            Log.d("PreferencesManager", "Current user email: $email")
            return email
        }

    companion object {
        private const val PREFS_NAME = "MelochPrefs"
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_TOTAL_BALANCE = "total_balance"
        private const val KEY_MONTHLY_INCOME = "monthly_income"
        private const val KEY_MONTHLY_EXPENSES = "monthly_expenses"
        private const val KEY_CATEGORY_EXPENSES = "category_expenses"
        private const val KEY_ENTERTAINMENT_BUDGET = "entertainment_budget"
        private const val KEY_FOOD_BUDGET = "food_budget"
        private const val KEY_TRANSPORT_BUDGET = "transport_budget"
        private const val KEY_LIFESTYLE_BUDGET = "lifestyle_budget"
        private const val KEY_BUDGET_RESET_FLAG = "budget_reset_flag"
        private const val KEY_BUDGET_LEFT = "budget_left"
        private const val KEY_LAST_RESET_TIME = "last_reset_time"
    }

    fun saveTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        transactions.add(transaction)
        val json = gson.toJson(transactions)
        val userKey = getUserSpecificKey(KEY_TRANSACTIONS)
        sharedPreferences.edit().putString(userKey, json).apply()

        // Update total balance
        val currentBalance = getTotalBalance()
        val newBalance = when (transaction.type) {
            TransactionType.INCOME -> currentBalance + transaction.amount
            TransactionType.EXPENSE -> currentBalance - transaction.amount
        }
        setTotalBalance(newBalance)

        // Update monthly budget based on transaction type
        if (transaction.type == TransactionType.INCOME) {
            // If it's income, add to the monthly budget
            val currentBudget = getMonthlyBudget()
            setMonthlyBudget(currentBudget + transaction.amount)
        }

        // Update category expenses for UI purposes
        if (transaction.type == TransactionType.EXPENSE) {
            updateCategoryExpense(transaction.category, transaction.amount)

            // Update budget left if budget has been reset
            if (isBudgetReset()) {
                val currentBudgetLeft = getBudgetLeft()
                setBudgetLeft(currentBudgetLeft - transaction.amount)
            }
        } else if (transaction.type == TransactionType.INCOME) {
            // Update budget left if budget has been reset and it's not a salary income
            if (isBudgetReset() && transaction.category != Category.SALARY) {
                val currentBudgetLeft = getBudgetLeft()
                setBudgetLeft(currentBudgetLeft + transaction.amount)
                Log.d("Budget", "Income (${transaction.category}) added to budget left: +${transaction.amount}")
            } else if (transaction.category == Category.SALARY) {
                Log.d("Budget", "Salary income not added to budget left: ${transaction.amount}")
            }
        }
    }

    fun getTransactions(): List<Transaction> {
        // Get user-specific key
        val userKey = getUserSpecificKey(KEY_TRANSACTIONS)
        val json = sharedPreferences.getString(userKey, "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun getUserSpecificKey(baseKey: String): String {
        val email = currentUserEmail
        val result = if (email != null) {
            "${email}_$baseKey"
        } else {
            // Fallback to the base key if no user is logged in
            baseKey
        }

        // Add debug logging to track key generation
        Log.d("UserSpecificKey", "Generated key: $result from base key: $baseKey for user: $email")

        return result
    }

    fun saveTransactions(transactions: List<Transaction>) {
        val userKey = getUserSpecificKey(KEY_TRANSACTIONS)
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString(userKey, json).apply()
    }

    fun setMonthlyBudget(amount: Double) {
        val userKey = getUserSpecificKey(KEY_MONTHLY_BUDGET)
        sharedPreferences.edit().putFloat(userKey, amount.toFloat()).apply()
    }

    fun getMonthlyBudget(): Double {
        val userKey = getUserSpecificKey(KEY_MONTHLY_BUDGET)
        return sharedPreferences.getFloat(userKey, 21000f).toDouble()
    }

    fun setCurrency(currency: String) {
        val userKey = getUserSpecificKey(KEY_CURRENCY)
        sharedPreferences.edit().putString(userKey, currency).apply()
    }

    fun getCurrency(): String {
        val userKey = getUserSpecificKey(KEY_CURRENCY)
        return sharedPreferences.getString(userKey, "LKR") ?: "LKR"
    }

    fun setTotalBalance(balance: Double) {
        val userKey = getUserSpecificKey(KEY_TOTAL_BALANCE)
        sharedPreferences.edit().putFloat(userKey, balance.toFloat()).apply()
    }

    fun getTotalBalance(): Double {
        val userKey = getUserSpecificKey(KEY_TOTAL_BALANCE)
        return sharedPreferences.getFloat(userKey, 0f).toDouble()
    }

    fun getMonthlyExpenses(): Double {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return getTransactions()
            .filter { transaction ->
                val cal = Calendar.getInstance().apply { time = transaction.date }
                cal.get(Calendar.MONTH) == currentMonth &&
                cal.get(Calendar.YEAR) == currentYear &&
                transaction.type == TransactionType.EXPENSE
            }
            .sumOf { it.amount }
    }

    fun resetBudget() {
        // Get the current monthly budget and expenses
        val currentBudget = getMonthlyBudget()
        val currentExpenses = getMonthlyExpenses()

        // Reset category expenses
        for (category in Category.values()) {
            setCategoryExpense(category, 0.0)
        }

        // Set the budget left value to equal the total budget
        setBudgetLeft(currentBudget)

        // Store the current timestamp as the last reset time
        setLastResetTime(System.currentTimeMillis())

        // Set the budget reset flag to true
        setBudgetResetFlag(true)

        // Verify the flag was set
        val isReset = isBudgetReset()
        Log.d("Budget", "Verified budget reset flag after setting: $isReset")

        // Update total balance by adding back the expenses only
        // (Budget amount is already subtracted in BudgetActivity)
        val currentBalance = getTotalBalance()
        val newBalance = currentBalance + currentExpenses
        setTotalBalance(newBalance)
        Log.d("Budget", "Total balance updated: $currentBalance + $currentExpenses = $newBalance")

        // Log the reset for debugging
        Log.d("Budget", "Budget reset. Current budget: $currentBudget, Budget left: $currentBudget, Expenses reset: $currentExpenses")
    }

    fun resetBudgetWithoutBalanceUpdate() {
        // Get the current monthly budget and expenses
        val currentBudget = getMonthlyBudget()
        val currentExpenses = getMonthlyExpenses()

        Log.d("Budget", "Resetting budget without balance update")
        Log.d("Budget", "Current budget: $currentBudget, Current expenses: $currentExpenses")

        // Reset category expenses
        for (category in Category.values()) {
            setCategoryExpense(category, 0.0)
        }

        // Set the budget left value to equal the total budget
        setBudgetLeft(currentBudget)
        Log.d("Budget", "Budget left set to: $currentBudget")

        // Store the current timestamp as the last reset time
        setLastResetTime(System.currentTimeMillis())

        // Set the budget reset flag to true
        setBudgetResetFlag(true)

        // Verify the flag was set
        val isReset = isBudgetReset()
        Log.d("Budget", "Verified budget reset flag after setting: $isReset")

        // Log the reset for debugging
        Log.d("Budget", "Budget reset without balance update. Current budget: $currentBudget, Budget left: $currentBudget, Expenses reset: $currentExpenses")
    }

    fun setBudgetResetFlag(reset: Boolean) {
        val userKey = getUserSpecificKey(KEY_BUDGET_RESET_FLAG)
        Log.d("Budget", "Setting budget reset flag to: $reset for key: $userKey")
        // Use commit() instead of apply() to ensure the change is immediately written to disk
        sharedPreferences.edit().putBoolean(userKey, reset).commit()
    }

    fun isBudgetReset(): Boolean {
        val userKey = getUserSpecificKey(KEY_BUDGET_RESET_FLAG)
        val isReset = sharedPreferences.getBoolean(userKey, false)
        Log.d("Budget", "Budget reset flag: $isReset for key: $userKey")
        return isReset
    }

    fun setBudgetLeft(amount: Double) {
        val userKey = getUserSpecificKey(KEY_BUDGET_LEFT)
        Log.d("Budget", "Setting budget left to: $amount for key: $userKey")
        // Use commit() instead of apply() to ensure the change is immediately written to disk
        sharedPreferences.edit().putFloat(userKey, amount.toFloat()).commit()
    }

    fun getBudgetLeft(): Double {
        // If budget has never been reset, return the monthly budget
        if (!isBudgetReset()) {
            val monthlyBudget = getMonthlyBudget()
            Log.d("Budget", "Budget not reset, returning monthly budget: $monthlyBudget")
            return monthlyBudget
        }
        val userKey = getUserSpecificKey(KEY_BUDGET_LEFT)
        val budgetLeft = sharedPreferences.getFloat(userKey, getMonthlyBudget().toFloat()).toDouble()
        Log.d("Budget", "Budget reset, returning budget left: $budgetLeft for key: $userKey")
        return budgetLeft
    }

    fun setLastResetTime(timestamp: Long) {
        val userKey = getUserSpecificKey(KEY_LAST_RESET_TIME)
        Log.d("Budget", "Setting last reset time to: $timestamp for key: $userKey")
        sharedPreferences.edit().putLong(userKey, timestamp).commit()
    }

    fun getLastResetTime(): Long {
        val userKey = getUserSpecificKey(KEY_LAST_RESET_TIME)
        val timestamp = sharedPreferences.getLong(userKey, 0)
        Log.d("Budget", "Last reset time: $timestamp for key: $userKey")
        return timestamp
    }

    fun getExpensesSinceLastReset(): Map<Category, Double> {
        val lastResetTime = getLastResetTime()
        if (lastResetTime == 0L) {
            // If never reset, return all expenses
            return getCategoryExpenses()
        }

        // Get transactions since the last reset
        return getTransactions()
            .filter { transaction ->
                transaction.date.time > lastResetTime &&
                transaction.type == TransactionType.EXPENSE
            }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    fun getCategoryExpenses(): Map<Category, Double> {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return getTransactions()
            .filter { transaction ->
                val cal = Calendar.getInstance().apply { time = transaction.date }
                cal.get(Calendar.MONTH) == currentMonth &&
                cal.get(Calendar.YEAR) == currentYear &&
                transaction.type == TransactionType.EXPENSE
            }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    fun getMonthlyIncome(): Double {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return getTransactions()
            .filter { transaction ->
                val cal = Calendar.getInstance().apply { time = transaction.date }
                cal.get(Calendar.MONTH) == currentMonth &&
                cal.get(Calendar.YEAR) == currentYear &&
                transaction.type == TransactionType.INCOME
            }
            .sumOf { it.amount }
    }

    fun updateMonthlyIncome(amount: Double) {
        val currentIncome = getMonthlyIncome()
        val newIncome = currentIncome + amount
        val userKey = getUserSpecificKey(KEY_MONTHLY_INCOME)
        sharedPreferences.edit().putFloat(userKey, newIncome.toFloat()).apply()
    }

    fun updateMonthlyExpenses(amount: Double) {
        val currentExpenses = getMonthlyExpenses()
        val newExpenses = currentExpenses + amount
        val userKey = getUserSpecificKey(KEY_MONTHLY_EXPENSES)
        sharedPreferences.edit().putFloat(userKey, newExpenses.toFloat()).apply()
    }

    fun updateCategoryExpense(category: Category, amount: Double) {
        val categoryExpenses = getCategoryExpensesFromPrefs()
        val currentAmount = categoryExpenses[category] ?: 0.0
        categoryExpenses[category] = currentAmount + amount
        saveCategoryExpenses(categoryExpenses)
    }

    fun setCategoryExpense(category: Category, amount: Double) {
        val categoryExpenses = getCategoryExpensesFromPrefs()
        categoryExpenses[category] = amount
        saveCategoryExpenses(categoryExpenses)
    }

    private fun getCategoryExpensesFromPrefs(): MutableMap<Category, Double> {
        val userKey = getUserSpecificKey(KEY_CATEGORY_EXPENSES)
        val json = sharedPreferences.getString(userKey, "{}")
        val type = object : TypeToken<Map<String, Double>>() {}.type
        val stringMap: Map<String, Double> = gson.fromJson(json, type) ?: emptyMap()

        // Convert string keys to Category enum
        return stringMap.entries.associate {
            Category.valueOf(it.key) to it.value
        }.toMutableMap()
    }

    private fun saveCategoryExpenses(expenses: Map<Category, Double>) {
        // Convert Category enum keys to strings
        val stringMap = expenses.entries.associate {
            it.key.name to it.value
        }
        val json = gson.toJson(stringMap)
        val userKey = getUserSpecificKey(KEY_CATEGORY_EXPENSES)
        sharedPreferences.edit().putString(userKey, json).apply()
    }

    fun deleteTransaction(transactionId: String) {
        val transactions = getTransactions().toMutableList()
        val transaction = transactions.find { it.id == transactionId }

        if (transaction != null) {
            transactions.remove(transaction)
            val json = gson.toJson(transactions)
            val userKey = getUserSpecificKey(KEY_TRANSACTIONS)
            sharedPreferences.edit().putString(userKey, json).apply()

            // Update total balance
            val currentBalance = getTotalBalance()
            val newBalance = when (transaction.type) {
                TransactionType.INCOME -> currentBalance - transaction.amount
                TransactionType.EXPENSE -> currentBalance + transaction.amount
            }
            setTotalBalance(newBalance)

            // Update monthly budget if it was an income transaction
            if (transaction.type == TransactionType.INCOME) {
                val currentBudget = getMonthlyBudget()
                setMonthlyBudget(currentBudget - transaction.amount)
            }

            // Update category expenses for UI purposes if it was an expense
            if (transaction.type == TransactionType.EXPENSE) {
                updateCategoryExpense(transaction.category, -transaction.amount)

                // Update budget left if budget has been reset
                if (isBudgetReset()) {
                    val currentBudgetLeft = getBudgetLeft()
                    setBudgetLeft(currentBudgetLeft + transaction.amount)
                }
            } else if (transaction.type == TransactionType.INCOME) {
                // Update budget left if budget has been reset and it's not a salary income
                if (isBudgetReset() && transaction.category != Category.SALARY) {
                    val currentBudgetLeft = getBudgetLeft()
                    setBudgetLeft(currentBudgetLeft - transaction.amount)
                    Log.d("Budget", "Income (${transaction.category}) removed from budget left: -${transaction.amount}")
                } else if (transaction.category == Category.SALARY) {
                    Log.d("Budget", "Salary income not removed from budget left: ${transaction.amount}")
                }
            }
        }
    }

    // Category Budget Methods
    fun saveEntertainmentBudget(amount: Double) {
        val userKey = getUserSpecificKey(KEY_ENTERTAINMENT_BUDGET)
        sharedPreferences.edit().putFloat(userKey, amount.toFloat()).apply()
    }

    fun saveFoodBudget(amount: Double) {
        val userKey = getUserSpecificKey(KEY_FOOD_BUDGET)
        sharedPreferences.edit().putFloat(userKey, amount.toFloat()).apply()
    }

    fun saveTransportBudget(amount: Double) {
        val userKey = getUserSpecificKey(KEY_TRANSPORT_BUDGET)
        sharedPreferences.edit().putFloat(userKey, amount.toFloat()).apply()
    }

    fun saveLifestyleBudget(amount: Double) {
        val userKey = getUserSpecificKey(KEY_LIFESTYLE_BUDGET)
        sharedPreferences.edit().putFloat(userKey, amount.toFloat()).apply()
    }

    fun getEntertainmentBudget(): Double {
        val userKey = getUserSpecificKey(KEY_ENTERTAINMENT_BUDGET)
        return sharedPreferences.getFloat(userKey, 10000f).toDouble()
    }

    fun getFoodBudget(): Double {
        val userKey = getUserSpecificKey(KEY_FOOD_BUDGET)
        return sharedPreferences.getFloat(userKey, 5000f).toDouble()
    }

    fun getTransportBudget(): Double {
        val userKey = getUserSpecificKey(KEY_TRANSPORT_BUDGET)
        return sharedPreferences.getFloat(userKey, 1000f).toDouble()
    }

    fun getLifestyleBudget(): Double {
        val userKey = getUserSpecificKey(KEY_LIFESTYLE_BUDGET)
        return sharedPreferences.getFloat(userKey, 5000f).toDouble()
    }

    fun saveCategoryBudget(category: String, amount: Double) {
        when (category) {
            "Entertainment" -> saveEntertainmentBudget(amount)
            "Food" -> saveFoodBudget(amount)
            "Transport" -> saveTransportBudget(amount)
            "Lifestyle" -> saveLifestyleBudget(amount)
        }
    }

    fun getCategoryBudget(category: String): Double {
        return when (category) {
            "Entertainment" -> getEntertainmentBudget()
            "Food" -> getFoodBudget()
            "Transport" -> getTransportBudget()
            "Lifestyle" -> getLifestyleBudget()
            else -> 0.0
        }
    }

    fun clearAllData() {
        // Only clear data for the current user
        val email = currentUserEmail ?: return

        // Get all keys in SharedPreferences
        val allPrefs = sharedPreferences.all
        val editor = sharedPreferences.edit()

        // Remove only keys that start with the current user's email
        for (key in allPrefs.keys) {
            if (key.startsWith("${email}_")) {
                editor.remove(key)
            }
        }

        // Apply changes
        editor.apply()
        Log.d("PreferencesManager", "Cleared all data for user: $email")
    }
}