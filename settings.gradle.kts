pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SRLakesTone"

include(":app")
include(":core:model")
include(":core:analysis")
include(":core:protocol")
include(":device:api")
include(":device:mock")
include(":audio")
include(":data")
include(":sync")
include(":update")
