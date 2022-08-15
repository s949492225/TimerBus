package com.syiyi.timebus.core.log

import android.util.Log

interface Logger {
    fun info(msg: String)
    fun error(msg: String)

    companion object Factory {
        class DefaultLogger : Logger {
            override fun info(msg: String) {
                Log.i(TAG, msg)
            }

            override fun error(msg: String) {
                Log.e(TAG, msg)
            }

            companion object {
                const val TAG = "TimerTaskLogger"
            }

        }

        operator fun invoke(): Logger = DefaultLogger()
    }

}