package com.sribnyak.thesound

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.SystemClock.uptimeMillis
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        textView.text = "Permission denied :("

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MIC_PERMISSION_CODE
            )
        } else onMicPermissionGranted()
    }

    private fun onMicPermissionGranted() {
        textView.text = "Permission granted!"

        Thread {
            val sampleRateInHz = DEFAULT_SAMPLE_RATE
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            val bufferSizeInBytes = AudioRecord.getMinBufferSize(
                sampleRateInHz, channelConfig, audioFormat) * 2
            val bufferSizeInFrames = bufferSizeInBytes / 2

            val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)
            audioRecord.startRecording()
            val audioData = ShortArray(bufferSizeInFrames)

            val sleepTime = (16 * 4).toLong()
            var time = uptimeMillis()
            var deltaTime: Long

            while (true) {
                audioRecord.read(audioData, 0, audioData.size)
                textView.post { textView.text = "Permission granted!\n\nVolume: ${Engine.getVolume(audioData)}%" }

                deltaTime = uptimeMillis() - time
                Thread.sleep(max(3, sleepTime - deltaTime))
                time = uptimeMillis()
            }
        }.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MIC_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED)
            onMicPermissionGranted()
    }

    companion object {
        private const val MIC_PERMISSION_CODE = 20567
        private const val DEFAULT_SAMPLE_RATE = 44100
    }
}

object Engine {
    fun getVolume(audioData: ShortArray): Int {
        return ((audioData.maxOrNull() ?: 0).toDouble() / Short.MAX_VALUE * 100).roundToInt()
    }
}
