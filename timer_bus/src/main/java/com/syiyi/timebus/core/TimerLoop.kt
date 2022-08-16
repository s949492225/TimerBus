package com.syiyi.timebus.core

import com.syiyi.timebus.core.log.Logger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.thread

interface TimerLoop : Runnable {
    var logger: Logger
    val timerTaskCache: TimerTaskCache
    fun add(task: TimerTask)
    fun cancel(task: TimerTask)
    fun start()
    fun exit()

    companion object Factory {

        operator fun invoke(timerTaskCache: TimerTaskCache, logger: Logger): TimerLoop {
            return SingleExecutorTimerLoop(timerTaskCache, logger)
        }

        class SingleExecutorTimerLoop(
            override val timerTaskCache: TimerTaskCache,
            override var logger: Logger
        ) : TimerLoop {
            private val exit = AtomicBoolean(false)
            private val queue = TimerTaskQueue()

            @Volatile
            private var currentThread: Thread? = null

            override fun start() {
                thread {
                    currentThread = Thread.currentThread()
                    logger.info("TimerLoop init success")
                    this.run()
                }
            }

            override fun exit() {
                exit.set(true)
            }

            override fun add(task: TimerTask) {
                while (null == this.currentThread) {
                    //wait init
                }
                if (exit.get()) {
                    return
                }
                queue.add(task)
                LockSupport.unpark(this.currentThread)
            }

            override fun cancel(task: TimerTask) {
                queue.remove(task.taskKey)?.apply {
                    timerTaskCache.add(this)
                }
                LockSupport.unpark(this.currentThread)
            }

            override fun run() {
                try {
                    while (true) {
                        val exit = exit.get()
                        if (exit) {
                            queue.clear()
                            break
                        }
                        loop()
                    }
                } catch (e: Exception) {
                    logger.error(e.message ?: "unknown exception")
                }
            }

            private fun loop() {
                //判断是否到期
                val now = System.nanoTime()
                queue.executeBatch(10,//一次拿十条,为了尽快将已经到期的任务尽快执行
                    onEmpty = {
                        //此时不持有锁,如果此时添加任务unpack,这里的park则会直接跳过,故而不能用await
                        //没有则休眠
                        Thread.yield()
                        logger.info("park long")
                        LockSupport.park()
                    },
                    onHeaderPeekCheck = { task ->
                        //持有锁
                        return@executeBatch task.runTime <= now
                    },
                    onHeaderPeekDeny = { task ->
                        //此时不持有锁,如果此时添加任务unpack,这里的park则会直接跳过,故而不能用await
                        // 未到期,则休眠
                        Thread.yield()
                        logger.info("key:${task.taskKey} ,休眠时间:${(task.runTime - now) / 1000000}")
                        LockSupport.parkNanos(task.runTime - now)
                        logger.info("key:${task.taskKey} ,实际休眠时间:${(System.nanoTime() - now) / 1000000}")
                    },
                    onPollFilter = { task ->
                        return@executeBatch task.runTime <= now
                    },
                    onPoll = { task ->
                        //此时不持有锁
                        //此时批处理会循环执行,为了尽快将已经到期的任务尽快执行
                        task.ifNoCanceled {
                            val originAction = task.action!!
                            //加入缓存
                            timerTaskCache.add(task)
                            //判断是否是循环任务
                            val isLoop = task.period != -1L
                            if (isLoop) {
                                //重复添加任务
                                val nextTask = timerTaskCache.obtain(task, originAction)
                                nextTask.runTime = now + task.timeUnit.toNanos(task.period)
                                add(nextTask)
                            }
                            //已经到期则立即执行
                            task.action?.execute(task.taskName, task.taskKey)
                        }.ifCanceled {
                            timerTaskCache.add(task)
                        }
                    })

            }
        }
    }
}