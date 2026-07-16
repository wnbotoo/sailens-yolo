import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val enableLitertNpuRuntime = providers.gradleProperty("sailens.enableLitertNpuRuntime")
    .map { it.toBooleanStrict() }
    .getOrElse(false)
val litertNpuRuntimeRoot = rootProject.layout.projectDirectory.dir("litert_npu_runtime_libraries_jit").asFile
val litertNpuRuntimeFeatureModules = listOf(
    "qualcomm_runtime_v81",
    "qualcomm_runtime_v79",
    "qualcomm_runtime_v75",
    "qualcomm_runtime_v73",
    "qualcomm_runtime_v69",
)
val missingLitertNpuRuntimeFeatures = litertNpuRuntimeFeatureModules.filterNot { moduleName ->
    litertNpuRuntimeRoot.resolve(moduleName).resolve("build.gradle.kts").isFile
}
val hasLitertNpuRuntimeStrings =
    litertNpuRuntimeRoot.resolve("runtime_strings").resolve("build.gradle.kts").isFile
if (enableLitertNpuRuntime) {
    check(hasLitertNpuRuntimeStrings && missingLitertNpuRuntimeFeatures.isEmpty()) {
        "sailens.enableLitertNpuRuntime=true requires runtime_strings and all Qualcomm LiteRT NPU " +
            "runtime feature modules under ${litertNpuRuntimeRoot.path}; " +
            "missingFeatures=${missingLitertNpuRuntimeFeatures.joinToString()}, " +
            "runtimeStringsPresent=$hasLitertNpuRuntimeStrings"
    }
}
val availableLitertNpuRuntimeFeatures = litertNpuRuntimeFeatureModules
    .takeIf { enableLitertNpuRuntime }
    .orEmpty()
    .map { moduleName -> ":litert_npu_runtime_libraries_jit:$moduleName" }
    .toSet()

android {
    namespace = "com.sailens"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sailens"
        // Android 12 (API 31). 31 is the floor for Build.SOC_MANUFACTURER/SOC_MODEL used by
        // DeviceHardwareProfileProvider, and keeps device reach broad for the GPU-only release.
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // YOLO Edition: this build bundles AGPL-3.0 weights, so the distributed work is AGPL-3.0
        // and users must be pointed at THIS repository's corresponding source, not upstream's.
        // These two lines are the whole of the edition's runtime identity delta — see
        // docs/repository-license-strategy.md.
        buildConfigField("String", "APP_LICENSE", "\"AGPL-3.0\"")
        buildConfigField("String", "APP_SOURCE_URL", "\"https://github.com/wnbotoo/sailens-yolo\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // arm64-v8a covers every modern 64-bit Android SoC (Qualcomm, MediaTek Dimensity, Google
        // Tensor) — this is NOT a vendor restriction. It drops 32-bit-only and x86 (emulator/ChromeOS)
        // to shrink the APK and native build time for the packaged native libs (OpenCV + sailens_ml).
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    if (enableLitertNpuRuntime) {
        dynamicFeatures += availableLitertNpuRuntimeFeatures
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "SHOW_DIAGNOSTICS", "true")
        }
        release {
            buildConfigField("boolean", "SHOW_DIAGNOSTICS", "false")
            // R8 relies on the default native-methods keep rule plus the JNI keeps in
            // :data consumer-rules.pro (JNI is name-bound). Smoke-test a release build on
            // device before relying on it: name-based JNI / reflection break at runtime, not build.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            // Only the Qualcomm NPU path needs extracted native libs: LiteRT's QNN dispatch dlopen()s
            // libLiteRtDispatch_Qualcomm.so from the app's lib dir (/data/app/.../lib/arm64), which is
            // empty under the modern uncompressed-in-APK default. GPU-only builds don't need it, so tie
            // legacy packaging to the NPU runtime flag to keep the default (GPU) release install smaller.
            // NOTE: native-lib load failures surface at runtime, not build — smoke-test a release build.
            useLegacyPackaging = enableLitertNpuRuntime
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    // Required for LiteRT's downloadLibrary() to install the on-demand Qualcomm NPU runtime module.
    implementation(libs.google.play.feature.delivery)
    implementation(libs.google.play.feature.delivery.ktx)
    if (enableLitertNpuRuntime) {
        implementation(project(":litert_npu_runtime_libraries_jit:runtime_strings"))
    }
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":camera"))
    implementation(project(":presentation"))
    implementation(project(":ux"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
