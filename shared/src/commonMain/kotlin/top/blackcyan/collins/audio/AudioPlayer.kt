package top.blackcyan.collins.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * Streams one Collins mp3 pronunciation at a time, entirely in-app: tapping a
 * pronunciation button never leaves for the browser. [activeUrl] is the url
 * currently playing (or loading); the UI uses it to draw the play/stop state.
 */
interface AudioPlayer {
    val activeUrl: StateFlow<String?>

    /** Plays [url], replacing whatever is currently playing; toggles off when [url] is already active. */
    fun playOrToggle(url: String)

    /** Stops playback and resets [activeUrl] to null. */
    fun stop()
}

expect fun createAudioPlayer(): AudioPlayer
