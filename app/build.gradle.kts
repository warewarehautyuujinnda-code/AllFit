plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// 「どのビルドが端末に入っているか」を本人が見分けられるようにするための表示名。
// CI では Actions の実行番号とコミットが入り、手元のビルドでは (local) になる。
val buildLabel: String = run {
    val runNumber = System.getenv("GITHUB_RUN_NUMBER")
    val shortSha = System.getenv("GITHUB_SHA")?.take(7)
    when {
        runNumber != null && shortSha != null -> "1.0 (build $runNumber / $shortSha)"
        runNumber != null -> "1.0 (build $runNumber)"
        else -> "1.0 (local)"
    }
}

android {
    namespace = "com.hinata.fitlog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hinata.fitlog"
        minSdk = 26
        targetSdk = 35
        // versionCode はあえて 1 のまま固定する。このアプリは Play ストアではなく
        // GitHub のリリースや Actions のアーティファクトから直接APKを入れるので、
        // 「ブランチのビルドを試して、やっぱり main の版に戻す」という行き来が普通に起きる。
        // versionCode を上げてしまうと、番号の小さいAPKがダウングレード扱いで弾かれて
        // 入れ直せなくなる。署名が同じなら同じ versionCode の上書きは常に通るため、
        // どのビルドかは versionName 側（buildLabel）で見分ける。
        versionCode = 1
        versionName = buildLabel

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 端末へはPlayストアを通さずサイドロードするので、署名鍵はリポジトリにコミットした
    // 共有鍵ひとつに統一し、debug と release の両方をこれで署名する。CIが作ったAPKと
    // 手元のビルドを同じアプリとして上書きインストールできるようにするため（鍵が違うと拒否される）。
    // この鍵は公開リポジトリに置いてある以上ひみつではない。Playストアに出す場合は
    // ここを GitHub Secrets から読む専用の鍵に差し替えること。
    signingConfigs {
        getByName("debug") {
            storeFile = file("../keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // 端末に配るのはこの release ビルド。debug ビルドはマニフェストに
            // android:debuggable="true" が付くため、Play プロテクトにサイドロードを
            // ブロックされやすく、デバッグ用ツールを同梱するぶんサイズも大きい。
            // R8 は Room / kotlinx.serialization を壊しうるので今は有効にしない。
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        // アプリ内にバージョンを出すために BuildConfig.VERSION_NAME を使う
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room（ローカルDB）
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // JSON（書き出し・読み込み用）
    implementation(libs.kotlinx.serialization.json)

    // 単体テスト。domain パッケージは Android に依存しないため JVM 上でそのまま動かせる
    testImplementation(libs.junit)
}
