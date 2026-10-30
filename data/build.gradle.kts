plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.minorityvault.data"
}

dependencies {
    implementation(project(":protocol"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
}
