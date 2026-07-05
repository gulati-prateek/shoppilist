package com.shoppilist.shared.di

import com.shoppilist.shared.data.repository.NotificationRepository
import com.shoppilist.shared.data.repository.RoomNotificationRepository
import org.koin.dsl.module

val notificationModule = module {
    single<NotificationRepository> { RoomNotificationRepository(get()) }
}
