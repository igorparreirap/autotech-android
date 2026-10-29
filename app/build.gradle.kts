import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Chaves locais (não versionadas) lidas do arquivo local.properties na raiz do projeto.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.autotech.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.autotech.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        manifestPlaceholders["MAPS_API_KEY"] =
            localProps.getProperty("MAPS_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
                ?: (project.findProperty("MAPS_API_KEY") as String?) ?: "YOUR_GOOGLE_MAPS_KEY"
        buildConfigField(
            "String", "GOOGLE_WEB_CLIENT_ID",
            "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID")?.trim()?.takeIf { it.isNotEmpty() } ?: (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?) ?: "YOUR_WEB_CLIENT_ID"}\""
        )
        // RF02: chave da SimplesAPI (consulta por placa) — definir SIMPLES_API_KEY no local.properties
        buildConfigField(
            "String", "SIMPLES_API_KEY",
            "\"${localProps.getProperty("SIMPLES_API_KEY", "").trim()}\""
        )
        // RF01: recuperação de senha por e-mail (EmailJS) — chaves no local.properties
        listOf("EMAILJS_SERVICE_ID", "EMAILJS_TEMPLATE_ID", "EMAILJS_PUBLIC_KEY", "EMAILJS_PRIVATE_KEY").forEach { key ->
            buildConfigField("String", key, "\"${localProps.getProperty(key, "").trim()}\"")
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    val room = "2.8.4"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    implementation("org.mindrot:jbcrypt:0.4")

    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose:6.2.1")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Google Sign-In via Credential Manager
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Places API (nearby search for Oficinas, Borracharias, Postos)
    implementation("com.google.android.libraries.places:places:4.1.0")

    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Testes de unidade da camada de domínio (app/src/test)
    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
