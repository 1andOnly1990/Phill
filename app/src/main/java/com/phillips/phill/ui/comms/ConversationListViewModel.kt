package com.phillips.phill.ui.comms

import android.app.Activity
import android.telephony.SmsManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.ConversationEntity
import com.phillips.phill.data.entity.CustomerEntity
import com.phillips.phill.data.entity.MessageEntity
import com.phillips.phill.data.repository.CommsRepository
import com.phillips.phill.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationWithCustomer(
    val conversation: ConversationEntity,
    val customer: CustomerEntity? = null
)

data class MessageSearchResult(
    val message: MessageEntity,
    val conversationId: String,
    val displayName: String
)

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val commsRepository: CommsRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationWithCustomer>>(emptyList())
    val conversations: StateFlow<List<ConversationWithCustomer>> = _conversations.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MessageSearchResult>>(emptyList())
    val searchResults: StateFlow<List<MessageSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            commsRepository.observeAllConversations()
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .collect { convos ->
                    val withCustomers = convos.map { convo ->
                        val customer = convo.customerId?.let { customerRepository.getCustomerById(it) }
                            ?: customerRepository.getCustomerByPhone(convo.phoneNumber)
                        ConversationWithCustomer(convo, customer)
                    }
                    _conversations.value = withCustomers
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            commsRepository.searchMessages(query)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .collect { messages ->
                    val results = messages.take(50).mapNotNull { msg ->
                        val convo = commsRepository.getConversationById(msg.conversationId)
                            ?: return@mapNotNull null
                        val customer = convo.customerId?.let { customerRepository.getCustomerById(it) }
                        val displayName = customer?.let { "${it.firstName} ${it.lastName}" }
                            ?: convo.displayName
                            ?: convo.phoneNumber
                        MessageSearchResult(
                            message = msg,
                            conversationId = msg.conversationId,
                            displayName = displayName
                        )
                    }
                    _searchResults.value = results
                }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    /**
     * Link a conversation to a customer record (manual linking per §10 Function 1).
     */
    fun linkToCustomer(conversationId: String, customerId: String) {
        viewModelScope.launch {
            val convo = commsRepository.getConversationById(conversationId) ?: return@launch
            commsRepository.saveConversation(convo.copy(customerId = customerId))
        }
    }

    /**
     * Dismiss a conversation (mark as not a lead / delete from Phill Comms).
     * The conversation and its messages are removed from Phill's database.
     * The original SMS remain in the device's native messaging app.
     */
    fun dismissConversation(conversationId: String) {
        viewModelScope.launch {
            val convo = commsRepository.getConversationById(conversationId) ?: return@launch
            commsRepository.deleteConversation(convo)
        }
    }
}
