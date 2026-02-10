package com.example.meloch.data.model

import java.util.Date

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val date: Date,
    val paymentMethod: PaymentMethod
) {
    val isIncome: Boolean
        get() = type == TransactionType.INCOME

    val paymentMethodString: String
        get() = when (paymentMethod) {
            PaymentMethod.CASH -> "Cash"
            PaymentMethod.CREDIT_CARD -> "Credit Card"
            PaymentMethod.DEBIT_CARD -> "Debit Card"
        }
}

enum class TransactionType {
    INCOME, EXPENSE
}

enum class Category(val displayName: String) {
    FOOD("Food"),
    ENTERTAINMENT("Entertainment"),
    TRANSPORT("Transport"),
    HEALTH("Health"),
    SHOPPING("Shopping"),
    SALARY("Salary"),
    SIDE_BUSINESS("Side Business"),
    VACATION("Vacation")
}

enum class PaymentMethod {
    CASH, CREDIT_CARD, DEBIT_CARD
}