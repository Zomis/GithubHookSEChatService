package net.zomis.duga.chat

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class DugaPoster(val duga: DugaBot) {

    private val logger = LoggerFactory.getLogger(DugaPoster::class.java)
    private val mapper = jacksonObjectMapper()

    data class PostResult(val id: Long, val time: Long)
    private suspend fun internalPost(room: String, message: String): PostResult? {
        try {
            val result = duga.httpClient.post<String>(duga.chatUrl + "/chats/$room/messages/new") {
                body = FormDataContent(Parameters.build {
                    append("text", message)
                    append("fkey", duga.fkey())
                })
            }
            logger.info("Post result: $result")
            return mapper.readValue(result, PostResult::class.java)
        } catch (e: ClientRequestException) {
            if (e.message?.contains("You can perform this action again in") == true) {
                val performAgainIn = e.message!!.substringAfter("You can perform this action again in").trim()
                logger.info("perform again in $performAgainIn")
                if (performAgainIn.substringAfter(' ').contains("second")) {
                    delay(performAgainIn.substringBefore(' ').toLong() * 1000)
                    return internalPost(room, message)
                }
            }
            throw e
        }
    }

    suspend fun postMessage(room: String, message: String): PostResult? {
        return try {
            internalPost(room, message)
        } catch (e: Exception) {
            logger.warn("Unable to post '$message' to room $room. Retrying in a little while", e)
            delay(10_000L)
            duga.refreshFKey()
            try {
                internalPost(room, message)
            } catch (e: Exception) {
                logger.error("Retry failed. Unable to post '$message' to room $room", e)
                return null
            }
        }
    }

    suspend fun editMessage(messageId: Long, newText: String): Boolean {
        val result = duga.httpClient.post<String>(duga.chatUrl + "/messages/$messageId") {
            body = FormDataContent(Parameters.build {
                append("text", newText)
                append("fkey", duga.fkey())
            })
        }
        logger.info("Edit $messageId to '$newText': $result")
        return result.trim() == "ok"
    }

    suspend fun getMessage(messageId: Long): String {
        return duga.httpClient.get(duga.chatUrl + "/message/$messageId?plain=true") // &_=timestampMs
    }

}
