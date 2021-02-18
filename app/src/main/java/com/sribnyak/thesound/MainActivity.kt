package com.sribnyak.thesound

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random

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
            var f1: Int
            var f2: Int
            while (true) {
                f1 = Random.nextInt(210, 450)
                f2 = Random.nextInt(350, 590)
                textView.post { textView.text = "Permission granted!\nPeaks:\n$f1\n$f2" }
                Thread.sleep(16 * 4)
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

    companion object { private const val MIC_PERMISSION_CODE = 20567 }
}
