package com.example.carsomeweatherapp.ui.home

import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import aveek.com.management.ui.db.AppDatabase
import com.example.carsomeweatherapp.R
import com.example.carsomeweatherapp.core.events.ListenToCityAdapterItemCall
import com.example.carsomeweatherapp.databinding.ActivityMainBinding
import com.example.carsomeweatherapp.db.WeatherModel
import com.example.carsomeweatherapp.model.WeatherData
import com.example.carsomeweatherapp.model.forecast.ForecastCustomizedModel
import com.example.carsomeweatherapp.model.forecast.ForecastData
import com.example.carsomeweatherapp.network.NetworkActivity
import com.example.carsomeweatherapp.ui.home.cities.adapter.CitiesListAdapter
import com.example.carsomeweatherapp.ui.home.cities.adapter.WeatherForecastListAdapter
import com.example.carsomeweatherapp.utils.EnumDataState
import com.example.carsomeweatherapp.utils.getDate
import com.example.carsomeweatherapp.viewModel.ViewModelProviderFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.HttpException
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class MainActivity : NetworkActivity(), LifecycleOwner, HasSupportFragmentInjector {

    @Inject
    lateinit var viewModelProviderFactory: ViewModelProviderFactory

    private lateinit var viewModel: MainActivityViewModel

    private lateinit var mLifecycleRegistry: LifecycleRegistry

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    // TODO : Inject Database
    private lateinit var database: AppDatabase

    private lateinit var compositeDisposable: CompositeDisposable

    private lateinit var citiesListAdapter: CitiesListAdapter
    private lateinit var citiesRecyclerView: RecyclerView
    private lateinit var citiesListLayoutManager: LinearLayoutManager

    private lateinit var weatherForecastListAdapter: WeatherForecastListAdapter
    private lateinit var weatherForecastRecyclerView: RecyclerView
    private lateinit var weatherForecastListLayoutManager: LinearLayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        compositeDisposable = CompositeDisposable()

        viewModel = ViewModelProviders.of(this, viewModelProviderFactory)
            .get(MainActivityViewModel::class.java)

        citiesListLayoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.HORIZONTAL
        }
        weatherForecastListLayoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.HORIZONTAL
        }

        initDatabase()

        if (isAppRunningFirstTime) {
            insertInitialDataInsideDB()
        }

        initBinding()

        initCitiesAdapter()

        initWeatherForecastListAdapter()

        mLifecycleRegistry = LifecycleRegistry(this).apply {
            markState(Lifecycle.State.CREATED)
        }

        handleObserver(binding)

        initCitiesRecyclerView()

        initWeatherForecastRecyclerView()

        loadInitialDataToCitiesAdapter()

    }

    private fun initWeatherForecastRecyclerView() {
        weatherForecastRecyclerView = findViewById<RecyclerView>(R.id.rcv_future_weather).apply {
            this.layoutManager = this@MainActivity.weatherForecastListLayoutManager
            this.adapter = this@MainActivity.weatherForecastListAdapter
        }.also {
            it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) { // Detects if it is scrolling downwards
                        val lastVisibleItemPosition =
                            (it.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                        if (lastVisibleItemPosition == weatherForecastListAdapter.itemCount - 1) {
                        }
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                }
            })
        }
    }


    private fun initDatabase() {
        database = AppDatabase.getAppDataBase(this)!!
    }

    private fun insertInitialDataInsideDB() {
        compositeDisposable.add(
            Completable.fromAction {
                val thread = Thread {
                    with(database) {
                        weatherDao().insert(
                            WeatherModel(
                                UUID.randomUUID().toString(),
                                "Kuala Lumpur"
                            )
                        )
                        weatherDao().insert(
                            WeatherModel(
                                UUID.randomUUID().toString(),
                                "George Town"
                            )
                        )
                        weatherDao().insert(
                            WeatherModel(
                                UUID.randomUUID().toString(),
                                "Johor Bahru"
                            )
                        )
                    }
                }
                thread.start()
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this@MainActivity::successCallBack, this@MainActivity::errorCallback)
        )
    }

    private fun loadInitialDataToCitiesAdapter() {
        citiesListAdapter.clearData()
        val disposable = database.weatherDao().getAllData()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                citiesListAdapter.setData(it)
            }, {
                errorCallback(it)
            })
        compositeDisposable.add(disposable)
    }


    private fun initBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this // To enable Live Data object to update the XML on update
    }

    private fun initCitiesAdapter() {
        citiesListAdapter = CitiesListAdapter(this)
    }
    private fun initWeatherForecastListAdapter() {
        weatherForecastListAdapter = WeatherForecastListAdapter(this,viewModelProviderFactory)
    }

    private fun initCitiesRecyclerView() {
        citiesRecyclerView = findViewById<RecyclerView>(R.id.cities_recycler_view).apply {
            this.layoutManager = this@MainActivity.citiesListLayoutManager
            this.adapter = this@MainActivity.citiesListAdapter
        }.also {
            it.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) { // Detects if it is scrolling downwards
                        val lastVisibleItemPosition =
                            (it.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                        if (lastVisibleItemPosition == citiesListAdapter.itemCount - 1) {
                        }
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                }
            })
        }
    }

    private fun handleObserver(binding: ActivityMainBinding) {
        val isNetworkAvailable = true
        binding.viewModel?.let { localViewModel ->
            with(localViewModel) {
                getWeatherDataClick.observe(this@MainActivity, Observer { weatherLiveData ->
                    weatherLiveData.getContentIfNotHandled()?.let {
                        if (isNetworkAvailable) {
                            getWeatherData().observe(this@MainActivity, Observer {
                                it?.let { pair ->
                                    if (pair.first == EnumDataState.SUCCESS.type) {
                                        with(pair.second as WeatherData) {
                                            weatherCondition.value = this.weather[0].main
                                            temparatureInDegreeCelcius.value = String.format(
                                                this@MainActivity.getString(R.string.degree_in_celcius),
                                                this.main.temp
                                            )
                                        }
                                    } else if (pair.first == EnumDataState.ERROR.type) {
                                        with(pair.second as Throwable) {
                                            errorCallback(this)
                                        }
                                    }
                                }
                            })
                            getWeatherForecastData().observe(this@MainActivity, Observer {
                                it?.let { pair ->
                                    if (pair.first == EnumDataState.SUCCESS.type) {
                                        with(pair.second as ForecastData) {
                                            weatherForecastListAdapter.setData(prepareForecastAdapter(this))
                                        }
                                    }
                                    else{
                                        with(pair.second as Throwable) {
                                            errorCallback(this)
                                        }
                                    }
                                }
                            })

                        } else {
//                            showNetWorkNotAvailableDialog()
                        }
                    }
                })
            }
        }
    }

    private fun prepareForecastAdapter(forecastData: ForecastData): List<ForecastCustomizedModel> {

        val listOfForecastAdapter = ArrayList<ForecastCustomizedModel>()

        val list = forecastData.list
        // TODO : Filter date
        for (listWeatherInfo in list) {

            Toast.makeText(this, getDate(listWeatherInfo.dtTxt).toString(), Toast.LENGTH_LONG).show()

            val forecastCustomizedModel = ForecastCustomizedModel().apply {
                this.location = forecastData.city.name
                this.dayOfTheWeek = com.example.carsomeweatherapp.utils.getDayOfTheWeek(listWeatherInfo.dtTxt)
                this.dateOfTheMonth = com.example.carsomeweatherapp.utils.getDateOfTheMonth(listWeatherInfo.dtTxt)
                this.monthOfTheYear = com.example.carsomeweatherapp.utils.getMonthOfTheYear(listWeatherInfo.dtTxt)
                this.temperature = listWeatherInfo.main.temp.toString()
                this.weatherType = listWeatherInfo.weather[0].main
            }
            listOfForecastAdapter.add(forecastCustomizedModel)
        }
        return listOfForecastAdapter
    }

    private fun successCallBack() {
        Toast.makeText(this, "Success", Toast.LENGTH_LONG).show()
    }

    private fun errorCallback(error: Throwable) {
        if (error is HttpException) {

        }
    }

    override fun onStart() {
        super.onStart()
        mLifecycleRegistry.markState(Lifecycle.State.STARTED)
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        mLifecycleRegistry.markState(Lifecycle.State.RESUMED)
    }

    override fun onPause() {
        super.onPause()

    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this);
    }

    override fun onDestroy() {
        super.onDestroy()
        mLifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        compositeDisposable.dispose()
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentDispatchingAndroidInjector
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ListenToCityAdapterItemCall) {
        with(binding) {
            viewModel?.let {
                it.cityName?.let { valueName ->
                    valueName.set(event.getMessage())
                }
                it.openWeatherData()
            }
        }
//        processRequest(binding,event.getMessage())
    }

    fun processRequest(binding: ActivityMainBinding, cityNameData: String) {
        val isNetworkAvailable = true
        binding.viewModel?.let { localViewModel ->
            with(localViewModel) {
                openWeatherData()
                getWeatherDataClick.observe(this@MainActivity, Observer { weatherLiveData ->
                    weatherLiveData.getContentIfNotHandled()?.let {
                        if (isNetworkAvailable) {
                            getWeatherData().observe(this@MainActivity, Observer {
                                it?.let { pair ->
                                    if (pair.first == EnumDataState.SUCCESS.type) {
                                        with(pair.second as WeatherData) {
                                            weatherCondition.value = this.weather[0].main
                                            temparatureInDegreeCelcius.value = String.format(
                                                this@MainActivity.getString(R.string.degree_in_celcius),
                                                this.main.temp
                                            )
                                        }
                                    } else if (pair.first == EnumDataState.ERROR.type) {
                                        with(pair.second as Throwable) {
                                            errorCallback(this)
                                        }
                                    }
                                }
                            })
                        } else {
//                            showNetWorkNotAvailableDialog()
                        }
                    }
                })
            }
        }
    }
}
