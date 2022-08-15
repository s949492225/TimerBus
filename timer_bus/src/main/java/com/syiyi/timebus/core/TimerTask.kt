package com.syiyi.timebus.core

import java.util.concurrent.TimeUnit

data class TimerTask(
    val taskName: String,
    val taskKey: Long,
    val period: Long,
    val delay: Long,
    val timeUnit: TimeUnit,
    var action: TaskAction?,
) {
    private var addTime: Long = 0
    var runTime: Long = 0

    @Volatile
    private var canceled: Boolean = false

    @Synchronized
    fun ifNoCanceled(action: () -> Unit): TimerTask {
        if (!canceled) {
            action()
        }
        return this
    }

    @Synchronized
    fun ifCanceled(action: () -> Unit): TimerTask {
        if (canceled) {
            action()
        }
        return this
    }

    @Synchronized
    fun cancel() {
        canceled = true
    }

    @Synchronized
    fun resetCancel() {
        canceled = false
    }

    fun init() {
        addTime = System.nanoTime()
        runTime = addTime + timeUnit.toNanos(delay)
    }
}