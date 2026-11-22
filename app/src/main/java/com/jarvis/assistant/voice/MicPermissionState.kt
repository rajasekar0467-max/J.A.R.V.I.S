package com.jarvis.assistant.voice

enum class MicPermissionState {
    GRANTED,
    DENIED_RETRYABLE,
    DENIED_PERMANENT
}

object MicPermissionResolver {
    fun resolve(
        isGranted: Boolean,
        hasRequestedBefore: Boolean,
        shouldShowRationale: Boolean
    ): MicPermissionState = when {
        isGranted -> MicPermissionState.GRANTED
        !hasRequestedBefore || shouldShowRationale -> MicPermissionState.DENIED_RETRYABLE
        else -> MicPermissionState.DENIED_PERMANENT
    }
}
