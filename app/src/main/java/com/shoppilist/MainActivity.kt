package com.shoppilist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.shoppilist.ui.navigation.AppNavigation
import com.shoppilist.ui.theme.ShoppiListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    ShoppiListTheme {
        Surface {
            AppNavigation()
        }
    }
}
