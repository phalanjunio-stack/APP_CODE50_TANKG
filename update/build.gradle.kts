plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.srlakes.tone.update"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    // org.json no dispositivo real e o de verdade, do proprio Android.
    // Nos testes de unidade (JVM local) o android.jar so tem stubs que
    // lancam "not mocked" - a mesma implementacao real que o
    // socket.io-client ja usa no :sync resolve isso aqui tambem.
    testImplementation(libs.org.json)
    testImplementation(libs.kotlinx.coroutines.test)
}
