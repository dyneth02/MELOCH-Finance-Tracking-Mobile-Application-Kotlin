package com.example.meloch.data

import android.content.Context
import android.util.Log
import com.example.meloch.util.NotificationHelper

/**
 * Helper class specifically for handling budget reset operations
 */
class BudgetResetHelper(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val notificationHelper = NotificationHelper(context)

    /**
     * Resets the budget and subtracts the budget amount from the total balance
     * @return true if reset was successful
     */
    fun resetBudgetAndUpdateBalance(): Boolean {
        try {
            // Get current values before reset
            val currentBudget = preferencesManager.getMonthlyBudget()
            val currentBalance = preferencesManager.getTotalBalance()

            Log.d("BudgetReset", "Starting budget reset operation")
            Log.d("BudgetReset", "Current budget: $currentBudget")
            Log.d("BudgetReset", "Current balance: $currentBalance")

            // IMPORTANT: Directly subtract the budget amount from the total balance
            // This is the key operation that needs to happen
            val newBalance = currentBalance - currentBudget
            Log.d("BudgetReset", "New balance after subtracting budget: $newBalance")

            // Update total balance BEFORE resetting the budget
            preferencesManager.setTotalBalance(newBalance)
            Log.d("BudgetReset", "Total balance updated to: $newBalance")

            // Verify the balance was updated
            val balanceAfterUpdate = preferencesManager.getTotalBalance()
            Log.d("BudgetReset", "Verified balance after update: $balanceAfterUpdate")

            // Now reset the budget in PreferencesManager
            // But tell it NOT to modify the balance again
            preferencesManager.resetBudgetWithoutBalanceUpdate()

            // Verify the changes again
            val finalBalance = preferencesManager.getTotalBalance()
            val budgetLeft = preferencesManager.getBudgetLeft()

            Log.d("BudgetReset", "Final balance after reset: $finalBalance")
            Log.d("BudgetReset", "Budget left after reset: $budgetLeft")

            // Show notification
            notificationHelper.showBudgetResetNotification(currentBudget, "LKR")

            return true
        } catch (e: Exception) {
            Log.e("BudgetReset", "Error resetting budget", e)
            return false
        }
    }
}
