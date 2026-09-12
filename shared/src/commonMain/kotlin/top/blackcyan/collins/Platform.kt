package top.blackcyan.collins

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun getCollinsAccessKey(): String

/** Opens [url] in the platform's default browser. */
expect fun openUrl(url: String)
