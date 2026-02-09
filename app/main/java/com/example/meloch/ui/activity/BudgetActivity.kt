package com.example.meloch.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.meloch.R
import com.example.meloch.data.Budget
import com.example.meloch.data.BudgetResetHelper
import com.example.meloch.data.PreferencesManager
import com.example.meloch.data.model.Category
import com.example.meloch.databinding.ActivityBudgetBinding
import com.example.meloch.ui.dialog.EditBudgetDialogFragment
import com.example.meloch.util.NotificationHelper
import java.text.NumberFormat
import java.util.*

class BudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var budgetResetHelper: BudgetResetHelper
    private val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("LKR")
    }

    // Budget data
    private var entertainmentBudget = 0.0
    private var foodBudget = 0.0
    private var transportBudget = 0.0
    private var lifestyleBudget = 0.0

    // Spent amounts (for demo purposes)
    private var entertainmentSpent = 5450.30
    private var foodSpent = 5450.30
    private var transportSpent = 0.0
    private var lifestyleSpent = 5450.30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize helpers
        preferencesManager = PreferencesManager(this)
        notificationHelper = NotificationHelper(this)
        budgetResetHelper = BudgetResetHelper(this)

        // Load budget values from SharedPreferences
        loadBudgetValues()

        setupToolbar()
        setupBudgetSections()
        updateTotalBudget()
    }

    private fun loadBudgetValues() {
        entertainmentBudget = preferencesManager.getEntertainmentBudget()
        foodBudget = preferencesManager.getFoodBudget()
        transportBudget = preferencesManager.getTransportBudget()
        lifestyleBudget = preferencesManager.getLifestyleBudget()

        // TODO: In a real app, we would also load the actual spent amounts from transactions
        // For now, we'll keep the demo values
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            // Set result to indicate data has changed
            setResult(RESULT_OK)
            finish()
        }

        binding.addBudgetButton.setOnClickListener {
            showAddBudgetDialog()
        }

        // Setup Reset Budget button
        binding.resetBudgetButton.setOnClickListener {
            showResetBudgetConfirmationDialog()
        }
    }

    private fun showResetBudgetConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Budget")
            .setMessage("Are you sure you want to reset your budget? This will reset all category expenses to zero.")
            .setPositiveButton("Reset") { _, _ ->
                resetBudget()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetBudget() {
        // IMPORTANT: Get the current budget and balance BEFORE any changes
        val currentBudget = preferencesManager.getMonthlyBudget()
        val currentBalance = preferencesManager.getTotalBalance()

        // Log the values for debugging
        Log.d("BudgetReset", "DIRECT RESET: Current budget: $currentBudget")
        Log.d("BudgetReset", "DIRECT RESET: Current balance: $currentBalance")

        // IMPORTANT: Directly subtract the budget amount from the total balance
        val newBalance = currentBalance - currentBudget
        Log.d("BudgetReset", "DIRECT RESET: New balance after subtracting budget: $newBalance")

        // Update the total balance
        preferencesManager.setTotalBalance(newBalance)

        // Verify the balance was updated
        val verifiedBalance = preferencesManager.getTotalBalance()
        Log.d("BudgetReset", "DIRECT RESET: Verified balance after update: $verifiedBalance")

        // Now reset the budget without updating the balance again
        preferencesManager.resetBudgetWithoutBalanceUpdate()

        // Reset spent amounts in the UI
        entertainmentSpent = 0.0
        foodSpent = 0.0
        transportSpent = 0.0
        lifestyleSpent = 0.0

        // Update UI
        setupBudgetSections()
        updateTotalBudget()

        // Show notification
        notificationHelper.showBudgetResetNotification(currentBudget, "LKR")

        // Show success message
        Toast.makeText(this, "Budget has been reset successfully", Toast.LENGTH_SHORT).show()

        // Set result to indicate data has changed
        setResult(RESULT_OK)
    }

    private fun setupBudgetSections() {
        // Entertainment section
        updateBudgetSection(
            binding.entertainmentTitle,
            binding.entertainmentAmount,
            binding.entertainmentProgressBar,
            binding.entertainmentSpent,
            binding.entertainmentLeft,
            binding.entertainmentPercentage,
            entertainmentBudget,
            entertainmentSpent,
            R.color.category_entertainment
        )

        binding.entertainmentMenu.setOnClickListener { view ->
            showBudgetMenu(view, "Entertainment", entertainmentBudget)
        }

        // Food section
        updateBudgetSection(
            binding.foodTitle,
            binding.foodAmount,
            binding.foodProgressBar,
            binding.foodSpent,
            binding.foodLeft,
            binding.foodPercentage,
            foodBudget,
            foodSpent,
            R.color.category_food
        )

        binding.foodMenu.setOnClickListener { view ->
            showBudgetMenu(view, "Food", foodBudget)
        }

        // Transport section
        updateBudgetSection(
            binding.transportTitle,
            binding.transportAmount,
            binding.transportProgressBar,
            binding.transportSpent,
            binding.transportLeft,
            binding.transportPercentage,
            transportBudget,
            transportSpent,
            R.color.category_transport
        )

        binding.transportMenu.setOnClickListener { view ->
            showBudgetMenu(view, "Transport", transportBudget)
        }

        // Lifestyle section
        updateBudgetSection(
            binding.lifestyleTitle,
            binding.lifestyleAmount,
            binding.lifestyleProgressBar,
            binding.lifestyleSpent,
            binding.lifestyleLeft,
            binding.lifestylePercentage,
            lifestyleBudget,
            lifestyleSpent,
            R.color.category_shopping
        )

        binding.lifestyleMenu.setOnClickListener { view ->
            showBudgetMenu(view, "Lifestyle", lifestyleBudget)
        }
    }

    private fun updateBudgetSection(
        titleView: android.widget.TextView,
        amountView: android.widget.TextView,
        progressBar: android.widget.ProgressBar,
        spentView: android.widget.TextView,
        leftView: android.widget.TextView,
        percentageView: android.widget.TextView,
        budget: Double,
        spent: Double,
        colorResId: Int
    ) {
        amountView.text = currencyFormatter.format(budget)
        spentView.text = "-${currencyFormatter.format(spent)} spent"

        val remaining = budget - spent
        val percentage = (spent / budget * 100).toInt()

        if (remaining >= 0) {
            leftView.text = "${currencyFormatter.format(remaining)} left"
            leftView.setTextColor(getColor(R.color.text_primary))
            percentageView.text = "%$percentage"
        } else {
            leftView.text = "${currencyFormatter.format(Math.abs(remaining))} overspending"
            leftView.setTextColor(getColor(R.color.expense_red))
            percentageView.text = "-%$percentage"
            percentageView.setTextColor(getColor(R.color.expense_red))
        }

        progressBar.progress = if (percentage > 100) 100 else percentage
        titleView.setTextColor(getColor(colorResId))
    }

    private fun updateTotalBudget() {
        val totalBudget = entertainmentBudget + foodBudget + transportBudget + lifestyleBudget
        val totalSpent = entertainmentSpent + foodSpent + transportSpent + lifestyleSpent
        val remaining = totalBudget - totalSpent
        val percentage = if (totalBudget > 0) (totalSpent / totalBudget * 100).toInt() else 0

        // Save the total budget to PreferencesManager
        preferencesManager.setMonthlyBudget(totalBudget)

        // Update UI
        binding.totalBudgetAmount.text = currencyFormatter.format(totalBudget)
        binding.totalBudgetSpent.text = "-${currencyFormatter.format(totalSpent)} spent"
        binding.totalBudgetLeft.text = "${currencyFormatter.format(remaining)} left"
        binding.totalBudgetPercentage.text = "%$percentage"
        binding.totalBudgetProgressBar.progress = percentage
    }

    private fun showBudgetMenu(view: View, category: String, currentAmount: Double) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.budget_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    showEditBudgetDialog(category, currentAmount)
                    true
                }
                R.id.action_delete -> {
                    deleteBudget(category)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showAddBudgetDialog() {
        val dialog = EditBudgetDialogFragment(null, 0.0) { category, amount ->
            addBudget(category, amount)
        }
        dialog.show(supportFragmentManager, "AddBudgetDialog")
    }

    private fun showEditBudgetDialog(category: String, currentAmount: Double) {
        val dialog = EditBudgetDialogFragment(category, currentAmount) { _, amount ->
            updateBudget(category, amount)
        }
        dialog.show(supportFragmentManager, "EditBudgetDialog")
    }

    private fun addBudget(category: String, amount: Double) {
        // Update local variable
        when (category) {
            "Entertainment" -> entertainmentBudget = amount
            "Food" -> foodBudget = amount
            "Transport" -> transportBudget = amount
            "Lifestyle" -> lifestyleBudget = amount
        }

        // Save to SharedPreferences
        preferencesManager.saveCategoryBudget(category, amount)

        // Update UI
        setupBudgetSections()
        updateTotalBudget()
    }

    private fun updateBudget(category: String, amount: Double) {
        // Update local variable
        when (category) {
            "Entertainment" -> entertainmentBudget = amount
            "Food" -> foodBudget = amount
            "Transport" -> transportBudget = amount
            "Lifestyle" -> lifestyleBudget = amount
        }

        // Save to SharedPreferences
        preferencesManager.saveCategoryBudget(category, amount)

        // Update UI
        setupBudgetSections()
        updateTotalBudget()
    }

    private fun deleteBudget(category: String) {
        // Update local variable
        when (category) {
            "Entertainment" -> entertainmentBudget = 0.0
            "Food" -> foodBudget = 0.0
            "Transport" -> transportBudget = 0.0
            "Lifestyle" -> lifestyleBudget = 0.0
        }

        // Save to SharedPreferences
        preferencesManager.saveCategoryBudget(category, 0.0)

        // Update UI
        setupBudgetSections()
        updateTotalBudget()
    }
}
