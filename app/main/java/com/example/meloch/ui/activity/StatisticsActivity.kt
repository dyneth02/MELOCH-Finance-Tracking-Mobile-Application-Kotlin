package com.example.meloch.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meloch.R
import com.example.meloch.data.PreferencesManager
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.Transaction
import com.example.meloch.data.model.TransactionType
import com.example.meloch.databinding.ActivityStatisticsBinding
import com.example.meloch.ui.adapter.CategoryLegendAdapter
import com.example.meloch.ui.chart.RoundedPieChartRenderer
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var categoryLegendAdapter: CategoryLegendAdapter
    private lateinit var currencyFormatter: NumberFormat

    private var currentMonth = 0
    private var currentYear = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize PreferencesManager
        preferencesManager = PreferencesManager(this)

        // Initialize currency formatter
        currencyFormatter = NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(preferencesManager.getCurrency())
        }

        // Set current month and year
        val calendar = Calendar.getInstance()
        currentMonth = calendar.get(Calendar.MONTH)
        currentYear = calendar.get(Calendar.YEAR)

        // Setup UI
        setupToolbar()
        setupMonthSelector()
        setupCategoryLegend()

        // Load data for current month
        loadMonthData()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupMonthSelector() {
        updateMonthYearText()

        binding.prevMonthButton.setOnClickListener {
            // Go to previous month
            if (currentMonth == 0) {
                currentMonth = 11
                currentYear--
            } else {
                currentMonth--
            }
            updateMonthYearText()
            loadMonthData()
        }

        binding.nextMonthButton.setOnClickListener {
            // Go to next month
            if (currentMonth == 11) {
                currentMonth = 0
                currentYear++
            } else {
                currentMonth++
            }
            updateMonthYearText()
            loadMonthData()
        }
    }

    private fun updateMonthYearText() {
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth, 1)
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.monthYearText.text = dateFormat.format(calendar.time)
    }

    private fun setupCategoryLegend() {
        categoryLegendAdapter = CategoryLegendAdapter()
        binding.categoryLegendRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@StatisticsActivity)
            adapter = categoryLegendAdapter
        }
    }

    private fun loadMonthData() {
        // Get transactions for the selected month
        val transactions = getTransactionsForMonth(currentMonth, currentYear)

        // Calculate income and expense totals
        val incomeTotal = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val expenseTotal = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val balance = incomeTotal - expenseTotal

        // Update summary texts
        binding.totalIncomeText.text = currencyFormatter.format(incomeTotal)
        binding.totalExpenseText.text = currencyFormatter.format(expenseTotal)
        binding.balanceText.text = currencyFormatter.format(balance)

        // Set balance text color based on value
        binding.balanceText.setTextColor(
            if (balance >= 0) getColor(R.color.income_green) else getColor(R.color.expense_red)
        )

        // Update charts
        setupIncomeExpenseChart(incomeTotal, expenseTotal)
        setupCategoryPieChart(transactions)
        setupDailyTrendChart(transactions)
    }

    private fun getTransactionsForMonth(month: Int, year: Int): List<Transaction> {
        val allTransactions = preferencesManager.getTransactions()

        return allTransactions.filter { transaction ->
            val calendar = Calendar.getInstance().apply { time = transaction.date }
            calendar.get(Calendar.MONTH) == month && calendar.get(Calendar.YEAR) == year
        }
    }

    private fun setupIncomeExpenseChart(incomeTotal: Double, expenseTotal: Double) {
        val barChart: BarChart = binding.incomeExpenseChart

        // Create entries
        val entries = listOf(
            BarEntry(0f, incomeTotal.toFloat()),
            BarEntry(1f, expenseTotal.toFloat())
        )

        val dataSet = BarDataSet(entries, "Income vs Expense")
        dataSet.colors = listOf(getColor(R.color.income_green), getColor(R.color.expense_red))
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barChart.data = barData

        // Customize chart
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawValueAboveBar(true)

        // X-axis customization
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.granularity = 1f
        xAxis.textColor = Color.WHITE
        xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense"))

        // Y-axis customization
        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisRight.isEnabled = false

        // Animate and refresh
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun setupCategoryPieChart(transactions: List<Transaction>) {
        val pieChart: PieChart = binding.categoryPieChart

        // Set custom renderer first to ensure styling is applied
        pieChart.renderer = RoundedPieChartRenderer(pieChart, pieChart.animator, pieChart.viewPortHandler)

        // Filter expense transactions and group by category
        val expensesByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

        // If no expenses, show empty chart
        if (expensesByCategory.isEmpty()) {
            pieChart.setNoDataText("No expense data available")
            pieChart.setNoDataTextColor(getColor(R.color.text_secondary))
            pieChart.clear()
            pieChart.invalidate()
            categoryLegendAdapter.updateItems(emptyMap())
            return
        }

        // Filter out income categories and only show expense categories
        val filteredExpenses = expensesByCategory.filter { (category, _) ->
            category != Category.SALARY && category != Category.SIDE_BUSINESS
        }

        // Create pie entries
        val entries = filteredExpenses.map { (category, amount) ->
            PieEntry(amount.toFloat(), category.displayName)
        }

        if (entries.isEmpty()) {
            // If no expense data, show empty state
            pieChart.setNoDataText("No expense data available")
            pieChart.setNoDataTextColor(getColor(R.color.text_secondary))
            pieChart.invalidate()
            return
        }

        // Create and configure the dataset
        val dataSet = PieDataSet(entries, "Expenses by Category").apply {
            // Map colors to entries based on category
            val mappedColors = ArrayList<Int>()
            filteredExpenses.keys.forEach { category ->
                when (category) {
                    Category.FOOD -> mappedColors.add(Color.parseColor("#FF9800"))
                    Category.ENTERTAINMENT -> mappedColors.add(Color.parseColor("#4CAF50"))
                    Category.TRANSPORT -> mappedColors.add(Color.parseColor("#2196F3"))
                    Category.HEALTH -> mappedColors.add(Color.parseColor("#F44336"))
                    Category.SHOPPING -> mappedColors.add(Color.parseColor("#9C27B0"))
                    Category.VACATION -> mappedColors.add(Color.parseColor("#3F51B5")) // Indigo color
                    else -> mappedColors.add(Color.parseColor("#7A5FFF"))
                }
            }

            // Use the mapped colors for the dataset
            colors = mappedColors
            setDrawValues(false)

            // Increase slice space to create rounded corners effect
            sliceSpace = 3f

            // Add more visual appeal
            selectionShift = 5f
        }

        // Configure the pie data
        val pieData = PieData(dataSet)

        // Update the pie chart
        pieChart.apply {
            // Clear any existing data
            clear()

            // Set new data
            data = pieData

            // Set center text
            val totalExpenses = filteredExpenses.values.sum()
            centerText = "Expenses\n${currencyFormatter.format(totalExpenses)}"

            // Animate and refresh
            animateY(2000)
            invalidate()
        }

        // Update legend
        categoryLegendAdapter.updateItems(filteredExpenses)
    }

    private fun setupDailyTrendChart(transactions: List<Transaction>) {
        val lineChart: LineChart = binding.dailyTrendChart

        // Get days in month
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Group expenses by day
        val expensesByDay = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy {
                val cal = Calendar.getInstance().apply { time = it.date }
                cal.get(Calendar.DAY_OF_MONTH)
            }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

        // Create entries for each day
        val entries = mutableListOf<Entry>()
        for (day in 1..daysInMonth) {
            val amount = expensesByDay[day] ?: 0.0
            entries.add(Entry(day.toFloat(), amount.toFloat()))
        }

        // Create dataset
        val dataSet = LineDataSet(entries, "Daily Expenses")
        dataSet.color = getColor(R.color.expense_red)
        dataSet.setCircleColor(getColor(R.color.expense_red))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 3f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 9f
        dataSet.valueTextColor = Color.WHITE
        dataSet.setDrawFilled(true)
        dataSet.fillColor = getColor(R.color.expense_red)
        dataSet.fillAlpha = 30
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        // Create and set data
        val lineData = LineData(dataSet)
        lineChart.data = lineData

        // Customize chart
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setDrawGridBackground(false)

        // X-axis customization
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.WHITE
        xAxis.granularity = 5f

        // Y-axis customization
        lineChart.axisLeft.textColor = Color.WHITE
        lineChart.axisRight.isEnabled = false

        // Animate and refresh
        lineChart.animateX(1000)
        lineChart.invalidate()
    }

    private fun getCategoryColor(category: Category): Int {
        return when (category) {
            Category.FOOD -> getColor(R.color.category_food)
            Category.ENTERTAINMENT -> getColor(R.color.category_entertainment)
            Category.TRANSPORT -> getColor(R.color.category_transport)
            Category.HEALTH -> getColor(R.color.category_health)
            Category.SHOPPING -> getColor(R.color.category_shopping)
            Category.SALARY -> getColor(R.color.category_salary)
            Category.SIDE_BUSINESS -> getColor(R.color.category_side_business)
            Category.VACATION -> getColor(R.color.category_vacation)
        }
    }
}
