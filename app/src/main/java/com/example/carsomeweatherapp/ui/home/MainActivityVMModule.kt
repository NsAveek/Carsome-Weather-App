package com.example.carsomeweatherapp.ui.home

import androidx.lifecycle.ViewModel
import com.example.carsomeweatherapp.viewModel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class MainActivityVMModule {
    @Binds
    @IntoMap
    @ViewModelKey(MainActivityViewModel::class)
    abstract fun bindMainViewModel (viewModel : MainActivityViewModel): ViewModel

}