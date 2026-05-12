plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {

    namespace = "com.sanidad.movil"

    compileSdk = 34

    defaultConfig {

        applicationId = "com.sanidad.movil"

        minSdk = 30
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "BASE_URL",
            "\"http://172.16.66.6:8080/\""
        )
    }

    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    val composeBom =
        platform("androidx.compose:compose-bom:2024.02.00")

    implementation(composeBom)

    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")

    implementation(
        "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.7.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0"
    )

    implementation(
        "androidx.activity:activity-compose:1.8.2"
    )

    implementation(
        "androidx.navigation:navigation-compose:2.7.7"
    )

    implementation("androidx.compose.ui:ui")

    implementation(
        "androidx.compose.ui:ui-tooling-preview"
    )

    implementation(
        "androidx.compose.material3:material3"
    )

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    )

    implementation(
        "androidx.datastore:datastore-preferences:1.0.0"
    )

    implementation(
        "com.squareup.retrofit2:retrofit:2.9.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.9.0"
    )

    implementation(
        "com.squareup.okhttp3:okhttp:4.12.0"
    )

    implementation(
        "com.squareup.okhttp3:logging-interceptor:4.12.0"
    )

    testImplementation("junit:junit:4.13.2")
}