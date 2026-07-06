package com.dphascow.app.utils

class PlatformLogger(private val tag: String) {
    fun log(message: String) {
        println("[$tag] $message")
    }
}
