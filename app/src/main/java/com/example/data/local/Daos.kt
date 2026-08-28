package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BusRouteEntity
import com.example.data.model.CommunityReportEntity
import com.example.data.model.SavedPlaceEntity
import com.example.data.model.TariffEntity
import com.example.data.model.TransportType
import com.example.data.model.TripHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TariffDao {
    @Query("SELECT * FROM tariffs WHERE city = :cityName ORDER BY transportType ASC")
    fun getTariffsForCity(cityName: String): Flow<List<TariffEntity>>

    @Query("SELECT * FROM tariffs WHERE city = :cityName AND transportType = :transportType LIMIT 1")
    suspend fun getTariff(cityName: String, transportType: TransportType): TariffEntity?

    @Query("SELECT * FROM tariffs ORDER BY city ASC, transportType ASC")
    fun getAllTariffs(): Flow<List<TariffEntity>>

    @Query("SELECT DISTINCT city FROM tariffs ORDER BY city ASC")
    fun getDistinctCities(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariff(tariff: TariffEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tariffs: List<TariffEntity>)
}

@Dao
interface TripHistoryDao {
    @Query("SELECT * FROM trip_history ORDER BY timestamp DESC")
    fun getAllTrips(): Flow<List<TripHistoryEntity>>

    @Query("SELECT * FROM trip_history ORDER BY timestamp DESC LIMIT 5")
    fun getRecentTrips(): Flow<List<TripHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripHistoryEntity): Long

    @Query("DELETE FROM trip_history WHERE id = :id")
    suspend fun deleteTrip(id: Long)

    @Query("DELETE FROM trip_history")
    suspend fun clearAll()
}

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY visitCount DESC, timestamp DESC")
    fun getAllSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places WHERE city = :cityName ORDER BY visitCount DESC")
    fun getPlacesForCity(cityName: String): Flow<List<SavedPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: SavedPlaceEntity): Long

    @Update
    suspend fun updatePlace(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deletePlace(id: Long)
}

@Dao
interface CommunityReportDao {
    @Query("SELECT * FROM community_reports WHERE city = :cityName ORDER BY timestamp DESC")
    fun getReportsForCity(cityName: String): Flow<List<CommunityReportEntity>>

    @Query("SELECT * FROM community_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<CommunityReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: CommunityReportEntity): Long

    @Query("UPDATE community_reports SET upvotes = upvotes + 1 WHERE id = :id")
    suspend fun upvoteReport(id: Long)
}

@Dao
interface BusRouteDao {
    @Query("SELECT * FROM bus_routes WHERE city = :cityName ORDER BY routeNumber ASC")
    fun getBusRoutesForCity(cityName: String): Flow<List<BusRouteEntity>>

    @Query("SELECT * FROM bus_routes WHERE city = :cityName AND (routeNumber LIKE '%' || :query || '%' OR routeName LIKE '%' || :query || '%' OR intermediateStopsCsv LIKE '%' || :query || '%')")
    fun searchBusRoutes(cityName: String, query: String): Flow<List<BusRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBusRoutes(routes: List<BusRouteEntity>)
}
