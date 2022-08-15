package com.syiyi.timebus.core

import java.util.*
import java.util.concurrent.TimeUnit

class TimerTaskCache(private val maxSize: Int = 10) {
    private val cache = LinkedList<TimerTask>()
    private val taskKeyGenerator: TaskKeyGenerator by lazy { TaskKeyGenerator() }

    @Synchronized
    fun add(task: TimerTask) {
        task.action = null
        task.resetCancel()
        val none = cache.none { it.taskKey == task.taskKey }
        if (none && cache.size < maxSize) {
            cache.add(task)
        }
    }

    @Synchronized
    fun obtain(
        taskName: String,
        period: Long,
        delay: Long,
        timeUnit: TimeUnit,
        action: TaskAction
    ): TimerTask {
        val task = cache.poll() ?: TimerTask(
            taskName,
            taskKeyGenerator.generate(),
            period,
            delay,
            timeUnit,
            action,
        )
        task.init()
        return task
    }

    fun obtain(task: TimerTask, originAction: TaskAction): TimerTask {
        return (cache.poll() ?: TimerTask(
            task.taskName,
            taskKeyGenerator.generate(),
            task.period,
            task.delay,
            task.timeUnit,
            originAction
        )).apply { this.action = originAction }
    }
}