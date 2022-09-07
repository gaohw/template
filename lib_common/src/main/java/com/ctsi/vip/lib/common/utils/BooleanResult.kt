package com.ctsi.vip.lib.common.utils

/**
 * Created by GaoHW at 2022-6-27.
 *
 *
 * Desc:
 */
class BooleanResult<T> {
    val isSuccess: Boolean
    var errorMsg: String? = null
    var data: T? = null

    private constructor(data: T?) {
        this.isSuccess = true
        this.data = data
    }

    private constructor(errorMsg: String) {
        this.isSuccess = false
        this.errorMsg = errorMsg
    }

    companion object {
        fun <T> success(data: T?): BooleanResult<T> {
            return BooleanResult(data)
        }

        fun failed(msg: String): BooleanResult<*> {
            return BooleanResult<Any?>(msg)
        }
    }
}