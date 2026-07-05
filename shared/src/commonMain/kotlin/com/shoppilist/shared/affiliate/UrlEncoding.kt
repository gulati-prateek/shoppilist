package com.shoppilist.shared.affiliate

private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.*"

/**
 * `java.net.URLEncoder` doesn't exist on Kotlin/Native — this is a portable equivalent
 * (application/x-www-form-urlencoded: space -> '+', UTF-8 percent-encoding otherwise).
 */
internal fun encodeUrlComponent(value: String): String {
    val builder = StringBuilder()
    for (byte in value.encodeToByteArray()) {
        val char = byte.toInt().toChar()
        when {
            char == ' ' -> builder.append('+')
            UNRESERVED.contains(char) -> builder.append(char)
            else -> {
                val unsignedByte = byte.toInt() and 0xFF
                builder.append('%')
                builder.append(unsignedByte.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
    return builder.toString()
}
