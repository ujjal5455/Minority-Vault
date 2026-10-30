import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

val minSdkLevel = libs.versions.minSdk.get().toInt()
val compileSdkLevel = libs.versions.compileSdk.get().toInt()
val ktlintVersion = libs.versions.ktlint.get()
val detektKotlinVersion = libs.versions.detektKotlin.get()

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<KtlintExtension> {
        version.set(ktlintVersion)
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        parallel = true
    }

    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion(detektKotlinVersion)
        }
    }

    // Shared Android settings, so module build files only declare what is specific to them.
    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> {
            compileSdk = compileSdkLevel
            defaultConfig { minSdk = minSdkLevel }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            compileSdk = compileSdkLevel
            defaultConfig { minSdk = minSdkLevel }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}

// Enforces the dependency direction in CLAUDE.md §5:
// feature → (designsystem, ai, net, data, audio) → protocol → engine. engine depends on nothing.
val core = setOf(":engine", ":protocol")
val services = setOf(":designsystem", ":ai", ":net", ":data", ":audio")
val allowedProjectDependencies: (String) -> Set<String>? = { path ->
    when {
        path == ":engine" -> emptySet()
        path == ":protocol" -> setOf(":engine")
        path in services -> core
        path.startsWith(":feature:") -> core + services
        path == ":app" -> null // the app wires everything together
        else -> emptySet()
    }
}

gradle.projectsEvaluated {
    val violations = subprojects.flatMap { project ->
        val allowed = allowedProjectDependencies(project.path) ?: return@flatMap emptyList()
        project.configurations
            .flatMap { it.dependencies.withType<ProjectDependency>() }
            .map { it.path }
            .filter { it != project.path && it !in allowed }
            .distinct()
            .map { "${project.path} -> $it" }
    }
    if (violations.isNotEmpty()) {
        throw GradleException(
            "Module dependency rule broken (see CLAUDE.md §5):\n" + violations.joinToString("\n"),
        )
    }
}
