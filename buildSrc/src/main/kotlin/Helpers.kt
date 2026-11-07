import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.AbstractAppExtension
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import java.util.Base64
import java.util.Properties
import kotlin.system.exitProcess

private val Project.android get() = extensions.getByName<ApplicationExtension>("android")

private lateinit var localProperties: Properties

/** Reserve one patch version per APK/AAB build; persist it only after a successful build. */
fun Project.requireMetadata(): Properties {
    val cacheKey = "nb4a.buildMetadata"
    val extra = rootProject.extensions.extraProperties
    if (extra.has(cacheKey)) return extra.get(cacheKey) as Properties

    val metadataFile = rootProject.file("nb4a.properties")
    val originalText = metadataFile.readText()
    val values = Properties().apply { originalText.reader().use { load(it) } }
    val producesApp = !gradle.startParameter.isDryRun && gradle.startParameter.taskNames.any {
        val taskName = it.substringAfterLast(':')
        taskName == "build" || taskName == "buildNeeded" || taskName == "buildDependents" ||
            listOf("assemble", "bundle", "package", "install").any { prefix -> taskName.startsWith(prefix) }
    }
    if (producesApp) {
        val previousVersion = values.getProperty("VERSION_NAME")
        val parts = previousVersion.split('.')
        require(parts.size == 3 && parts.all { it.toIntOrNull() != null }) {
            "VERSION_NAME must have the format major.minor.patch"
        }
        val nextVersion = "${parts[0]}.${parts[1]}.${Math.addExact(parts[2].toInt(), 1)}"
        values.setProperty("VERSION_NAME", nextVersion)
        values.setProperty("VERSION_CODE", Math.addExact(values.getProperty("VERSION_CODE").toInt(), 1).toString())
        val previewVersion = values.getProperty("PRE_VERSION_NAME")
        if (previewVersion?.startsWith("pre-$previousVersion-") == true) {
            values.setProperty("PRE_VERSION_NAME", "pre-$nextVersion-" + previewVersion.removePrefix("pre-$previousVersion-"))
        }
        logger.lifecycle("App version: $previousVersion -> $nextVersion")
        // Test, lint, sync and dry-run invocations do not reserve or persist a version.
        // Failed packaging can be retried using the same version.
        @Suppress("DEPRECATION")
        gradle.buildFinished {
            if (failure == null) {
                check(metadataFile.readText() == originalText) {
                    "nb4a.properties changed during the build; refusing to overwrite it"
                }
                var updatedText = originalText
                for (key in listOf("VERSION_NAME", "VERSION_CODE", "PRE_VERSION_NAME")) {
                    val value = values.getProperty(key) ?: continue
                    updatedText = updatedText.replace(Regex("(?m)^$key=[^\r\n]*")) { "$key=$value" }
                }
                metadataFile.writeText(updatedText)
            }
        }
    }
    extra.set(cacheKey, values)
    return values
}

fun Project.requireLocalProperties(): Properties {
    if (!::localProperties.isInitialized) {
        localProperties = Properties()

        val base64 = System.getenv("LOCAL_PROPERTIES")
        if (!base64.isNullOrBlank()) {

            localProperties.load(Base64.getDecoder().decode(base64).inputStream())
        } else if (project.rootProject.file("local.properties").exists()) {
            localProperties.load(rootProject.file("local.properties").inputStream())
        }
    }
    return localProperties
}

fun Project.setupCommon() {
    android.apply {
        buildToolsVersion = "35.0.1"
        compileSdk = 35
        defaultConfig {
            minSdk = 21
            targetSdk = 35
        }
        buildTypes {
            getByName("release") {
                isMinifyEnabled = true
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        (android as ExtensionAware).extensions.getByName<KotlinJvmOptions>("kotlinOptions").apply {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
        lint {
            showAll = true
            checkAllWarnings = true
            checkReleaseBuilds = true
            warningsAsErrors = true
            textOutput = project.file("build/lint.txt")
            htmlOutput = project.file("build/lint.html")
        }
        packaging {
            resources.excludes.addAll(
                listOf(
                    "**/*.kotlin_*",
                    "/META-INF/*.version",
                    "/META-INF/native/**",
                    "/META-INF/native-image/**",
                    "/META-INF/INDEX.LIST",
                    "DebugProbesKt.bin",
                    "com/**",
                    "org/**",
                    "**/*.java",
                    "**/*.proto",
                    "okhttp3/**"
                )
            )
        }
        (this as? AbstractAppExtension)?.apply {
            buildTypes {
                getByName("release") {
                    isShrinkResources = true
                    if (System.getenv("nkmr_minify") == "0") {
                        isShrinkResources = false
                        isMinifyEnabled = false
                    }
                }
                getByName("debug") {
                    applicationIdSuffix = "debug"
                    debuggable(true)
                    jniDebuggable(true)
                }
            }
            applicationVariants.forEach { variant ->
                variant.outputs.forEach {
                    it as BaseVariantOutputImpl
                    it.outputFileName = it.outputFileName.replace(
                        "app", "${project.name}-" + variant.versionName
                    ).replace("-release", "").replace("-oss", "")
                }
            }
        }
    }
}

fun Project.setupAppCommon() {
    setupCommon()

    val lp = requireLocalProperties()
    val keystorePwd = lp.getProperty("KEYSTORE_PASS") ?: System.getenv("KEYSTORE_PASS")
    val alias = lp.getProperty("ALIAS_NAME") ?: System.getenv("ALIAS_NAME")
    val pwd = lp.getProperty("ALIAS_PASS") ?: System.getenv("ALIAS_PASS")

    android.apply {
        if (keystorePwd != null) {
            signingConfigs {
                create("release") {
                    storeFile = rootProject.file("release.keystore")
                    storePassword = keystorePwd
                    keyAlias = alias
                    keyPassword = pwd
                }
            }
        }
        buildTypes {
            val key = signingConfigs.findByName("release")
            if (key != null) {
                getByName("release").signingConfig = key
                getByName("debug").signingConfig = key
            }
        }
    }
}

fun Project.setupApp() {
    val pkgName = requireMetadata().getProperty("PACKAGE_NAME")
    val verName = requireMetadata().getProperty("VERSION_NAME")
    val verCode = (requireMetadata().getProperty("VERSION_CODE").toInt()) * 5
    android.apply {
        defaultConfig {
            applicationId = pkgName
            versionCode = verCode
            versionName = verName
            buildConfigField("String", "PRE_VERSION_NAME", "\"\"")
        }
    }
    setupAppCommon()

    android.apply {
        this as AbstractAppExtension

        buildTypes {
            getByName("release") {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro")
                )
            }
        }

        splits.abi {
            reset()
            isEnable = true
            isUniversalApk = false
            include("arm64-v8a")
        }

        flavorDimensions += "vendor"
        productFlavors {
            create("oss")
            create("fdroid")
            create("play")
            create("preview") {
                buildConfigField(
                    "String",
                    "PRE_VERSION_NAME",
                    "\"${requireMetadata().getProperty("PRE_VERSION_NAME")}\""
                )
            }
        }

        applicationVariants.all {
            outputs.all {
                this as BaseVariantOutputImpl
                val isPreview = outputFileName.contains("-preview")
                outputFileName = if (isPreview) {
                    outputFileName.replace(
                        project.name,
                        "NekoBox-" + requireMetadata().getProperty("PRE_VERSION_NAME")
                    ).replace("-preview", "")
                } else {
                    outputFileName.replace(project.name, "NekoBox-$versionName")
                        .replace("-release", "")
                        .replace("-oss", "")
                }
            }
        }

        for (abi in listOf("Arm64", "Arm", "X64", "X86")) {
            tasks.create("assemble" + abi + "FdroidRelease") {
                dependsOn("assembleFdroidRelease")
            }
        }

        sourceSets.getByName("main").apply {
            jniLibs.srcDir("executableSo")
        }
    }
}
