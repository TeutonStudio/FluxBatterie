import java.security.MessageDigest

plugins {
    `java-library`
    kotlin("jvm") version "2.1.21"
    id("net.neoforged.moddev") version "2.0.141"
}
version = "0.1.2"
group = "de.teutonstudio.fluxbattery"
base.archivesName.set("fluxbattery")
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
kotlin.jvmToolchain(21)
repositories {
    mavenCentral()
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://modmaven.dev/")
    maven("https://maven.blamejared.com/") { content { includeGroup("mezz.jei"); includeGroup("net.mezzdev.config") } }
    maven("https://cursemaven.com") { content { includeGroup("curse.maven") } }
}
// Packaged smoke runs exclude loose main output, preventing a resource-only duplicate mod.
val packagedRun = if (project.hasProperty("packagedAddon")) sourceSets.create("packagedRun").apply {
    runtimeClasspath += sourceSets.main.get().runtimeClasspath - sourceSets.main.get().output
} else null
val clientRuntime by sourceSets.creating {
    runtimeClasspath += (packagedRun ?: sourceSets.main.get()).runtimeClasspath
}
neoForge {
    version = "21.1.231"
    runs {
        configureEach {
            // IDEA's delegated Application runner can lose the JVM module path.
            // Use the checked-in Gradle runs so ModDev owns the complete launch.
            disableIdeRun()
            if (packagedRun != null) sourceSet.set(packagedRun)
        }
        create("client") {
            sourceSet.set(clientRuntime)
            client()
            gameDirectory = file("run-client")
            if (project.hasProperty("clientSmoke")) systemProperty("fluxbattery.clientSmoke", "true")
        }
        create("server") {
            server(); programArgument("--nogui")
            if (project.hasProperty("dedicatedRegression")) {
                systemProperty("fluxbattery.dedicatedRegression", "true")
                systemProperty("neoforge.enableGameTest", "true")
                systemProperty("neoforge.enabledGameTestNamespaces", "fluxbattery")
            }
        }
        create("gameTestServer") { type = "gameTestServer"; gameDirectory = file(if (project.hasProperty("restartStage")) "run-restart" else "run-gametest"); if (project.hasProperty("restartStage")) systemProperty("fluxbattery.restartStage", project.property("restartStage").toString()); systemProperty("neoforge.enabledGameTestNamespaces", "fluxbattery"); if (project.hasProperty("compatFixture")) systemProperty("fluxbattery.compatFixture", "true") }
    }
    mods { if (!project.hasProperty("packagedAddon")) create("fluxbattery") { sourceSet(sourceSets.main.get()) } }
}
val originalFlux = files("libs/FluxNetworks-1.21.1-8.0.0.jar")
dependencies {
    add(clientRuntime.runtimeOnlyConfigurationName, "mezz.jei:jei-1.21.1-neoforge:19.39.0.372")
    implementation("thedarkcolour:kotlinforforge-neoforge:5.9.0")
    implementation("mekanism:Mekanism:1.21.1-10.7.19.85")
    if (project.hasProperty("packagedAddon")) runtimeOnly(files(tasks.named("jar")))
    compileOnly(originalFlux)
    runtimeOnly(files(providers.gradleProperty("fluxRuntimeJar").getOrElse(originalFlux.singleFile.path)))
    if (project.hasProperty("compatFixture")) runtimeOnly(files(project.property("compatFixture")))
    if (project.hasProperty("compatFixtureExtra")) runtimeOnly(files(project.property("compatFixtureExtra")))

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
    dependsOn(verifyFluxInput)
    classpath = loaderTools.runtimeClasspath
    mainClass.set("de.teutonstudio.fluxbattery.tools.LoaderVerification")
    args("generate")
}
val verifyLoader by tasks.registering(JavaExec::class) {
    dependsOn(verifyFluxInput)
    classpath = loaderTools.runtimeClasspath
    mainClass.set("de.teutonstudio.fluxbattery.tools.LoaderVerification")
}

// Separate external test mod: exercises an AFTER-phase Mixin against FluxNetwork.
val compatFixture by sourceSets.creating
compatFixture.compileClasspath += sourceSets.main.get().compileClasspath
tasks.register<Jar>("compatFixtureJar") {
    archiveFileName.set("fluxbattery-mixin-fixture.jar")
    destinationDirectory.set(layout.buildDirectory.dir("fixtures"))
    from(compatFixture.output)
}

// Dedicated-server smoke tests and local development can issue a clean console stop.
tasks.named<JavaExec>("runServer") { standardInput = System.`in` }
// If maintenance generation and build are requested together, package the newly generated schema.
tasks.named("processResources") { mustRunAfter("generateFluxShapes") }
