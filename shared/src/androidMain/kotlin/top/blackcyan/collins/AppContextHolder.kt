package top.blackcyan.collins

import android.content.Context

/**
 * Holds the application Context needed for launching browser intents from
 * shared code. Initialized in MainActivity.onCreate.
 */
object AppContextHolder {
    lateinit var applicationContext: Context
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
