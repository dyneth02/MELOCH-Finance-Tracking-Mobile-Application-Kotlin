package com.example.meloch.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.meloch.data.PreferencesManager
import com.example.meloch.data.UserManager
import com.example.meloch.data.CardManager
import com.example.meloch.data.model.Card
import com.example.meloch.data.model.Transaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JsonBackupGenerator(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val userManager = UserManager(context)
    private val cardManager = CardManager(context)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    fun generateBackup(): Uri? {
        try {
            // Create the JSON data
            val jsonData = createBackupJson()

            // Get current timestamp for filename
            val timestamp = dateFormatter.format(Date())
            val fileName = "Meloch_Backup_$timestamp.json"

            // Create the file
            val file: File
            val outputStream: FileOutputStream
            var uri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Meloch/Backup")
                }

                uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri == null) {
                    throw IOException("Failed to create new MediaStore record.")
                }

                // Create a temporary file to write the JSON
                file = File(context.cacheDir, fileName)
                outputStream = FileOutputStream(file)
            } else {
                // For Android 9 and below, use direct file access
                val externalDir = Environment.getExternalStorageDirectory()
                val melochDir = File(externalDir, "Meloch")
                val backupDir = File(melochDir, "Backup")

                // Create directory structure if it doesn't exist
                if (!melochDir.exists()) {
                    val mkdirsSuccess = melochDir.mkdirs()
                    if (!mkdirsSuccess && !melochDir.exists()) {
                        throw IOException("Failed to create directory: ${melochDir.absolutePath}")
                    }
                }
                if (!backupDir.exists()) {
                    val mkdirsSuccess = backupDir.mkdirs()
                    if (!mkdirsSuccess && !backupDir.exists()) {
                        throw IOException("Failed to create directory: ${backupDir.absolutePath}")
                    }
                }

                // Create the file in external storage
                file = File(backupDir, fileName)
                outputStream = FileOutputStream(file)
            }

            // Write the JSON data to the file
            outputStream.write(jsonData.toString(4).toByteArray())
            outputStream.close()

            // For Android 10+, copy the temp file to MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        file.inputStream().use { it.copyTo(os) }
                    }

                    return uri
                } catch (e: Exception) {
                    Log.e("JsonBackupGenerator", "Error copying JSON to MediaStore", e)
                    return null
                }
            } else {
                // For Android 9 and below, return FileProvider URI
                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
        } catch (e: Exception) {
            Log.e("JsonBackupGenerator", "Error generating backup: ${e.message}", e)
            return null
        }
    }

    private fun createBackupJson(): JSONObject {
        val rootJson = JSONObject()

        // Add metadata
        val metadataJson = JSONObject()
        metadataJson.put("app", "Meloch")
        metadataJson.put("version", "1.0")
        metadataJson.put("timestamp", System.currentTimeMillis())
        metadataJson.put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        rootJson.put("metadata", metadataJson)

        // Add user info
        val userJson = JSONObject()
        val currentUser = userManager.getCurrentUser()
        if (currentUser != null) {
            userJson.put("username", currentUser.username)
            userJson.put("email", currentUser.email)
        } else {
            userJson.put("username", "Unknown")
            userJson.put("email", userManager.getCurrentUserEmail() ?: "Unknown")
        }
        rootJson.put("user", userJson)

        // Add financial data
        val financialJson = JSONObject()
        financialJson.put("totalBalance", preferencesManager.getTotalBalance())
        financialJson.put("pocketMoney", cardManager.getPocketMoney())
        financialJson.put("budgetLeft", preferencesManager.getBudgetLeft())
        financialJson.put("totalBudget", preferencesManager.getMonthlyBudget())

        // Add budget categories
        val budgetCategoriesJson = JSONObject()
        budgetCategoriesJson.put("entertainment", preferencesManager.getEntertainmentBudget())
        budgetCategoriesJson.put("food", preferencesManager.getFoodBudget())
        budgetCategoriesJson.put("transport", preferencesManager.getTransportBudget())
        budgetCategoriesJson.put("lifestyle", preferencesManager.getLifestyleBudget())
        financialJson.put("budgetCategories", budgetCategoriesJson)

        rootJson.put("financial", financialJson)

        // Add cards
        val cardsJson = JSONArray()
        val cards = cardManager.getCards()
        for (card in cards) {
            val cardJson = JSONObject()
            cardJson.put("id", card.id)
            cardJson.put("cardNumber", card.cardNumber)
            cardJson.put("cardholderName", card.cardholderName)
            cardJson.put("expiryMonth", card.expiryMonth)
            cardJson.put("expiryYear", card.expiryYear)
            cardJson.put("expiryDate", card.getExpiryDate())
            cardJson.put("cvv", card.cvv)
            cardJson.put("type", card.type.name)
            cardJson.put("balance", card.balance)
            cardsJson.put(cardJson)
        }
        rootJson.put("cards", cardsJson)

        // Add transactions
        val transactionsJson = JSONArray()
        val transactions = preferencesManager.getTransactions()
        for (transaction in transactions) {
            val transactionJson = JSONObject()
            transactionJson.put("id", transaction.id)
            transactionJson.put("amount", transaction.amount)
            transactionJson.put("category", transaction.category.name)
            transactionJson.put("type", transaction.type.name)
            transactionJson.put("date", transaction.date.time)
            transactionsJson.put(transactionJson)
        }
        rootJson.put("transactions", transactionsJson)

        return rootJson
    }
}
