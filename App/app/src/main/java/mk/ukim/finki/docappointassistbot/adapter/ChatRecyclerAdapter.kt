package mk.ukim.finki.docappointassistbot.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mk.ukim.finki.docappointassistbot.databinding.ChatMessageRecyclerRowBinding
import mk.ukim.finki.docappointassistbot.domain.MessagesModel
import mk.ukim.finki.docappointassistbot.domain.enums.ChatRole


class ChatRecyclerAdapter(private var messages: List<MessagesModel>) :
    RecyclerView.Adapter<ChatRecyclerAdapter.ViewHolder>() {

    class ViewHolder(val binding: ChatMessageRecyclerRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ChatMessageRecyclerRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = messages[position]
        holder.apply {
            binding.apply {
                if (currentItem.role == ChatRole.CHATBOT) {
                    rightChatLayout.visibility = View.GONE
                    leftChatLayout.visibility = View.VISIBLE
                    leftChatTextView.text = currentItem.content
                } else {
                    leftChatLayout.visibility = View.GONE;
                    rightChatLayout.visibility = View.VISIBLE;
                    rightChatTextView.text = currentItem.content
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun updateMessages(newMessages: List<MessagesModel>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
