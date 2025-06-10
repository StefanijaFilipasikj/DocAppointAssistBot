package mk.ukim.finki.docappointassistbot.ui.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mk.ukim.finki.docappointassistbot.domain.MessagesModel
import mk.ukim.finki.docappointassistbot.domain.enums.ChatRole

class MessagesViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<MessagesModel>>()
    val messages: LiveData<List<MessagesModel>> get() = _messages

    fun addMessage(message: MessagesModel){
        val currentList = _messages.value.orEmpty()
        _messages.value = currentList + message
    }

    init {
        _messages.value = listOf(
            MessagesModel(
                message = "Welcome! How can I assist you today?",
                role = ChatRole.CHATBOT
            )
        )
    }

}
