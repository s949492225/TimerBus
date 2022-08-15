package com.syiyi.timebus.core

interface TaskAction {
    fun execute(taskName: String, taskKey: Long)
}