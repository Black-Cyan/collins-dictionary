package top.blackcyan.collins.audio

import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private class AndroidAudioPlayer : AudioPlayer {
    private val _activeUrl = MutableStateFlow<String?>(null)
    override val activeUrl = _activeUrl.asStateFlow()

    private var player: MediaPlayer? = null

    override fun playOrToggle(url: String) {
        if (_activeUrl.value == url) {
            stop()
            return
        }
        stop()

        val mp = MediaPlayer()
        try {
            mp.setDataSource(url)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { releasePlayer(it) }
            mp.setOnErrorListener { failed, _, _ ->
                releasePlayer(failed)
                true
            }
            // Remote mp3 source; prepares off the main thread.
            mp.prepareAsync()
            player = mp
            _activeUrl.value = url
        } catch (e: Exception) {
            mp.release()
            _activeUrl.value = null
        }
    }

    override fun stop() {
        player?.let { releasePlayer(it) }
    }

    private fun releasePlayer(mp: MediaPlayer) {
        runCatching { mp.stop() }
        mp.release()
        if (player === mp) {
            player = null
            _activeUrl.value = null
        }
    }
}

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
