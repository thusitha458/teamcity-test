import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript
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
        param("env.NEXT_VERSION", "0.0.0")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "Check for new changes"
            scriptContent = """
                count=$(git log --oneline -1 | grep -c -E "\[skip ci\]")
                if [ "${'$'}count" -eq 1 ]; then
                    echo "##teamcity[buildStop comment='No changes found!' readdToQueue='false']"
                fi
            """.trimIndent()
        }
        script {
            name = "Read current version"
            scriptContent = """
                CURRENT_VERSION=${'$'}(cat package.json | grep version | head -1 | awk -F: '{ print ${'$'}2 }' | sed 's/[",]//g')
                echo "##teamcity[setParameter name='env.CURRENT_VERSION' value='${'$'}CURRENT_VERSION']"
            """.trimIndent()
        }
        kotlinScript {
            name = "Calculate next version"
            content = """
                #!/usr/bin/env kotlin
                
                import java.util.TimeZone;
                import java.util.Date;
                import java.util.Locale;
                import java.util.Calendar;
                                
                fun getVersionPrefix(): String {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Stockholm"))
                    cal.setTime(Date())
                    val year = cal.get(Calendar.YEAR)
                    val week = cal.get(Calendar.WEEK_OF_YEAR)
                    return "${'$'}year.${'$'}week"
                }
                
                val currentVersion = args[0]
                val nextVersionPrefix = getVersionPrefix()
                if (currentVersion.startsWith(nextVersionPrefix)) {
                    val currentBuildNo = currentVersion.replace("${'$'}nextVersionPrefix.", "").toIntOrNull() ?: 0
                    val nextBuildNo = currentBuildNo + 1
                    print("##teamcity[setParameter name='env.NEXT_VERSION' value='${'$'}nextVersionPrefix.${'$'}nextBuildNo']")
                } else {
                    print("##teamcity[setParameter name='env.NEXT_VERSION' value='${'$'}nextVersionPrefix.0']")
                }
            """.trimIndent()
            arguments = "%env.CURRENT_VERSION%"
        }
        script {
            name = "Update version"
            scriptContent = """
                echo "Current version is %env.CURRENT_VERSION%"
                echo "Next version is %env.NEXT_VERSION%"
                npm version %env.NEXT_VERSION% -m "[skip ci] Bump version to %env.NEXT_VERSION%"
            """.trimIndent()
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
