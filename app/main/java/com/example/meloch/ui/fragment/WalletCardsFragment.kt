package com.example.meloch.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.meloch.MainActivity
import com.example.meloch.R
import com.example.meloch.data.CardManager
import com.example.meloch.data.model.Card
import com.example.meloch.data.model.CardType
import com.example.meloch.databinding.FragmentWalletCardsBinding
import com.example.meloch.ui.dialog.AddCardDialogFragment

class WalletCardsFragment : Fragment() {
    private var _binding: FragmentWalletCardsBinding? = null
    private val binding get() = _binding!!

    private lateinit var cardManager: CardManager

    private var creditCard: Card? = null
    private var debitCard: Card? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletCardsBinding.inflate(inflater, container, false)
        cardManager = CardManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadCards()
        displayCards()
    }

    private fun setupUI() {
        binding.addCardButton.setOnClickListener {
            showAddCardDialog()
        }
    }

    private fun loadCards() {
        val creditCards = cardManager.getCreditCards()
        val debitCards = cardManager.getDebitCards()

        creditCard = creditCards.firstOrNull()
        debitCard = debitCards.firstOrNull()
    }

    private fun displayCards() {
        // Display or hide credit card section
        if (creditCard != null) {
            binding.creditCardSection.visibility = View.VISIBLE
            binding.creditCardBankName.text = creditCard!!.bankName
            binding.creditCardNumber.text = creditCard!!.getMaskedCardNumber()
            binding.creditCardholderName.text = creditCard!!.cardholderName
            binding.creditCardCvv.text = "CVV: ${creditCard!!.getMaskedCvv()}"
            binding.creditCardExpiry.text = creditCard!!.getMaskedExpiryDate()

            binding.creditCardVisibilityToggle.setOnClickListener {
                toggleCreditCardVisibility()
            }

            binding.creditCardOptionsMenu.setOnClickListener { view ->
                showCardOptionsMenu(view, creditCard!!)
            }
        } else {
            binding.creditCardSection.visibility = View.GONE
        }

        // Display or hide debit card section
        if (debitCard != null) {
            binding.debitCardSection.visibility = View.VISIBLE
            binding.debitCardBankName.text = debitCard!!.bankName
            binding.debitCardNumber.text = debitCard!!.getMaskedCardNumber()
            binding.debitCardholderName.text = debitCard!!.cardholderName
            binding.debitCardCvv.text = "CVV: ${debitCard!!.getMaskedCvv()}"
            binding.debitCardExpiry.text = debitCard!!.getMaskedExpiryDate()

            binding.debitCardVisibilityToggle.setOnClickListener {
                toggleDebitCardVisibility()
            }

            binding.debitCardOptionsMenu.setOnClickListener { view ->
                showCardOptionsMenu(view, debitCard!!)
            }
        } else {
            binding.debitCardSection.visibility = View.GONE
        }

        // Show empty state message if no cards
        if (creditCard == null && debitCard == null) {
            binding.emptyStateMessage.visibility = View.VISIBLE
        } else {
            binding.emptyStateMessage.visibility = View.GONE
        }
    }

    private fun toggleCreditCardVisibility() {
        creditCard?.let {
            it.isVisible = !it.isVisible
            cardManager.saveCard(it)

            if (it.isVisible) {
                binding.creditCardNumber.text = it.cardNumber
                binding.creditCardCvv.text = "CVV: ${it.cvv}"
                binding.creditCardExpiry.text = it.getExpiryDate()
                binding.creditCardVisibilityToggle.setImageResource(R.drawable.ic_visibility_on)
            } else {
                binding.creditCardNumber.text = it.getMaskedCardNumber()
                binding.creditCardCvv.text = "CVV: ${it.getMaskedCvv()}"
                binding.creditCardExpiry.text = it.getMaskedExpiryDate()
                binding.creditCardVisibilityToggle.setImageResource(R.drawable.ic_visibility_off)
            }
        }
    }

    private fun toggleDebitCardVisibility() {
        debitCard?.let {
            it.isVisible = !it.isVisible
            cardManager.saveCard(it)

            if (it.isVisible) {
                binding.debitCardNumber.text = it.cardNumber
                binding.debitCardCvv.text = "CVV: ${it.cvv}"
                binding.debitCardExpiry.text = it.getExpiryDate()
                binding.debitCardVisibilityToggle.setImageResource(R.drawable.ic_visibility_on)
            } else {
                binding.debitCardNumber.text = it.getMaskedCardNumber()
                binding.debitCardCvv.text = "CVV: ${it.getMaskedCvv()}"
                binding.debitCardExpiry.text = it.getMaskedExpiryDate()
                binding.debitCardVisibilityToggle.setImageResource(R.drawable.ic_visibility_off)
            }
        }
    }

    private fun showCardOptionsMenu(view: View, card: Card) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.card_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    showEditCardDialog(card)
                    true
                }
                R.id.action_delete -> {
                    deleteCard(card)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun deleteCard(card: Card) {
        cardManager.deleteCard(card.id)
        loadCards()
        displayCards()
        updateMainActivity()
        Toast.makeText(requireContext(), "Card deleted", Toast.LENGTH_SHORT).show()
    }

    private fun showAddCardDialog() {
        val dialog = AddCardDialogFragment(null) { newCard ->
            cardManager.saveCard(newCard)
            loadCards()
            displayCards()
            updateMainActivity()
            Toast.makeText(requireContext(), "Card added", Toast.LENGTH_SHORT).show()
        }
        dialog.show(childFragmentManager, "AddCardDialog")
    }

    private fun showEditCardDialog(card: Card) {
        val dialog = AddCardDialogFragment(card) { updatedCard ->
            cardManager.saveCard(updatedCard)
            loadCards()
            displayCards()
            updateMainActivity()
            Toast.makeText(requireContext(), "Card updated", Toast.LENGTH_SHORT).show()
        }
        dialog.show(childFragmentManager, "EditCardDialog")
    }

    private fun updateMainActivity() {
        // Update the MainActivity's dashboard to reflect the new total balance
        (activity as? MainActivity)?.updateDashboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
