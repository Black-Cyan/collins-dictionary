package top.blackcyan.collins

import android.content.Intent
import android.net.Uri
import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
actual fun getCollinsAccessKey(): String =
    System.getenv("COLLINS_ACCESS_KEY") ?: ""

actual fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    AppContextHolder.applicationContext.startActivity(intent)
}
