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
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "Create Tag"
            scriptContent = """
                echo "Team city"
                curl --location 'https://api.github.com/repos/thusitha458/teamcity-test/git/refs/tags' \
                                --header 'X-GitHub-Api-Version: 2022-11-28' \
                                --header 'Authorization: Bearer %env.GITHUB_TOKEN%'
            """.trimIndent()
        }
        kotlinScript {
            name = "Kotlin FTW"
            content = """
                #!/usr/bin/env kotlin
                
                @file:Repository("https://repo1.maven.org/maven2/")
                @file:DependsOn("com.squareup.okhttp3:okhttp:4.9.1")
                @file:DependsOn("com.google.code.gson:gson:2.8.6")
                
                import okhttp3.*;
                import com.google.gson.Gson;
                import java.util.TimeZone;
                import java.util.Date;
                import java.util.Locale;
                import java.util.Calendar;
                
                val githubToken = args[0];
                val client = OkHttpClient()
                val gson = Gson()
                
                data class GithubTag (
                    val ref: String
                )
                
                fun getTagsFromGithub(): List<GithubTag> {
                    val request = Request.Builder()
                        .url("https://api.github.com/repos/thusitha458/teamcity-test/git/refs/tags")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .header("Authorization", "Bearer ${'$'}githubToken")
                        .build()
                    
                    return client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw java.io.IOException("Unexpected code ${'$'}response")
                        
                        return@use gson
                            .fromJson(response.body!!.string() , Array<GithubTag>::class.java)
                            .toList()
                    }
                }
                
                fun getVersionSuffix(): String {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Stockholm"))
                    cal.setTime(Date())
                    val year = cal.get(Calendar.YEAR)
                    val week = cal.get(Calendar.WEEK_OF_YEAR)
                    return "${'$'}year.${'$'}week"
                }
                
                fun run() {
                    val versionSuffix = getVersionSuffix()
                    val tagsWithSameVersionSuffix = getTagsFromGithub().map {
                        it.ref.replace("refs/tags/", "")
                    }.filter { it.startsWith(versionSuffix) }
                    println(tagsWithSameVersionSuffix)
                    
                    var buildNumber = "01"
                    if (!tagsWithSameVersionSuffix.isNullOrEmpty()) {
                        var lastBuildNumber = tagsWithSameVersionSuffix
                            .map { it.replace(versionSuffix, "").toIntOrNull() }
                            .filter { it != null }
                            .first() ?: 0
                        buildNumber = (lastBuildNumber++).toString()
                        if (buildNumber.length == 1) {
                            buildNumber = "0" + buildNumber
                        }
                    }
                    
                    val newVersion = versionSuffix + buildNumber
                    println(newVersion)
                }
                
                run()
            """.trimIndent()
            arguments = "%env.GITHUB_TOKEN%"
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
