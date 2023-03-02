package com.snw.samllnewweather.db

import androidx.room.*
import com.snw.samllnewweather.model.DayInfo
import com.snw.samllnewweather.model.HourInfo
import com.snw.samllnewweather.model.Location
import com.snw.samllnewweather.screen.WeatherInfo

@Dao
interface WeatherInfoDao {
    @Insert
    suspend fun insertBaseInfo(info: WeatherInfo)


    @Query("SELECT * FROM weather_info_table  WHERE cityId = (:cityId) AND cityName = (:cityName)")
    suspend fun getBaseInfo(cityId: String, cityName: String): List<WeatherInfo>

    @Delete
    suspend fun deleteCurrentDataById(data: WeatherInfo)

    @Insert
    suspend fun insertHourInfo(info: HourInfo)

    @Query("SELECT * FROM hour_info_table WHERE cityId = (:cityId) AND cityName = (:cityName)")
    suspend fun getHourInfo(cityId: String, cityName: String): List<HourInfo>

    @Delete
    suspend fun deleteHourDataById(hourInfo: HourInfo)

    @Insert
    suspend fun insertDayInfo(info: DayInfo)

    @Query("SELECT * FROM day_info_table WHERE cityId = (:cityId) AND cityName = (:cityName)")
    suspend fun getDayInfo(cityId: String, cityName: String): List<DayInfo>

    @Delete
    suspend fun deleteDayDataById(dayInfo: DayInfo)


    @Insert
    suspend fun insertLocationInfo(info: Location)

    /**
     * SELECT
     *
    FROM
    table_a
    WHERE
    p_postions = (SELECT MAX(p_postions) FROM table_a)
    LIMIT 1;
     */
    @Query("SELECT * FROM location_info_table WHERE timeTamp =(SELECT MAX(timeTamp) FROM location_info_table) LIMIT 1")
    suspend fun getLocationInfo(): Location

    @Delete
    suspend fun deleteLocationDataById(location: Location)

}