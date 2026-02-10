package com.example.meloch.data.model

enum class CardType {
    CREDIT,
    DEBIT
}

data class Card(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: CardType,
    val bankName: String,
    val cardNumber: String,
    val cardholderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    var isVisible: Boolean = false,
    val balance: Double = 0.0
) {
    fun getMaskedCardNumber(): String {
        return "**** **** **** " + cardNumber.takeLast(4)
    }

    fun getMaskedCvv(): String {
        return "***"
    }

    fun getExpiryDate(): String {
        val month = if (expiryMonth < 10) "0$expiryMonth" else expiryMonth.toString()
        val year = expiryYear.toString().takeLast(2)
        return "$month/$year"
    }

    fun getMaskedExpiryDate(): String {
        return "**/**"
    }
}
