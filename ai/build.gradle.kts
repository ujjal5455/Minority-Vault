plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.minorityvault.ai"
}

dependencies {
    implementation(project(":protocol"))
    implementation(libs.kotlinx.coroutines.android)
}
