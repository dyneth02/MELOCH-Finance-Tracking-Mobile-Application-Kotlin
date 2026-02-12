package com.example.meloch.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.meloch.R
import com.example.meloch.databinding.DialogMoneyAtHandBinding

class MoneyAtHandDialogFragment(
    private val currentAmount: Double = 0.0,
    private val onAmountSaved: (Double) -> Unit
) : DialogFragment() {

    private var _binding: DialogMoneyAtHandBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMoneyAtHandBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set dialog width to match parent
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        setupInitialValues()
        setupButtons()
    }
    
    private fun setupInitialValues() {
        if (currentAmount > 0) {
            binding.amountInput.setText(currentAmount.toString())
        }
    }
    
    private fun setupButtons() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        
        binding.saveButton.setOnClickListener {
            val amountText = binding.amountInput.text.toString()
            
            if (amountText.isEmpty()) {
                Toast.makeText(context, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount < 0) {
                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            onAmountSaved(amount)
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
