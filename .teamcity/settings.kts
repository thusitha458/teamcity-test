import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2022.10"

project {

    buildType(GitTags)
}

object GitTags : BuildType({
    name = "Git Tags"

    params {
        password("env.GITHUB_TOKEN", "credentialsJSON:643c4fa5-223a-4e42-a7dd-2dfbbd35c3a7")
        param("env.CURRENT_VERSION", "0.0.0")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "Read current version"
            scriptContent = """
                CURRENT_VERSION=$(yarn app:version)
                echo "${'$'}CURRENT_VERSION"
                echo "##teamcity[setParameter name='env.CURRENT_VERSION' value='${'$'}CURRENT_VERSION']"
            """.trimIndent()
        }
        script {
            name = "Read version"
            scriptContent = """echo "Next version is %env.CURRENT_VERSION%""""
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
    }
})
