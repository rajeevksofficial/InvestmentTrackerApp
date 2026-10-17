plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.investmenttracker.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.investmenttracker.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    testImplementation(libs.junit)
}

tasks.register<Copy>("copyApk") {
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(rootProject.file("apk"))
    rename { "InvestTrack.apk" }
    doLast {
        copy {
            from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
            into(rootProject.file("apk"))
            rename { "InvestTrack-v1.1.apk" }
        }
        copy {
            from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
            into(rootProject.file("apk"))
            rename { "InvestmentTracker.apk" }
        }
    }
    dependsOn("assembleDebug")
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy("copyApk")
}
