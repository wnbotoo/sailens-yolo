package com.sailens.data.source.mapper

import com.sailens.domain.model.perception.ClassMapper
import com.sailens.domain.model.perception.ClassMapperProvider

/**
 * 类别映射器提供者实现
 */
class ClassMapperProviderImpl : ClassMapperProvider {

    private val cityscapesMapper = CityscapesClassMapper()
    private val cocoMapper = CocoClassMapper()

    override fun getSemanticClassMapper(): ClassMapper {
        return cityscapesMapper
    }

    override fun getObstacleClassMapper(): ClassMapper? {
        return cocoMapper
    }
}
