package com.sailens.data.source.ml

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract guard for the weights this edition ships.
 *
 * Upstream ships no weights, so this test lives here: it pins that `sem.tflite` / `det.tflite`
 * still satisfy the I/O contract the pipeline decodes against (docs/models.md). If a model update
 * changes shape, dtype, or tensor count, this fails here instead of misbehaving on a device.
 *
 * It does NOT check class channel order — metadata cannot express that. Order stays a manual gate
 * on every model update; see docs/yolo-models.md.
 */
class TfliteModelMetadataReaderTest {

    @Test
    fun `bundled semantic model matches the sem contract`() {
        // 19 dense Cityscapes trainId channels, NHWC, float32 I/O, no SignatureDef (onnx2tf export).
        val model = readModelMetadata("sem.tflite")
        assertTensor(
            tensor = model.inputs.single(),
            shape = listOf(1, 640, 640, 3),
            elementType = TfliteTensorElementType.FLOAT32,
            hasQuantization = false,
        )
        assertTensor(
            tensor = model.outputs.single(),
            shape = listOf(1, 640, 640, 19),
            elementType = TfliteTensorElementType.FLOAT32,
            hasQuantization = false,
        )
        assertTrue(model.signatures.isEmpty())
    }

    @Test
    fun `bundled detection model matches the det contract`() {
        // Single RAW_TRANSPOSED tensor [1, 4+80, N]; float keeps the zero-copy handle fast path.
        val model = readModelMetadata("det.tflite")
        assertTensor(
            tensor = model.inputs.single(),
            shape = listOf(1, 640, 640, 3),
            elementType = TfliteTensorElementType.FLOAT32,
            hasQuantization = false,
        )
        assertTensor(
            tensor = model.outputs.single(),
            shape = listOf(1, 84, 8400),
            elementType = TfliteTensorElementType.FLOAT32,
            hasQuantization = false,
        )
        assertTrue(model.signatures.isEmpty())
    }

    private fun assertTensor(
        tensor: TfliteTensorMetadata,
        shape: List<Int>,
        elementType: TfliteTensorElementType,
        hasQuantization: Boolean,
    ) {
        assertEquals(shape, tensor.shape)
        assertEquals(elementType, tensor.elementType)
        if (hasQuantization) {
            assertNotNull(tensor.quantization)
        } else {
            assertNull(tensor.quantization)
        }
    }

    private fun readModelMetadata(assetName: String): TfliteModelMetadata {
        return TfliteModelMetadataReader.read(assetFile(assetName).readBytes())
    }

    private fun assetFile(assetName: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        val repoRoot = generateSequence(File(userDir).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        return File(repoRoot, "data/src/main/assets/$assetName")
    }
}
