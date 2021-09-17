package com.wintech.diydr

import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest

class SecondActivity : AppCompatActivity() {
    var list: ListView? = null
    var maintitle = arrayOf(
            "Dr witness", "Dr future geekstar",
            "Dr geekwitness", "Dr nkge",
            "Dr Itumeleng")
    var subtitle = arrayOf(
            "book appointment", "book appointment",
            "book appointment", "book appointment",
            "book appointment"
    )
    var imgid = arrayOf(
            R.drawable.icon, R.drawable.icon,
            R.drawable.icon, R.drawable.icon,
            R.drawable.icon
    )
    var msg = "I would like to book an appointment at 12:00 pm"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        val adapter = ListAdapter(this, maintitle, subtitle, imgid)
        list = findViewById<View>(R.id.listview) as ListView
        list!!.adapter = adapter
        list!!.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                if (position == 0) {
                    val no = "20140"
                    sendSMS(msg, no)
                } else if (position == 1) {
                    val no = "30140"
                    sendSMS(msg, no)
                } else if (position == 2) {
                    val no = "40140"
                    sendSMS(msg, no)
                } else if (position == 3) {
                    val no = "50140"
                    sendSMS(msg, no)
                } else if (position == 4) {
                    val no = "60140"
                    sendSMS(msg, no)
                }
            }
        })

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this, object : OnInitializationCompleteListener() {
            fun onInitializationComplete(initializationStatus: InitializationStatus?) {
                Toast.makeText(applicationContext, " succesfull ", Toast.LENGTH_SHORT).show()
            }
        })
        val mAdView: AdView
        mAdView = findViewById(R.id.adView)
        val adRequest: AdRequest = Builder().build()
        mAdView.loadAd(adRequest)
        findViewById<View>(R.id.button).setOnClickListener { v: View? -> startActivity(Intent(this@SecondActivity, ThirdActivity::class.java)) }
    }

    protected fun sendSMS(ms: String?, no: String?) {
        Log.i("Send SMS", "")
        val smsIntent = Intent(Intent.ACTION_VIEW)
        smsIntent.setData(Uri.parse("smsto:"))
        smsIntent.setType("vnd.android-dir/mms-sms")
        smsIntent.putExtra("address", String(no))
        smsIntent.putExtra("sms_body", ms)
        try {
            startActivity(smsIntent)
            finish()
            Log.i("Finished sending SMS...", "")
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this@SecondActivity,
                    "SMS faild, please try again later.", Toast.LENGTH_SHORT).show()
        }
    }
}