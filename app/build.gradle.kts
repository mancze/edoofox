plugins { id("com.android.application") }

android {
    namespace = "cz.weborama.edoofox"
    compileSdk = 37
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "cz.weborama.edoofox"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "0.3.3"
        testInstrumentationRunner = "cz.weborama.edoofox.SmokeInstrumentation"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            matchingFallbacks += listOf("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    testBuildType = if (providers.gradleProperty("qa").isPresent) "qa" else "debug"
}

dependencies { testImplementation("junit:junit:4.13.2") }
