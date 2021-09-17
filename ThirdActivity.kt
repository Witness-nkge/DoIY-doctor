package com.wintech.diydr

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ThirdActivity : AppCompatActivity() {
    private var main: View? = null
    private var imageView: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)
        val ring: MediaPlayer = MediaPlayer.create(this@ThirdActivity, R.raw.ring)
        ring.start()
        main = findViewById(R.id.main)
        imageView = findViewById<View>(R.id.imageView) as ImageView
        val btn = findViewById<View>(R.id.take) as Button
        btn.setOnClickListener {
            val b: Bitmap = Screenshot.takescreenshotOfRootView(imageView)
            imageView!!.setImageBitmap(b)
            main.setBackgroundColor(Color.parseColor("#999999"))
        }
    }
}