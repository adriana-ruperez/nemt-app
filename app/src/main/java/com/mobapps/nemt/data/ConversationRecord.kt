package com.mobapps.nemt.data

data class ConversationRecord(
    val id: String = "",
    val riderUid: String = "",
    val type: String = "",
    val participantIds: List<String> = emptyList(),
    val counterpartId: String = "",
    val counterpartName: String = "",
    val counterpartRole: String = "",
    val title: String = "",
    val subtitle: String = "",
    val lastMessageText: String = "",
    val lastMessageAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val createdAtMillis: Long = 0L,
    val driverId: String? = null,
    val driverName: String? = null
)

data class ConversationMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderRole: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val createdAtMillis: Long = 0L
)
