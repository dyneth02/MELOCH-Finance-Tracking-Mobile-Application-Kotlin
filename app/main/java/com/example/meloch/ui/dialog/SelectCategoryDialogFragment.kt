package com.example.meloch.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.TransactionType
import com.example.meloch.databinding.DialogSelectCategoryBinding
import com.example.meloch.ui.adapter.CategoryAdapter

class SelectCategoryDialogFragment(
    private val transactionType: TransactionType,
    private val onCategorySelected: (Category) -> Unit
) : DialogFragment() {

    private var _binding: DialogSelectCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSelectCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.categoriesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.categoriesRecyclerView.adapter = CategoryAdapter(
            transactionType = transactionType,
            onCategorySelected = { category ->
                onCategorySelected(category)
                dismiss()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}