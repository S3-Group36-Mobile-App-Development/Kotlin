package com.zenmind.kotlin.features.dailycheckin.model

enum class SaveResult {
    ALREADY_REGISTERED,
    SYNCED,
    PENDING,
    GUEST,
    LOCAL_ONLY
}