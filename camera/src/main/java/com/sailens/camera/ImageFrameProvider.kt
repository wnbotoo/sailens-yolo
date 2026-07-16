package com.sailens.camera

import com.sailens.domain.model.perception.ImageFrame
import kotlinx.coroutines.flow.SharedFlow

interface ImageFrameProvider {
    val frames: SharedFlow<ImageFrame>
}