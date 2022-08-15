package com.syiyi.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.syiyi.timebus.TimerBus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.begin)
        val content: TextView = findViewById(R.id.content)
        button.setOnClickListener {
            originUse(content)
//            flowUse(content)
        }
    }

    private fun originUse(content: TextView) {
        // 普通使用
        TimerBus.schedule(
            "origin timer",
            period = 3,
            delay = 1,
            timeUnit = TimeUnit.SECONDS
        ) { _, taskKey ->
            content.post {
                content.text = "task_key${taskKey}\n${content.text}"
            }
        }
    }

    private fun flowUse(content: TextView) {
        //flow使用
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                TimerBus.scheduleFlow(
                    "flow timer",
                    period = 3,
                    delay = 1,
                    timeUnit = TimeUnit.SECONDS
                ).shareIn(
                    lifecycleScope,
                    replay = 1,
                    started = SharingStarted.WhileSubscribed()
                ).onEach {
                    content.text = "task_key${it.taskKey}\n${content.text}"
                }.collect()
            }
        }
    }
}