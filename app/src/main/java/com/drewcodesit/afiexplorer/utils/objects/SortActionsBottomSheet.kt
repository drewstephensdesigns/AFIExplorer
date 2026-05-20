package com.drewcodesit.afiexplorer.utils.objects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.BottomSheetSortMenuBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SortActionsBottomSheet(
    private val hasItems: Boolean,
    private val listener: SortActionListener
): BottomSheetDialogFragment() {

    private var _binding: BottomSheetSortMenuBinding? = null
    private val binding get() = _binding!!

    interface SortActionListener{
        fun onSortByTitle()
        fun onSortByNumber()
        fun onDeleteAll()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSortMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSortTitle.setOnClickListener {
            listener.onSortByTitle()
            dismiss()
        }

        binding.btnSortNumber.setOnClickListener {
            listener.onSortByNumber()
            dismiss()
        }

        binding.btnDeleteAll.apply {
            setEnabled(hasItems)
            setAlpha(if (hasItems) 1f else 0.4f)

            setOnClickListener {
                if (hasItems) {
                    showDeleteConfirmation()
                }
            }
        }
    }

        private fun showDeleteConfirmation() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Everything")
                .setMessage(R.string.action_nuke_database)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    listener.onDeleteAll()
                    dismiss()
                }
                .show()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }