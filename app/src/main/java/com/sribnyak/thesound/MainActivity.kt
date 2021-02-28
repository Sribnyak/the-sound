package com.sribnyak.thesound

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.*

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

            val bufferSizeInBytes = max(
                AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat),
                sampleRateInHz / DEFAULT_UPDATE_RATE * Short.SIZE_BYTES)
            val bufferSizeInFrames = bufferSizeInBytes / Short.SIZE_BYTES

            val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)
            audioRecord.startRecording()
            val audioData = ShortArray(bufferSizeInFrames)

            val ftRe = DoubleArray(bufferSizeInFrames)
            val ftIm = DoubleArray(bufferSizeInFrames)
            val ftResult = Array(bufferSizeInFrames / 2) { Pair(0, 0.0) }
            var volume: Int
            var textToShow: CharSequence
            while (true) {
                audioRecord.read(audioData, 0, bufferSizeInFrames)

                for (k in ftRe.indices) {
                    ftRe[k] = 0.0
                    ftIm[k] = 0.0
                    for (n in audioData.indices) {
                        ftRe[k] += audioData[n] * cos(2 * PI * k * n / bufferSizeInFrames)
                        ftIm[k] += audioData[n] * sin(2 * PI * k * n / bufferSizeInFrames)
                    }
                    ftRe[k] /= bufferSizeInFrames.toDouble()
                    ftIm[k] /= bufferSizeInFrames.toDouble()
                }
                for (k in ftResult.indices) {
                    ftResult[k] = Pair(
                        (1.0 * k * sampleRateInHz / bufferSizeInFrames).roundToInt(),
                        sqrt(ftRe[k] * ftRe[k] + ftIm[k] * ftIm[k])
                    )
                }
                ftResult.sortByDescending { it.second }

                volume = ((audioData.maxOrNull() ?: 0).toDouble() / Short.MAX_VALUE * 100).roundToInt()
                textToShow = "Permission granted!\n\nVolume: ${volume}%\n\nPeaks:"
                for (i in 0..4) {
                    volume = (ftResult[i].second / Short.MAX_VALUE * 1000).roundToInt()
                    if (volume < 1)
                        break
                    textToShow += "\n${ftResult[i].first} ($volume)"
                }
                textView.post { textView.text = textToShow }
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
        private const val DEFAULT_UPDATE_RATE = 30 // better 10
    }
}
