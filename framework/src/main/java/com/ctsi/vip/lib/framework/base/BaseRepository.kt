package com.ctsi.vip.lib.framework.base

import com.ctsi.vip.lib.framework.http.ApiService
import com.ctsi.vip.lib.framework.http.HttpConstants
import com.ctsi.vip.lib.framework.http.RetrofitManager
import com.ctsi.vip.lib.framework.http.response.BeanResponse
import com.ctsi.vip.lib.framework.utils.JsonUtils
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call

/**
 * Created by GaoHW at 2022-6-27.
 *
 * Desc:
 */
open class BaseRepository {

    val apiService = createService(ApiService::class.java)

    fun <T> createService(clazz: Class<T>): T = RetrofitManager.create(clazz)

    suspend fun <T : Any> request(call: suspend () -> Call<ResponseBody>):
            BeanResponse<T> {
        return try {
            val response = requestOrigin(call)
            JsonUtils.fromJson(response, object : TypeToken<BeanResponse<T>>() {}.type)
        } catch (e: Exception) {
            BeanResponse<T>().apply {
                code = HttpConstants.Status.UnknownError
                msg = e.message
            }
        }.apply {
            when (code) {
                HttpConstants.Status.TokenInvalidError -> throw TokenInvalidException()
            }
        }
    }

    suspend fun <T : Any> requestCus(call: suspend () -> Call<ResponseBody>): T? {
        return try {
            val response = requestOrigin(call)
            JsonUtils.fromJson(response, object : TypeToken<BeanResponse<T>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun requestOrigin(call: suspend () -> Call<ResponseBody>): String {
        return withContext(Dispatchers.IO) {
            try {
                val request = call.invoke().execute()
                val response: String =
                    if (request.isSuccessful && request.body() != null) {
                        request.body()!!.string()
                    } else {
                        generateErrorJson("${request.code()}", request.errorBody()?.string())
                    }
                response
            } catch (e: Exception) {
                generateErrorJson(HttpConstants.Status.NetError, e.message)
            }
        }
    }

    private fun generateErrorJson(code: String, errMsg: String?): String {
        return "{\"code\":\"${code}\", \"msg\" : \"${errMsg}\"}"
    }

    class TokenInvalidException(msg: String = "Token Invalid") : Exception(msg)
}