rootProject.name = "SwimResults"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":swimResultsApp")
include(":core:common")
include(":core:di")
include(":core:ui")
include(":feature:settings:api")
include(":feature:settings:impl")
include(":feature:stopwatch:api")
include(":feature:stopwatch:impl")
include(":feature:checkresult:api")
include(":feature:checkresult:impl")
include(":feature:history:api")
include(":feature:history:impl")
