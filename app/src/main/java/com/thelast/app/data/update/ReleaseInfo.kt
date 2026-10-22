package com.thelast.app.data.update

import org.json.JSONObject

data class ReleaseInfo(val version: String, val notes: String, val url: String, val sha256: String, val size: Long) {
    companion object {
        fun parse(json: String, repository: String): ReleaseInfo {
            val root = JSONObject(json)
            require(!root.optBoolean("draft") && !root.optBoolean("prerelease")) { "暂不提供测试版本" }
            val version = root.getString("tag_name").removePrefix("v")
            require(version.matches(Regex("\\d+\\.\\d+\\.\\d+"))) { "发布版本号需为 v主版本.次版本.修订号" }
            val assets = root.getJSONArray("assets")
            val apks = (0 until assets.length()).map { assets.getJSONObject(it) }
                .filter { it.getString("name").endsWith(".apk") }
            require(apks.size == 1) { "正式版本需包含且只包含一个通用 APK" }
            val asset = apks.single()
            val url = asset.getString("browser_download_url")
            require(url.startsWith("https://github.com/$repository/releases/download/")) { "安装包来源不匹配" }
            val hash = asset.optString("digest").removePrefix("sha256:")
            require(hash.matches(Regex("[a-fA-F0-9]{64}"))) { "发布缺少 SHA-256 校验信息" }
            val size = asset.getLong("size")
            require(size in 1..200L * 1024 * 1024) { "安装包大小不符合要求" }
            return ReleaseInfo(version, root.optString("body").take(12000), url, hash, size)
        }

        fun isNewer(candidate: String, current: String): Boolean {
            fun parts(value: String) = value.removePrefix("v").split('.').map { it.toLongOrNull() ?: return emptyList<Long>() }
            val a = parts(candidate)
            val b = parts(current)
            if (a.size != 3 || b.size != 3) return false
            for (i in 0..2) if (a[i] != b[i]) return a[i] > b[i]
            return false
        }

        fun repository(value: String): String {
            val repo = value.trim().removePrefix("https://github.com/").removeSuffix("/").removeSuffix(".git")
            require(repo.matches(Regex("[A-Za-z0-9-]+/[A-Za-z0-9_.-]+")) && !repo.endsWith("/..") && !repo.endsWith("/.")) {
                "请输入 GitHub 仓库地址或 owner/repository"
            }
            return repo
        }
    }
}
