package com.example.acarive.video

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

// 추억 영상을 카카오톡·인스타그램 같은 다른 앱으로 보낸다.
// 받을 앱이 없으면 false (차량 기기에는 공유 앱이 없을 수 있다)
fun shareVideo(context: Context, file: File): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (send.resolveActivity(context.packageManager) == null) return false
    return try {
        context.startActivity(Intent.createChooser(send, "드라이브 영상 공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
