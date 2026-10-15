import java.security.MessageDigest

plugins {
    `java-library`
    kotlin("jvm") version "2.1.21"
    id("net.neoforged.moddev") version "2.0.141"
}
version = "0.1.0-dev"
group = "de.voxelteuton.fluxbattery"
base.archivesName.set("fluxbattery")
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
kotlin.jvmToolchain(21)
repositories {
    mavenCentral()
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://modmaven.dev/")
    maven("https://cursemaven.com") { content { includeGroup("curse.maven") } }
}
neoForge {
    version = "21.1.231"
    runs {
        create("client") { client(); gameDirectory = file("run-client"); if (project.hasProperty("clientSmoke")) systemProperty("fluxbattery.clientSmoke", "true") }
        create("server") { server(); programArgument("--nogui") }
        create("gameTestServer") { type = "gameTestServer"; gameDirectory = file(if (project.hasProperty("restartStage")) "run-restart" else "run-gametest"); if (project.hasProperty("restartStage")) systemProperty("fluxbattery.restartStage", project.property("restartStage").toString()); systemProperty("neoforge.enabledGameTestNamespaces", "fluxbattery") }
    }
    mods { create("fluxbattery") { sourceSet(sourceSets.main.get()) } }
}
val bridge by sourceSets.creating { java.srcDir("bridge/src/main/java") }
configurations[bridge.implementationConfigurationName].extendsFrom(configurations.implementation.get())
bridge.compileClasspath += sourceSets.main.get().compileClasspath
val originalFlux = files("libs/FluxNetworks-1.21.1-8.0.0.jar")
bridge.compileClasspath += originalFlux
val patchedFlux by tasks.registering(Jar::class) {
    dependsOn(tasks.named(bridge.compileJavaTaskName))
    archiveBaseName.set("FluxNetworks")
    archiveVersion.set("1.21.1-8.0.0-fluxbattery.1")
    destinationDirectory.set(layout.buildDirectory.dir("bridge"))
    from(bridge.output)
    from(zipTree(originalFlux.singleFile)) {
        exclude("sonar/fluxnetworks/common/connection/FluxNetwork.class", "sonar/fluxnetworks/common/connection/FluxNetwork$*.class")
        exclude("sonar/fluxnetworks/common/connection/ServerFluxNetwork*.class")
        exclude("sonar/fluxnetworks/common/connection/ClientFluxNetwork*.class")
        exclude("sonar/fluxnetworks/common/connection/TransferIterator*.class")
        exclude("sonar/fluxnetworks/common/connection/NetworkStatistics*.class")
        exclude("sonar/fluxnetworks/common/connection/PhantomFluxDevice*.class")
        exclude("sonar/fluxnetworks/common/device/TileFluxDevice*.class")
        exclude("sonar/fluxnetworks/register/Messages*.class")
        exclude("META-INF/MANIFEST.MF")
    }
    from("bridge/LICENSE") { rename { "FLUX-NETWORKS-LICENSE" } }
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:5.9.0")
    implementation("mekanism:Mekanism:1.21.1-10.7.19.85")
    // Compile original ABI plus replacement classes; only the patched jar is loaded at runtime.
    compileOnly(originalFlux)
    runtimeOnly(files(patchedFlux))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}
sourceSets.main.get().compileClasspath = files(bridge.output) + sourceSets.main.get().compileClasspath
tasks.named("compileKotlin") { dependsOn(tasks.named(bridge.compileJavaTaskName)) }
tasks.test { useJUnitPlatform() }
tasks.build { dependsOn(patchedFlux) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
val verifyFluxInput by tasks.registering {
    inputs.files(originalFlux)
    doLast {
        val digest = MessageDigest.getInstance("SHA-256").digest(originalFlux.singleFile.readBytes()).joinToString("") { "%02x".format(it) }
        check(digest == "29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6") { "Wrong Flux Networks input jar. Run scripts/bootstrap-dependencies.py." }
    }
}
tasks.named(bridge.compileJavaTaskName) { dependsOn(verifyFluxInput) }
