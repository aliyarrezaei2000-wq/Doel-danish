package com.example.util

private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

fun String.toPersianDigits(): String {
    val sb = StringBuilder(length)
    for (ch in this) {
        if (ch in '0'..'9') {
            sb.append(PERSIAN_DIGITS[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

fun Int.toPersianDigits(): String = toString().toPersianDigits()

fun Long.toPersianDigits(): String = toString().toPersianDigits()
