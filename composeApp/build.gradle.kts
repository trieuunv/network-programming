import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compottie)
            implementation(libs.compottie.lite)
            implementation(libs.compottie.dot)
            implementation(libs.compottie.network)
            implementation(libs.compottie.resources)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)

            val ktorVersion = "3.0.3"
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-okhttp:$ktorVersion") // ✅ Đổi sang OkHttp
            implementation("io.ktor:ktor-client-logging:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            val javaFxVersion = "21.0.1"
            implementation("org.openjfx:javafx-base:$javaFxVersion")
            implementation("org.openjfx:javafx-graphics:$javaFxVersion")
            implementation("org.openjfx:javafx-controls:$javaFxVersion")

            // Nếu bạn cần WebView, hãy đảm bảo thêm nó vào:
            implementation("org.openjfx:javafx-web:$javaFxVersion")
            // Và module cho Swing Interop:
            implementation("org.openjfx:javafx-swing:$javaFxVersion")
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.np.MainKt"

        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.np"
            packageVersion = "1.0.0"
        }
    }
}
