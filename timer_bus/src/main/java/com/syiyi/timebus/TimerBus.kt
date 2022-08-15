package com.syiyi.timebus

import com.syiyi.timebus.core.*
import com.syiyi.timebus.core.log.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import java.util.concurrent.TimeUnit

/**
 * 应用内全局定时器
 *
 * 支持尽可能的执行不同时间的定时器
 * 内部使用缓存,防止创建过多的任务
 * 使用单线程,尽可能减少线程资源开销,任务不执行时休眠不占用cpu
 *
 * 使用场景,例如app内有好多banner,使用此可以一个线程满足所有banner定时切换功能
 */
object TimerBus {
    private var logger = Logger()
    private val timerTaskCache: TimerTaskCache = TimerTaskCache(20)
    private val loop: TimerLoop = TimerLoop(timerTaskCache, logger)

    init {
        loop.start()
    }

    /**
     * 生成一个支持计时的flow
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @JvmStatic
    fun scheduleFlow(
        taskName: String = "anonymity",
        period: Long = -1,
        delay: Long = 0,
        timeUnit: TimeUnit = TimeUnit.SECONDS,
    ) = channelFlow {
        val taskCall = schedule(taskName, period, delay, timeUnit) { taskName, taskKey ->
            offer(TaskInfo(taskName, taskKey))
        }
        awaitClose {
            taskCall.cancel()
        }
    }

    /**
     * 调度一个计时器,主要为kotlin使用
     */
    @JvmStatic
    fun schedule(
        taskName: String = "anonymity",
        period: Long = -1,
        delay: Long = 0,
        timeUnit: TimeUnit = TimeUnit.SECONDS,
        action: (String, Long) -> Unit,
    ): TimerCall {
        return schedule(
            taskName,
            period,
            delay,
            timeUnit,
            object : TaskAction {
                override fun execute(taskName: String, taskKey: Long) {
                    action(taskName, taskKey)
                }
            }
        )
    }

    /**
     * 调度一个计时器,主要为java使用
     */
    @JvmStatic
    fun schedule(
        taskName: String,
        period: Long = -1,
        delay: Long = 0,
        timeUnit: TimeUnit = TimeUnit.SECONDS,
        action: TaskAction,
    ): TimerCall {
        return innerSchedule(
            taskName,
            period,
            delay,
            timeUnit,
            action
        )
    }

    @JvmStatic
    private fun innerSchedule(
        taskName: String,
        period: Long = -1,
        delay: Long,
        timeUnit: TimeUnit,
        action: TaskAction
    ): TimerCall {
        val task = timerTaskCache.obtain(taskName, period, delay, timeUnit, action)
        loop.add(task)
        return TimerCall(task)
    }

    /**
     * 设置自定义logger
     */
    @JvmStatic
    fun setLogger(logger: Logger) {
        this.logger = logger
    }

    /**
     * 退出计时器,此时计时器不再可用,注意此方法应在进程需要结束时调用
     */
    @JvmStatic
    fun exit() {
        loop.exit()
    }

    internal fun cancel(task: TimerTask) {
        task.cancel()
        loop.cancel(task)
    }

}