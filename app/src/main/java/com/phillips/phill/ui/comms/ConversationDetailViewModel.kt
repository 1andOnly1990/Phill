package com.phillips.phill.ui.comms

import android.app.Application
import android.telephony.SmsManager
import androidx.lifecycle.SavedStateHandle
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
    savedStateHandle: SavedStateHandle,
    private val commsRepository: CommsRepository,
    private val customerRepository: CustomerRepository,
    private val jobRepository: JobRepository,
    private val smsSyncManager: SmsSyncManager,
    private val application: Application
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: ""

    private val _uiState = MutableStateFlow(ConversationDetailUiState())
    val uiState: StateFlow<ConversationDetailUiState> = _uiState.asStateFlow()

    init {
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
                // Combine messages, customer, and jobs flows
                val customer = customerRepository.getCustomerById(customerId)
                combine(
                    commsRepository.observeMessages(conversationId),
                    jobRepository.observeByCustomer(customerId)
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
            } else {
                // Try linking if phone number matches a customer
                val customer = customerRepository.getCustomerByPhone(conversation.phoneNumber)
                if (customer != null) {
                    commsRepository.saveConversation(conversation.copy(customerId = customer.id))
                    // Re-load will be triggered implicitly or next time, but let's just do a basic load for now
                }
                
                commsRepository.observeMessages(conversationId)
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                    .collect { messages ->
                        _uiState.value = _uiState.value.copy(
                            conversation = conversation,
                            customer = customer,
                            messages = messages,
                            isLoading = false
                        )
                    }
            }
        }
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
}
