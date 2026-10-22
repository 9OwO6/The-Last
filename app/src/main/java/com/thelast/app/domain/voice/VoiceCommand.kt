package com.thelast.app.domain.voice

sealed interface VoiceCommand {
    data class RecordEvent(
        val eventName: String,
        val note: String?,
        val rawText: String,
    ) : VoiceCommand

    data class CreateEvent(
        val eventName: String,
        val rawText: String,
    ) : VoiceCommand

    data class Unknown(
        val rawText: String,
    ) : VoiceCommand
}

