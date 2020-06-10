package com.example.spaigh

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.spaigh.network.DbConstants
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    /*Initiates service on click of a button*/
    fun startService(v: View?) {
        getPermission(android.Manifest.permission.READ_PHONE_STATE, 1)
        //Get server URL from user
        DbConstants.SERVER_URL = server_address.text.toString()

        //Send an intent to start service
        val serviceIntent = Intent(this, SpaighService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }


    /*Stops service on click of a button*/
    fun stopService(v: View?) {
        val serviceIntent = Intent(this, SpaighService::class.java)
        stopService(serviceIntent)
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
        super.onDestroy()
    }
}