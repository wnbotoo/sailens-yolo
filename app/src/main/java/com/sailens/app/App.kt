package com.sailens.app

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import com.sailens.presentation.navigation.SailensApp
import org.koin.androidx.compose.koinViewModel

@Composable
fun App(
    windowSizeClass: WindowSizeClass,
    @Suppress("UNUSED_PARAMETER") appViewModel: AppViewModel = koinViewModel(),
) {
    // appViewModel is resolved for its side effect: it starts/stops device-sensor observation for
    // the lifetime of the app's root composition. Theming + navigation live in SailensApp.
    SailensApp(windowSizeClass = windowSizeClass)
}
