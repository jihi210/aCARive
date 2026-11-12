package com.example.acarive.video

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// 기기 다운로드 폴더 안에 이 이름으로 모은다
private const val DOWNLOAD_FOLDER = "aCARive"

// 추억 영상을 기기 다운로드 폴더(Download/aCARive)에 복사한다. 실패하면 false.
// Android 10 미만은 저장소 권한이 따로 필요해서 지원하지 않는다
suspend fun saveVideoToDownloads(context: Context, file: File, name: String): Boolean = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, name)
        put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_FOLDER")
        // 다 쓰기 전에는 다른 앱에 안 보이게
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
    try {
        resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            ?: error("저장 위치를 열 수 없음")
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        true
    } catch (e: Exception) {
        // 만들다 만 항목은 지운다
        resolver.delete(uri, null, null)
        if (e is kotlinx.coroutines.CancellationException) throw e
        false
    }
}
