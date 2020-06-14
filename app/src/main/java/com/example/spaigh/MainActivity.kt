package com.example.spaigh

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.spaigh.network.DbConstants
import kotlinx.android.synthetic.main.activity_main.*

var serverConnection = "server address"

class MainActivity : AppCompatActivity() {
    private val handler = Handler()
    private val delay = 100L

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        server_address.setHint(DbConstants.SERVER_URL)

        if (serviceRunning){
            service_state.setText(" Service is Running")
        }

        call_state.setTextColor(Color.BLUE)
        device_state.setTextColor(Color.BLUE)
        connection.setTextColor(Color.RED)
        sync_state.setTextColor(Color.RED)

        handler.postDelayed(object : Runnable {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun run() {
                call_state.text = callStateToDb
                device_state.text = moveState
                connection.text = isConnected
                sync_state.text = isSynced

                if(call_state.text == "CALL-IDLE"){
                    call_state.setTextColor(Color.BLUE)
                }
                else if (call_state.text == "RINGING"){
                    call_state.setTextColor(Color.RED)
                }
                else{call_state.setTextColor(Color.GREEN)}

                if(device_state.text == "DEVICE-MOVING"){
                    device_state.setTextColor(Color.RED)
                }
                else{device_state.setTextColor(Color.BLUE)}

                if(connection.text == "Internet: Not Connected"){
                    connection.setTextColor(Color.RED)
                }
                else{connection.setTextColor(Color.BLUE)}

                if(sync_state.text == "Syncing data"){
                    sync_state.setTextColor(Color.RED)
                }
                else{sync_state.setTextColor(Color.BLUE)}

                if (isConnected != "Internet: Connected"){
                    server_address.hint = serverConnection
                }

                if(isConnected == "Internet: Connected" && serviceRunning){
                    server_address.setHint("Connected to " + DbConstants.SERVER_URL)
                }

                handler.postDelayed(this, delay)
            }
        }, delay)
    }


    /*Initiates service on click of a button*/
    fun startService(v: View?) {
        getPermission(android.Manifest.permission.READ_PHONE_STATE, 1)
        //Get server URL from user
        DbConstants.SERVER_URL = server_address.text.toString()
        //Send an intent to start service
        val serviceIntent = Intent(this, SpaighService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        service_state.setText(" Service is Running")
        server_address.setText("")
        server_address.hint = "Connected to " + DbConstants.SERVER_URL
    }


    /*Stops service on click of a button*/
    fun stopService(v: View?) {
        val serviceIntent = Intent(this, SpaighService::class.java)
        stopService(serviceIntent)
        service_state.setText(" Service Stopped")
    }


    /*Runtime permission request*/
    private fun getPermission(permission: String, requestCode: Int) {

        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission)
                                                    == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                                                arrayOf(permission),
                                                requestCode)
        }
    }

    /*Handle request response*/
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //do nothing
                } else {
                   Toast.makeText(this, "Call-state function disabled", Toast.LENGTH_LONG).show()
                }
                return
            }

            else -> {
                // Ignore all other requests.
            }
        }
    }
    override fun onResume() {
        super.onResume()
    }


    override fun onPause() {
        super.onPause()
    }


    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}