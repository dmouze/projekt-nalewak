package com.kierman.projektnalewak.di

import com.kierman.projektnalewak.viewmodel.NalewakViewModel
import com.kierman.projektnalewak.viewmodel.Repository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { NalewakViewModel(get()) }
}

val repositoryModule = module{
    single{
        Repository()
    }
}
