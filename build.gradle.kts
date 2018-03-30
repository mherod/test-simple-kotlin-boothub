import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.gradle.DokkaTask
import org.gradle.api.Task
import nl.javadude.gradle.plugins.license.LicenseExtension
import org.gradle.jvm.tasks.Jar
import groovy.util.Node
import org.gradle.plugins.signing.Sign
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*

plugins {
    application
    idea
    eclipse
    kotlin("jvm") version "1.2.30"
    id("org.jetbrains.dokka") version "0.9.16"
    id ("maven-publish")
    id ("com.jfrog.bintray") version "1.7.2"
    id ("net.saliman.properties") version "1.4.6"
    id ("com.github.ethankhall.semantic-versioning") version "1.1.0"
    id ("com.github.hierynomus.license") version "0.12.1"
}

val testSimpleKotlinBoothubVersionMajor by project
val testSimpleKotlinBoothubVersionMinor by project
val testSimpleKotlinBoothubVersionPatch by project
val testSimpleKotlinBoothubReleaseBuild by project
val releaseBuild = testSimpleKotlinBoothubReleaseBuild.toString().toBoolean()
val testSimpleKotlinBoothubVersion = "" + testSimpleKotlinBoothubVersionMajor + "." + testSimpleKotlinBoothubVersionMinor + "." + testSimpleKotlinBoothubVersionPatch + (if(releaseBuild) "" else "-SNAPSHOT")

repositories {
    jcenter()
    mavenCentral()
}

apply {
    plugin("application")
    plugin("eclipse")
    plugin("idea")
    plugin("signing")
    plugin("org.jetbrains.dokka")
    plugin("com.github.hierynomus.license")
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
group = "co.herod"
version = testSimpleKotlinBoothubVersion

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

license {
    header = file("license-header.txt")
    skipExistingHeaders = true
    ignoreFailures = false
}

tasks.withType<Sign> {
    sign(configurations.archives)
    onlyIf { gradle.taskGraph.allTasks.any{task: Task -> isPublishTask(task)} }
}

dependencies {
    compile(kotlin("reflect"))
    compile(kotlin("stdlib"))
    compile("org.slf4j:slf4j-api:1.7.21")
    runtime ("ch.qos.logback:logback-classic:1.1.7")
    testCompile("io.kotlintest:kotlintest:2.0.7")
    testCompile("ch.qos.logback:logback-classic:1.1.7")
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf(
            "Implementation-Title" to "testSimpleKotlinBoothub",
            "Main-Class" to "co.herod.testsimplekotlinboothub.TestSimpleKotlinBoothubMain",
            "Implementation-Version" to testSimpleKotlinBoothubVersion
        ))
    }
}

application {
    mainClassName = "co.herod.testsimplekotlinboothub.TestSimpleKotlinBoothubMain"
}

val sourcesJar = task<Jar>("sourcesJar") {
    dependsOn("classes")
    from(sourceSets("main").allSource)
    classifier = "sources"
}

val dokkaJar = task<Jar>("dokkaJar") {
    dependsOn("dokka")
    classifier = "javadoc"
    from((tasks.getByName("dokka") as DokkaTask).outputDirectory)
}

artifacts {
    add("archives", sourcesJar)
    add("archives", dokkaJar)
}
publishing {
    (publications) {
        "testSimpleKotlinBoothub".invoke(MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar) { classifier = "sources" }
            artifact(dokkaJar) { classifier = "javadoc" }
            groupId = "co.herod"
            artifactId = project.name
            version = testSimpleKotlinBoothubVersion

            pom.withXml {
                val root = asNode()
                root.appendNode("name", "Module ${project.name}")
                root.appendNode("description", "The ${project.name} artifact")
                root.appendNode("url", "https://github.com/mherod/test-simple-kotlin-boothub")

                val scm = root.appendNode("scm")
                scm.appendNode("url", "https://github.com/mherod/test-simple-kotlin-boothub")
                scm.appendNode("connection", "https://github.com/mherod/test-simple-kotlin-boothub.git")
                scm.appendNode("developerConnection", "https://github.com/mherod/test-simple-kotlin-boothub.git")

                val developers = root.appendNode("developers")
                var developer : Node
                developer = developers.appendNode("developer")
                developer.appendNode("id", "mherod")
                developer.appendNode("name", "Matthew Herod")
                developer.appendNode("email", "matthew.herod@gmail.com")

                val licenseNode = root.appendNode("licenses").appendNode("license")
                licenseNode.appendNode("name", "The Do WTF You Want To Public License, Version 2")
                licenseNode.appendNode("url", "http://www.wtfpl.net/")
                licenseNode.appendNode("distribution", "repo")
            }
        }
    }
}


fun readPasswordFromConsole(title: String, prompt: String) : String{
    val panel = JPanel()
    val label = JLabel(prompt)
    val pass = JPasswordField(24)
    panel.add(label)
    panel.add(pass)
    val options = arrayOf("OK", "Cancel")
    val option = JOptionPane.showOptionDialog(null, panel, title,
            JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null)
    if(option != 0) throw InvalidUserDataException("Operation cancelled by the user.")
    return String(pass.password)
}

fun isPublishTask(task: Task): Boolean {
    return task.name.startsWith("publish")
}

gradle.taskGraph.whenReady {
    if (gradle.taskGraph.allTasks.any {task : Task -> isPublishTask(task)}) {
        val signingKeyId = propertyOrElse("signingKeyId", "")
        val signingSecretKeyRingFile = propertyOrElse("signingSecretKeyRingFile", "")
        if(signingKeyId.isEmpty() || signingSecretKeyRingFile.isEmpty())
            throw InvalidUserDataException("Please configure your signing credentials in gradle-local.properties.")
        val password = readPasswordFromConsole("Please enter your PGP credentials", "PGP Private Key Password")
        allprojects { ext["signing.keyId"] = signingKeyId }
        allprojects { ext["signing.secretKeyRingFile"] = signingSecretKeyRingFile }
        allprojects { ext["signing.password"] = password }
    }
}

bintray {
    user = propertyOrElse("bintrayUser", "unknownUser")
    key = propertyOrElse("bintrayKey", "unknownKey")
    setPublications("testSimpleKotlinBoothub")
    with(pkg) {
        repo = "maven"
        name = "testSimpleKotlinBoothub"
        userOrg = "mherod"
        setLicenses("DWTFPL")
        vcsUrl = "https://github.com/mherod/test-simple-kotlin-boothub.git"
        with(version) {
            name = testSimpleKotlinBoothubVersion
            desc = "testSimpleKotlinBoothub $testSimpleKotlinBoothubVersion"
            released = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"))
            vcsTag = testSimpleKotlinBoothubVersion
            with(gpg) {
                sign = true
            }
        }
    }
}

fun propertyOrElse(propName: String, defVal: String) : String = if(project.hasProperty(propName)) (project.property(propName) as String) else defVal
fun sourceSets(name: String) = the<JavaPluginConvention>().sourceSets.getByName(name)
