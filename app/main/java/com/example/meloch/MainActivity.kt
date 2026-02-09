package com.example.meloch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.meloch.data.UserManager
import com.example.meloch.ui.activity.LoginActivity
import com.example.meloch.ui.activity.StatisticsActivity
import com.example.meloch.ui.chart.RoundedPieChartRenderer
import com.example.meloch.ui.fragment.ProfileFragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meloch.data.CardManager
import com.example.meloch.data.PreferencesManager
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.Transaction
import com.example.meloch.databinding.ActivityMainBinding
import com.example.meloch.ui.activity.BudgetActivity
import com.example.meloch.ui.adapter.CategoryLegendAdapter
import com.example.meloch.ui.dialog.AddRecordDialogFragment
import com.example.meloch.ui.dialog.PocketMoneyDialogFragment
import com.example.meloch.ui.fragment.RecordsFragment
import com.example.meloch.ui.fragment.WalletCardsFragment
import com.example.meloch.util.NotificationHelper
import com.example.meloch.viewmodel.TransactionViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.chip.Chip
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var cardManager: CardManager
    private lateinit var viewModel: TransactionViewModel
    private lateinit var categoryLegendAdapter: CategoryLegendAdapter
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var userManager: UserManager
    private val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("LKR")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userManager = UserManager(this)

        // Log the current user
        val currentUser = userManager.getCurrentUser()
        Log.d("MainActivity", "Initializing for user: ${currentUser?.email}")

        // Initialize other managers with the current user context
        preferencesManager = PreferencesManager(this)
        cardManager = CardManager(this)
        viewModel = TransactionViewModel()
        notificationHelper = NotificationHelper(this)

        // Observe transactions for changes
        viewModel.transactions.observe(this) { transactions ->
            updateDashboard()

            // Notify the RecordsFragment if it's currently visible
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment is RecordsFragment) {
                currentFragment.refreshTransactions()
            }
        }

        setupUI()
        setupBottomNavigation()
        showHomeScreen()
        updateDashboard()

        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    private fun setupUI() {
        binding.categoryPieChart.apply {
            setNoDataText("No expense data available")
            setNoDataTextColor(getColor(R.color.text_secondary))

            renderer = RoundedPieChartRenderer(this, animator, viewPortHandler)
        }

        categoryLegendAdapter = CategoryLegendAdapter()
        binding.categoryLegendRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.categoryLegendRecyclerView.adapter = categoryLegendAdapter

        // Setup FAB
        binding.addTransactionFab.setOnClickListener {
            showAddRecordDialog()
        }

        binding.allBudget.setOnClickListener {
            val intent = Intent(this, BudgetActivity::class.java)
            startActivityForResult(intent, REQUEST_BUDGET_ACTIVITY)
        }

        binding.statisticsButton.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    showHomeScreen()
                    true
                }
                R.id.navigation_records -> {
                    loadFragment(RecordsFragment())
                    true
                }
                R.id.navigation_add -> {
                    // Show the pocket money dialog
                    showPocketMoneyDialog()
                    false // Return false to prevent selecting this item
                }
                R.id.navigation_cards -> {
                    loadFragment(WalletCardsFragment())
                    true
                }
                R.id.navigation_menu -> {
                    // Load profile fragment
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun showPocketMoneyDialog() {
        val currentAmount = cardManager.getPocketMoney()
        val dialog = PocketMoneyDialogFragment(currentAmount) { amount ->
            cardManager.savePocketMoney(amount)
            updateDashboard()
            Toast.makeText(this, "Pocket money updated", Toast.LENGTH_SHORT).show()
        }
        dialog.show(supportFragmentManager, PocketMoneyDialogFragment.TAG)
    }

    fun showAddRecordDialog() {
        val dialog = AddRecordDialogFragment.newInstance(viewModel)
        dialog.show(supportFragmentManager, "AddRecordDialog")
    }

    private fun loadFragment(fragment: Fragment) {
        binding.homeScrollView.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun showHomeScreen() {
        binding.homeScrollView.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE
    }

    fun updateDashboard() {
        // Force reload data from SharedPreferences
        preferencesManager = PreferencesManager(this)

        // Log the current user for debugging
        val currentUser = userManager.getCurrentUser()
        Log.d("MainActivity", "Updating dashboard for user: ${currentUser?.email}")

        // Reinitialize CardManager to ensure it has the latest user context
        cardManager = CardManager(this)

        // Get card and pocket money balances
        val cardsBalance = cardManager.getCards().sumOf { it.balance }
        val pocketMoney = cardManager.getPocketMoney()

        Log.d("MainActivity", "Cards balance: $cardsBalance, Pocket money: $pocketMoney")

        // Get income and expense totals
        val monthlyIncome = preferencesManager.getMonthlyIncome()
        val monthlyExpenses = preferencesManager.getMonthlyExpenses()

        // Calculate total balance including transactions
        val totalBalance = cardsBalance + pocketMoney + monthlyIncome - monthlyExpenses

        binding.totalBalanceText.text = currencyFormatter.format(totalBalance)

        // Update balance breakdown
        val breakdownText = "Cards: ${currencyFormatter.format(cardsBalance)} | " +
                          "Pocket: ${currencyFormatter.format(pocketMoney)} | " +
                          "Income: ${currencyFormatter.format(monthlyIncome)} | " +
                          "Expenses: ${currencyFormatter.format(monthlyExpenses)}"
        binding.balanceBreakdownText.text = breakdownText

        // Update budget information
        val monthlyBudget = preferencesManager.getMonthlyBudget()

        val remainingBudget = if (preferencesManager.isBudgetReset()) {
            preferencesManager.getBudgetLeft()
        } else {
            monthlyBudget - monthlyExpenses
        }

        // Log values for debugging
        Log.d("Budget", "Monthly Budget: $monthlyBudget")
        Log.d("Budget", "Monthly Income: $monthlyIncome")
        Log.d("Budget", "Monthly Expenses: $monthlyExpenses")
        Log.d("Budget", "Remaining Budget: $remainingBudget")

        // Check if there are any transactions for this month
        val hasTransactions = monthlyIncome > 0 || monthlyExpenses > 0

        // Update UI
        binding.remainingBudgetText.text = "${currencyFormatter.format(remainingBudget)} left"
        binding.spentAmountText.text = "-${currencyFormatter.format(monthlyExpenses)} spent this month"

        // Only apply special styling and notifications if there are transactions
        if (hasTransactions) {
            // Change text color to orange when budget is zero
            if (remainingBudget <= 0) {
                binding.remainingBudgetText.setTextColor(getColor(R.color.orange))

                // Show budget zero notification
                notificationHelper.showBudgetZeroNotification("LKR")
                // Cancel the low budget notification
                notificationHelper.cancelBudgetAlertNotification()
            } else {
                // Reset text color to normal
                binding.remainingBudgetText.setTextColor(getColor(R.color.text_primary))

                // Cancel the zero budget notification
                notificationHelper.cancelBudgetZeroNotification()

                // Check if budget is below threshold (LKR 5000) and show notification
                val budgetThreshold = 5000.0
                if (remainingBudget < budgetThreshold) {
                    // Show budget alert notification
                    notificationHelper.showBudgetAlertNotification(remainingBudget, "LKR")
                } else {
                    // Cancel notification if budget is above threshold
                    notificationHelper.cancelBudgetAlertNotification()
                }
            }
        } else {
            // For new users with no transactions, use normal styling
            binding.remainingBudgetText.setTextColor(getColor(R.color.text_primary))
            // Cancel any notifications
            notificationHelper.cancelBudgetAlertNotification()
            notificationHelper.cancelBudgetZeroNotification()
        }

        // Calculate progress based on expenses relative to budget or income
        val progressBase = if (monthlyIncome > 0) monthlyIncome else monthlyBudget
        val progress = if (preferencesManager.isBudgetReset()) {
            // Get expenses since the last reset
            val expensesSinceReset = preferencesManager.getExpensesSinceLastReset().values.sum()
            Log.d("Progress", "Expenses since last reset: $expensesSinceReset")

            // Get the budget left value
            val budgetLeft = preferencesManager.getBudgetLeft()
            Log.d("Progress", "Budget left: $budgetLeft, Monthly budget: $monthlyBudget")

            // Calculate progress based on expenses since reset relative to total budget
            if (monthlyBudget > 0) {
                val progressValue = (expensesSinceReset / monthlyBudget * 100).toInt()
                Log.d("Progress", "Progress value: $progressValue%")
                progressValue
            } else {
                0
            }
        } else {
            // Calculate progress normally
            if (progressBase > 0) ((monthlyExpenses / progressBase) * 100).toInt() else 0
        }
        binding.budgetProgressBar.progress = progress

        // Update category chips
        updateCategoryChips()

        // Update pie chart
        updatePieChart()

        // Force layout refresh
        binding.root.invalidate()
    }

    private fun updateCategoryChips() {
        binding.categoryChipGroup.removeAllViews()

        // Get expenses based on whether budget has been reset
        val categoryExpenses = if (preferencesManager.isBudgetReset()) {
            // Get expenses since the last reset
            val expensesSinceReset = preferencesManager.getExpensesSinceLastReset()
            Log.d("CategoryChips", "Expenses since last reset: $expensesSinceReset")

            // If no expenses since reset, don't show any category chips
            if (expensesSinceReset.isEmpty()) {
                return
            }

            expensesSinceReset
        } else {
            // Get all expenses for the current month
            preferencesManager.getCategoryExpenses()
        }

        // Filter out income categories
        val expenseCategories = categoryExpenses.filter { (category, _) ->
            category != Category.SALARY && category != Category.SIDE_BUSINESS
        }

        // Add chips for each expense category
        expenseCategories.forEach { (category, amount) ->
            val chip = Chip(this).apply {
                text = "${category.displayName} - ${currencyFormatter.format(amount)}"
                setChipBackgroundColorResource(getCategoryColor(category))
                setTextColor(getColor(R.color.text_primary))
            }
            binding.categoryChipGroup.addView(chip)
        }
    }

    private fun updatePieChart() {
        // Get expenses based on whether budget has been reset
        val categoryExpenses = if (preferencesManager.isBudgetReset()) {
            // Get expenses since the last reset
            val expensesSinceReset = preferencesManager.getExpensesSinceLastReset()
            Log.d("PieChart", "Expenses since last reset: $expensesSinceReset")

            // If no expenses since reset, show empty pie chart
            if (expensesSinceReset.isEmpty()) {
                binding.categoryPieChart.setNoDataText("No expense data available")
                binding.categoryPieChart.setNoDataTextColor(getColor(R.color.text_secondary))
                binding.categoryPieChart.clear()
                binding.categoryPieChart.invalidate()

                // Clear the category legend
                categoryLegendAdapter.updateItems(emptyMap())
                return
            }

            expensesSinceReset
        } else {
            // Get all expenses for the current month
            preferencesManager.getCategoryExpenses()
        }

        // Filter out income categories and only show expense categories
        val expenseCategories = categoryExpenses.filter { (category, _) ->
            category != Category.SALARY && category != Category.SIDE_BUSINESS
        }

        // Calculate total expenses for center text
        val totalExpenses = expenseCategories.values.sum()

        // Create pie entries
        val entries = expenseCategories.map { (category, amount) ->
            PieEntry(amount.toFloat(), category.displayName)
        }

        if (entries.isEmpty()) {
            // If no expense data, show empty state
            binding.categoryPieChart.setNoDataText("No expense data available")
            binding.categoryPieChart.setNoDataTextColor(getColor(R.color.text_secondary))
            binding.categoryPieChart.invalidate()
            return
        }

        // Create and configure the dataset
        val dataSet = PieDataSet(entries, "Expenses by Category").apply {
            // Create a list of actual color integers for each category
            val colorList = ArrayList<Int>()

            // Use hardcoded color values to ensure they're applied correctly
            colorList.add(Color.parseColor("#FF9800"))  // Food
            colorList.add(Color.parseColor("#4CAF50"))  // Entertainment
            colorList.add(Color.parseColor("#2196F3"))  // Transport
            colorList.add(Color.parseColor("#F44336"))  // Health
            colorList.add(Color.parseColor("#9C27B0"))  // Shopping
            colorList.add(Color.parseColor("#3F51B5"))  // Vacation (Indigo)

            // Map colors to entries based on category
            val mappedColors = ArrayList<Int>()
            expenseCategories.keys.forEach { category ->
                when (category) {
                    Category.FOOD -> mappedColors.add(Color.parseColor("#FF9800"))
                    Category.ENTERTAINMENT -> mappedColors.add(Color.parseColor("#4CAF50"))
                    Category.TRANSPORT -> mappedColors.add(Color.parseColor("#2196F3"))
                    Category.HEALTH -> mappedColors.add(Color.parseColor("#F44336"))
                    Category.SHOPPING -> mappedColors.add(Color.parseColor("#9C27B0"))
                    Category.VACATION -> mappedColors.add(Color.parseColor("#3F51B5"))
                    else -> mappedColors.add(Color.parseColor("#7A5FFF"))
                }
            }

            // Log colors for debugging
            Log.d("PieChart", "Mapped colors size: ${mappedColors.size}")
            mappedColors.forEachIndexed { index, color ->
                Log.d("PieChart", "Color $index: $color")
            }

            // Use the mapped colors for the dataset
            colors = mappedColors
            setDrawValues(false)

            // Increase slice space to create rounded corners effect
            sliceSpace = 3f

            // Add more visual appeal
            selectionShift = 5f

            // No cornerRadius property in this version of MPAndroidChart
        }

        // Configure the pie data
        val pieData = PieData(dataSet).apply {
            setValueTextSize(12f)
            setValueTextColor(getColor(R.color.text_primary))
        }

        // Update the pie chart
        binding.categoryPieChart.apply {
            // Clear any existing data
            clear()

            // Set new data
            data = pieData

            // Set center text
            centerText = "Expenses\n${currencyFormatter.format(totalExpenses)}"

            // Make sure we're using the custom renderer with transparent hole
            if (renderer !is RoundedPieChartRenderer) {
                renderer = RoundedPieChartRenderer(this, animator, viewPortHandler)
            }

            // Animate and refresh
            animateY(2000)
            invalidate()

            // Log for debugging
            Log.d("PieChart", "Pie chart updated with ${entries.size} entries")
        }

        // Update the category legend
        categoryLegendAdapter.updateItems(expenseCategories)
    }

    private fun getCategoryColor(category: Category): Int {
        return when (category) {
            Category.FOOD -> R.color.category_food
            Category.ENTERTAINMENT -> R.color.category_entertainment
            Category.TRANSPORT -> R.color.category_transport
            Category.HEALTH -> R.color.category_health
            Category.SHOPPING -> R.color.category_shopping
            Category.SALARY -> R.color.primary
            Category.SIDE_BUSINESS -> R.color.category_side_business
            Category.VACATION -> R.color.primary
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_BUDGET_ACTIVITY && resultCode == RESULT_OK) {
            // Budget data has changed, update the dashboard
            updateDashboard()
        }
    }

    companion object {
        private const val REQUEST_BUDGET_ACTIVITY = 1001
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Register the permissions callback
                val requestPermissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        // Permission granted, update dashboard to show notification if needed
                        updateDashboard()
                    } else {
                        // Permission denied, inform user that they won't receive notifications
                        Toast.makeText(
                            this,
                            "You won't receive budget alerts without notification permission",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}