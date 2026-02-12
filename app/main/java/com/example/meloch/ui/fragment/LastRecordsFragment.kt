package com.example.meloch.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meloch.R
import com.example.meloch.ui.adapter.LastRecordsAdapter
import com.example.meloch.viewmodel.TransactionViewModel

class LastRecordsFragment : Fragment() {
    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: LastRecordsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_last_records, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[TransactionViewModel::class.java]
        setupRecyclerView(view)
        observeTransactions()
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        
        adapter = LastRecordsAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@LastRecordsFragment.adapter
        }
    }

    private fun observeTransactions() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyStateText.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyStateText.visibility = View.GONE
                adapter.updateItems(transactions)
            }
        }
    }
} 