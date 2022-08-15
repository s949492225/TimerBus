package com.syiyi.timebus.core

import java.util.*

/**
 * 以执行时间排序的优先队列
 */
class TimerTaskQueue {
    private val queue = LinkedList<TimerTask>()


    /**
     * 向队列添加任务
     * 任务按照执行时间排序,时间越小就先执行,则放到队列的前面
     */
    fun add(task: TimerTask) {
        synchronized(this) {
            //空则添加
            val headTask = queue.peek()
            if (headTask == null) {
                queue.addFirst(task)
                return
            }
            //否则按执行时间排序
            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                val currentTask = iterator.next()
                val currentIndex = queue.indexOf(currentTask)
                //当前的任务先执行
                if (currentTask.runTime > task.runTime) {
                    queue.add(currentIndex, task)
                    break
                }
                //当前的任务后执行
                if (currentIndex >= queue.size - 1) {
                    //最后一个了,则直接添加
                    queue.addLast(task)
                    break
                }
                //和下一个比
                val nextTask = queue[currentIndex + 1]
                if (nextTask.runTime <= task.runTime) {
                    //当前的比下一个后执行,则找下一个
                    continue
                }
                //当前的比下一个先执行,放到下一个前面
                queue.add(currentIndex, task)
                break
            }

        }
    }

    fun remove(taskKey: Long): TimerTask? {
        synchronized(this) {
            queue.iterator().apply {
                while (this.hasNext()) {
                    val task = this.next()
                    if (task.taskKey == taskKey) {
                        this.remove()
                        return task
                    }
                }
                return null
            }
        }
    }

    /**
     * 执行批处理指定数量的任务,对任务的读取和操作隔离,以防死锁
     *
     * count 指定的数量,实际拿出来的可能不满足
     *
     * 主要分为三种情况:
     * onEmpty 队列为空-需要park
     * onHeaderPeekCheck onHeaderPeekDeny,第一个header任务执行时间未到-需要park指定时间
     * onPollFilter,onPoll,第一个header执行时间已到,找到小于等于count的那些执行时间已到的任务
     */
    fun executeBatch(
        count: Int,
        onEmpty: () -> Unit,
        onHeaderPeekCheck: (TimerTask) -> Boolean,
        onHeaderPeekDeny: (TimerTask) -> Unit,
        onPollFilter: (TimerTask) -> Boolean,
        onPoll: (TimerTask) -> Unit
    ) {
        var status = 0
        val batchList = mutableListOf<TimerTask>()
        var headTask: TimerTask?
        //操作数据要加锁,park时不能持有锁
        synchronized(this) {
            headTask = queue.peek()
            if (headTask == null) {
                status = -1//无任务
            }
            if (status != -1) {
                val executeOk = onHeaderPeekCheck(headTask!!)
                status = if (executeOk) {
                    val maxCount = count.coerceAtMost(queue.size)
                    var currReadCount = 0
                    while (currReadCount < maxCount) {
                        val task = queue.peek()
                        val pollIf = onPollFilter(task!!)
                        if (pollIf) {
                            queue.poll()?.let { batchList.add(it) }
                        }
                        currReadCount++
                    }
                    1//有任务需要执行
                } else {
                    -2//所有任务执行时间未到
                }
            }
        }
        when (status) {
            -1 -> {
                onEmpty()
            }
            -2 -> {
                onHeaderPeekDeny(headTask!!)
            }
            1 -> {
                batchList.forEach { onPoll(it) }
            }
        }
    }

    fun clear() {
        synchronized(this) {
            return queue.clear()
        }
    }
}