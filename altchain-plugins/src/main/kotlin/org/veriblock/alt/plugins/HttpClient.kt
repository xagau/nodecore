// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins

import com.google.gson.Gson
import com.google.gson.JsonElement
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import java.lang.reflect.Type

class HttpException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

private val gson = Gson()

fun <T> String.fromJson(type: Type): T = gson.fromJson(this, type)
fun <T> JsonElement.fromJson(type: Type): T = gson.fromJson(this, type)

class HttpAuthConfig(
    val username: String,
    val password: String
)

fun createHttpClient(authConfig: HttpAuthConfig? = null, contentTypes: List<ContentType>? = null) = HttpClient(CIO) {
    install(JsonFeature) {
        if (contentTypes != null) {
            acceptContentTypes = contentTypes
        }
    }
    if (authConfig != null) {
        //install(Auth) {
        //    basic {
        //        username = authConfig.username
        //        password = authConfig.password
        //    }
        //}
    }
}
