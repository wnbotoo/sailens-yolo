package com.sailens.domain.processor.perception

import com.sailens.domain.config.AnalysisConfig
import com.sailens.domain.model.perception.ClassMapper
import com.sailens.domain.model.perception.SegmentationAnalysis
import com.sailens.domain.model.perception.SegmentationAnalysisStats
import com.sailens.domain.model.perception.SegmentationMask
import com.sailens.domain.util.BooleanStabilizer
import com.sailens.domain.util.FloatSmoother
import kotlin.math.roundToInt

/**
 * 语义分割分析器
 * 统一承接 Kotlin/native 单帧统计，并在 domain 层完成稳定化。
 */
class SegmentationAnalyzer(
    private val config: AnalysisConfig,
    private val classMapper: ClassMapper,
    private val statsExtractor: SegmentationStatsExtractor = KotlinSegmentationStatsExtractor(config, classMapper),
) : SegmentationAnalysisProcessor {
    // 稳定器
    private val roadRatioSmoother = FloatSmoother(windowSize = config.roadRatioSmoothWindow)
    private val bottomCenterRoadRatioSmoother =
        FloatSmoother(windowSize = config.roadRatioSmoothWindow)
    private val navigationPassableRatioSmoother =
        FloatSmoother(windowSize = config.roadRatioSmoothWindow)
    private val trafficLightStabilizer =
        BooleanStabilizer(requiredFrames = config.trafficLightDebounceFrames)

    override fun analyze(
        segmentation: SegmentationMask,
        stats: SegmentationAnalysisStats?,
    ): SegmentationAnalysis {
        val frameStats = stats ?: statsExtractor.extract(segmentation)

        // 稳定化
        val stableRoadRatio = roadRatioSmoother.update(frameStats.roadRatio)
        val stableBottomCenterRoadRatio =
            bottomCenterRoadRatioSmoother.update(frameStats.bottomCenterRoadRatio)
        val stableNavigationPassableRatio =
            navigationPassableRatioSmoother.update(frameStats.navigationPassableRatio)
        val totalPixels = (segmentation.width * segmentation.height).coerceAtLeast(1)
        val trafficLightPixelRatio = trafficLightPixelRatio(frameStats.classCounts, totalPixels)
        val hasTrafficLightCandidate = frameStats.hasTrafficLight &&
            trafficLightPixelRatio >= config.trafficLightMinPixelRatio &&
            stableRoadRatio >= config.trafficLightMinRoadRatio
        val stableHasTrafficLight = trafficLightStabilizer.update(hasTrafficLightCandidate)
        val top3Indices = buildTop3Indices(frameStats.classCounts)
        val dominantClassNames = top3Indices.map { classMapper.getClassName(it) }
        val dominantClassPercentages = top3Indices.map { i ->
            val percent = (frameStats.classCounts[i] * 100f / totalPixels).roundToInt()
            "${classMapper.getClassName(i)}:$percent%"
        }

        return SegmentationAnalysis(
            passableMask = frameStats.passableMask,
            obstacleMask = frameStats.obstacleMask,
            roadRatio = stableRoadRatio,
            hasTrafficLight = stableHasTrafficLight,
            bottomCenterGroundDistribution = frameStats.bottomCenterGroundDistribution,
            bottomCenterRoadRatio = stableBottomCenterRoadRatio,
            bottomStats = frameStats.bottomStats,
            passablePixelCount = frameStats.passablePixelCount,
            navigationPassableRatio = stableNavigationPassableRatio,
            obstaclePixelCount = frameStats.obstaclePixelCount,
            dominantClassNames = dominantClassNames,
            segmentation = segmentation,
            width = segmentation.width,
            height = segmentation.height,
            dominantClassPercentages = dominantClassPercentages,
        )
    }

    override fun reset() {
        roadRatioSmoother.reset()
        bottomCenterRoadRatioSmoother.reset()
        navigationPassableRatioSmoother.reset()
        trafficLightStabilizer.reset()
    }

    private fun trafficLightPixelRatio(classCounts: IntArray, totalPixels: Int): Float {
        if (totalPixels <= 0) return 0f
        var trafficLightPixels = 0
        for (classId in classCounts.indices) {
            if (classMapper.isTrafficLight(classId)) {
                trafficLightPixels += classCounts[classId]
            }
        }
        return trafficLightPixels.toFloat() / totalPixels
    }

    // Returns indices of up to 3 dominant classes (by pixel count), in descending order.
    // O(n) single pass; zero heap allocation.
    private fun buildTop3Indices(classCounts: IntArray): List<Int> {
        var firstIdx = -1; var firstCount = 0
        var secondIdx = -1; var secondCount = 0
        var thirdIdx = -1; var thirdCount = 0
        for (i in classCounts.indices) {
            val count = classCounts[i]
            if (count <= 0) continue
            when {
                count > firstCount -> {
                    thirdIdx = secondIdx; thirdCount = secondCount
                    secondIdx = firstIdx; secondCount = firstCount
                    firstIdx = i; firstCount = count
                }
                count > secondCount -> {
                    thirdIdx = secondIdx; thirdCount = secondCount
                    secondIdx = i; secondCount = count
                }
                count > thirdCount -> {
                    thirdIdx = i; thirdCount = count
                }
            }
        }
        return buildList {
            if (firstIdx >= 0) add(firstIdx)
            if (secondIdx >= 0) add(secondIdx)
            if (thirdIdx >= 0) add(thirdIdx)
        }
    }
}
