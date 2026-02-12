package com.example.meloch.ui.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.meloch.R
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.PaymentMethod
import com.example.meloch.data.model.Transaction
import com.example.meloch.data.model.TransactionType
import com.example.meloch.databinding.DialogAddRecordBinding
import com.example.meloch.viewmodel.TransactionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AddRecordDialogFragment : DialogFragment() {
    private var _binding: DialogAddRecordBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionViewModel
    private var selectedType: TransactionType = TransactionType.EXPENSE
    private var selectedCategory: Category? = null
    private var selectedPaymentMethod: PaymentMethod? = null
    private var selectedDate: Date = Date()
    private val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("LKR")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Set dialog width to match parent
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Set default date
        updateDateDisplay()

        // Set initial tab colors and text
        updateTabColors()

        // Set default payment method
        selectedPaymentMethod = PaymentMethod.CASH
        updatePaymentMethodDisplay()
    }

    private fun setupClickListeners() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.recordTypeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedType = when (tab?.position) {
                    0 -> TransactionType.EXPENSE
                    1 -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }
                updateTabColors()
                updateCategoryOptions()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.amountValue.setOnClickListener {
            showAmountInputDialog()
        }

        binding.categoryCard.setOnClickListener {
            showCategorySelectionDialog()
        }

        binding.paymentMethodCard.setOnClickListener {
            showPaymentMethodDialog()
        }

        binding.addRecordButton.setOnClickListener {
            addTransaction()
        }
    }

    private fun showAmountInputDialog() {
        val currentAmount = binding.amountValue.text.toString().replace(Regex("[^0-9.]"), "")
        val inputView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_amount_input, null)
        val amountInput = inputView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.amountInput)

        // Set current amount if it's valid
        if (currentAmount.isNotEmpty() && currentAmount.toDoubleOrNull() != null) {
            amountInput.setText(currentAmount)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enter Amount")
            .setView(inputView)
            .setPositiveButton("OK") { _, _ ->
                val amountText = amountInput.text.toString().trim()
                val newAmount = if (amountText.isEmpty()) 0.0 else amountText.toDoubleOrNull() ?: 0.0

                if (newAmount > 0) {
                    updateAmountDisplay(newAmount)
                } else {
                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAmountDisplay(amount: Double) {
        val isIncome = selectedType == TransactionType.INCOME
        binding.amountValue.text = "${if (isIncome) "+" else "-"}${currencyFormatter.format(amount)}"
    }

    private fun updateTabColors() {
        val isIncome = selectedType == TransactionType.INCOME
        val colorRes = if (isIncome) R.color.income_green else R.color.expense_red

        // Update tab indicator color
        binding.recordTypeTabs.setSelectedTabIndicatorColor(ContextCompat.getColor(requireContext(), colorRes))

        // Update tab text colors
        binding.recordTypeTabs.setTabTextColors(
            ContextCompat.getColor(requireContext(), R.color.text_primary),
            ContextCompat.getColor(requireContext(), colorRes)
        )

        // Update amount label and value colors
        binding.amountLabel.text = if (isIncome) "Income" else "Expense"
        binding.amountLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        binding.amountValue.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        // Update amount prefix
        val currentAmount = binding.amountValue.text.toString().replace(Regex("[^0-9.]"), "")
        binding.amountValue.text = "${if (isIncome) "+" else "-"}${currencyFormatter.format(currentAmount.toDoubleOrNull() ?: 0.0)}"
    }

    private fun updateCategoryOptions() {
        // Reset selected category when switching between income and expense
        selectedCategory = null

        // Update the category display with default values
        binding.categoryValue.text = "Select Category"
        binding.categoryValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))

        // Set default icon based on transaction type
        val defaultIcon = if (selectedType == TransactionType.INCOME) {
            R.drawable.ic_salary
        } else {
            R.drawable.ic_shopping
        }

        binding.categoryIcon.setImageResource(defaultIcon)
        binding.categoryIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary))
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.dateTimeValue.text = dateFormat.format(selectedDate)
    }

    private fun showCategorySelectionDialog() {
        val dialog = SelectCategoryDialogFragment(
            transactionType = selectedType,
            onCategorySelected = { category ->
                selectedCategory = category
                updateCategoryDisplay()
            }
        )
        dialog.show(childFragmentManager, "SelectCategoryDialog")
    }

    private fun updateCategoryDisplay() {
        selectedCategory?.let { category ->
            binding.categoryValue.text = category.displayName
            binding.categoryValue.setTextColor(ContextCompat.getColor(requireContext(), getCategoryColor(category)))

            // Update the right-hand side icon instead of using compound drawables
            binding.categoryIcon.setImageResource(getCategoryIcon(category))
            binding.categoryIcon.setColorFilter(ContextCompat.getColor(requireContext(), getCategoryColor(category)))
        }
    }

    private fun getCategoryIcon(category: Category): Int {
        return when (category) {
            Category.FOOD -> R.drawable.ic_food
            Category.ENTERTAINMENT -> R.drawable.ic_entertainment
            Category.TRANSPORT -> R.drawable.ic_transport
            Category.HEALTH -> R.drawable.ic_health
            Category.SHOPPING -> R.drawable.ic_shopping
            Category.SALARY -> R.drawable.ic_salary
            Category.SIDE_BUSINESS -> R.drawable.ic_side_business
            Category.VACATION -> R.drawable.ic_vacation
        }
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

    private fun showPaymentMethodDialog() {
        val paymentMethods = PaymentMethod.values()
        val paymentMethodNames = paymentMethods.map {
            when (it) {
                PaymentMethod.CASH -> "Cash"
                PaymentMethod.CREDIT_CARD -> "Credit Card"
                PaymentMethod.DEBIT_CARD -> "Debit Card"
            }
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Payment Method")
            .setItems(paymentMethodNames) { _, which ->
                selectedPaymentMethod = paymentMethods[which]
                updatePaymentMethodDisplay()
            }
            .show()
    }

    private fun updatePaymentMethodDisplay() {
        selectedPaymentMethod?.let { method ->
            val methodName = when (method) {
                PaymentMethod.CASH -> "Cash"
                PaymentMethod.CREDIT_CARD -> "Credit Card"
                PaymentMethod.DEBIT_CARD -> "Debit Card"
            }
            binding.paymentMethodValue.text = methodName
        }
    }

    private fun addTransaction() {
        // Extract amount from the display text
        val amountText = binding.amountValue.text.toString().replace(Regex("[^0-9.]"), "")
        val amount = amountText.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategory == null) {
            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Set default payment method if not selected
        if (selectedPaymentMethod == null) {
            selectedPaymentMethod = PaymentMethod.CASH
            updatePaymentMethodDisplay()
        }

        // Check if budget is spent for expense transactions
        if (selectedType == TransactionType.EXPENSE) {
            val preferencesManager = com.example.meloch.data.PreferencesManager(requireContext())

            // Get the remaining budget - use getBudgetLeft if budget has been reset
            val remainingBudget = if (preferencesManager.isBudgetReset()) {
                preferencesManager.getBudgetLeft()
            } else {
                preferencesManager.getMonthlyBudget() - preferencesManager.getMonthlyExpenses()
            }

            // Log for debugging
            Log.d("Budget", "Remaining budget: $remainingBudget, Budget reset: ${preferencesManager.isBudgetReset()}")

            if (remainingBudget <= 0) {
                Toast.makeText(context, "Budget limit reached! You cannot add more expenses.", Toast.LENGTH_LONG).show()
                return
            } else if (amount > remainingBudget) {
                Toast.makeText(context, "Amount exceeds remaining budget of ${currencyFormatter.format(remainingBudget)}!", Toast.LENGTH_LONG).show()
                return
            }
        }

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            title = selectedCategory!!.displayName,
            amount = amount,
            type = selectedType,
            category = selectedCategory!!,
            date = selectedDate,
            paymentMethod = selectedPaymentMethod!!
        )

        // Initialize PreferencesManager to save the transaction
        val preferencesManager = com.example.meloch.data.PreferencesManager(requireContext())
        preferencesManager.saveTransaction(transaction)

        // Also add to ViewModel for immediate UI update
        viewModel.addTransaction(transaction)

        // Update the dashboard in MainActivity
        val mainActivity = activity as? com.example.meloch.MainActivity
        mainActivity?.updateDashboard()

        // Directly notify the RecordsFragment if it's visible
        val currentFragment = mainActivity?.supportFragmentManager?.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is com.example.meloch.ui.fragment.RecordsFragment) {
            currentFragment.refreshTransactions()
        }

        Toast.makeText(context, "Transaction added successfully", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(viewModel: TransactionViewModel): AddRecordDialogFragment {
            return AddRecordDialogFragment().apply {
                this.viewModel = viewModel
            }
        }
    }
}