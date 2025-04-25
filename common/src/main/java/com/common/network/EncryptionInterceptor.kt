package com.common.network

import okhttp3.Interceptor
import okhttp3.Response

import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException
import java.security.MessageDigest

/**
 * 加解密拦截器 - 实现 MD5 签名和验证
 */
class EncryptionInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        return runCatching {
            val signedRequest = signRequest(originalRequest)
            val response = chain.proceed(signedRequest)
            verifyResponse(response)
        }.getOrElse {
            createErrorResponse(it.message, originalRequest)
        }
    }

    /**
     * 对请求进行签名
     */
    @Throws(IOException::class)
    private fun signRequest(request: Request): Request {
        return when (request.method) {
            "GET" -> signGetRequest(request)
            "POST", "PUT", "PATCH" -> signBodyRequest(request)
            else -> request // 其他方法不处理
        }
    }

    /**
     * 对 GET 请求进行签名
     */
    private fun signGetRequest(request: Request): Request {
        val urlBuilder = request.url.newBuilder()
        val queryParams = request.url.queryParameterNames.sorted()
        val queryString =
            queryParams.joinToString("&") { "$it=${request.url.queryParameter(it)}" }
        val sign = md5(queryString) // 计算 MD5 签名
        urlBuilder.addQueryParameter("sign", sign) // 将签名添加到查询参数
        return request.newBuilder().url(urlBuilder.build()).build()
    }

    /**
     * 对包含请求体的请求 (POST, PUT, PATCH) 进行签名
     */
    @Throws(IOException::class)
    private fun signBodyRequest(request: Request): Request {
        val body = request.body ?: return request // 如果没有 body，则不处理
        val contentType = body.contentType()
        val sign: String
        val newRequestBody: RequestBody
        if (body is FormBody) {
            // 直接处理 FormBody
            val params = mutableMapOf<String, String>()
            for (i in 0 until body.size) {
                params[body.encodedName(i)] = body.encodedValue(i)
            }
            val sortedParams = params.toSortedMap()
            val queryString = sortedParams.map { "${it.key}=${it.value}" }.joinToString("&")
            sign = md5(queryString)
            newRequestBody = body // FormBody 不需要重新创建
        } else {
            // 处理其他类型的请求体 (e.g., JSON)
            val buffer = Buffer()
            body.writeTo(buffer)
            val bodyString = buffer.readUtf8()
            sign = md5(bodyString)
            // 重新创建请求体，因为原始的已被读取
            newRequestBody = bodyString.toRequestBody(contentType)
        }

        return request.newBuilder()
            .header("X-Sign", sign) // 将签名添加到请求头
            .method(request.method, newRequestBody) // 使用新的请求体
            .build()
    }

    /**
     * 验证响应签名
     */
    @Throws(IOException::class)
    private fun verifyResponse(response: Response): Response {
        val responseBody = response.body
        if (!response.isSuccessful || responseBody == null) {
            return response // 非成功响应或无响应体，直接返回
        }

        val serverSign = response.header("X-Sign") // 假设服务器在 X-Sign 头中返回 MD5
        val bodyString = responseBody.string() // 读取响应体 (注意：这会消耗掉原始响应体)

        if (serverSign != null) {
            val calculatedSign = md5(bodyString)
            if (serverSign != calculatedSign) {
                // MD5 验证失败，可以抛出异常或返回错误
                throw IOException("Response MD5 verification failed: serverSign=$serverSign, calculatedSign=$calculatedSign")
            }
        }

        // 将读取过的响应体重新包装回去
        val newResponseBody = bodyString.toResponseBody(responseBody.contentType())
        return response.newBuilder().body(newResponseBody).build()
    }


    /**
     * 请求异常时创建错误响应
     */
    private fun createErrorResponse(errorMessage: String?, request: Request): Response {
        val message: String = errorMessage ?: "网络异常"

        // body 需要定义为统一返回json 格式
        val body = message.toResponseBody()
        return Response.Builder()
            .code(200)
            .message(message)
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(body)
            .build()
    }

    /**
     * 计算字符串的 MD5 值
     */
    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digested = md.digest(input.toByteArray())
            digested.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            // 处理异常，例如记录日志或返回默认值
            e.printStackTrace()
            ""
        }
    }
}