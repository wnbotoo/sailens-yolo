package com.sailens.camera.di

import androidx.camera.core.ImageAnalysis
import com.sailens.camera.Camera
import com.sailens.camera.CameraViewModel
import com.sailens.camera.ImageFrameAnalyzer
import com.sailens.camera.ImageFrameProvider
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val cameraModule = module {
    single { Camera() }
    single { ImageFrameAnalyzer() }
        .binds(arrayOf(ImageAnalysis.Analyzer::class, ImageFrameProvider::class))
    viewModel { CameraViewModel(get(), get(), get()) }
}
