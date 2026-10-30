plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.minorityvault"

    defaultConfig {
        applicationId = "com.example.minorityvault"
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // R8 rules arrive in Phase 11.
            isMinifyEnabled = false
        }
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":protocol"))
    implementation(project(":net"))
    implementation(project(":ai"))
    implementation(project(":data"))
    implementation(project(":designsystem"))
    implementation(project(":audio"))
    implementation(project(":feature:lobby"))
    implementation(project(":feature:round"))
    implementation(project(":feature:results"))
    implementation(project(":feature:history"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:tutorial"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
