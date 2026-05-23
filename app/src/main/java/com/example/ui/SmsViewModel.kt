package com.example.ui

import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.SmsLog
import com.example.data.model.SmsTemplate
import com.example.data.repository.SmsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class RateLimitResult {
    data class ExceededMinute(val current: Int, val limit: Int) : RateLimitResult()
    data class ExceededHour(val current: Int, val limit: Int) : RateLimitResult()
    data class NotExceeded(val currentMin: Int, val currentHour: Int) : RateLimitResult()
}

class SmsViewModel(private val repository: SmsRepository) : ViewModel() {

    var targetNumber = MutableStateFlow("")
        private set
    var messagePattern = MutableStateFlow("SMS Attack Stress Packet #{index} [Ref: {rand}]")
        private set
    var repeatCount = MutableStateFlow("15")
        private set
    var delayMs = MutableStateFlow("600") // 600 ms gap
        private set
    var templateName = MutableStateFlow("")
        private set

    // Rate Limiting parameters
    var maxPerMinute = MutableStateFlow("30")
        private set
    var maxPerHour = MutableStateFlow("150")
        private set

    private val _currentMinCount = MutableStateFlow(0)
    val currentMinCount: StateFlow<Int> = _currentMinCount.asStateFlow()

    private val _currentHourCount = MutableStateFlow(0)
    val currentHourCount: StateFlow<Int> = _currentHourCount.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _currentCount = MutableStateFlow(0)
    val currentCount: StateFlow<Int> = _currentCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready for bulk SMS blast testing.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isSimulatorMode = MutableStateFlow(true) // Safe Simulator mode default
    val isSimulatorMode: StateFlow<Boolean> = _isSimulatorMode.asStateFlow()

    val savedTemplates: StateFlow<List<SmsTemplate>> = repository.allTemplates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val executionLogs: StateFlow<List<SmsLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var activeSendingJob: Job? = null

    init {
        refreshRateCounts()
    }

    fun setTargetNumber(value: String) {
        targetNumber.value = value
    }

    fun setMessagePattern(value: String) {
        messagePattern.value = value
    }

    fun setRepeatCount(value: String) {
        repeatCount.value = value
    }

    fun setDelayMs(value: String) {
        delayMs.value = value
    }

    fun setTemplateName(value: String) {
        templateName.value = value
    }

    fun setMaxPerMinute(value: String) {
        maxPerMinute.value = value
        refreshRateCounts()
    }

    fun setMaxPerHour(value: String) {
        maxPerHour.value = value
        refreshRateCounts()
    }

    fun setSimulatorMode(value: Boolean) {
        _isSimulatorMode.value = value
    }

    fun refreshRateCounts() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _currentMinCount.value = repository.getSentCountSince(now - 60_000L)
            _currentHourCount.value = repository.getSentCountSince(now - 3_600_000L)
        }
    }

    suspend fun checkRateLimitsExceeded(): RateLimitResult {
        val limitMin = maxPerMinute.value.toIntOrNull() ?: Int.MAX_VALUE
        val limitHour = maxPerHour.value.toIntOrNull() ?: Int.MAX_VALUE

        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000L
        val oneHourAgo = now - 3_600_000L

        val countMin = repository.getSentCountSince(oneMinuteAgo)
        val countHour = repository.getSentCountSince(oneHourAgo)

        _currentMinCount.value = countMin
        _currentHourCount.value = countHour

        if (countMin >= limitMin) {
            return RateLimitResult.ExceededMinute(countMin, limitMin)
        }
        if (countHour >= limitHour) {
            return RateLimitResult.ExceededHour(countHour, limitHour)
        }
        return RateLimitResult.NotExceeded(countMin, countHour)
    }

    fun loadTemplate(template: SmsTemplate) {
        targetNumber.value = template.targetNumber
        messagePattern.value = template.messagePattern
        repeatCount.value = template.repeatCount.toString()
        delayMs.value = template.delayMs.toString()
        _statusMessage.value = "Loaded template: '${template.name}'"
    }

    fun saveCurrentAsTemplate() {
        val name = templateName.value.ifBlank { "Template " + (savedTemplates.value.size + 1) }
        val count = repeatCount.value.toIntOrNull() ?: 15
        val delayValue = delayMs.value.toLongOrNull() ?: 600L

        viewModelScope.launch {
            repository.insertTemplate(
                SmsTemplate(
                    name = name,
                    targetNumber = targetNumber.value,
                    messagePattern = messagePattern.value,
                    repeatCount = count,
                    delayMs = delayValue
                )
            )
            templateName.value = ""
            _statusMessage.value = "Saved template '$name'"
        }
    }

    fun deleteTemplate(id: Int) {
        viewModelScope.launch {
            repository.deleteTemplateById(id)
        }
    }

    fun deleteLogById(id: Int) {
        viewModelScope.launch {
            repository.deleteLogById(id)
            refreshRateCounts()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _statusMessage.value = "History transmission logs cleared."
            refreshRateCounts()
        }
    }

    fun stopSending() {
        activeSendingJob?.cancel()
        _isSending.value = false
        _statusMessage.value = "Sending stopped by operator."
    }

    fun startSending(context: Context) {
        if (_isSending.value) return

        val number = targetNumber.value.trim()
        if (number.isEmpty()) {
            _statusMessage.value = "Error: Input target receiver number required!"
            return
        }

        val pattern = messagePattern.value
        val count = repeatCount.value.toIntOrNull() ?: 0
        if (count <= 0) {
            _statusMessage.value = "Error: Bomb amount must be positive!"
            return
        }

        val parsedDelay = delayMs.value.toLongOrNull() ?: 600L
        if (parsedDelay < 50) {
            _statusMessage.value = "Error: Interval delay must be at least 50ms!"
            return
        }

        activeSendingJob = viewModelScope.launch {
            // Initial safety check
            val initialCheck = checkRateLimitsExceeded()
            if (initialCheck is RateLimitResult.ExceededMinute) {
                _statusMessage.value = "Halted: Minute limit of ${initialCheck.limit} exceeded! (Sent: ${initialCheck.current})"
                _isSending.value = false
                return@launch
            } else if (initialCheck is RateLimitResult.ExceededHour) {
                _statusMessage.value = "Halted: Hourly limit of ${initialCheck.limit} exceeded! (Sent: ${initialCheck.current})"
                _isSending.value = false
                return@launch
            }

            _isSending.value = true
            _currentCount.value = 0
            _totalCount.value = count
            _statusMessage.value = "Firing SMS array sequence..."

            val smsManager: SmsManager? = if (!_isSimulatorMode.value) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        context.getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

            for (i in 1..count) {
                _currentCount.value = i - 1

                // Check limit continuously inside dispatch iteration
                val rateCheck = checkRateLimitsExceeded()
                if (rateCheck is RateLimitResult.ExceededMinute) {
                    _statusMessage.value = "Halted: Min Limit of ${rateCheck.limit} Exceeded! (${rateCheck.current}/${rateCheck.limit})"
                    break
                } else if (rateCheck is RateLimitResult.ExceededHour) {
                    _statusMessage.value = "Halted: Hr Limit of ${rateCheck.limit} Exceeded! (${rateCheck.current}/${rateCheck.limit})"
                    break
                }

                val messageText = pattern
                    .replace("{index}", i.toString())
                    .replace("{rand}", (100000..999999).random().toString())
                    .replace("{time}", (System.currentTimeMillis() / 1000).toString())

                if (!isSending.value) break

                if (_isSimulatorMode.value) {
                    delay(parsedDelay)
                    // High-speed simulation delivery logs
                    val isSuccess = (1..100).random() <= 94
                    val log = SmsLog(
                        targetNumber = number,
                        messageText = messageText,
                        status = if (isSuccess) "SIMULATED_SENT" else "SIMULATED_FAILED",
                        errorMessage = if (isSuccess) null else "Carrier Simulation Spacing Fail"
                    )
                    repository.insertLog(log)
                    refreshRateCounts()
                    _currentCount.value = i
                    _statusMessage.value = "Simulated injection packet $i of $count"
                } else {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPerm) {
                        val log = SmsLog(
                            targetNumber = number,
                            messageText = messageText,
                            status = "FAILED",
                            errorMessage = "SmsPermission SEND_SMS is missing."
                        )
                        repository.insertLog(log)
                        refreshRateCounts()
                        _statusMessage.value = "Aborted: Dynamic SEND_SMS permission missing."
                        _isSending.value = false
                        return@launch
                    }

                    try {
                        if (smsManager != null) {
                            smsManager.sendTextMessage(number, null, messageText, null, null)
                            val log = SmsLog(
                                targetNumber = number,
                                messageText = messageText,
                                status = "SENT"
                            )
                            repository.insertLog(log)
                        } else {
                            throw Exception("Carrier system hardware unavailable")
                        }
                    } catch (e: Exception) {
                        val log = SmsLog(
                            targetNumber = number,
                            messageText = messageText,
                            status = "FAILED",
                            errorMessage = e.message ?: "Carrier Transmission Blocked"
                        )
                        repository.insertLog(log)
                    }
                    refreshRateCounts()
                    delay(parsedDelay)
                    _currentCount.value = i
                    _statusMessage.value = "Dispatched packet $i of $count"
                }
            }

            _isSending.value = false
            // Keep actual limit status text if halted mid-blast
            val finalCheck = checkRateLimitsExceeded()
            if (finalCheck is RateLimitResult.ExceededMinute) {
                _statusMessage.value = "Halted: Min Limit of ${finalCheck.limit} Exceeded! (${finalCheck.current}/${finalCheck.limit})"
            } else if (finalCheck is RateLimitResult.ExceededHour) {
                _statusMessage.value = "Halted: Hr Limit of ${finalCheck.limit} Exceeded! (${finalCheck.current}/${finalCheck.limit})"
            } else {
                _statusMessage.value = "Blast session completed! Sent $count packets."
            }
        }
    }
}

class SmsViewModelFactory(private val repository: SmsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SmsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
