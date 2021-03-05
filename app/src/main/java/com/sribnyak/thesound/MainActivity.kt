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

const val MIC_PERMISSION_CODE = 20567
const val DEFAULT_SAMPLE_RATE = 44100
const val DEFAULT_UPDATE_RATE = 20 // better 10

class MainActivity : AppCompatActivity() {

    lateinit var textView: TextView // required to update the textView outside onCreate method

    // Entry point
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
            ) // the result of the request is checked in onRequestPermissionsResult method
        } else onMicPermissionGranted()
    }

    // if the mic permission is granted, this method will be called necessarily
    private fun onMicPermissionGranted() {
        textView.text = "Permission granted!"

        Thread { // create worker thread
            val sampleRateInHz = DEFAULT_SAMPLE_RATE
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            val bufferSizeInBytes = max( // min size or the size to reach the default update rate
                AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat),
                sampleRateInHz / DEFAULT_UPDATE_RATE * Short.SIZE_BYTES)
            val numberOfSamples = bufferSizeInBytes / Short.SIZE_BYTES

            val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)
            audioRecord.startRecording()
            val audioData = ShortArray(numberOfSamples) // array to store data from the mic

            val ftRe = DoubleArray(numberOfSamples / 2) // the real part and
            val ftIm = DoubleArray(numberOfSamples / 2) // the imaginary part of the DFT
            val ftResult = Array(numberOfSamples / 2) { Pair(0, 0.0) } // required later
            var volume: Int                                            // to show the results
            var textToShow: CharSequence                               // of the DFT

            // Discrete Fourier transform
            fun calculateDFT() {
                for (k in ftRe.indices) {
                    ftRe[k] = 0.0
                    ftIm[k] = 0.0
                    for (n in 0 until numberOfSamples) {
                        ftRe[k] += audioData[n] * cos(2 * PI * k * n / numberOfSamples)
                        ftIm[k] += audioData[n] * sin(2 * PI * k * n / numberOfSamples)
                    }
                }
            }

            while (true) {
                audioRecord.read(audioData, 0, numberOfSamples) // waits until all is read
                calculateDFT()

                for (k in ftResult.indices) // fill the ftResult array
                    ftResult[k] = Pair(
                        (1.0 * k * sampleRateInHz / numberOfSamples).roundToInt(),    // frequency
                        sqrt(ftRe[k] * ftRe[k] + ftIm[k] * ftIm[k]) / numberOfSamples // amplitude
                    )
                ftResult.sortByDescending { it.second }

                volume = ((audioData.maxOrNull() ?: 0).toDouble() / Short.MAX_VALUE * 100).roundToInt()
                textToShow = "Permission granted!\n\nVolume: ${volume}%\n\nPeaks:"
                for (i in 0 until 5) { // 5 most intensive frequencies
                    volume = (ftResult[i].second / Short.MAX_VALUE * 1000).roundToInt()
                    if (volume < 1)
                        break
                    textToShow += "\n${ftResult[i].first} ($volume)"
                }
                textView.post { textView.text = textToShow } // run on the UI thread
            }
        }.start() // no need to save a reference to the worker thread, so just start it here
    }

    // used for checking the result of the mic permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MIC_PERMISSION_CODE && grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            onMicPermissionGranted()
    }
}
