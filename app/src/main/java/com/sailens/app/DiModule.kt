package com.sailens.app

import com.sailens.BuildConfig
import com.sailens.camera.di.cameraModule
import com.sailens.data.di.dataModule
import com.sailens.presentation.config.UiFeatureFlags
import com.sailens.presentation.di.presentationDebugModule
import com.sailens.presentation.di.presentationModule
import com.sailens.presentation.settings.AppInfo
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(dataModule)
    includes(profileBindingsModule)
    includes(domainBindingsModule)
    includes(cameraModule)
    includes(presentationModule)
    includes(presentationDebugModule)

    single {
        UiFeatureFlags(
            showDiagnostics = BuildConfig.SHOW_DIAGNOSTICS,
        )
    }

    single {
        AppInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            license = BuildConfig.APP_LICENSE,
            sourceUrl = BuildConfig.APP_SOURCE_URL,
        )
    }

    viewModel {
        AppViewModel(get())
    }

}
