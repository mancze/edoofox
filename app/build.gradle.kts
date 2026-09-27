import java.util.Properties

plugins { id("com.android.application") }

val keystorePropertiesFile = rootProject.file("keystore.properties")
// Public clones and CI can build without the maintainer's private signing key.
val releaseSigning = providers.gradleProperty("releaseSigning").orElse("auto").get()
require(releaseSigning in listOf("auto", "disabled", "required")) {
    "releaseSigning must be auto, disabled, or required"
}
val keystoreProperties = Properties().apply {
    if (releaseSigning != "disabled" && keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}
val signingKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val hasReleaseSigning = signingKeys.all { !keystoreProperties.getProperty(it).isNullOrBlank() }
check(keystoreProperties.isEmpty() || hasReleaseSigning) {
    "Incomplete keystore.properties: storeFile, storePassword, keyAlias and keyPassword are required"
}
check(releaseSigning != "required" || hasReleaseSigning) {
    "Release signing requested but not configured. See docs/SIGNING.md."
}

android {
    namespace = "cz.weborama.edoofox"
    compileSdk = 37
    buildToolsVersion = "36.0.0"
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "cz.weborama.edoofox"
        minSdk = 26
        targetSdk = 37
        versionCode = 13
        versionName = "0.3.10"
        testInstrumentationRunner = "cz.weborama.edoofox.SmokeInstrumentation"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        if (hasReleaseSigning) create("release") {
            storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
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
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
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
            val unsigned = if (variant.buildType == "release" && !hasReleaseSigning) "-unsigned" else ""
            output.outputFileName.set("edoofox-$artifactVersionName-${variant.buildType}$unsigned.apk")
        }
    }
}

dependencies { testImplementation("junit:junit:4.13.2") }
