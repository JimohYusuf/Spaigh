package com.example.spaigh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager.*

//call state written to database
var callStateToDb = "CALL-IDLE"

//current call state
var callstate = "CALL-IDLE"

//previous/most recent call state
var callstatePrev = "CALL-IDLE"

class IncomingReceiver : BroadcastReceiver() {
    private var callStates =
        mutableMapOf("IDLE" to "true", "OFF-HOOK" to "false", "RINGING" to "false")

    override fun onReceive(context: Context, intent: Intent) {
        if (serviceRunning) {
            val phoneState = intent.getStringExtra(EXTRA_STATE)
            callStates["IDLE"] = (phoneState == EXTRA_STATE_IDLE).toString()
            callStates["OFF-HOOK"] = (phoneState == EXTRA_STATE_OFFHOOK).toString()
            callStates["RINGING"] = (phoneState == EXTRA_STATE_RINGING).toString()

            if (callStates["IDLE"] == "true") {
                callstate = stateTypes[0]
            } else if (callStates["OFF-HOOK"] == "true") {
                callstate = stateTypes[2]
            } else if (callStates["RINGING"] == "true") {
                callstate = stateTypes[3]
            }
        }
    }
}



