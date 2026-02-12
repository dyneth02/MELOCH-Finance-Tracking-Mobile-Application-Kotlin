package com.example.meloch.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.meloch.R
import com.example.meloch.databinding.DialogEditBudgetBinding

class EditBudgetDialogFragment(
    private val initialCategory: String?,
    private val initialAmount: Double,
    private val onBudgetSaved: (String, Double) -> Unit
) : DialogFragment() {

    private var _binding: DialogEditBudgetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set dialog width to match parent
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        setupCategoryDropdown()
        setupInitialValues()
        setupButtons()
    }
    
    private fun setupCategoryDropdown() {
        val categories = arrayOf("Entertainment", "Food", "Transport", "Lifestyle")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.categoryDropdown.setAdapter(adapter)
    }
    
    private fun setupInitialValues() {
        if (initialCategory != null) {
            binding.categoryDropdown.setText(initialCategory)
            binding.categoryDropdown.isEnabled = false
        }
        
        if (initialAmount > 0) {
            binding.amountInput.setText(initialAmount.toString())
        }
    }
    
    private fun setupButtons() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        
        binding.saveButton.setOnClickListener {
            val category = binding.categoryDropdown.text.toString()
            val amountText = binding.amountInput.text.toString()
            
            if (category.isEmpty()) {
                Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            onBudgetSaved(category, amount)
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
