package com.example.spaigh

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.example.spaigh.App.Companion.CHANNEL_ID
import com.example.spaigh.database.AppDatabase
import com.example.spaigh.database.Data
import com.example.spaigh.database.DataDao
import com.example.spaigh.network.DbConstants
import com.example.spaigh.network.VolleySingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.HashMap
import kotlin.math.abs

//all possible device/call states
val stateTypes: List<String> = listOf("CALL-IDLE", "DEVICE-IDLE", "OFF-HOOK", "RINGING", "DEVICE-MOVING", "CALL-ACTIVE", "OUTGOING-CALL")

var moveState = stateTypes[1]
var isConnected = "INTERNET: UNKNOWN"
var isSynced = "NOT SYNCING"
var serviceRunning = false

//parameter controls sensitivity of app to phone movement
var moveThreshold: Float = 0.01F

class SpaighService() : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var manager: PackageManager

    //acquire gravity and linear acceleration sensors
    private var sGrav: Sensor? = null
    private var sLinAccel: Sensor? = null

    //array for storing sensor data
    private var gravData = FloatArray(3) { 0f }
    private var linAccelData = FloatArray(3) { 0f }

    //array for storing most recent sensor data
    private var gravDataTemp = FloatArray(3) { 0f }
    private var linAccelDataTemp = FloatArray(3) { 0f }

    //array for storing difference between current and most recent sensor data
    private var gravDataDelta = FloatArray(3) { 0f }
    private var linAccelDataDelta = FloatArray(3) { 0f }

    //determine if sensor data should be printed to console
    private var canPrint: Boolean = false

    //cartesian coordinates
    private val axis = mapOf(0 to "X", 1 to "Y", 2 to "Z")

    //record changes in sensor state
    private var gravChanged: Boolean = false
    private var linAccelChanged: Boolean = false

    //coroutine scope for background threads
    private val myScope = CoroutineScope(Dispatchers.IO)

    //combining sensor data to record device motion
    private var moveStatePrev = stateTypes[1]

    //handle to database
    private lateinit var dataControl: DataDao

    //used to implement delay before device motion state goes idle
    private var intervalStart: Long = 0L
    private var intervalStop: Long = 0L

    //local time-stamp
    private var localTime: String = ""

    //allow running of block of code periodically (defined by delay)
    private val handler = Handler()
    private val delay = 200 //milliseconds

    //wait time of no active motion before device goes to idle mode
    private val waitTime = 2000

    //time interval for cleaning up local database (daily)
    private val dayInSeconds = 86400000L //in milliseconds

    //parameters controlling cleaning of local database
    private var syncSuccess = true
    private var dbCleaned = false

    private lateinit var jsonObject: JSONObject

    var syncCheck = 0


    override fun onCreate() {
        super.onCreate()

        serviceRunning = true

        manager = packageManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        //acquire specific sensor objects
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            sGrav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        } else {
            Toast.makeText(this, "No gravity sensor found. Motion detection disabled", Toast.LENGTH_LONG).show()
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            sLinAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        } else {
            Toast.makeText(this, "No linear acceleration sensor found", Toast.LENGTH_LONG).show()
        }

        //register sensor listeners
        sGrav?.also { Grav ->
            sensorManager.registerListener(this, Grav, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sLinAccel?.also { LinAccel ->
            sensorManager.registerListener(this, LinAccel, SensorManager.SENSOR_DELAY_NORMAL)
        }

        //initialize handle to database
        dataControl = AppDatabase.getDatabase(application, myScope).dataDao()

        //Code to run every X seconds
        handler.postDelayed(object : Runnable {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {


                    //sync data in case of lost connections
                    myScope.launch {
                        if (isNetworkAvailable()) {
                            isConnected = "INTERNET: CONNECTED"
                            val allData = dataControl.getAll()
                            if ((System.currentTimeMillis() % 30000) < 200){
                                for (data in allData) {
                                    if (data.syncStatus == DbConstants.UNSENT) {
                                        syncCheck += 1
                                        reSyncWithServer(
                                            data.timeStamp,
                                            data.phnState.toString(),
                                            data.callState.toString()
                                        )
                                    }
                                }
                            }

                            if (syncCheck != 0){
                                isSynced = "SYNCING DATA"
                                syncCheck = 0
                            }
                            else{
                                isSynced = "SYNCED ALL DATA"
                            }
                        } else{
                            isConnected = "INTERNET: NOT CONNECTED"
                            isSynced = "SYNCING DATA"
                        }
                    }

                intervalStop = System.currentTimeMillis()
                localTime = getLocalTime()

                //Check For Device Motion
                if (gravChanged && linAccelChanged){
                    moveState = stateTypes[4]
                    intervalStart = System.currentTimeMillis()
                }
                if( moveState == stateTypes[4] && (intervalStop - intervalStart) >= waitTime){
                    if(!gravChanged && !linAccelChanged){
                        moveState = stateTypes[1]
                    }
                }

                // Finite State Machine For Call States
                if (callstatePrev == stateTypes[0] && callstate == stateTypes[2]){
                    callStateToDb = stateTypes[6]
                }
                else if(callstatePrev == stateTypes[3] && callstate == stateTypes[2]){
                    callStateToDb = stateTypes[5]
                }
                else{
                    callStateToDb = callstate
                }


                //Check For Change In Device/Call Data And Write To Database
                if( moveState != moveStatePrev || callstate != callstatePrev)
                {
                    syncWithServer(localTime, moveState, callStateToDb)
                    println("State Changed: Just after syncing with server")
                    moveStatePrev = moveState
                    callstatePrev = callstate
                }

                if (!dbCleaned && syncSuccess && (System.currentTimeMillis() % dayInSeconds) < 2000 ){
                    myScope.launch {
                        dataControl.deleteAllData()
                    }
                    dbCleaned = true
                }
                else{
                    dbCleaned = false
                }

                handler.postDelayed(this, delay.toLong())
            }
        }, delay.toLong())


    }



    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}



    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GRAVITY) {
            for ((counter, value) in event.values.withIndex()) {
                gravData[counter] = value
            }
            for ((counter, value) in gravData.withIndex()){
                gravDataDelta[counter] = abs(gravData[counter] - gravDataTemp[counter])
            }
            for ((count, value) in gravData.withIndex()){
                gravDataTemp[count] = gravData[count]
            }
        }
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            for ((counter, value) in event.values.withIndex()) {
                linAccelData[counter] = value
            }
            for ((counter, value) in linAccelData.withIndex()){
                linAccelDataDelta[counter] = abs(linAccelData[counter] - linAccelDataTemp[counter])
            }
            for ((count, value) in linAccelData.withIndex()){
                linAccelDataTemp[count] = linAccelData[count]
            }
        }

        //check for motion
        gravChanged = (abs(gravDataDelta[0]) > moveThreshold || abs(gravDataDelta[1]) > moveThreshold || abs(gravDataDelta[2]) > moveThreshold)
        linAccelChanged =  (abs(linAccelDataDelta[0]) > moveThreshold || abs(linAccelDataDelta[1]) > moveThreshold || abs(linAccelDataDelta[2]) > moveThreshold)

    }



    override fun onBind(intent: Intent?): IBinder? { return null }



    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        Toast.makeText(this, "service started", Toast.LENGTH_LONG).show()

        //Foreground service notification build
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Spaigh Service")
            .setContentText("Running... Tap to go to app home.")
            .setSmallIcon(R.drawable.ic_fan)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)


        return START_NOT_STICKY
    }


    //does exactly what it says
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getLocalTime(): String {
        val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/y"))
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        return "$date,$time"
    }



    //this function checks whether the device is on a network
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkAvailable(): Boolean{
        val connManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo= connManager.activeNetworkInfo
        return networkInfo?.isConnected == true
    }



    //this function syncs the device's database data to server
    @RequiresApi(Build.VERSION_CODES.M)
    fun syncWithServer(localTime: String, devState: String, callStateL: String){
        if (isNetworkAvailable()){

            // Formulate POST request and handle response.
            var stringRequest = object: StringRequest(
                Request.Method.POST, DbConstants.SERVER_URL,
                Response.Listener { response ->
                    try {
                        if (response == "POST SUCCESS"){
                            syncToLocalSQLite(localTime,devState,callStateL,DbConstants.SENT)
                            println("POST SUCCESSFUL")
                        }
                        else{
                            syncToLocalSQLite(localTime,devState,callStateL,DbConstants.UNSENT)
                            println("POST UNSUCCESSFUL")
                        }
                        println("Response: Inside syncWithServer: $response")
                    } catch (e: JSONException){
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    println("An Error Occurred: Inside SyncWithServer: Inside Response.ErrorListener")
                    println("ErrorListener (Inside Sync) Message: " + error.message)
                    syncToLocalSQLite(localTime,devState,callStateL,DbConstants.UNSENT)
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
            println("Unfortunately no Internet Connection: Inside syncWithServer")
            syncToLocalSQLite(localTime,devState,callStateL,DbConstants.UNSENT)
        }

    }



    @RequiresApi(Build.VERSION_CODES.M)
    fun reSyncWithServer(localTime: String, devState: String, callStateL: String){
        if (isNetworkAvailable()){

            // Formulate POST request and handle response.
            var stringRequest = object: StringRequest(
                Request.Method.POST, DbConstants.SERVER_URL,
                Response.Listener { response ->
                    try {
                        if (response == "POST SUCCESS"){
                            updateLocalSQLite(localTime,devState,callStateL,DbConstants.SENT)
                            println("Inside reSyncWithServer: On Post Success : and Updating DB")
                        }
                    } catch (e: JSONException){
                        e.printStackTrace()
                        println(e.message)
                    }
                },
                Response.ErrorListener {
                    println("An Error Occurred: Inside reSyncWithServer: Inside Response.ErrorListener")
                    println("ErrorListener (Inside Re-sync) Message: " + it.message)
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

        }
    }



    private fun syncToLocalSQLite(localTime: String, devState: String, callStateL: String,
                                  syncStatus: Int = DbConstants.UNSENT){
        myScope.launch {
            dataControl.insert(Data(localTime,devState,callStateL,syncStatus))
            println("Inserted into Local DB")
        }
    }


    private fun updateLocalSQLite(localTime: String, devState: String, callStateL: String,
                                  syncStatus: Int){
        myScope.launch {
            dataControl.update(Data(localTime,devState,callStateL,syncStatus))
            println("Updated Local DB")
        }
    }


    private fun printData (sensorData: FloatArray) {
        var temp = 0.0F
        val df = DecimalFormat("#.###")
        df.roundingMode = RoundingMode.CEILING

        canPrint =
            !(abs(sensorData[0]) < moveThreshold && abs(sensorData[1]) < moveThreshold && abs(sensorData[2]) < moveThreshold)

        if (canPrint) {
            for ((counter, value) in sensorData.withIndex()) {
                if(abs(value) > moveThreshold) {
                    temp = df.format(value).toFloat()
                    print("Axis ${axis[counter]} = $temp")
                }
                else{ print("Axis ${axis[counter]} = 0 ") }
            }
            println()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //Return to default states on stop service
        callStateToDb = "CALL-IDLE"
        moveState = "DEVICE-IDLE"
        isConnected = "INTERNET: UNKNOWN"
        isSynced = "NOT SYNCING"
        //serverConnection = "Disconnected from " + DbConstants.SERVER_URL
        serviceRunning = false
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
    }
}