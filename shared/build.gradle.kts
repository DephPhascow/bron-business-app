import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun String.toKotlinStringLiteralContent(): String =
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n")

val graphQlServerUrl = localProperties.getProperty("graphql.server.url").orEmpty()
val generatedGraphQlConfigDir = layout.buildDirectory.dir("generated/source/graphqlConfig/commonMain/kotlin")
val generateGraphQlConfig by tasks.registering {
    outputs.dir(generatedGraphQlConfigDir)

    doLast {
        val packageDir = generatedGraphQlConfigDir.get().asFile.resolve("com/dphascow/app/config")
        packageDir.mkdirs()
        packageDir.resolve("GraphQlLocalConfig.kt").writeText(
            """
            package com.dphascow.app.config

            object GraphQlLocalConfig {
                const val SERVER_URL: String = "${graphQlServerUrl.toKotlinStringLiteralContent()}"
            }
            """.trimIndent()
        )
    }
}

fun requiredConfig(name: String): String {
    val localValue = gradleLocalProperties(rootDir, providers).getProperty(name)
    val envValue = providers.environmentVariable(name).orNull
    return localValue ?: envValue ?: error(
        "Missing required config '$name'. Add it to local.properties or Xcode Cloud environment variables."
    )
}

val apiUrl: String = requiredConfig("API_URL")
val apiHost: String = requiredConfig("API_HOST")
val apiAPI: String = requiredConfig("API_API")
val wsUrl: String = requiredConfig("WS_URL")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.apollo)
    id("com.codingfeline.buildkonfig") version "0.22.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("org.jetbrains.kotlin.native.cocoapods")
}

buildkonfig {
    packageName = "com.dphascow"

    defaultConfigs {
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "API_URL",
            apiUrl
        )
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "API_HOST",
            apiHost
        )
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "API_API",
            apiAPI
        )
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "WS_URL",
            wsUrl
        )
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "APP_VERSION",
            "1.0"
        )
    }
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
    jvm("desktop")
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
    cocoapods {
        version = "1.0.0"
        summary = "Shared module"
        homepage = "https://example.com"
        ios.deploymentTarget = "14.0"
        framework {
            baseName = "shared"
            isStatic = true
        }
    }
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generatedGraphQlConfigDir)

            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.no.arg)
                implementation(libs.multiplatform.settings.serialization)
                implementation(libs.apollo.runtime)
                implementation(compose.materialIconsExtended)
                implementation(libs.ktor.client.core)
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.coil3.compose)
                implementation(libs.coil3.network.ktor)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.composecreditcardview)
                implementation(libs.coil.compose)
                implementation(libs.androidx.camera.core)
                implementation(libs.androidx.camera.camera2)
                implementation(libs.androidx.camera.lifecycle)
                implementation(libs.androidx.camera.view)
                implementation(libs.ktor.client.okhttp)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(generateGraphQlConfig)
}

android {
    namespace = "com.dphascow.app"
    compileSdk = 36
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
compose.resources {
    publicResClass = true // генерирует Res.*
    packageOfResClass = "com.dphascow.app.resources"
}

apollo {
    service("businessAppAdminSide") {
        packageName.set("com.dphascow.app.graphql")
        srcDir("src/commonMain/kotlin/graphql")
        schemaFile.set(file("src/commonMain/kotlin/graphql/schema.json"))
        // optional: introspection, schema file etc.
        introspection {
            endpointUrl.set(apiUrl.replace("10.0.2.2", "localhost"))
        }
        mapScalarToKotlinString("Date")
        mapScalarToKotlinString("DateTime")
        mapScalarToKotlinString("JSON")
        mapScalarToKotlinString("Time")
    }
}
