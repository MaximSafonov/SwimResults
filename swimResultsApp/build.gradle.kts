import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.koin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidxRoom)
}

// Only these four named values participate in SDK selection. Never print them.
val vkIdKeys = listOf("VKIDClientID", "VKIDClientSecret", "VKIDRedirectHost", "VKIDRedirectScheme")
val vkIdLocalProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.isFile) localFile.inputStream().use { load(it) }
}
val vkIdConfiguration = vkIdKeys.associateWith { key ->
    providers.gradleProperty(key).orNull ?: vkIdLocalProperties.getProperty(key).orEmpty()
}
val vkIdClientId = vkIdConfiguration.getValue("VKIDClientID")
val vkIdConfigured = vkIdClientId.toIntOrNull()?.let { it > 0 && it.toString() == vkIdClientId } == true &&
    vkIdConfiguration.getValue("VKIDClientSecret").isNotBlank() &&
    vkIdConfiguration.getValue("VKIDRedirectHost") == "vk.ru" &&
    vkIdConfiguration.getValue("VKIDRedirectScheme") == "vk$vkIdClientId"

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework { baseName = "SwimResults"; isStatic = true }
    }
    sourceSets {
        androidMain {
            kotlin.srcDir(if (vkIdConfigured) "src/androidVkIdConfigured/kotlin" else "src/androidVkIdUnconfigured/kotlin")
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.androidx.runner)
            implementation(libs.androidx.core)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            if (vkIdConfigured) {
                implementation(libs.vkid.core)
                implementation(libs.vkid.onetap)
            }
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.navigation)
            implementation(libs.compose.material.icons)
            implementation(libs.kotlinx.serialization)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.okio)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.annotations)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
    koinCompiler { userLogs = true }
}

room { schemaDirectory("$projectDir/schemas") }
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

android {
    namespace = "com.msafonov.swimresults"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = "SwimResults-v${variant.versionName}-${variant.buildType.name}.apk"
        }
    }
    defaultConfig {
        applicationId = "com.msafonov.swimresults"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        if (vkIdConfigured) addManifestPlaceholders(vkIdConfiguration)
        versionCode = 10
        versionName = "2.6"
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies { debugImplementation(libs.compose.uiTooling) }
