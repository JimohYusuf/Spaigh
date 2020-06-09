package com.example.spaigh

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.spaigh.network.DbConstants
import kotlinx.android.synthetic.main.activity_main.*

var url = ""
class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    //initiates service on click of a button
    fun startService(v: View?) {
        DbConstants.SERVER_URL = server_address.text.toString()
        println(DbConstants.SERVER_URL)
        val serviceIntent = Intent(this, SpaighService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    //stops service on click of a button
    fun stopService(v: View?) {
        val serviceIntent = Intent(this, SpaighService::class.java)
        stopService(serviceIntent)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}