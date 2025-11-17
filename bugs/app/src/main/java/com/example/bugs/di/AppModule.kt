package com.example.bugs.di

import com.example.bugs.repository.GoldRepository
import com.example.bugs.viewmodel.GameViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Singleton для репозитория
    single { GoldRepository() }

    // ViewModel factory
    viewModel { GameViewModel(get()) }
}