import java.util.Properties

plugins { id("com.android.application") }

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "cz.weborama.edoofox"
    compileSdk = 37
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "cz.weborama.edoofox"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "0.3.5"
        testInstrumentationRunner = "cz.weborama.edoofox.SmokeInstrumentation"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        create("release") {
            check(keystorePropertiesFile.exists()) {
                "Missing keystore.properties. See README.md for local release-signing setup."
            }
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }
    buildTypes {
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            matchingFallbacks += listOf("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    testBuildType = if (providers.gradleProperty("qa").isPresent) "qa" else "debug"
}

val artifactVersionName = android.defaultConfig.versionName
    ?: error("defaultConfig.versionName must be set for APK artifact naming")

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("edoofox-$artifactVersionName-${variant.buildType}.apk")
        }
    }
}

dependencies { testImplementation("junit:junit:4.13.2") }
