package com.shoppilist.shared.di

import com.shoppilist.shared.presentation.*
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AuthViewModel(get(), get()) }
    viewModel { SplashViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CreateListViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel {
        ListDetailViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get()
        )
    }
    viewModel { AssignmentsViewModel(get(), get(), get(), get()) }
    viewModel { InviteViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get()) }
    viewModel { OrderOnlineViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { AdminViewModel(get(), get(), get(), get(), get()) }
    viewModel { VoiceViewModel(get(), get()) }
    viewModel { ActivityViewModel(get()) }
    viewModel { CategoriesViewModel(get()) }
}
