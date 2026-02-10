package com.example.meloch.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meloch.R
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.Transaction
import com.example.meloch.data.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class LastRecordsAdapter : RecyclerView.Adapter<LastRecordsAdapter.ViewHolder>() {
    private var items: List<Transaction> = emptyList()
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.transactionIcon)
        val title: TextView = view.findViewById(R.id.transactionTitle)
        val paymentMethod: TextView = view.findViewById(R.id.paymentMethod)
        val amount: TextView = view.findViewById(R.id.transactionAmount)
        val date: TextView = view.findViewById(R.id.transactionDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_last_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = items[position]
        holder.title.text = transaction.title
        holder.paymentMethod.text = transaction.paymentMethod.name
        holder.date.text = dateFormatter.format(transaction.date)

        val amountText = currencyFormatter.format(transaction.amount)
        holder.amount.text = when (transaction.type) {
            TransactionType.INCOME -> "+$amountText"
            TransactionType.EXPENSE -> "-$amountText"
        }
        holder.amount.setTextColor(holder.itemView.context.getColor(
            when (transaction.type) {
                TransactionType.INCOME -> R.color.income_green
                TransactionType.EXPENSE -> R.color.expense_red
            }
        ))

        holder.icon.setImageResource(getCategoryIcon(transaction.category))
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<Transaction>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun getCategoryIcon(category: Category): Int {
        return when (category) {
            Category.FOOD -> R.drawable.ic_food
            Category.ENTERTAINMENT -> R.drawable.ic_entertainment
            Category.TRANSPORT -> R.drawable.ic_transport
            Category.HEALTH -> R.drawable.ic_health
            Category.SHOPPING -> R.drawable.ic_shopping
            Category.SALARY -> R.drawable.ic_salary
            Category.SIDE_BUSINESS -> R.drawable.ic_side_business
            Category.VACATION -> R.drawable.ic_vacation
        }
    }
}