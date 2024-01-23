package com.kierman.projektnalewak.util

open class Event<out T>(private val content: T) {
    private var hasBeenHandled = false

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) { // Jeśli zdarzenie zostało już obsłużone
            null // zwróć null,
        } else { // W przeciwnym razie
            hasBeenHandled = true // oznacz zdarzenie jako obsłużone
            content // zwróć zawartość
        }
    }


    // Zwraca zawartość zdarzenia niezależnie od tego, czy zostało obsłużone.

    fun peekContent(): T = content
}