package com.example.meloch.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.PaymentMethod
import com.example.meloch.data.model.Transaction
import com.example.meloch.data.model.TransactionType
import java.util.Date

class TransactionViewModel : ViewModel() {
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    init {
        // Initialize with an empty list
        _transactions.value = emptyList()
    }

    fun addTransaction(transaction: Transaction) {
        val currentList = _transactions.value?.toMutableList() ?: mutableListOf()
        currentList.add(transaction)
        _transactions.value = currentList
    }

    fun updateTransaction(transaction: Transaction) {
        val currentList = _transactions.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            currentList[index] = transaction
            _transactions.value = currentList
        }
    }

    fun deleteTransaction(transactionId: String) {
        val currentList = _transactions.value?.toMutableList() ?: mutableListOf()
        currentList.removeIf { it.id == transactionId }
        _transactions.value = currentList
    }

    fun removeTransaction(transaction: Transaction) {
        val currentList = _transactions.value?.toMutableList() ?: mutableListOf()
        currentList.remove(transaction)
        _transactions.value = currentList
    }

    fun getTransactions(): List<Transaction> {
        return _transactions.value ?: emptyList()
    }
}