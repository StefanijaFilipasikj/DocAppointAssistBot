package mk.ukim.finki.docappointassistbot.ui.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mk.ukim.finki.docappointassistbot.domain.MessagesModel
import mk.ukim.finki.docappointassistbot.domain.enums.ChatRole

class MessagesViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<MessagesModel>>()
    val messages: LiveData<List<MessagesModel>> get() = _messages

    init {
        _messages.value = listOf(
            MessagesModel(
                content = "Welcome! How can I assist you today?",
                role = ChatRole.CHATBOT
            )
        )
    }

    fun addMessage(message: MessagesModel){
        val currentList = _messages.value.orEmpty()
        _messages.value = currentList + message
    }

    fun addOrUpdateMessage(contentChunk: String) {
        val currentMessages = _messages.value.orEmpty()
        val lastMessage = currentMessages.lastOrNull()

        if (lastMessage != null && lastMessage.role == ChatRole.CHATBOT) {
            val updated = lastMessage.copy(content = lastMessage.content + contentChunk)
            _messages.value = currentMessages.dropLast(1) + updated
        } else {
            _messages.value = currentMessages + MessagesModel(contentChunk, ChatRole.CHATBOT)
        }
    }

}
