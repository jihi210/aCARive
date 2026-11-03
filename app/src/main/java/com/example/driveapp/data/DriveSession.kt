package com.example.driveapp.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "drive_session")
data class DriveSession(
    @PrimaryKey(autoGenerate = true) val id: Long=0,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long,
    @ColumnInfo(name = "distance_meters") val distanceMeters: Double,
    @ColumnInfo(name="point_count") val pointCount: Int
)