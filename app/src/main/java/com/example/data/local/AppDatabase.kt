package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BusRouteEntity
import com.example.data.model.CommunityReportEntity
import com.example.data.model.PlaceCategory
import com.example.data.model.SavedPlaceEntity
import com.example.data.model.TariffEntity
import com.example.data.model.TransportType
import com.example.data.model.TripHistoryEntity
import com.example.data.model.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromTransportType(type: TransportType?): String = type?.name ?: TransportType.AUTO_RICKSHAW.name

    @TypeConverter
    fun toTransportType(value: String?): TransportType =
        value?.let { runCatching { TransportType.valueOf(it) }.getOrDefault(TransportType.AUTO_RICKSHAW) }
            ?: TransportType.AUTO_RICKSHAW

    @TypeConverter
    fun fromVerificationStatus(status: VerificationStatus?): String = status?.name ?: VerificationStatus.OFFICIAL.name

    @TypeConverter
    fun toVerificationStatus(value: String?): VerificationStatus =
        value?.let { runCatching { VerificationStatus.valueOf(it) }.getOrDefault(VerificationStatus.OFFICIAL) }
            ?: VerificationStatus.OFFICIAL

    @TypeConverter
    fun fromPlaceCategory(category: PlaceCategory?): String = category?.name ?: PlaceCategory.RECENT.name

    @TypeConverter
    fun toPlaceCategory(value: String?): PlaceCategory =
        value?.let { runCatching { PlaceCategory.valueOf(it) }.getOrDefault(PlaceCategory.RECENT) }
            ?: PlaceCategory.RECENT
}

@Database(
    entities = [
        TariffEntity::class,
        TripHistoryEntity::class,
        SavedPlaceEntity::class,
        CommunityReportEntity::class,
        BusRouteEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tariffDao(): TariffDao
    abstract fun tripHistoryDao(): TripHistoryDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun communityReportDao(): CommunityReportDao
    abstract fun busRouteDao(): BusRouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            // The seeding callback must not call getInstance(): that re-enters the
            // same lock while INSTANCE is still null and could deadlock or build a
            // second database. Hold the instance in a local reference instead.
            var databaseRef: AppDatabase? = null

            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // Seed data ships with the app and is re-inserted on create, so
                // dropping the schema on upgrade is safe and avoids crashing the
                // app on version bumps with no migration.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val database = databaseRef ?: return
                        seedingScope.launch {
                            runCatching {
                                database.tariffDao().insertAll(InitialData.DEFAULT_TARIFFS)
                                database.busRouteDao()
                                    .insertAllBusRoutes(InitialData.DEFAULT_BUS_ROUTES)
                                InitialData.DEFAULT_SAVED_PLACES.forEach {
                                    database.savedPlaceDao().insertPlace(it)
                                }
                                InitialData.DEFAULT_REPORTS.forEach {
                                    database.communityReportDao().insertReport(it)
                                }
                            }
                        }
                    }
                })
                .build()

            databaseRef = instance
            return instance
        }

        private const val DATABASE_NAME = "fairfare_database.db"

        private val seedingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
