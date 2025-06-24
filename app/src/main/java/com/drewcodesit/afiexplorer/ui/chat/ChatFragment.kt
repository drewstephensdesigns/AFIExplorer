package com.drewcodesit.afiexplorer.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.drewcodesit.afiexplorer.MainActivity
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.favorites.FavoriteDatabase
import com.drewcodesit.afiexplorer.databinding.FragmentChatBinding
import com.drewcodesit.afiexplorer.models.ChatMessage
import com.drewcodesit.afiexplorer.utils.Config.showToast
import com.drewcodesit.afiexplorer.utils.toast.ToastType

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatViewModel
    private lateinit var chatAdapter: ChatAdapter
    private var pubId: Int = -1
    private var pubTitle: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Receive pubId from args
        pubId = arguments?.getInt("pubId", -1) ?: -1
        pubTitle = arguments?.getString("title")


        (activity as MainActivity).supportActionBar?.title = pubTitle

        if (pubId == -1) {
            Log.e("ChatFragment", "Invalid or missing pubId")
            showToast(
                requireContext(),
                "No valid data source",
                ToastType.ERROR,
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_error)
            )
            return
        }

        val vectorDAO = FavoriteDatabase.getDatabase(requireContext()).vectorDAO()
        viewModel =
            ViewModelProvider(this, ChatViewModelFactory(vectorDAO!!))[ChatViewModel::class.java]

        chatAdapter = ChatAdapter(mutableListOf())
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }

        // hides AI Disclaimer Message
        binding.aiDisclaimerDismiss.setOnClickListener {
            binding.aiDisclaimerCard.visibility = View.GONE
        }

        binding.sendButton.setOnClickListener {
            binding.loading.visibility = View.VISIBLE
            val question = binding.queryInput.text.toString().trim()
            if (question.isNotEmpty()) {
                sendMessage(question)
            } else {
                binding.loading.visibility = View.GONE
                showToast(requireContext(), "Please enter a question", ToastType.WARNING, null)
            }
        }
    }

    private fun sendMessage(question: String) {
        chatAdapter.addMessage(ChatMessage(question, true))
        binding.queryInput.text?.clear()
        binding.queryEmptyText.visibility = View.GONE

        viewModel.askGemini(pubId, question) { answer ->
            binding.loading.visibility = View.GONE
            if (answer != null) {
                chatAdapter.addMessage(ChatMessage(answer, false))
            } else {
                chatAdapter.addMessage(ChatMessage("No answer found.", false))
            }
            binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

