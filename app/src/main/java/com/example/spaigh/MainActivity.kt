package com.example.spaigh

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amitshekhar.sqlite.DBFactory
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.example.spaigh.network.DbConstants
import com.example.spaigh.network.VolleySingleton
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

var serverConnection = "server address"

class MainActivity : AppCompatActivity() {
    private val handler = Handler()
    private val delay = 100L

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref: SharedPreferences = applicationContext.getSharedPreferences(getString(R.string.shared_pref), Context.MODE_PRIVATE)

        server_address.hint = DbConstants.SERVER_URL

        if (serviceRunning){
            service_state.setText(" SERVICE RUNNING")
        }

        //set server address to stored address on startup
        if(!serviceRunning){
            server_address.setText(sharedPref.getString(getString(R.string.ip_address),"") )
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

                if(connection.text == "INTERNET: NOT CONNECTED"){
                    connection.setTextColor(Color.RED)
                }
                else{connection.setTextColor(Color.BLUE)}

                if(sync_state.text == "SYNCING DATA"){
                    sync_state.setTextColor(Color.RED)
                }
                else{sync_state.setTextColor(Color.BLUE)}

                if (isConnected != "INTERNET: CONNECTED"){
                    server_address.hint = serverConnection
                }

                if(isConnected == "INTERNET: CONNECTED" && serviceRunning){
                    server_address.hint = "Connected to " + DbConstants.SERVER_URL
                }

                handler.postDelayed(this, delay)
            }
        }, delay)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /*Initiates service on click of a button*/
    @RequiresApi(Build.VERSION_CODES.O)
    fun startService(v: View?) {
        if (!serviceRunning){
            getPermission(android.Manifest.permission.READ_PHONE_STATE, 1)
            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/y"))
            val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            //Get server URL from user
            DbConstants.SERVER_ADDR = server_address.text.toString()

            val sharedPref: SharedPreferences = applicationContext.getSharedPreferences(getString(R.string.shared_pref), Context.MODE_PRIVATE)
            with(sharedPref.edit()){
                putString(getString(R.string.ip_address), DbConstants.SERVER_ADDR)
                commit()
            }

            DbConstants.SERVER_URL = "http://" + server_address.text.toString()
            checkWithServer("$date,$time", "UNDEFINED", "UNDEFINED")
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /*Stops service on click of a button*/
    fun stopService(v: View?) {
        val serviceIntent = Intent(this, SpaighService::class.java)
        stopService(serviceIntent)
        service_state.text = " SERVICE STOPPED"
        server_address.setText(DbConstants.SERVER_ADDR)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /*Handle permission request response*/
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
                   Toast.makeText(this, "Phone monitoring function disabled", Toast.LENGTH_LONG).show()
                }
                return
            }

            else -> {
                // Ignore all other requests.
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onResume() {
        super.onResume()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onPause() {
        super.onPause()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkAvailable(): Boolean{
        val connManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo= connManager.activeNetworkInfo
        return networkInfo?.isConnected == true
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    @RequiresApi(Build.VERSION_CODES.M)
    fun checkWithServer(localTime: String, devState: String, callStateL: String){
        if (isNetworkAvailable()){

            // Formulate POST request and handle response.
            var stringRequest = object: StringRequest(
                Request.Method.POST, DbConstants.SERVER_URL,
                Response.Listener { response ->
                    try {
                        if (response == "POST SUCCESS"){
                            //Send an intent to start service
                            val serviceIntent = Intent(this, SpaighService::class.java)
                            ContextCompat.startForegroundService(this, serviceIntent)
                            service_state.text = " SERVICE IS RUNNING"
                            server_address.setText("")
                            server_address.hint = "Connected to " + DbConstants.SERVER_URL
                        }
                        else{
                            Toast.makeText(this, "Could not Connect. Check entered server address and Check if server is running.", Toast.LENGTH_LONG).show()
                        }

                    } catch (e: JSONException){
                        e.printStackTrace()
                        Toast.makeText(this, "Could not Connect. Check entered server address and Check if server is running.", Toast.LENGTH_LONG).show()
                    }
                },
                Response.ErrorListener {
                    Toast.makeText(this, "Could not Connect. Check entered server address and Check if server is running.", Toast.LENGTH_LONG).show()
                }){

                @Throws(AuthFailureError::class)
                override fun getParams(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    //Change with your post params
                    params["time_stamp"] = localTime
                    params["device_state"] = devState
                    params["call_state"] = callStateL
                    return params
                }
            }

            stringRequest.retryPolicy = DefaultRetryPolicy(
                1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )

            // Add the request to the RequestQueue.
            VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)

        } else{
            Toast.makeText(this, "No Active Internet Connection", Toast.LENGTH_LONG).show()
        }

    }
}