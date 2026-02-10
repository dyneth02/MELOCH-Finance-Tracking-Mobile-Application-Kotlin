package com.example.meloch.data

data class Budget(
    val id: Long = System.currentTimeMillis(),
    val category: String,
    val amount: Double,
    val month: Int,
    val year: Int
) 