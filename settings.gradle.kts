// Needed because of https://issuetracker.google.com/issues/315023802
gradle.startParameter.excludedTaskNames.addAll(listOf(":build-logic:testClasses"))

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "vocable-android"

include(":app")
include(":basetest")
