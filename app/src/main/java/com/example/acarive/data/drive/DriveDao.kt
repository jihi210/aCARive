package com.example.acarive.data.drive

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveDao {
    @Insert
    suspend fun insertSession(session: DriveSession):Long//저장후에 id돌려줌

    @Insert
    suspend fun insertPoints(points: List<RoutePoint>)

    //드라이브 목록 내림차순
    @Query("SELECT * FROM drive_session ORDER BY start_time DESC")
    fun getSessions(): Flow<List<DriveSession>>

    @Query("SELECT * FROM drive_session WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): DriveSession?

    //드라이브 하나 좌표 순서대로
    @Query("SELECT * FROM route_point WHERE session_id =:sessionId ORDER BY seq")
    suspend fun getPoints(sessionId: Long): List<RoutePoint>
}