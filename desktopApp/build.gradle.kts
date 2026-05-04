plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.compose.compiler)   // для Kotlin 2.x
    alias(libs.plugins.composeMultiplatform)
}


dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

// Блок application тоже не нужен, всё задаём здесь:
compose.desktop {
    application {
        mainClass = "com.dphascow.desktopApp.MyClass"
        // при желании:
        // nativeDistributions {
        //     targetFormats(TargetFormat.Exe, TargetFormat.Msi)
        //     packageName = "app"
        //     packageVersion = "1.0.0"
        // }
    }
}
