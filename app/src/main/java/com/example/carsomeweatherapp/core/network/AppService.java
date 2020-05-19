package com.example.carsomeweatherapp.core.network;

import com.example.carsomeweatherapp.model.WeatherData;
import com.example.carsomeweatherapp.model.forecast.ForecastData;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AppService {


    @GET("weather")
    Observable<WeatherData> getWeatherByCityName(@Query("q") String cityName,
                                                 @Query("APPID") String appID,
                                                 @Query("units") String units);
    @GET("forecast")
    Observable<ForecastData> getWeatherForecastByCityName(@Query("q") String cityName,
                                                          @Query("APPID") String appID,
                                                          @Query("units") String units);

}
