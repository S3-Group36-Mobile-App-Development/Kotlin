package com.zenmind.kotlin.features.authentication.data.repositories

enum class AuthFailure { INVALID_DATA, EMAIL_REGISTERED, WRONG_CREDENTIALS, SERVER }

class AuthException(val reason: AuthFailure) : Exception()
