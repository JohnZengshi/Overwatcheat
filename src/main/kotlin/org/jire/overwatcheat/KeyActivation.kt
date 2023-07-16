/*
 * Free, open-source undetected color cheat for Overwatch!
 * Copyright (C) 2017  Thomas G. Nappo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jire.overwatcheat

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class KeyActivation {
    private val keyFile = File("key.txt")
    private lateinit var key: String
    private var expiration: Long = 0

    fun activateKey() {
        if (!keyFile.exists()) {
            createKeyFile()
            inputKey()
        } else {
            readKeyFromFile()
        }
    }

    private fun createKeyFile() {
        keyFile.createNewFile()
    }

    private fun readKeyFromFile() {
        key = keyFile.readText().trim()
        if (key.isEmpty()) { inputKey() }else{ activate() }
    }

    private fun inputKey() {
        println("请输入密钥：")
        key = readlnOrNull().orEmpty().trim()
        if(activate()) keyFile.writeText(key)
    }

    private fun activate(): Boolean {
        val url = URL("https://service-9slkctpg-1258523888.gz.apigw.tencentcs.com/release/activate_key")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json; utf-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true

        val jsonInputString = "{\"key\": \"$key\"}"
        connection.outputStream.use { outputStream ->
            outputStream.write(jsonInputString.toByteArray())
        }

        val responseCode = connection.responseCode
        val responseMessage = connection.responseMessage

        if (responseCode == 200) {
            val responseJson = connection.inputStream.use { it.reader().use { reader -> reader.readText() } }
            val jsonResponse = Json.decodeFromString<JsonObject>(responseJson)
//            println("jsonResponse:${jsonResponse}")
            val statusCode = jsonResponse["status_code"]?.jsonPrimitive?.intOrNull
            val message = jsonResponse["message"]?.jsonPrimitive?.content

            if (statusCode == 200) {
                val expirationDate = jsonResponse["expiration"]?.jsonPrimitive?.content
                expiration = Date(expirationDate).time
                println("密钥有效：$message")
                thread(start = true) {
                    while (true) {
                        if (isKeyExpired()) {
                            println("密钥已过期")
                            exitProcess(0)
                        }
                        Thread.sleep(60000*5) // 轮询间隔，这里设置为60秒
                    }
                }
                return true
            } else if (statusCode == 400) {
                println("密钥无效：$message")
                inputKey()
                return false
            }
        } else {
            println("请求失败：$responseMessage")
            inputKey()
            return false
        }

        return false
    }


    private fun isKeyExpired(): Boolean {
        val currentTime = getNetworkTime()
//        println("currentTime:${currentTime}")
        return currentTime > expiration
    }

    private fun getNetworkTime(): Long {
        val url = URL("https://www.baidu.com")
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()
        val dateHeader = connection.getHeaderField("Date")
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        val date = dateFormat.parse(dateHeader)
        return date.time
    }
}

//fun main() {
//    val keyActivation = KeyActivation()
//
//    keyActivation.activateKey()
//
//    println("主线程继续执行其他任务")
//    // 主线程继续执行其他任务
//    // ...
//}
