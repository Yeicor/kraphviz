plugins {
  kotlin("multiplatform") version "1.8.21"
  `maven-publish`
  @Suppress("SpellCheckingInspection")
  id("com.ncorti.ktfmt.gradle") version "0.12.0"
}

group = "com.github.yeicor"

version = "1.0.0"

repositories {
  mavenCentral()
  maven("https://repsy.io/mvn/yeicor/github-public")
}

kotlin {
  explicitApi()

  jvm {}

  js(IR) {
    binaries.executable()
    browser {
      testTask {
        useMocha {
          timeout = "60000" // ms
        }
      }
    }
    nodejs {
      testTask {
        useMocha {
          timeout = "60000" // ms
        }
      }
    }
  }

  @Suppress("OPT_IN_USAGE") wasm {
    d8 {}
  }

  val hostOs = System.getProperty("os.name")
  val isMingwX64 = hostOs.startsWith("Windows")
  when {
    hostOs == "Mac OS X" -> macosX64("native")
    hostOs == "Linux" -> linuxX64("native")
    isMingwX64 -> mingwX64("native")
    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
  } // Native config: .apply {}

  @Suppress("UNUSED_VARIABLE")
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.github.yeicor:ktmpwasm:1.0.0") { isChanging = true }
      }
    }
    val commonTest by getting { dependencies { implementation(kotlin("test")) } }
    val jvmMain by getting
    val jvmTest by getting
    val jsMain by getting
    val jsTest by getting
    val nativeMain by getting
    val nativeTest by getting
  }
}

publishing {
  repositories {
    val envBasedMavenRepo = {repoUrl: String, userEnv: String, passEnv: String, repoName: String ->
      (System.getenv(userEnv) to System.getenv(passEnv))
        .takeIf {
          if (it.first == null)
            logger.warn("$userEnv is not set, disabling $repoName publishing")
          else if (it.second == null)
            logger.warn("$passEnv is not set, disabling $repoName publishing")
          it.first != null && it.second != null
        }
        ?.let { (mvnUsername, mvnPassword) ->
          maven {
            name = repoName
            url = uri(repoUrl)
            credentials {
              username = mvnUsername
              password = mvnPassword
            }
          }
        }
    }
    envBasedMavenRepo("https://maven.pkg.github.com/Yeicor/${project.name}", "GITHUB_ACTOR", "GITHUB_TOKEN", "GitHubPackages")
    envBasedMavenRepo("https://repsy.io/mvn/yeicor/github-public/", "REPSY_USER", "REPSY_PASS", "Repsy")
  }
}
