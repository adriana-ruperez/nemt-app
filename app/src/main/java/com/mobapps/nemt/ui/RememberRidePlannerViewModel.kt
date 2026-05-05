package com.mobapps.nemt.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobapps.nemt.maps.RidePlannerViewModel

@Composable
fun rememberRidePlannerViewModel(): RidePlannerViewModel {
    val activity = LocalContext.current as ComponentActivity
    return viewModel(activity)
}
