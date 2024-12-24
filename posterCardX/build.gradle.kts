import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
    id("signing")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

mavenPublishing {
    coordinates("com.lunacattus.postercardx", "postercardx", "1.0.0")
    pom {
        name = "PosterCardX"
        description = "An extended view based on androidx.cardview.widget.CardView, which generates a nice background based on a poster image."
        url = "https://github.com/Sereah/PosterCardX"
        licenses {
            license {
                name = "LGPL-3.0 license"
                url = "https://www.gnu.org/licenses/lgpl-3.0.html"
                distribution = "https://www.gnu.org/licenses/lgpl-3.0.html"
            }
        }
        developers {
            developer {
                name = "sereah"
                url = "https://github.com/Sereah"
                email = "sereahha@gmail.com"
            }
        }
        scm {
            connection = "scm:git:git://github.com/Sereah/PosterCardX.git"
            developerConnection = "scm:git:ssh://github.com/Sereah/PosterCardX.git"
            url = "https://github.com/Sereah/PosterCardX.git"
        }
    }
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
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