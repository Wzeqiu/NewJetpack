plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    id("io.objectbox")
}

android {
    namespace = "com.common.common"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "17"
    }

    viewBinding {
        enable = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    api(libs.xxpermissions)
    api(libs.glide)
    implementation(libs.mmkv)
    implementation(libs.kotlin.reflect)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.retrofit)
    api(libs.utilcode)
    implementation(libs.face.detection)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.baserecyclerviewadapterhelper4)
    
    // Media3 dependencies
    api(libs.media3.exoplayer)
    api(libs.media3.ui)
    api(libs.media3.exoplayer.dash)
    api(libs.media3.exoplayer.hls)
    api(libs.media3.exoplayer.rtsp)
    api(libs.media3.database)
    api(libs.media3.common)
    api(libs.media3.session)
    api(libs.media3.datasource.okhttp)
    api(libs.androidx.media3.transformer)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}