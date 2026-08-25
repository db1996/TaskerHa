package com.github.db1996.taskerha.util

object UrlSecurity {

    /**
     * True when the URL would carry the access token unencrypted.
     */
    fun isCleartext(url: String): Boolean =
        url.trim().startsWith("http://", ignoreCase = true)
}
