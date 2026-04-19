plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.grepguru.zenlock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.grepguru.zenlock"
        minSdk = 28
        targetSdk = 36
        versionCode = 31
        versionName = "1.10.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // major.minor.patch

    buildFeatures {
        buildConfig = true
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Security: Disable debugging and logging in release
            buildConfigField("boolean", "DEBUG_LOGGING", "false")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "DEBUG_LOGGING", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_15
        targetCompatibility = JavaVersion.VERSION_15
    }

    // Product flavors:
    //   playstore = default build, no SMS permission. Used for Google Play releases
    //               and for the default GitHub artifact.
    //   sms       = adds SEND_SMS permission via a flavor-specific manifest in
    //               app/src/sms/AndroidManifest.xml. Built only by CI and
    //               attached to GitHub Releases as an optional "SMS-enabled" variant.
    //               Named "sms" (not "github") so CI task names and APK filenames
    //               clearly indicate which variant includes the SMS feature.
    flavorDimensions += "dist"
    productFlavors {
        create("playstore") {
            dimension = "dist"
            isDefault = true
        }
        create("sms") {
            dimension = "dist"
            versionNameSuffix = "-sms"
        }
    }
}

// Android Studio Build Variants panel defaults to playstoreDebug / playstoreRelease
// so local builds never accidentally include the SMS permission.

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Room components for SQLite database
    implementation("androidx.room:room-runtime:2.6.1")
    // room-ktx removed: this is a Java project, room-runtime is sufficient
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    
    // LiveData for reactive UI updates
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    
    // MPAndroidChart for native charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}
