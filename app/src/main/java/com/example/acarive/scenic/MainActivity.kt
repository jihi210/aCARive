package com.acarive.scenic

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: DriveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "추천 테스트 실행 중 (Logcat 확인)" })
        vm.startDrive()
    }
}