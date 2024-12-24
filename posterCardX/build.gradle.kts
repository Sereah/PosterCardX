plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.dokka") version "2.0.0"
}

android {
    namespace = "com.lunacattus.postercardx"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        version = 1.0

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(file("${projectDir}/docs"))
    dokkaSourceSets {
        named("main") {
            sourceRoots.from(files("src/main/java"))
            suppressInheritedMembers.set(true)
            noStdlibLink.set(true)
            noJdkLink.set(true)
            jdkVersion.set(11)
        }
    }
}

tasks.register<Jar>("dokkaJar") {
    dependsOn("dokkaHtml")
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml.get().outputDirectory)
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.glide)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}