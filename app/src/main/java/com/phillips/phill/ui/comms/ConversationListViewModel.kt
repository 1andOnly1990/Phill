package com.phillips.phill.ui.comms

import android.app.Activity
import android.telephony.SmsManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phillips.phill.data.entity.ConversationEntity
import com.phillips.phill.data.entity.CustomerEntity
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

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val commsRepository: CommsRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationWithCustomer>>(emptyList())
    val conversations: StateFlow<List<ConversationWithCustomer>> = _conversations.asStateFlow()

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

    /**
     * Link a conversation to a customer record (manual linking per §10 Function 1).
     */
    fun linkToCustomer(conversationId: String, customerId: String) {
        viewModelScope.launch {
            val convo = commsRepository.getConversationById(conversationId) ?: return@launch
            commsRepository.saveConversation(convo.copy(customerId = customerId))
        }
    }
}
