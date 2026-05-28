package com.phillips.phill.ui.comms

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.ConversationEntity
import com.phillips.phill.data.entity.MessageEntity
import com.phillips.phill.data.repository.CommsRepository
import com.phillips.phill.domain.enums.MessageStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.repository.CustomerRepository
import com.phillips.phill.data.repository.JobRepository
import com.phillips.phill.domain.enums.JobStatus
import com.phillips.phill.sms.SmsSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

import androidx.core.content.FileProvider
import com.phillips.phill.data.dao.AttachmentDao
import com.phillips.phill.data.entity.AttachmentEntity
import java.io.File

data class ConversationDetailUiState(
    val conversation: ConversationEntity? = null,
    val customer: CustomerEntity? = null,
    val activeJobsCount: Int = 0,
    val outstandingBalance: Long = 0L,
    val messages: List<MessageEntity> = emptyList(),
    val draftMessage: String = "",
    val isSending: Boolean = false,
    val sendError: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ConversationDetailViewModel @Inject constructor(
    private val commsRepository: CommsRepository,
    private val customerRepository: CustomerRepository,
    private val jobRepository: JobRepository,
    private val smsSyncManager: SmsSyncManager,
    private val attachmentDao: AttachmentDao,
    private val application: Application
) : ViewModel() {

    private var conversationId: String = ""
    private var initialized = false

    private val _uiState = MutableStateFlow(ConversationDetailUiState())
    val uiState: StateFlow<ConversationDetailUiState> = _uiState.asStateFlow()

    fun initialize(conversationId: String) {
        if (initialized) return
        initialized = true
        this.conversationId = conversationId
        loadConversation()
    }

    private fun loadConversation() {
        viewModelScope.launch {
            val conversation = commsRepository.getConversationById(conversationId)
            if (conversation == null) {
                _uiState.value = ConversationDetailUiState(isLoading = false)
                return@launch
            }

            // Trigger background sync with device SMS
            launch {
                try {
                    smsSyncManager.syncConversation(conversationId)
                } catch (e: Exception) {
                    // Ignore for now if no permission
                }
            }

            // Mark as read
            if (conversation.unreadCount > 0) {
                commsRepository.saveConversation(conversation.copy(unreadCount = 0))
            }

            // Observe relational data if a customer is linked
            val customerId = conversation.customerId
            if (customerId != null) {
                val customer = customerRepository.getCustomerById(customerId)
                loadLinkedConversation(conversation, customer)
            } else {
                // Try linking if phone number matches a customer
                val customer = customerRepository.getCustomerByPhone(conversation.phoneNumber)
                if (customer != null) {
                    val updatedConversation = conversation.copy(customerId = customer.id)
                    commsRepository.saveConversation(updatedConversation)
                    // Re-enter the linked path with the freshly linked conversation
                    loadLinkedConversation(updatedConversation, customer)
                    return@launch
                }

                // Truly un-linked — message-only collection
                commsRepository.observeMessages(conversationId)
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                    .collect { messages ->
                        _uiState.value = _uiState.value.copy(
                            conversation = conversation,
                            messages = messages,
                            isLoading = false
                        )
                    }
            }
        }
    }

    /**
     * Loads the full relational data flow for a linked conversation:
     * messages + active jobs count via combine.
     */
    private suspend fun loadLinkedConversation(conversation: ConversationEntity, customer: CustomerEntity?) {
        val custId = conversation.customerId ?: return
        combine(
            commsRepository.observeMessages(conversationId),
            jobRepository.observeByCustomer(custId)
        ) { messages, jobs ->
            val activeCount = jobs.count { it.status == JobStatus.EN_ROUTE || it.status == JobStatus.ON_SITE || it.status == JobStatus.SCHEDULED }
            _uiState.value = _uiState.value.copy(
                conversation = conversation,
                customer = customer,
                activeJobsCount = activeCount,
                messages = messages,
                isLoading = false
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Unit)
    }

    fun updateDraft(text: String) {
        _uiState.value = _uiState.value.copy(draftMessage = text, sendError = null)
    }

    /**
     * Send SMS — HITL Rule 1: Only called when operator explicitly taps "Send".
     * No autonomous sending. The operator typed the message and tapped the button.
     */
    fun sendMessage() {
        val state = _uiState.value
        val conversation = state.conversation ?: return
        val body = state.draftMessage.trim()
        if (body.isBlank()) return

        _uiState.value = state.copy(isSending = true, sendError = null)

        viewModelScope.launch {
            try {
                // Use SmsManager to send
                val smsManager = application.getSystemService(SmsManager::class.java)
                val parts = smsManager.divideMessage(body)
                smsManager.sendMultipartTextMessage(
                    conversation.phoneNumber,
                    null,
                    parts,
                    null,
                    null
                )

                // Store outbound message
                val message = MessageEntity(
                    conversationId = conversationId,
                    body = body,
                    timestampEpoch = System.currentTimeMillis(),
                    isInbound = false,
                    status = MessageStatus.SENT
                )
                commsRepository.saveMessage(message)

                // Update conversation
                commsRepository.saveConversation(
                    conversation.copy(lastMessageEpoch = System.currentTimeMillis())
                )

                _uiState.value = _uiState.value.copy(
                    draftMessage = "",
                    isSending = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    sendError = "Failed to send: ${e.message}"
                )
            }
        }
    }

    /**
     * Share a media file via Intent to the default messaging app.
     * HITL Rule 1: Operator explicitly selected the file and tapped attach.
     *
     * Steps:
     * 1. Copy the selected file to app-private shared_media/ directory
     * 2. Create a content:// URI via FileProvider
     * 3. Launch ACTION_SEND Intent
     * 4. Record the attachment in the database
     */
    fun shareMedia(context: Context, contentUri: Uri, phoneNumber: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Copy file to app-private directory for FileProvider access
                    val mediaDir = File(context.filesDir, "shared_media").apply { mkdirs() }
                    val fileName = contentUri.lastPathSegment?.substringAfterLast('/') ?: "attachment"
                    val destFile = File(mediaDir, "${System.currentTimeMillis()}_$fileName")

                    context.contentResolver.openInputStream(contentUri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@withContext

                    val mimeType = context.contentResolver.getType(contentUri) ?: "application/octet-stream"

                    // Create FileProvider URI
                    val shareUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        destFile
                    )

                    // Launch share Intent
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        putExtra("address", phoneNumber)
                        type = mimeType
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    val chooserIntent = Intent.createChooser(sendIntent, "Share via...").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooserIntent)

                    // Record in database
                    val attachment = AttachmentEntity(
                        conversationId = conversationId,
                        fileUri = destFile.absolutePath,
                        fileName = destFile.name,
                        mimeType = mimeType,
                        fileSizeBytes = destFile.length(),
                        sharedAtEpoch = System.currentTimeMillis()
                    )
                    attachmentDao.insert(attachment)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        sendError = "Failed to share: ${e.message}"
                    )
                }
            }
        }
    }
}
