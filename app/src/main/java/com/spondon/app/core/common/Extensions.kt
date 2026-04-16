package com.spondon.app.core.common

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun Date.daysSince(): Long {
    val diff = System.currentTimeMillis() - this.time
    return TimeUnit.MILLISECONDS.toDays(diff)
}

fun Date.formatDisplay(): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(this)
}

fun Long.toDate(): Date = Date(this)