package com.syiyi.timebus.core

import java.util.concurrent.atomic.AtomicLong

class TaskKeyGenerator {
    private var index = AtomicLong(100)

    fun generate(): Long {
        return index.getAndIncrement()
    }
}