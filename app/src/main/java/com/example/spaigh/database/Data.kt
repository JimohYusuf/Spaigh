package com.example.spaigh.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Data (
    @PrimaryKey val timeStamp: String,
    //device current state: moving or idle
    @ColumnInfo(name = "DeviceState") val phnState: String?,
    //call current state: idle, ringing, off-hook, active
    @ColumnInfo(name = "CallState") val callState: String?,
    //sync status: success or failure
    @ColumnInfo(name = "syncStatus") val syncStatus: Int?
)