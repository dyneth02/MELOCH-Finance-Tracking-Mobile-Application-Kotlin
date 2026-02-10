package com.example.meloch.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.meloch.R
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class RecordsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<Any>()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("LKR")
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TRANSACTION = 1
    }

    fun submitList(newItems: List<Any>?) {
        if (newItems == null) {
            // If null, just clear the list
            val oldSize = items.size
            items.clear()
            notifyItemRangeRemoved(0, oldSize)
            return
        }

        // Clear and add new items
        items.clear()
        items.addAll(newItems)

        // Notify adapter of the change
        notifyDataSetChanged()
    }

    fun getTransactionAt(position: Int): Transaction {
        return items[position] as Transaction
    }

    fun removeTransaction(position: Int) {
        if (position >= 0 && position < items.size && items[position] is Transaction) {
            items.removeAt(position)
            notifyItemRemoved(position)

            // Check if we need to remove a header
            if (position > 0 && position < items.size &&
                items[position - 1] is String && (position == items.size || items[position] !is Transaction)) {
                items.removeAt(position - 1)
                notifyItemRemoved(position - 1)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is String -> TYPE_HEADER
            is Transaction -> TYPE_TRANSACTION
            else -> throw IllegalArgumentException("Unknown view type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            TYPE_TRANSACTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_record, parent, false)
                TransactionViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateHeaderViewHolder -> {
                holder.bind(items[position] as String)
            }
            is TransactionViewHolder -> {
                holder.bind(items[position] as Transaction)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.dateHeaderText)

        fun bind(date: String) {
            dateText.text = date
        }
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
        private val categoryBackground: View = itemView.findViewById(R.id.categoryIconBackground)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitleText)
        private val amountText: TextView = itemView.findViewById(R.id.amountText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)

        fun bind(transaction: Transaction) {
            // Set category icon and background
            setCategoryIconAndBackground(transaction.category)

            // Set title and subtitle
            titleText.text = transaction.category.displayName
            subtitleText.text = transaction.paymentMethodString

            // Set amount with color
            val prefix = if (transaction.isIncome) "+" else "-"
            amountText.text = "$prefix${currencyFormatter.format(transaction.amount)}"
            amountText.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (transaction.isIncome) R.color.income_green else R.color.expense_red
                )
            )

            // Set date
            dateText.text = dateFormat.format(transaction.date)
        }

        private fun setCategoryIconAndBackground(category: Category) {
            // Set icon based on category
            val iconResId = when (category) {
                Category.FOOD -> R.drawable.ic_food
                Category.ENTERTAINMENT -> R.drawable.ic_entertainment
                Category.TRANSPORT -> R.drawable.ic_transport
                Category.HEALTH -> R.drawable.ic_health
                Category.SHOPPING -> R.drawable.ic_shopping
                Category.SALARY -> R.drawable.ic_salary
                Category.SIDE_BUSINESS -> R.drawable.ic_business
                Category.VACATION -> R.drawable.ic_vacation
            }

            categoryIcon.setImageResource(iconResId)

            // Set background color based on category
            val colorResId = when (category) {
                Category.FOOD -> R.color.category_food
                Category.ENTERTAINMENT -> R.color.category_entertainment
                Category.TRANSPORT -> R.color.category_transport
                Category.HEALTH -> R.color.category_health
                Category.SHOPPING -> R.color.category_shopping
                Category.SALARY -> R.color.category_salary
                Category.SIDE_BUSINESS -> R.color.category_side_business
                Category.VACATION -> R.color.category_vacation
            }

            categoryBackground.backgroundTintList =
                ContextCompat.getColorStateList(itemView.context, colorResId)
        }
    }
}
