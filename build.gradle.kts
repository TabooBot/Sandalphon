plugins {
    `java-library`
    `maven-publish`
    id("io.izzel.taboolib") version "1.31"
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
}

taboolib {
    description {
        contributors {
            name("坏黑")
        }
    }
    install("common")
    install("common-5")
    install("module-database")
    install("module-configuration")
    install("module-chat")
    install("module-nms")
    install("module-nms-util")
    install("module-ui")
    install("module-kether", "expansion-command-helper", "expansion-player-database")
    install("platform-bukkit")
    classifier = null
    version = "6.0.3-23"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("public:MythicMobs:1.0.1")
    compileOnly("ink.ptms:Zaphkiel:1.7.2")
    compileOnly("ink.ptms:Adyeshach:1.4.1")
    compileOnly("ink.ptms.core:v11701:11701:universal")
    compileOnly("ink.ptms.core:v11605:11605")
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    repositories {
        maven {
            url = uri("https://repo2s.ptms.ink/repository/maven-releases/")
            credentials {
                username = project.findProperty("user").toString()
                password = project.findProperty("password").toString()
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            groupId = "ink.ptms"
        }
    }
}