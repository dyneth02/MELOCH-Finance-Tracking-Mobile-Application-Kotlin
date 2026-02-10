package com.example.meloch.data

import java.util.Date

data class Transaction(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val amount: Double,
    val category: String,
    val type: TransactionType,
    val date: Date,
    val paymentMethod: String
)

enum class TransactionType {
    INCOME,
    EXPENSE
} 