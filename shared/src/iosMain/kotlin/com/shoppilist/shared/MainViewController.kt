package com.shoppilist.shared

import androidx.compose.material3.Surface
import androidx.compose.ui.window.ComposeUIViewController
import com.shoppilist.shared.di.initKoinIos
import com.shoppilist.shared.ui.navigation.AppNavigation
import com.shoppilist.shared.ui.theme.ShoppiListTheme
import platform.UIKit.UIViewController

private var koinStarted = false

/** Entry point called from Swift (`iosApp`) to obtain the root Compose Multiplatform view controller. */
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        initKoinIos()
        koinStarted = true
    }
    return ComposeUIViewController {
        ShoppiListTheme {
            Surface {
                AppNavigation()
            }
        }
    }
}
