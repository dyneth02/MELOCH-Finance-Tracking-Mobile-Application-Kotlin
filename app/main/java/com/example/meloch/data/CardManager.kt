package com.example.meloch.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.meloch.data.model.Card
import com.example.meloch.data.model.CardType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CardManager(context: Context) {
    // Get the current user's email directly from SharedPreferences
    private val userEmail: String? = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        .getString("current_user", null)

    // Create a user-specific preferences file name
    private val prefsFileName = if (userEmail != null) "${userEmail}_$PREFS_NAME" else PREFS_NAME

    // Initialize SharedPreferences with the user-specific file name
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        Log.d("CardManager", "Initialized with user: $userEmail, using prefs file: $prefsFileName")
    }

    // Get the current user's email from SharedPreferences directly
    private val currentUserEmail: String?
        get() {
            val email = sharedPreferences.getString(UserManager.KEY_CURRENT_USER, null)
            Log.d("CardManager", "Current user email: $email")
            return email
        }

    companion object {
        private const val PREFS_NAME = "meloch_cards"
        private const val KEY_CARDS = "cards"
        private const val KEY_POCKET_MONEY = "pocket_money"
    }

    /**
     * Creates a user-specific key by prefixing the base key with the current user's email
     * This ensures that each user has their own separate data
     */
    private fun getUserSpecificKey(baseKey: String): String {
        val email = currentUserEmail
        val result = if (email != null) {
            "${email}_$baseKey"
        } else {
            // Fallback to the base key if no user is logged in
            baseKey
        }

        // Add debug logging to track key generation
        Log.d("CardManager", "Generated key: $result from base key: $baseKey for user: $email")

        return result
    }

    fun saveCard(card: Card) {
        val cards = getCards().toMutableList()

        // Check if card already exists (for editing)
        val existingIndex = cards.indexOfFirst { it.id == card.id }
        if (existingIndex >= 0) {
            // Update existing card
            cards[existingIndex] = card
        } else {
            // Add new card
            cards.add(card)
        }

        saveCards(cards)
    }

    fun deleteCard(cardId: String) {
        val cards = getCards().toMutableList()
        cards.removeAll { it.id == cardId }
        saveCards(cards)
    }

    fun getCards(): List<Card> {
        val userKey = getUserSpecificKey(KEY_CARDS)
        val cardsJson = sharedPreferences.getString(userKey, null) ?: return emptyList()
        val type = object : TypeToken<List<Card>>() {}.type
        return gson.fromJson(cardsJson, type) ?: emptyList()
    }

    fun getCreditCards(): List<Card> {
        return getCards().filter { it.type == CardType.CREDIT }
    }

    fun getDebitCards(): List<Card> {
        return getCards().filter { it.type == CardType.DEBIT }
    }

    fun saveCards(cards: List<Card>) {
        val userKey = getUserSpecificKey(KEY_CARDS)
        val cardsJson = gson.toJson(cards)
        sharedPreferences.edit().putString(userKey, cardsJson).apply()
    }

    fun clearAllCards() {
        val userKey = getUserSpecificKey(KEY_CARDS)
        sharedPreferences.edit().remove(userKey).apply()
    }

    fun savePocketMoney(amount: Double) {
        val userKey = getUserSpecificKey(KEY_POCKET_MONEY)
        sharedPreferences.edit().putFloat(userKey, amount.toFloat()).apply()
    }

    fun getPocketMoney(): Double {
        val userKey = getUserSpecificKey(KEY_POCKET_MONEY)
        return sharedPreferences.getFloat(userKey, 0f).toDouble()
    }

    fun getTotalBalance(): Double {
        val cardsBalance = getCards().sumOf { it.balance }
        val pocketMoney = getPocketMoney()
        return cardsBalance + pocketMoney
    }

    fun getCreditCardsBalance(): Double {
        return getCreditCards().sumOf { it.balance }
    }

    fun getDebitCardsBalance(): Double {
        return getDebitCards().sumOf { it.balance }
    }
}
