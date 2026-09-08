import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Assinatura de release.
//
// Lida de keystore.properties (fora do git - veja .gitignore). Sem esse
// arquivo o build de release continua funcionando, só que sem assinatura
// própria: ele usa a assinatura padrão de debug do Android Gradle Plugin,
// suficiente para compilar, mas incapaz de ATUALIZAR um app já instalado
// (o Android recusa instalar por cima quando a assinatura muda).
//
// A chave real mora em keystore/srlakes-release.jks. Perder esse arquivo
// significa perder a capacidade de atualizar, para sempre, qualquer
// celular que já tenha uma versão assinada com ele instalada - a única
// saída seria desinstalar e reinstalar, perdendo presets e histórico.
// Faça backup dos dois arquivos (o .jks e o keystore.properties) em um
// lugar que não seja só este computador.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore = keystorePropertiesFile.exists()

android {
    namespace = "com.srlakes.tone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.srlakes.tone"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        // Só este módulo: o resto do projeto mantém buildConfig desligado
        // (gradle.properties). Aqui é necessário para o atualizador ler
        // BuildConfig.VERSION_NAME em tempo de execução.
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:analysis"))
    implementation(project(":core:protocol"))
    implementation(project(":device:api"))
    implementation(project(":device:mock"))
    implementation(project(":audio"))
    implementation(project(":data"))
    implementation(project(":sync"))
    implementation(project(":update"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
}
