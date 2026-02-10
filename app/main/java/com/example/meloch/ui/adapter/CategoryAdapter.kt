package com.example.meloch.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meloch.R
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.TransactionType

class CategoryAdapter(
    private val transactionType: TransactionType,
    private val onCategorySelected: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.categoryIcon)
        val name: TextView = view.findViewById(R.id.categoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    // Get filtered categories based on transaction type
    private val filteredCategories: List<Category> = when (transactionType) {
        TransactionType.INCOME -> listOf(Category.SALARY, Category.SIDE_BUSINESS)
        TransactionType.EXPENSE -> Category.values().filter { it != Category.SALARY && it != Category.SIDE_BUSINESS }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = filteredCategories[position]
        holder.name.text = category.displayName
        holder.icon.setImageResource(getCategoryIcon(category))
        holder.icon.setColorFilter(holder.itemView.context.getColor(getCategoryColor(category)))

        holder.itemView.setOnClickListener {
            onCategorySelected(category)
        }
    }

    override fun getItemCount() = filteredCategories.size

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