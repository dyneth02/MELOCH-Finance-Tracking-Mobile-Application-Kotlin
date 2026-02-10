package com.example.meloch.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meloch.R
import com.example.meloch.data.model.Category
import java.text.NumberFormat
import java.util.*

class CategoryLegendAdapter : RecyclerView.Adapter<CategoryLegendAdapter.ViewHolder>() {
    private var items: List<Pair<Category, Double>> = emptyList()
    private val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("LKR")
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryName: TextView = view.findViewById(R.id.categoryName)
        val categoryAmount: TextView = view.findViewById(R.id.categoryAmount)
        val categoryColor: View = view.findViewById(R.id.categoryColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_legend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (category, amount) = items[position]
        holder.categoryName.text = category.displayName
        holder.categoryAmount.text = currencyFormatter.format(amount)
        holder.categoryColor.setBackgroundResource(getCategoryColor(category))
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: Map<Category, Double>) {
        items = newItems.toList()
        notifyDataSetChanged()
    }

    private fun getCategoryColor(category: Category): Int {
        return when (category) {
            Category.FOOD -> R.color.category_food
            Category.ENTERTAINMENT -> R.color.category_entertainment
            Category.TRANSPORT -> R.color.category_transport
            Category.HEALTH -> R.color.category_health
            Category.SHOPPING -> R.color.category_shopping
            Category.SALARY -> R.color.primary
            Category.SIDE_BUSINESS -> R.color.category_side_business
            Category.VACATION -> R.color.primary
        }
    }
}