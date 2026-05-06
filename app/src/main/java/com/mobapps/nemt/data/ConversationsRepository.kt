package com.mobapps.nemt.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ConversationsRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val conversationsCollection by lazy { firestore.collection("conversations") }
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _conversations = MutableStateFlow<List<ConversationRecord>>(emptyList())
    val conversations: StateFlow<List<ConversationRecord>> = _conversations.asStateFlow()

    private val messagesState = mutableMapOf<String, MutableStateFlow<List<ConversationMessage>>>()
    private var conversationsListener: ListenerRegistration? = null
    private val messagesListeners = mutableMapOf<String, ListenerRegistration>()

    fun startConversationsListener(riderUid: String) {
        conversationsListener?.remove()
        attachOrderedConversationsListener(riderUid)
    }

    fun stopConversationsListener() {
        conversationsListener?.remove()
        conversationsListener = null
    }

    fun messages(conversationId: String): StateFlow<List<ConversationMessage>> =
        messagesState.getOrPut(conversationId) { MutableStateFlow(emptyList()) }.asStateFlow()

    fun startMessagesListener(conversationId: String) {
        messagesListeners[conversationId]?.remove()
        val state = messagesState.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        messagesListeners[conversationId] = conversationsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                state.value = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toConversationMessage() }
            }
    }

    fun stopMessagesListener(conversationId: String) {
        messagesListeners.remove(conversationId)?.remove()
    }

    fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            onComplete(Result.failure(IllegalArgumentException("Message cannot be empty.")))
            return
        }
        val optimisticMessage = ConversationMessage(
            id = "local_${System.currentTimeMillis()}",
            conversationId = conversationId,
            senderRole = "rider",
            senderId = senderId,
            senderName = senderName,
            text = trimmed,
            createdAtMillis = System.currentTimeMillis()
        )
        appendOptimisticMessage(conversationId, optimisticMessage)
        repositoryScope.launch {
            runCatching {
                sendMessageHttp(
                    conversationId = conversationId,
                    senderId = senderId,
                    senderName = senderName,
                    text = trimmed
                )
            }.fold(
                onSuccess = {
                    withContext(Dispatchers.Main) {
                        onComplete(Result.success(Unit))
                    }
                },
                onFailure = {
                    removeOptimisticMessage(conversationId, optimisticMessage.id)
                    withContext(Dispatchers.Main) {
                        onComplete(Result.failure(it))
                    }
                }
            )
        }
    }

    private fun DocumentSnapshot.toConversationRecord(): ConversationRecord? {
        val riderUid = getString("riderUid") ?: return null
        val participantIds = (get("participantIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val createdAt = (get("createdAt") as? Timestamp)?.toDate()?.time ?: 0L
        val updatedAt = (get("updatedAt") as? Timestamp)?.toDate()?.time ?: createdAt
        val lastMessageAt = (get("lastMessageAt") as? Timestamp)?.toDate()?.time ?: updatedAt
        return ConversationRecord(
            id = getString("id") ?: id,
            riderUid = riderUid,
            type = getString("type").orEmpty(),
            participantIds = participantIds,
            counterpartId = getString("counterpartId").orEmpty(),
            counterpartName = getString("counterpartName").orEmpty(),
            counterpartRole = getString("counterpartRole").orEmpty(),
            title = getString("title").orEmpty(),
            subtitle = getString("subtitle").orEmpty(),
            lastMessageText = getString("lastMessageText").orEmpty(),
            lastMessageAtMillis = lastMessageAt,
            updatedAtMillis = updatedAt,
            createdAtMillis = createdAt,
            driverId = getString("driverId"),
            driverName = getString("driverName")
        )
    }

    private fun DocumentSnapshot.toConversationMessage(): ConversationMessage? {
        val createdAt = (get("createdAt") as? Timestamp)?.toDate()?.time ?: 0L
        return ConversationMessage(
            id = id,
            conversationId = getString("conversationId").orEmpty(),
            senderRole = getString("senderRole").orEmpty(),
            senderId = getString("senderId").orEmpty(),
            senderName = getString("senderName").orEmpty(),
            text = getString("text").orEmpty(),
            createdAtMillis = createdAt
        )
    }

    private fun attachOrderedConversationsListener(riderUid: String) {
        conversationsListener = conversationsCollection
            .whereEqualTo("riderUid", riderUid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (
                    error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                ) {
                    conversationsListener?.remove()
                    attachFallbackConversationsListener(riderUid)
                    return@addSnapshotListener
                }
                if (error != null) {
                    _conversations.value = emptyList()
                    return@addSnapshotListener
                }
                _conversations.value = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toConversationRecord() }
            }
    }

    private fun attachFallbackConversationsListener(riderUid: String) {
        conversationsListener = conversationsCollection
            .whereEqualTo("riderUid", riderUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _conversations.value = emptyList()
                    return@addSnapshotListener
                }
                _conversations.value = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toConversationRecord() }
                    .sortedByDescending { it.updatedAtMillis }
            }
    }

    private suspend fun sendMessageHttp(
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String
    ) {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("You must be signed in to send messages.")
        val idToken = currentUser.getIdToken(true).await().token
            ?: throw IllegalStateException("Unable to refresh session token.")

        val url = URL("https://us-central1-nemt-mobapps-423cf.cloudfunctions.net/sendMessageHttp")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $idToken")
        }

        try {
            val payload = JSONObject().apply {
                put("conversationId", conversationId)
                put("text", text)
                put("senderId", senderId)
                put("senderName", senderName)
            }
            connection.outputStream.bufferedWriter().use { it.write(payload.toString()) }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = runCatching {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }.getOrDefault("")
                throw IllegalStateException(parseSendMessageError(errorBody, responseCode))
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSendMessageError(body: String, responseCode: Int): String {
        val apiError = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
        return when {
            apiError == "conversation-not-found" -> "Conversation not found."
            apiError == "permission-denied" -> "You cannot send messages in this conversation."
            apiError == "missing-auth-token" || apiError == "invalid-auth-token" ->
                "Your session expired. Sign in again and retry."
            apiError.isNotBlank() -> apiError
            else -> "Unable to send message ($responseCode)."
        }
    }

    private fun appendOptimisticMessage(
        conversationId: String,
        message: ConversationMessage
    ) {
        val state = messagesState.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        state.value = (state.value + message).sortedBy { it.createdAtMillis }
        _conversations.value = _conversations.value.map { conversation ->
            if (conversation.id != conversationId) conversation else conversation.copy(
                lastMessageText = message.text,
                lastMessageAtMillis = message.createdAtMillis,
                updatedAtMillis = message.createdAtMillis
            )
        }.sortedByDescending { it.updatedAtMillis }
    }

    private fun removeOptimisticMessage(conversationId: String, messageId: String) {
        val state = messagesState[conversationId] ?: return
        state.value = state.value.filterNot { it.id == messageId }
    }
}
