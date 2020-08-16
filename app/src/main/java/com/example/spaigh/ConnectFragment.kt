package com.example.spaigh

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.ContentView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.example.spaigh.network.DbConstants
import com.example.spaigh.network.VolleySingleton
import kotlinx.android.synthetic.main.fragment_connect.*
import org.json.JSONException
import org.w3c.dom.Text
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ConnectFragment : Fragment(), View.OnClickListener {

    private val delay = 100L


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connect, container, false)
        val startService = view.findViewById<Button>(R.id.startService)
        val stopService = view.findViewById<Button>(R.id.stopService)

        startService.setOnClickListener(this)
        stopService.setOnClickListener(this)

        return view

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val server_address = activity?.findViewById<TextView>(R.id.server_address)
        val call_state = activity?.findViewById<TextView>(R.id.call_state)
        val device_state = activity?.findViewById<TextView>(R.id.device_state)
        val connection = activity?.findViewById<TextView>(R.id.connection)
        val sync_state = activity?.findViewById<TextView>(R.id.sync_state)
        val service_state = activity?.findViewById<TextView>(R.id.service_state)

        val sharedPref: SharedPreferences? = activity?.applicationContext?.getSharedPreferences(
            getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        )
        if (server_address != null) {
            server_address.hint = DbConstants.SERVER_URL
        }
        if (serviceRunning) {
            if (service_state != null) {
                service_state.text = " SERVICE RUNNING"
            }
        }
        //set server address to stored address on startup
        if (!serviceRunning) {
            if (sharedPref != null) {
                server_address?.text = sharedPref.getString(getString(R.string.ip_address), "")
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////////////
        call_state?.setTextColor(Color.BLUE)
        device_state?.setTextColor(Color.BLUE)
        connection?.setTextColor(Color.RED)
        sync_state?.setTextColor(Color.RED)
        ////////////////////////////////////////////////////////////////

        handler.postDelayed(object : Runnable {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun run() {

                if (call_state != null) {
                    call_state.text = callStateToDb
                }
                if (device_state != null) {
                    device_state.text = moveState
                }
                if (connection != null) {
                    connection.text = isConnected
                }
                if (sync_state != null) {
                    sync_state.text = isSynced
                }

                if (call_state != null) {
                    if (call_state.text == "CALL-IDLE") {
                        call_state.setTextColor(Color.BLUE)
                    } else if (call_state.text == "RINGING") {
                        call_state.setTextColor(Color.RED)
                    } else {
                        call_state.setTextColor(Color.GREEN)
                    }
                }

                if (device_state != null) {
                    if (device_state.text == "DEVICE-MOVING") {
                        device_state.setTextColor(Color.RED)
                    } else {
                        device_state.setTextColor(Color.BLUE)
                    }
                }

                if (connection != null) {
                    if (connection.text == "INTERNET: NOT CONNECTED") {
                        connection.setTextColor(Color.RED)
                    } else {
                        connection.setTextColor(Color.BLUE)
                    }
                }

                if (sync_state != null) {
                    if (sync_state.text == "SYNCING DATA") {
                        sync_state.setTextColor(Color.RED)
                    } else {
                        sync_state.setTextColor(Color.BLUE)
                    }
                }

                if (isConnected != "INTERNET: CONNECTED") {
                    if (server_address != null) {
                        server_address.hint = serverConnection
                    }
                }

                if (isConnected == "INTERNET: CONNECTED" && serviceRunning) {
                    if (server_address != null) {
                        server_address.hint = "Connected to " + DbConstants.SERVER_URL
                    }
                }




                handler.postDelayed(this, delay)
            }
        }, delay)
    }


    /*Runtime permission request*/
    private fun getPermission(permission: String, requestCode: Int) {

        // Checking if permission is not granted
        if (activity?.let { ContextCompat.checkSelfPermission(it, permission) }
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                activity!!,
                arrayOf(permission),
                requestCode
            )
        }
    }


    /*Handle permission request response*/
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    //do nothing
                } else {
                    Toast.makeText(
                        activity,
                        "Phone monitoring function disabled",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
                return
            }

            else -> {
                // Ignore all other requests.
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(v: View?) {
        if (v != null) {

            val server_address = activity?.findViewById<TextView>(R.id.server_address)
            val service_state = activity?.findViewById<TextView>(R.id.service_state)

            when (v.id) {
                R.id.startService -> {
                    if (!serviceRunning) {
                        print("and here")
                        getPermission(android.Manifest.permission.READ_PHONE_STATE, 1)
                        val date =
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/y"))
                        val time =
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

                        //Get server URL from user
                        if (server_address != null) {
                            DbConstants.SERVER_ADDR = server_address.text.toString()
                        }

                        val sharedPref: SharedPreferences? =
                            activity?.applicationContext?.getSharedPreferences(
                                getString(R.string.shared_pref),
                                Context.MODE_PRIVATE
                            )
                        if (sharedPref != null) {
                            with(sharedPref.edit()) {
                                putString(getString(R.string.ip_address), DbConstants.SERVER_ADDR)
                                commit()
                            }
                        }

                        if (server_address != null) {
                            DbConstants.SERVER_URL = "http://" + server_address.text.toString()
                        }
                        checkWithServer("$date,$time", "UNDEFINED", "UNDEFINED")
                    }
                }

                R.id.stopService -> {
                    val serviceIntent = Intent(activity, SpaighService::class.java)
                    activity?.stopService(serviceIntent)
                    if (service_state != null) {
                        service_state.text = " SERVICE STOPPED"
                    }
                    server_address?.text = DbConstants.SERVER_ADDR

                    Toast.makeText(
                        activity,
                        "Service stopped",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkAvailable(): Boolean {
        val connManager =
            activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connManager.activeNetworkInfo
        return networkInfo?.isConnected == true
    }


    /*
        This function sends a POST REQUEST to the serve to confirm if the server is active.
        If the server is active, the app then launches the foreground service, else, the service
        does not start
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun checkWithServer(localTime: String, devState: String, callStateL: String) {
        val server_address = activity?.findViewById<TextView>(R.id.server_address)
        val service_state = activity?.findViewById<TextView>(R.id.service_state)
        if (isNetworkAvailable()) {

            // Formulate POST request and handle response.
            var stringRequest = object : StringRequest(
                Request.Method.POST, DbConstants.SERVER_URL,
                Response.Listener { response ->
                    try {
                        if (response == "POST SUCCESS") {
                            //Send an intent to start service
                            val serviceIntent = Intent(activity, SpaighService::class.java)
                            activity?.let {
                                ContextCompat.startForegroundService(
                                    it,
                                    serviceIntent
                                )
                            }
                            if (service_state != null) {
                                service_state.text = " SERVICE IS RUNNING"
                            }
                            server_address?.text = ""
                            if (server_address != null) {
                                server_address.hint = "Connected to " + DbConstants.SERVER_URL
                            }
                        } else {
                            Toast.makeText(
                                activity,
                                "Could not Connect. Check entered server address and Check if server is running.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Toast.makeText(
                            activity?.applicationContext,
                            "Could not Connect. Check entered server address and Check if server is running.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                Response.ErrorListener {
                    Toast.makeText(
                        activity?.baseContext,
                        "Could not Connect. Check entered server address and Check if server is running.",
                        Toast.LENGTH_LONG
                    ).show()
                }) {

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
            activity?.let { VolleySingleton.getInstance(it).addToRequestQueue(stringRequest) }

        } else {
            Toast.makeText(activity, "No Active Internet Connection", Toast.LENGTH_LONG).show()
        }

    }


}