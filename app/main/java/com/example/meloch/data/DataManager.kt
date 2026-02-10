package com.example.meloch.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class DataManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("meloch_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Transaction operations
    fun saveTransaction(transaction: Transaction) {
        val transactions = getTransactions().toMutableList()
        transactions.add(transaction)
        saveTransactions(transactions)
    }

    fun getTransactions(): List<Transaction> {
        val json = sharedPreferences.getString(KEY_TRANSACTIONS, "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun getTransactionsByType(type: TransactionType): List<Transaction> {
        return getTransactions().filter { it.type == type }
    }

    fun getTransactionsByCategory(category: String): List<Transaction> {
        return getTransactions().filter { it.category == category }
    }

    fun getTotalExpensesForMonth(month: Int, year: Int): Double {
        return getTransactions()
            .filter { 
                it.type == TransactionType.EXPENSE && 
                it.date.month + 1 == month && 
                it.date.year + 1900 == year 
            }
            .sumOf { it.amount }
    }

    private fun saveTransactions(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString(KEY_TRANSACTIONS, json).apply()
    }

    // Budget operations
    fun saveBudget(budget: Budget) {
        val budgets = getBudgets().toMutableList()
        budgets.add(budget)
        saveBudgets(budgets)
    }

    fun getBudgets(): List<Budget> {
        val json = sharedPreferences.getString(KEY_BUDGETS, "[]")
        val type = object : TypeToken<List<Budget>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun getBudgetsForMonth(month: Int, year: Int): List<Budget> {
        return getBudgets().filter { it.month == month && it.year == year }
    }

    fun getBudgetForCategory(category: String, month: Int, year: Int): Budget? {
        return getBudgets().find { 
            it.category == category && it.month == month && it.year == year 
        }
    }

    private fun saveBudgets(budgets: List<Budget>) {
        val json = gson.toJson(budgets)
        sharedPreferences.edit().putString(KEY_BUDGETS, json).apply()
    }

    // User preferences
    fun saveCurrency(currency: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY, currency).apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, "LKR") ?: "LKR"
    }

    companion object {
        private const val KEY_TRANSACTIONS = "transactions"
        private const val KEY_BUDGETS = "budgets"
        private const val KEY_CURRENCY = "currency"
    }
} 