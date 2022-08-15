package com.syiyi.timebus.core

import com.syiyi.timebus.TimerBus

interface TimerCall {
    val task: TimerTask
    fun cancel()

    companion object Factory {
        class InnerTimerCall(
            override val task: TimerTask
        ) :
            TimerCall {
            override fun cancel() {
                TimerBus.cancel(task)
            }
        }

        operator fun invoke(task: TimerTask): InnerTimerCall {
            return InnerTimerCall(task)
        }
    }
}