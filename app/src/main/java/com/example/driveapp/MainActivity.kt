package com.example.driveapp

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.driveapp.ui.theme.DriveAppTheme
import com.example.driveapp.location.PleosLocation
import com.example.driveapp.ui.RecordScreen
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var pleosLocation: PleosLocation
    private val fusedPermission = "pleos.car.permission.FUSED_LOCATION"

    // 권한 팝업 결과를 받는 곳
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pleosLocation.start()
            } else {
                Log.d("FLS", "위치 권한 거부됨")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pleosLocation = PleosLocation(this)

        // 권한이 있으면 바로 시작
        if (ContextCompat.checkSelfPermission(this, fusedPermission) == PackageManager.PERMISSION_GRANTED) {
            pleosLocation.start()
        } else {
            permissionLauncher.launch(fusedPermission)
        }

        setContent {
            DriveAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RecordScreen(
                        modifier = Modifier.padding(innerPadding),
                        liveLocation = pleosLocation.latest,
                        useMock = false
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        pleosLocation.stop()
        super.onDestroy()
    }
}
