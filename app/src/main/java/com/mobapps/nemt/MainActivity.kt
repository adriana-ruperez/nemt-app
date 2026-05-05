package com.mobapps.nemt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mobapps.nemt.navigation.AppNavigation
import com.mobapps.nemt.ui.theme.NEMTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NEMTTheme {
                AppNavigation()
            }
        }
    }
}