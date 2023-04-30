#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.9.1")
@file:DependsOn("com.google.code.gson:gson:2.8.6")

import okhttp3.*;
import com.google.gson.Gson;
import java.util.TimeZone;
import java.util.Date;
import java.util.Calendar;

val githubToken = args[0]
val client = OkHttpClient()
val gson = Gson()

data class GithubTag (
    val ref: String
)

fun getTagsFromGithub(): List<GithubTag> {
    val request = Request.Builder()
        .url("https://api.github.com/repos/thusitha458/teamcity-test/git/refs/tags")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("Authorization", "Bearer $githubToken")
        .build()

    return client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")

        return@use gson
            .fromJson(response.body!!.string() , Array<GithubTag>::class.java)
            .toList()
    }
}

fun getVersionPrefix(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Stockholm"))
    cal.setTime(Date())
    val year = cal.get(Calendar.YEAR)
    val week = cal.get(Calendar.WEEK_OF_YEAR)
    return "$year.$week"
}

fun calculateVersion(): String {
    val versionPrefix = getVersionPrefix()
    val tagsWithSameVersionPrefix = getTagsFromGithub().map {
        it.ref.replace("refs/tags/", "")
    }.filter { it.startsWith(versionPrefix) }

    var buildNumber = "01"
    if (!tagsWithSameVersionPrefix.isNullOrEmpty()) {
        val lastBuildNumber = tagsWithSameVersionPrefix
            .mapNotNull { it.replace(versionPrefix, "").toIntOrNull() }
            .maxOrNull() ?: 0
        buildNumber = (lastBuildNumber + 1).toString()
        if (buildNumber.length == 1) {
            buildNumber = "0$buildNumber"
        }
    }

    return versionPrefix + buildNumber
}

fun run() {
    val version = calculateVersion()
    print(version)
    print("##teamcity[setParameter name='env.NEXT_VERSION' value='$version']")
}

run()