import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File

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

            val platform = when {
                org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
                org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "mac"
                else -> "linux"
            }

            implementation("org.openjfx:javafx-base:$javaFxVersion:$platform")
            implementation("org.openjfx:javafx-graphics:$javaFxVersion:$platform")
            implementation("org.openjfx:javafx-controls:$javaFxVersion:$platform")
            implementation("org.openjfx:javafx-swing:$javaFxVersion:$platform")
            implementation("org.openjfx:javafx-web:$javaFxVersion:$platform")

            implementation("org.openjfx:javafx-media:$javaFxVersion:$platform")

        }
    }
}

compose.desktop {
    application {
        mainClass = "org.np.MainKt"

        jvmArgs += listOf(
            "--add-modules", "ALL-MODULE-PATH"
        )

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
