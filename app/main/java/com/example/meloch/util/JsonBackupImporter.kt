package com.example.meloch.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.meloch.data.CardManager
import com.example.meloch.data.PreferencesManager
import com.example.meloch.data.UserManager
import com.example.meloch.data.model.Card
import com.example.meloch.data.model.CardType
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.PaymentMethod
import com.example.meloch.data.model.Transaction
import com.example.meloch.data.model.TransactionType
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Date
import java.util.UUID

/**
 * Utility class to import JSON backup files and restore data
 */
class JsonBackupImporter(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val userManager = UserManager(context)
    private val cardManager = CardManager(context)

    /**
     * Imports data from a backup file
     * @param uri URI of the backup file to import
     * @return true if import was successful, false otherwise
     */
    fun importBackup(uri: Uri): Boolean {
        try {
            // Read the JSON data from the file
            val jsonData = readJsonFromUri(uri)
            if (jsonData.isNullOrEmpty()) {
                Log.e("JsonBackupImporter", "Failed to read JSON data from file")
                return false
            }

            // Parse the JSON data
            val rootJson = JSONObject(jsonData)

            // Verify this is a valid Meloch backup file
            val metadataJson = rootJson.optJSONObject("metadata")
            if (metadataJson == null || metadataJson.optString("app") != "Meloch") {
                Log.e("JsonBackupImporter", "Invalid backup file format")
                return false
            }

            // Import the data
            val success = importData(rootJson)
            if (success) {
                Log.d("JsonBackupImporter", "Data imported successfully")
            } else {
                Log.e("JsonBackupImporter", "Failed to import data")
            }
            return success
        } catch (e: Exception) {
            Log.e("JsonBackupImporter", "Error importing backup: ${e.message}", e)
            return false
        }
    }

    /**
     * Reads JSON data from a URI
     * @param uri URI of the file to read
     * @return JSON string or null if reading failed
     */
    private fun readJsonFromUri(uri: Uri): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }

            inputStream.close()
            return stringBuilder.toString()
        } catch (e: IOException) {
            Log.e("JsonBackupImporter", "Error reading file: ${e.message}", e)
            return null
        }
    }

    /**
     * Imports data from the parsed JSON
     * @param rootJson Root JSON object containing all backup data
     * @return true if import was successful, false otherwise
     */
    private fun importData(rootJson: JSONObject): Boolean {
        try {
            // Import financial data
            importFinancialData(rootJson)

            // Import cards
            importCards(rootJson)

            // Import transactions
            importTransactions(rootJson)

            return true
        } catch (e: Exception) {
            Log.e("JsonBackupImporter", "Error importing data: ${e.message}", e)
            return false
        }
    }

    /**
     * Imports financial data from the backup
     * @param rootJson Root JSON object containing all backup data
     */
    private fun importFinancialData(rootJson: JSONObject) {
        try {
            val financialJson = rootJson.getJSONObject("financial")

            // Import total balance
            val totalBalance = financialJson.optDouble("totalBalance", 0.0)
            preferencesManager.setTotalBalance(totalBalance)

            // Import pocket money
            val pocketMoney = financialJson.optDouble("pocketMoney", 0.0)
            cardManager.savePocketMoney(pocketMoney)

            // Import budget left
            val budgetLeft = financialJson.optDouble("budgetLeft", 0.0)
            preferencesManager.setBudgetLeft(budgetLeft)

            // Import total budget
            val totalBudget = financialJson.optDouble("totalBudget", 0.0)
            preferencesManager.setMonthlyBudget(totalBudget)

            // Import budget categories
            val budgetCategoriesJson = financialJson.optJSONObject("budgetCategories")
            if (budgetCategoriesJson != null) {
                val entertainment = budgetCategoriesJson.optDouble("entertainment", 0.0)
                val food = budgetCategoriesJson.optDouble("food", 0.0)
                val transport = budgetCategoriesJson.optDouble("transport", 0.0)
                val lifestyle = budgetCategoriesJson.optDouble("lifestyle", 0.0)

                preferencesManager.saveEntertainmentBudget(entertainment)
                preferencesManager.saveFoodBudget(food)
                preferencesManager.saveTransportBudget(transport)
                preferencesManager.saveLifestyleBudget(lifestyle)
            }

            Log.d("JsonBackupImporter", "Financial data imported successfully")
        } catch (e: JSONException) {
            Log.e("JsonBackupImporter", "Error importing financial data: ${e.message}", e)
            throw e
        }
    }

    /**
     * Imports cards from the backup
     * @param rootJson Root JSON object containing all backup data
     */
    private fun importCards(rootJson: JSONObject) {
        try {
            val cardsJsonArray = rootJson.optJSONArray("cards")
            if (cardsJsonArray != null) {
                val cards = mutableListOf<Card>()

                for (i in 0 until cardsJsonArray.length()) {
                    val cardJson = cardsJsonArray.getJSONObject(i)

                    val id = cardJson.optString("id", UUID.randomUUID().toString())
                    val cardNumber = cardJson.optString("cardNumber", "")
                    val cardholderName = cardJson.optString("cardholderName", "")
                    val expiryMonth = cardJson.optInt("expiryMonth", 1)
                    val expiryYear = cardJson.optInt("expiryYear", 25)
                    val cvv = cardJson.optString("cvv", "")
                    val typeStr = cardJson.optString("type", CardType.DEBIT.name)
                    val balance = cardJson.optDouble("balance", 0.0)

                    val type = try {
                        CardType.valueOf(typeStr)
                    } catch (e: IllegalArgumentException) {
                        CardType.DEBIT
                    }

                    val card = Card(
                        id = id,
                        type = type,
                        bankName = cardJson.optString("bankName", "BANK"),
                        cardNumber = cardNumber,
                        cardholderName = cardholderName,
                        expiryMonth = expiryMonth,
                        expiryYear = expiryYear,
                        cvv = cvv,
                        isVisible = false,
                        balance = balance
                    )

                    cards.add(card)
                }

                // Save all cards
                cardManager.saveCards(cards)
                Log.d("JsonBackupImporter", "Imported ${cards.size} cards")
            }
        } catch (e: JSONException) {
            Log.e("JsonBackupImporter", "Error importing cards: ${e.message}", e)
            throw e
        }
    }

    /**
     * Imports transactions from the backup
     * @param rootJson Root JSON object containing all backup data
     */
    private fun importTransactions(rootJson: JSONObject) {
        try {
            val transactionsJsonArray = rootJson.optJSONArray("transactions")
            if (transactionsJsonArray != null) {
                val transactions = mutableListOf<Transaction>()

                for (i in 0 until transactionsJsonArray.length()) {
                    val transactionJson = transactionsJsonArray.getJSONObject(i)

                    val id = transactionJson.optString("id", UUID.randomUUID().toString())
                    val amount = transactionJson.optDouble("amount", 0.0)
                    val categoryStr = transactionJson.optString("category", Category.ENTERTAINMENT.name)
                    val typeStr = transactionJson.optString("type", TransactionType.EXPENSE.name)
                    val dateMillis = transactionJson.optLong("date", System.currentTimeMillis())

                    val category = try {
                        Category.valueOf(categoryStr)
                    } catch (e: IllegalArgumentException) {
                        Category.ENTERTAINMENT
                    }

                    val type = try {
                        TransactionType.valueOf(typeStr)
                    } catch (e: IllegalArgumentException) {
                        TransactionType.EXPENSE
                    }

                    // Parse payment method
                    val paymentMethodStr = transactionJson.optString("paymentMethod", PaymentMethod.CASH.name)
                    val paymentMethod = try {
                        PaymentMethod.valueOf(paymentMethodStr)
                    } catch (e: IllegalArgumentException) {
                        PaymentMethod.CASH // Default to CASH if invalid
                    }

                    val transaction = Transaction(
                        id = id,
                        title = transactionJson.optString("title", ""),
                        amount = amount,
                        type = type,
                        category = category,
                        date = Date(dateMillis),
                        paymentMethod = paymentMethod
                    )

                    transactions.add(transaction)
                }

                // Save all transactions
                preferencesManager.saveTransactions(transactions)
                Log.d("JsonBackupImporter", "Imported ${transactions.size} transactions")
            }
        } catch (e: JSONException) {
            Log.e("JsonBackupImporter", "Error importing transactions: ${e.message}", e)
            throw e
        }
    }
}
