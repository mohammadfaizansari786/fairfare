package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.InitialData
import com.example.data.model.BusRouteEntity
import com.example.data.model.CityInfo
import com.example.data.model.CommunityReportEntity
import com.example.data.model.SavedPlaceEntity
import com.example.data.model.TariffEntity
import com.example.data.model.TripHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class FareRepository(private val database: AppDatabase) {

    val allCities: List<CityInfo> = InitialData.CITIES

    fun getCityInfo(cityName: String): CityInfo {
        return allCities.find { it.name.equals(cityName, ignoreCase = true) } ?: allCities.first()
    }

    fun getTariffsForCity(cityName: String): Flow<List<TariffEntity>> {
        return database.tariffDao().getTariffsForCity(cityName).map { list ->
            list.distinctBy { it.transportType }
        }
    }

    suspend fun getTariffsForCitySync(cityName: String): List<TariffEntity> {
        val stored = runCatching {
            database.tariffDao().getTariffsForCity(cityName).first()
        }.getOrDefault(emptyList())

        // Fall back to the bundled tariff table when the database has not been
        // seeded yet (first launch) or the read failed, so fare calculation never
        // returns an empty comparison.
        return (stored.ifEmpty { InitialData.tariffsForCityOrFallback(cityName) }).distinctBy { it.transportType }
    }

    fun getAllTariffs(): Flow<List<TariffEntity>> = database.tariffDao().getAllTariffs()

    fun getBusRoutesForCity(cityName: String): Flow<List<BusRouteEntity>> {
        return database.busRouteDao().getBusRoutesForCity(cityName).map { list ->
            list.distinctBy { "${it.city}_${it.routeNumber}_${it.routeName}" }
        }
    }

    fun searchBusRoutes(cityName: String, query: String): Flow<List<BusRouteEntity>> {
        return database.busRouteDao().searchBusRoutes(cityName, query)
    }

    fun getAllSavedPlaces(): Flow<List<SavedPlaceEntity>> = database.savedPlaceDao().getAllSavedPlaces()

    suspend fun savePlace(place: SavedPlaceEntity) = database.savedPlaceDao().insertPlace(place)

    suspend fun deletePlace(id: Long) = database.savedPlaceDao().deletePlace(id)

    fun getAllTrips(): Flow<List<TripHistoryEntity>> = database.tripHistoryDao().getAllTrips()

    fun getRecentTrips(): Flow<List<TripHistoryEntity>> = database.tripHistoryDao().getRecentTrips()

    suspend fun saveTrip(trip: TripHistoryEntity): Long = database.tripHistoryDao().insertTrip(trip)

    suspend fun deleteTrip(id: Long) = database.tripHistoryDao().deleteTrip(id)

    suspend fun clearAllTrips() = database.tripHistoryDao().clearAll()

    fun getReportsForCity(cityName: String): Flow<List<CommunityReportEntity>> =
        database.communityReportDao().getReportsForCity(cityName)

    fun getAllReports(): Flow<List<CommunityReportEntity>> = database.communityReportDao().getAllReports()

    suspend fun submitReport(report: CommunityReportEntity): Long = database.communityReportDao().insertReport(report)

    suspend fun upvoteReport(id: Long) = database.communityReportDao().upvoteReport(id)

    suspend fun addTariff(tariff: TariffEntity): Long = database.tariffDao().insertTariff(tariff)
}
