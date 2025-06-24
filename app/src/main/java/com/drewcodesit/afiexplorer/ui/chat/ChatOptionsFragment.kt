package com.drewcodesit.afiexplorer.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.database.favorites.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.preloaded.AFITopics
import com.drewcodesit.afiexplorer.databinding.FragmentChatOptionsBinding
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.objects.LineDividerItemDecoration
import com.drewcodesit.afiexplorer.utils.toast.ToastType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatOptionsFragment : Fragment() {

    private var _binding: FragmentChatOptionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatOptionsViewModel
    private lateinit var chatOptionsAdapter: ChatOptionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repository = ChatOptionsRepository(FavoriteDatabase.getDatabase(requireContext()).afiTopicDAO()!!)
        val factory = ChatOptionsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ChatOptionsViewModel::class.java]

        chatOptionsAdapter = ChatOptionsAdapter(emptyList()) {topic ->
            val bundle = Bundle().apply {
                putInt("pubId", topic.pubId)
                putString("title", topic.title)
            }
            findNavController().navigate(R.id.action_chatOptions_navigateto_chat, bundle)
        }

        binding.topicRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.topicRecyclerView.adapter = chatOptionsAdapter
        binding.topicRecyclerView.addItemDecoration(
            LineDividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL,
                1
            )
        )

        lifecycleScope.launch {
            viewModel.topics.collectLatest { topics ->
                chatOptionsAdapter.updateTopics(topics)
            }
        }
    }
}