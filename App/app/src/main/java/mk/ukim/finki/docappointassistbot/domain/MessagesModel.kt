package mk.ukim.finki.docappointassistbot.domain

import mk.ukim.finki.docappointassistbot.domain.enums.ChatRole

data class MessagesModel(
    val content: String,
    val role: ChatRole
)