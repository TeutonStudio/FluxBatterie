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
val originalFlux = files("libs/FluxNetworks-1.21.1-8.0.0.jar")
dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:5.9.0")
    implementation("mekanism:Mekanism:1.21.1-10.7.19.85")
    implementation(originalFlux)
    if (project.hasProperty("compatFixture")) runtimeOnly(files(project.property("compatFixture")))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}
tasks.test { useJUnitPlatform() }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
val verifyFluxInput by tasks.registering {
    inputs.files(originalFlux)
    doLast {
        val digest = MessageDigest.getInstance("SHA-256").digest(originalFlux.singleFile.readBytes()).joinToString("") { "%02x".format(it) }
        check(digest == "29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6") { "Wrong Flux Networks input jar. Run scripts/bootstrap-dependencies.py." }
    }
}
tasks.named("compileKotlin") { dependsOn(verifyFluxInput) }
tasks.named("check") { dependsOn("verifyLoader") }

// Offline loader tooling never forms part of the delivered mod.
val loaderTools by sourceSets.creating
dependencies {
    for (module in listOf("asm-tree", "asm-commons", "asm-util")) add(loaderTools.implementationConfigurationName, "org.ow2.asm:$module:9.8")
    add(loaderTools.implementationConfigurationName, "org.openjdk.nashorn:nashorn-core:15.4")
}
tasks.register<JavaExec>("generateFluxShapes") {
    classpath = loaderTools.runtimeClasspath
    mainClass.set("de.voxelteuton.fluxbattery.tools.LoaderVerification")
    args("generate")
}
val verifyLoader by tasks.registering(JavaExec::class) {
    classpath = loaderTools.runtimeClasspath
    mainClass.set("de.voxelteuton.fluxbattery.tools.LoaderVerification")
}
