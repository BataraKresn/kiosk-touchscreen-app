package com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
sealed interface Message {
    data class Text(val content: String, val sender: String) : Message
    data class Binary(val data: ByteArray) : Message {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Binary

            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    data class Error(val message: String) : Message
    data class HeartBeat(val token: String, val isActive: Boolean, val message: String) : Message
}