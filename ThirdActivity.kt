package com.wintech.practise

import android.app.ActivityGroup
import android.widget.TabHost
import android.os.Bundle
import com.wintech.practise.R
import android.media.MediaPlayer
import android.graphics.Bitmap
import android.widget.TabHost.TabSpec
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.wintech.practise.VideoChatActivity

class ThirdActivity : ActivityGroup() {
    private var main: View? = null
    private var imageView: ImageView? = null
    var tabHost: TabHost? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)
        val ring = MediaPlayer.create(this@ThirdActivity, R.raw.ring)
        ring.start()
        main = findViewById(R.id.main)
        imageView = findViewById<View>(R.id.imageView) as ImageView
        val btn = findViewById<View>(R.id.take) as Button
        btn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                val b: Bitmap = Screenshot.takescreenshotOfRootView(imageView)
                imageView!!.setImageBitmap(b)
                main.setBackgroundColor(Color.parseColor("#999999"))

                tabHost = findViewById(R.id.tabHost)
                tabHost.setup(getLocalActivityManager())
                var spec = tabHost.newTabSpec("VideoCall")
                spec.setIndicator("VideoChat with Dr")
                spec.setContent(Intent(this, VideoChatActivity::class.java))
                tabHost.addTab(spec)
                spec = tabHost.newTabSpec("BrainMassage")
                spec.setIndicator("BrainMassage")
                spec.setContent(R.id.tab2)
                tabHost.addTab(spec)
            }
        })
    }
}