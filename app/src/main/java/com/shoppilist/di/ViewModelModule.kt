package com.shoppilist.di

import com.shoppilist.presentation.*
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AuthViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel {
        ListDetailViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get()
        )
    }
    viewModel { AssignmentsViewModel(get(), get(), get(), get()) }
    viewModel { InviteViewModel(get(), get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get()) }
    viewModel { OrderOnlineViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { VoiceViewModel(get(), get()) }
}
