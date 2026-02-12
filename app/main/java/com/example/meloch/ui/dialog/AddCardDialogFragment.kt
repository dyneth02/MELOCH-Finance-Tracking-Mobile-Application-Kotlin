package com.example.meloch.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.meloch.R
import com.example.meloch.data.model.Card
import com.example.meloch.data.model.CardType
import com.example.meloch.databinding.DialogAddCardBinding

class AddCardDialogFragment(
    private val card: Card? = null,
    private val onCardSaved: (Card) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddCardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set dialog width to match parent
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupCardTypeDropdown()
        setupInitialValues()
        setupButtons()
    }

    private fun setupCardTypeDropdown() {
        val cardTypes = arrayOf("Credit Card", "Debit Card")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cardTypes)
        binding.cardTypeDropdown.setAdapter(adapter)
    }

    private fun setupInitialValues() {
        if (card != null) {
            binding.dialogTitle.text = "Edit Card"
            binding.cardTypeDropdown.setText(if (card.type == CardType.CREDIT) "Credit Card" else "Debit Card")
            binding.bankNameInput.setText(card.bankName)
            binding.cardNumberInput.setText(card.cardNumber)
            binding.cardholderNameInput.setText(card.cardholderName)
            binding.expiryMonthInput.setText(card.expiryMonth.toString())
            binding.expiryYearInput.setText(card.expiryYear.toString())
            binding.cvvInput.setText(card.cvv)
        }
    }

    private fun setupButtons() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                val cardType = if (binding.cardTypeDropdown.text.toString() == "Credit Card") {
                    CardType.CREDIT
                } else {
                    CardType.DEBIT
                }

                val randomBalance = if (cardType == CardType.CREDIT) {
                    (150000..200000).random().toDouble()
                } else {
                    (100000..150000).random().toDouble()
                }

                val newCard = Card(
                    id = card?.id ?: java.util.UUID.randomUUID().toString(),
                    type = cardType,
                    bankName = binding.bankNameInput.text.toString().uppercase(),
                    cardNumber = binding.cardNumberInput.text.toString(),
                    cardholderName = binding.cardholderNameInput.text.toString(),
                    expiryMonth = binding.expiryMonthInput.text.toString().toInt(),
                    expiryYear = binding.expiryYearInput.text.toString().toInt(),
                    cvv = binding.cvvInput.text.toString(),
                    isVisible = card?.isVisible ?: false,
                    balance = card?.balance ?: randomBalance
                )

                onCardSaved(newCard)
                dismiss()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (binding.cardTypeDropdown.text.isNullOrEmpty()) {
            binding.cardTypeDropdown.error = "Please select a card type"
            isValid = false
        }

        if (binding.bankNameInput.text.isNullOrEmpty()) {
            binding.bankNameInput.error = "Please enter bank name"
            isValid = false
        }

        if (binding.cardNumberInput.text.isNullOrEmpty() || binding.cardNumberInput.text.toString().length < 13) {
            binding.cardNumberInput.error = "Please enter a valid card number"
            isValid = false
        }

        if (binding.cardholderNameInput.text.isNullOrEmpty()) {
            binding.cardholderNameInput.error = "Please enter cardholder name"
            isValid = false
        }

        val expiryMonth = binding.expiryMonthInput.text.toString().toIntOrNull()
        if (expiryMonth == null || expiryMonth < 1 || expiryMonth > 12) {
            binding.expiryMonthInput.error = "Please enter a valid month (1-12)"
            isValid = false
        }

        val expiryYear = binding.expiryYearInput.text.toString().toIntOrNull()
        if (expiryYear == null || expiryYear < 2023) {
            binding.expiryYearInput.error = "Please enter a valid year"
            isValid = false
        }

        if (binding.cvvInput.text.isNullOrEmpty() || binding.cvvInput.text.toString().length < 3) {
            binding.cvvInput.error = "Please enter a valid CVV"
            isValid = false
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
