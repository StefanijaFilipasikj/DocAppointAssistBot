package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import mk.ukim.finki.docappointassistbot.adapter.ChatRecyclerAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentChatbotBinding
import mk.ukim.finki.docappointassistbot.domain.MessagesModel
import mk.ukim.finki.docappointassistbot.domain.enums.ChatRole
import mk.ukim.finki.docappointassistbot.ui.viewModels.MessagesViewModel

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatRecyclerAdapter
    private lateinit var viewModel: MessagesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(MessagesViewModel::class.java)
        chatAdapter = ChatRecyclerAdapter(emptyList())
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = chatAdapter


        binding.messageSendBtn.setOnClickListener {
            viewModel.addMessage(MessagesModel(binding.chatMessageInput.text.toString(), ChatRole.USER))
            binding.chatMessageInput.setText("")
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.updateMessages(messages)
        }
    }
}