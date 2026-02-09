package com.example.meloch.ui.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.example.meloch.R
import com.example.meloch.data.model.Card
import com.example.meloch.data.model.CardType
import com.example.meloch.databinding.ActivityWalletCardsBinding
import com.example.meloch.ui.dialog.AddCardDialogFragment

class WalletCardsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWalletCardsBinding
    
    // Sample cards for demonstration
    private val creditCard = Card(
        type = CardType.CREDIT,
        bankName = "AMERICAN EXPRESS",
        cardNumber = "378282246310005",
        cardholderName = "John Doe",
        expiryMonth = 12,
        expiryYear = 2025,
        cvv = "123"
    )
    
    private val debitCard = Card(
        type = CardType.DEBIT,
        bankName = "AMERICAN EXPRESS",
        cardNumber = "371449635398431",
        cardholderName = "John Doe",
        expiryMonth = 6,
        expiryYear = 2024,
        cvv = "456"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletCardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupBottomNavigation()
        displayCards()
    }
    
    private fun setupUI() {
        binding.addCardButton.setOnClickListener {
            showAddCardDialog()
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_cards
        
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    finish()
                    true
                }
                R.id.navigation_records -> {
                    // TODO: Navigate to records
                    true
                }
                R.id.navigation_cards -> true
                R.id.navigation_menu -> {
                    // TODO: Navigate to menu
                    true
                }
                else -> false
            }
        }
    }
    
    private fun displayCards() {
        // Display credit card
        binding.creditCardBankName.text = creditCard.bankName
        binding.creditCardNumber.text = creditCard.getMaskedCardNumber()
        binding.creditCardholderName.text = creditCard.cardholderName
        binding.creditCardCvv.text = "CVV: ${creditCard.getMaskedCvv()}"
        binding.creditCardExpiry.text = creditCard.getMaskedExpiryDate()
        
        binding.creditCardVisibilityToggle.setOnClickListener {
            toggleCreditCardVisibility()
        }
        
        binding.creditCardOptionsMenu.setOnClickListener { view ->
            showCardOptionsMenu(view, creditCard)
        }
        
        // Display debit card
        binding.debitCardBankName.text = debitCard.bankName
        binding.debitCardNumber.text = debitCard.getMaskedCardNumber()
        binding.debitCardholderName.text = debitCard.cardholderName
        binding.debitCardCvv.text = "CVV: ${debitCard.getMaskedCvv()}"
        binding.debitCardExpiry.text = debitCard.getMaskedExpiryDate()
        
        binding.debitCardVisibilityToggle.setOnClickListener {
            toggleDebitCardVisibility()
        }
        
        binding.debitCardOptionsMenu.setOnClickListener { view ->
            showCardOptionsMenu(view, debitCard)
        }
    }
    
    private fun toggleCreditCardVisibility() {
        creditCard.isVisible = !creditCard.isVisible
        
        if (creditCard.isVisible) {
            binding.creditCardNumber.text = creditCard.cardNumber
            binding.creditCardCvv.text = "CVV: ${creditCard.cvv}"
            binding.creditCardExpiry.text = creditCard.getExpiryDate()
            binding.creditCardVisibilityToggle.setImageResource(R.drawable.ic_visibility_on)
        } else {
            binding.creditCardNumber.text = creditCard.getMaskedCardNumber()
            binding.creditCardCvv.text = "CVV: ${creditCard.getMaskedCvv()}"
            binding.creditCardExpiry.text = creditCard.getMaskedExpiryDate()
            binding.creditCardVisibilityToggle.setImageResource(R.drawable.ic_visibility_off)
        }
    }
    
    private fun toggleDebitCardVisibility() {
        debitCard.isVisible = !debitCard.isVisible
        
        if (debitCard.isVisible) {
            binding.debitCardNumber.text = debitCard.cardNumber
            binding.debitCardCvv.text = "CVV: ${debitCard.cvv}"
            binding.debitCardExpiry.text = debitCard.getExpiryDate()
            binding.debitCardVisibilityToggle.setImageResource(R.drawable.ic_visibility_on)
        } else {
            binding.debitCardNumber.text = debitCard.getMaskedCardNumber()
            binding.debitCardCvv.text = "CVV: ${debitCard.getMaskedCvv()}"
            binding.debitCardExpiry.text = debitCard.getMaskedExpiryDate()
            binding.debitCardVisibilityToggle.setImageResource(R.drawable.ic_visibility_off)
        }
    }
    
    private fun showCardOptionsMenu(view: View, card: Card) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.card_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    showEditCardDialog(card)
                    true
                }
                R.id.action_delete -> {
                    // TODO: Implement delete card functionality
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun showAddCardDialog() {
        val dialog = AddCardDialogFragment(null) { newCard ->
            // TODO: Implement add card functionality
        }
        dialog.show(supportFragmentManager, "AddCardDialog")
    }
    
    private fun showEditCardDialog(card: Card) {
        val dialog = AddCardDialogFragment(card) { updatedCard ->
            // TODO: Implement edit card functionality
        }
        dialog.show(supportFragmentManager, "EditCardDialog")
    }
}
