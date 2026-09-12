package top.blackcyan.collins.audio

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.Header
import javazoom.jl.decoder.SampleBuffer
import javazoom.jl.player.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import kotlin.concurrent.thread

/**
 * Desktop playback.
 *
 * On Linux the JVM's JavaSound talks straight to ALSA and picks the first
 * device that offers a playback line — which is often NOT the PipeWire/Pulse
 * default sink. (A USB speaker held exclusively by PipeWire doesn't even
 * expose a line to JavaSound, so JLayer silently plays onto the onboard
 * jack instead.) To follow the system default output we decode the MP3 to
 * raw PCM with JLayer's low-level API and stream it into `paplay`
 * (PipeWire ships a Pulse-compatible paplay). Elsewhere, and as a fallback
 * when paplay is missing or fails, JLayer's JavaSound player is used.
 */
private class JvmAudioPlayer : AudioPlayer {
    private val _activeUrl = MutableStateFlow<String?>(null)
    override val activeUrl = _activeUrl.asStateFlow()

    /** Everything stop() needs to interrupt whichever backend is playing. */
    private class Playback(val url: String) {
        @Volatile var process: Process? = null
        @Volatile var player: Player? = null
        @Volatile var input: InputStream? = null
        @Volatile var cancelled = false

        fun stop() {
            cancelled = true
            // Destroying paplay closes its stdin and breaks the decoder loop;
            // closing the JLayer player unblocks its decoding thread.
            process?.destroy()
            runCatching { player?.close() }
            runCatching { input?.close() }
        }
    }

    // All access is inside @Synchronized methods or synchronized(this) blocks.
    private var current: Playback? = null

    @Synchronized
    override fun playOrToggle(url: String) {
        if (_activeUrl.value == url) {
            stop()
            return
        }
        stop()

        val playback = Playback(url)
        current = playback
        _activeUrl.value = url

        thread(name = "collins-audio", isDaemon = true) {
            if (isLinux) playViaPulse(playback) || (!playback.cancelled && playViaJavaSound(playback))
            else playViaJavaSound(playback)
            synchronized(this) {
                if (current === playback) {
                    current = null
                    _activeUrl.value = null
                }
            }
        }
    }

    @Synchronized
    override fun stop() {
        current?.stop()
        current = null
        _activeUrl.value = null
    }

    // ── PulseAudio / PipeWire path ─────────────────────────────────────────

    /** Returns true when paplay drained the whole clip and exited cleanly. */
    private fun playViaPulse(playback: Playback): Boolean {
        var process: Process? = null
        var stdin: OutputStream? = null
        try {
            val connection = URL(playback.url).openStream()
            playback.input = connection
            val bitstream = Bitstream(connection.buffered())
            val decoder = Decoder()
            val firstHeader: Header = bitstream.readFrame() ?: return false
            val firstSamples = decoder.decodeFrame(firstHeader, bitstream) as SampleBuffer

            val proc = ProcessBuilder(
                "paplay",
                "--raw",
                "--rate", decoder.outputFrequency.toString(),
                "--format=s16le",
                "--channels", firstSamples.channelCount.toString(),
                "--client-name=CollinsDictionary",
                "--device=default",
            ).redirectErrorStream(false).start()
            process = proc
            playback.process = proc
            stdin = proc.outputStream
            // Drain stderr so a talkative server can't block the process.
            thread(name = "collins-audio-paplay-err", isDaemon = true) {
                runCatching { proc.errorStream.copyTo(NullOutputStream) }
            }

            val bytes = ByteArray(8192)
            var header: Header? = firstHeader
            var samples: SampleBuffer = firstSamples
            while (header != null) {
                writeLeShorts(stdin, bytes, samples.buffer, samples.bufferLength)
                bitstream.closeFrame()
                header = bitstream.readFrame()
                if (header != null) {
                    samples = decoder.decodeFrame(header, bitstream) as SampleBuffer
                }
            }
            stdin.flush()
            stdin.close()
            return proc.waitFor() == 0
        } catch (_: Exception) {
            return false
        } finally {
            runCatching { stdin?.close() }
            runCatching { playback.input?.close() }
            process?.let { if (it.isAlive) it.destroy() }
        }
    }

    private fun writeLeShorts(out: OutputStream, buf: ByteArray, samples: ShortArray, length: Int) {
        var bi = 0
        for (i in 0 until length) {
            if (bi + 2 > buf.size) {
                out.write(buf, 0, bi)
                bi = 0
            }
            val s = samples[i].toInt()
            buf[bi++] = (s and 0xff).toByte()
            buf[bi++] = ((s shr 8) and 0xff).toByte()
        }
        if (bi > 0) out.write(buf, 0, bi)
    }

    // ── JavaSound fallback (JLayer's own player) ───────────────────────────

    /** Returns true when the clip played to the end without an exception. */
    private fun playViaJavaSound(playback: Playback): Boolean {
        val player = try {
            val connection = URL(playback.url).openStream()
            playback.input = connection
            Player(connection).also { playback.player = it }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return try {
            player.play()
            true
        } catch (e: Exception) {
            // Stopping via Player.close() unblocks play() with an exception.
            if (!playback.cancelled) e.printStackTrace()
            false
        } finally {
            runCatching { player.close() }
            runCatching { playback.input?.close() }
        }
    }

    private val isLinux: Boolean
        get() = System.getProperty("os.name")?.lowercase()?.contains("linux") == true
}

private object NullOutputStream : OutputStream() {
    override fun write(b: Int) {}
    override fun write(b: ByteArray, off: Int, len: Int) {}
}

actual fun createAudioPlayer(): AudioPlayer = JvmAudioPlayer()
