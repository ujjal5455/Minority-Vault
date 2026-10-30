plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.minorityvault.audio"
}

dependencies {
    implementation(libs.androidx.media3.exoplayer)
}
