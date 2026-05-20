package com.drewcodesit.afiexplorer.utils.objects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.BottomsheetActionsBinding
import com.drewcodesit.afiexplorer.models.Pubs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ActionsBottomSheet(
    private val publications: Pubs,
    private val config: ActionConfig,
    private val onAction: (Action) -> Unit
) : BottomSheetDialogFragment() {

    data class ActionConfig(
        val showSave: Boolean = true,
        val showDelete: Boolean = false,
        val showDownload: Boolean = true
    )

    sealed class Action {
        object Save : Action()
        object CopyURL : Action()
        object Share : Action()
        object Download : Action()
        object Delete : Action()
    }

    private var _binding: BottomsheetActionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Brevity and uniformity for publication changes
        val actionText = when (publications.pubLastAction) {
            "GM" -> getString(R.string.guidance_memorandum)
            "AC" -> getString(R.string.ac)
            "IC" -> getString(R.string.interim_change)
            "UpdateContact" -> getString(R.string.update_contact)
            "Rewrite" -> getString(R.string.rewrite)
            "Transfer" -> getString(R.string.transfer)
            "Correction" -> getString(R.string.correction)
            "CertifiedCurrent" -> getString(R.string.certified_current)
            else -> getString(R.string.unknown_action)
        }

        // Adds publication number and title to bottom sheet
        binding.pubNumber.text = publications.pubNumber
        binding.pubTitle.text = publications.pubTitle
        binding.pubLastAction.text = getString(
            R.string.pub_last_action_format,
            actionText,
            publications.getCertDate()
        )

        // Configure button visibility based on where bottom sheet is used
        binding.btnSave.isVisible = config.showSave
        binding.btnDelete.isVisible = config.showDelete
        binding.btnDownload.isVisible = config.showDownload

        // Set click listeners for each action button
        binding.btnSave.setOnClickListener {
            onAction(Action.Save)
            dismiss()
        }

        binding.btnCopy.setOnClickListener {
            onAction(Action.CopyURL)
            dismiss()
        }

        binding.btnShare.setOnClickListener {
            onAction(Action.Share)
            dismiss()
        }

        binding.btnDownload.setOnClickListener {
            onAction(Action.Download)
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            onAction(Action.Delete)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}