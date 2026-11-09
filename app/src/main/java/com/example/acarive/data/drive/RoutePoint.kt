package com.example.acarive.data.drive

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "route_point",
        foreignKeys=[ForeignKey(
            entity = DriveSession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )],
    indices = [Index("session_id")],
)
data class RoutePoint(
    @PrimaryKey(autoGenerate = true) val id: Long=0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    val seq: Int,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "time") val time: Long,
    )