plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "in.hridayan.lazyselectioncontainer"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.tooling)
    implementation(libs.material3)
    implementation(libs.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}

mavenPublishing {
    coordinates(
        groupId = "io.github.dp-hridayan",
        artifactId = "lazyselectioncontainer",
        version = "1.2.0"
    )

    pom {
        name.set("LazySelectionContainer")
        description.set("The missing text selection container for Jetpack Compose LazyLists")
        inceptionYear.set("2026")
        url.set("https://github.com/DP-Hridayan/LazySelectionContainer")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }

        developers {
            developer {
                id.set("DP-Hridayan")
                name.set("Hridayan")
                email.set("hridayanofficial@gmail.com")
            }
        }

        scm {
            url.set("https://github.com/DP-Hridayan/LazySelectionContainer")
            connection.set("scm:git:git://github.com/DP-Hridayan/LazySelectionContainer.git")
            developerConnection.set("scm:git:ssh://git@github.com/DP-Hridayan/LazySelectionContainer.git")
        }
    }

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}
