//pluginManagement {
//    repositories {
//        google()
//        mavenCentral()
//        gradlePluginPortal()
//    }
//}
//
//dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Optional but recommended
//    repositories {
//        google()
//        mavenCentral()
//
//    }
//    versionCatalogs {
//        create("libs") {
//            // The 'from' call that caused the error has been removed.
//            // Gradle, by convention, automatically looks for a libs.versions.toml file
//            // in the root 'gradle' directory when a catalog is defined.
//        }
//    }
//}
//
//rootProject.name = "DoorLock"
//include(":app")
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
    versionCatalogs {
        create("libs") {

        }
    }
}

rootProject.name = "DoorLock"
include(":app")
