package com.snw.samllnewweather.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.snw.samllnewweather.ext.formatTemp
import com.snw.samllnewweather.ext.formatTime
import com.snw.samllnewweather.net.SNNetService
import com.snw.samllnewweather.screen.WeatherInfo
import com.snw.samllnewweather.screen.randomData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val snapi: SNNetService,
    @ApplicationContext private val context: Context
) : ViewModel() {


    private var city: String = ""
    private val _isRefresing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean>
        get() = _isRefresing.asStateFlow()

    private val _errMsg = MutableStateFlow("")
    val errMsg: StateFlow<String>
        get() = _errMsg.asStateFlow()
    private val _weatherData = MutableStateFlow<WeatherInfo>(WeatherInfo())
    val weatherData: StateFlow<WeatherInfo>
        get() = _weatherData.asStateFlow()

    private var loc = ""
    fun refresh(location: String = loc) {
        this.loc = location

        viewModelScope.launch {
            _isRefresing.emit(true)
            //请求网络加载数据
            snapi.getRealTimeInfo(location)
                .zip(snapi.getHourInfo(location)) { source1, source2 ->
                    val result = WeatherInfo()
                    with(source1.now) {
                        result.address = "UnKnow"
                        result.publishTime = source1.updateTime.formatTime()
                        result.temp = temp.formatTemp()
                        result.feelTemp = feelsLike
                        result.text = text
                        result.windDirect = windDir
                        result.windLevel = windScale
                    }
                    result.futureHours = source2.hourly

                    result

                }.zip(snapi.getDayInfo(location)) { source3, source4 ->
                    val dayInfo = source4.daily[0]
                    source3.riseTime = dayInfo.sunrise
                    source3.downTime = dayInfo.sunset
                    source3.tempMax = dayInfo.tempMax.formatTemp()
                    source3.tempMin = dayInfo.tempMin.formatTemp()
                    source3.futureDays = source4.daily
                    source3
                }.zip(snapi.getAirInfo(location)) { source5, source6 ->
                    source5.airAqi = source6.now.aqi
                    source5.airState = source6.now.category
                    source5
                }.flowOn(Dispatchers.IO)
                .catch {
                    _isRefresing.emit(false)
                    _errMsg.emit(it.toString() + "loc" + location)
                }
                .collect {
                    _weatherData.emit(it)
                    _isRefresing.emit(false)
                }
        }
    }

    lateinit var mLocationClient: LocationClient
    private val myListener = MyLocationListener(this)

    init {
        initLocaionClient()
        mLocationClient.start()
    }


    private fun initLocaionClient() {
        mLocationClient = LocationClient(context.applicationContext)
        //声明LocationClient类
        mLocationClient.registerLocationListener(myListener)

        val option = LocationClientOption()
        option.locationMode =
            LocationClientOption.LocationMode.Hight_Accuracy //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备

        option.setCoorType("bd09ll") //可选，默认gcj02，设置返回的定位结果坐标系

        val span = 1000
        option.setScanSpan(span) //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的

        option.setIsNeedAddress(true) //可选，设置是否需要地址信息，默认不需要

        option.isOpenGps = true //可选，默认false,设置是否使用gps

        option.isLocationNotify = true //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果

        option.setIsNeedLocationDescribe(true) //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”

        option.setIsNeedLocationPoiList(true) //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到

        option.setIgnoreKillProcess(false) //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死

        option.SetIgnoreCacheException(false) //可选，默认false，设置是否收集CRASH信息，默认收集

        option.setEnableSimulateGps(false) //可选，默认false，设置是否需要过滤gps仿真结果，默认需要

        mLocationClient.locOption = option

    }


    fun stopLocation() {
        mLocationClient.stop()
    }

    /**
     * 实现定位回调
     */
    class MyLocationListener(val viewModel: MainViewModel) : BDAbstractLocationListener() {
        override fun onReceiveLocation(location: BDLocation) {
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取经纬度相关（常用）的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明
            if (location != null) {
                //获取纬度信息
                val latitude: Double = location.getLatitude()
                //获取经度信息
                val longitude: Double = location.getLongitude()
                Log.i(
                    "location",
                    "latitude = $latitude  longitude = ${longitude}  location.city = ${location.city}"
                )
                Log.i("location.locType", "location.locType == " + location.locType)
                if (location.locType == 61 || location.locType == 161) {
                    viewModel.refresh(
                        "$longitude,$latitude"
                    )
                    viewModel.stopLocation()
                }

                //获取定位精度，默认值为0.0f
//                val radius: Float = location.getRadius()
//                //获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准
//                val coorType: String = location.getCoorType()
//                //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明
//                val errorCode: Int = location.getLocType()
            } else {
                Log.i("location", "latitude = ")
            }
        }
    }


}