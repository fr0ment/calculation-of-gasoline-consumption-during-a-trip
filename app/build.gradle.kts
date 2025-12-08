plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.cogcdat_2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.cogcdat_2"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.fragment:fragment:1.6.1")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("androidx.navigation:navigation-fragment:2.7.0")
    implementation("androidx.navigation:navigation-ui:2.7.0")

    // --- ДОБАВЛЕНЫ ОБЯЗАТЕЛЬНЫЕ ЗАВИСИМОСТИ ДЛЯ LIFECYCLE И LIVEDATA ---
    // ВЕРСИЯ БИБЛИОТЕК ВСТАВЛЕНА НАПРЯМУЮ, ЧТОБЫ ИЗБЕЖАТЬ СИНТАКСИЧЕСКИХ ОШИБОК GROOVY.
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    // Если требуется поддержка Java 8:
    // implementation("androidx.lifecycle:lifecycle-common-java8:2.6.2")
    // -------------------------------------------------------------------


    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}