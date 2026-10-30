plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.minorityvault.net"
}

dependencies {
    implementation(project(":protocol"))
    implementation(libs.play.services.nearby)
    implementation(libs.kotlinx.coroutines.android)
}
