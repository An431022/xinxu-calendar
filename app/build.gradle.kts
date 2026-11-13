import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 正式签名的口令放在 keystore.properties（已被 .gitignore 排除，**不要提交**）。
// 注意：真机上的 app 是用这个签名装上去的，以后所有升级包都必须用同一个密钥签，
// 否则 Android 会拒绝覆盖安装、只能先卸载 —— 那会把记录一起删掉。
// 所以 keystore/ 目录和 keystore.properties 要单独备份一份。
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// 导出 Room 的 schema 到版本库里（`app/schemas`）。
// Room 靠它做迁移：schema 在，将来改表结构时就能写出「从 v1 怎么走到 v2」；
// 没有它，升级数据库版本时只剩 fallbackToDestructiveMigration() 一条路 —— 那等于把用户的记录全删掉。
// 一本「足迹」应用把记录删了，就是最坏的事故，所以这个目录要跟着代码一起提交。
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.xinxu.calendar"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xinxu.calendar"
        minSdk = 26
        targetSdk = 37
        versionCode = 7
        versionName = "0.4.0"
    }

    signingConfigs {
        // 只在 keystore.properties 存在时创建。这样别人 clone 了仓库也能直接构建 debug 版，
        // 不会因为缺文件而配置失败。
        if (!keystoreProps.isEmpty) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 正式发布版：R8 混淆 + 资源压缩，APK 9.3MB → 1.3MB。
            // Room 和 Compose 都自带 consumer rules，不需要手写 keep。
            //
            // ⚠️ 它用 keystore.properties 里的**正式签名**，而手机上从 2026-10-04 起装的一直是
            // debug 签名的包，两把密钥对不上 —— 所以这个变体**至今没有覆盖安装过**。
            // 要装它必须先卸载（记录会丢，得先导出一份再导入）。
            // 日常自用装的是下面的 `daily`；这个变体留给将来真正发版。
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        // **装到手机上日常自用的版本（2026-10-19 起）。**
        //
        // 它和 debug 几乎完全一样：用同一把 debug 签名、不混淆、不压缩资源，
        // 所以能直接覆盖安装到已经装了 debug 包的手机上，**记录一条都不会丢**。
        // 唯一的差别是 `isDebuggable = false`。
        //
        // 它**不是** release 包：签名是 debug key，没有 R8，9.3MB（release 1.3MB）。
        // 选它不是为了省事，是因为换正式签名必须卸载重装 —— 手机上有 10-04 以来的真实记录，
        // 不值得为 8MB 和一点启动时间冒这个险。要发版时才走 release。
        //
        // 这一条差别的分量比看起来大得多：android:debuggable=true 会让 ART 只做 verify、
        // 禁止 AOT 编译，并且为了避免破坏可调试性而关掉一批 JIT 优化。实测（MEIZU 21 Pro）：
        //
        //   debuggable=true  冷启动后翻页：每帧约 19~25ms，六次甩动只渲染出 20~40 帧，大量丢帧
        //   debuggable=false 同一套动作：每帧约 4~5ms，渲染出 111~131 帧，janky 0%
        //   再做一次 AOT（cmd package compile -m speed）：每帧 1.2ms，141 帧
        //
        // 也就是说"刚打开 App 时滑动主页面卡顿、放一会儿自己就好"根本不是动画或布局的问题，
        // 是 debug 包本身在解释执行。以后**不要拿 assembleDebug 的包去判断手感**。
        create("daily") {
            initWith(getByName("debug"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 只服务调试用的调色盘（算 HCT 的彩度/明度），渲染路径不碰它。
    implementation(libs.kyant.hct)

    // 农历 / 节气 / 传统节日。纯算法、不联网；世界与公益节日在 data/almanac 里自己补。
    implementation(libs.lunar)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
