package com.example.meloch.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meloch.R
import com.example.meloch.data.PreferencesManager
import com.example.meloch.data.model.Transaction
import com.example.meloch.databinding.FragmentRecordsBinding
import com.example.meloch.ui.adapter.RecordsAdapter
import com.example.meloch.ui.adapter.SwipeToDeleteCallback
import com.example.meloch.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

class RecordsFragment : Fragment() {
    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TransactionViewModel
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var recordsAdapter: RecordsAdapter

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the ViewModel and load transactions from SharedPreferences
        viewModel = TransactionViewModel()
        preferencesManager = PreferencesManager(requireContext())

        // Load transactions from SharedPreferences into ViewModel
        val savedTransactions = preferencesManager.getTransactions()
        savedTransactions.forEach { transaction ->
            viewModel.addTransaction(transaction)
        }

        setupFilterSpinner()
        setupRecyclerView()
        setupEmptyState()
        loadTransactions()
    }

    private fun setupEmptyState() {
        binding.addTransactionButton.setOnClickListener {
            // Navigate back to home screen
            val mainActivity = activity as? com.example.meloch.MainActivity
            mainActivity?.let {
                // Select the home tab
                it.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.navigation_home

                // Show the add transaction dialog after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    it.showAddRecordDialog()
                }, 300)
            }
        }
    }

    private fun setupFilterSpinner() {
        val filters = arrayOf("This Year", "This Month", "This Week", "Today")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, filters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.filterSpinner.adapter = adapter
        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadTransactions(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupRecyclerView() {
        recordsAdapter = RecordsAdapter()
        binding.recordsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordsAdapter
        }

        // Setup swipe to delete
        val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = recordsAdapter.getTransactionAt(position)

                // Remove from adapter
                recordsAdapter.removeTransaction(position)

                // Remove from data source
                viewModel.removeTransaction(transaction)

                // Remove from SharedPreferences
                val preferencesManager = PreferencesManager(requireContext())
                preferencesManager.deleteTransaction(transaction.id)

                // Update preferences
                updatePreferencesAfterDelete(transaction)

                Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.recordsRecyclerView)
    }

    private fun loadTransactions(filterPosition: Int = 0) {
        // Get the latest transactions from SharedPreferences
        val allTransactions = preferencesManager.getTransactions()

        // Create a calendar for filtering
        val calendar = Calendar.getInstance()

        // Store the current year, month, and day for filtering
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        // Reset time to start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Apply filter
        when (filterPosition) {
            0 -> { // This Year
                calendar.set(currentYear, Calendar.JANUARY, 1) // First day of current year
            }
            1 -> { // This Month
                calendar.set(currentYear, currentMonth, 1) // First day of current month
            }
            2 -> { // This Week
                // Go back to the first day of the week
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            }
            3 -> { // Today
                calendar.set(currentYear, currentMonth, currentDay) // Start of today
            }
        }

        val startDate = calendar.time

        // Set end date to end of current day
        calendar.set(currentYear, currentMonth, currentDay, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time

        // Log for debugging
        android.util.Log.d("RecordsFilter", "Filter: $filterPosition, Start: $startDate, End: $endDate")

        val filteredTransactions = allTransactions.filter {
            it.date in startDate..endDate
        }

        // Sort transactions by date (newest first)
        val sortedTransactions = filteredTransactions.sortedByDescending { it.date }

        // Group transactions by date
        val groupedTransactions = sortedTransactions.groupBy {
            dateFormat.format(it.date)
        }

        // Convert to adapter format
        val adapterData = mutableListOf<Any>()

        // Sort the date groups by date (newest first)
        val sortedDateGroups = groupedTransactions.entries.sortedByDescending { entry ->
            // Parse the date string back to a Date object for sorting
            try {
                dateFormat.parse(entry.key)
            } catch (e: Exception) {
                // Fallback to current date if parsing fails
                Date()
            }
        }

        // Add sorted date groups to adapter data
        sortedDateGroups.forEach { (date, transactions) ->
            adapterData.add(date) // Add date header
            adapterData.addAll(transactions) // Add transactions for this date
        }

        // Update the adapter with the new data
        recordsAdapter.submitList(null) // Clear the current list
        recordsAdapter.submitList(adapterData) // Submit the new list

        // Show empty state if needed
        binding.emptyStateLayout.visibility = if (filteredTransactions.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updatePreferencesAfterDelete(transaction: Transaction) {
        // Update monthly expenses or income
        if (transaction.isIncome) {
            preferencesManager.updateMonthlyIncome(-transaction.amount) // Subtract the income
        } else {
            preferencesManager.updateMonthlyExpenses(-transaction.amount) // Subtract the expense
        }

        // Update category expenses if it's an expense
        if (!transaction.isIncome) {
            preferencesManager.updateCategoryExpense(transaction.category, -transaction.amount)
        }

        // Update the dashboard in MainActivity
        (activity as? com.example.meloch.MainActivity)?.updateDashboard()
    }

    fun refreshTransactions() {
        // First, reload transactions from SharedPreferences into ViewModel
        viewModel = TransactionViewModel()
        val savedTransactions = preferencesManager.getTransactions()
        savedTransactions.forEach { transaction ->
            viewModel.addTransaction(transaction)
        }

        // Then reload the UI with the current filter
        val currentFilterPosition = binding.filterSpinner.selectedItemPosition
        loadTransactions(currentFilterPosition)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
