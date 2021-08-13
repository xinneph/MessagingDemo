package pibc.messagingdemo

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Logger @Inject constructor() {
    fun v(logTag: LogTag, msg: String) {
        Log.v(logTag.name, msg)
    }

    fun i(logTag: LogTag, msg: String) {
        Log.i(logTag.name, msg)
    }

    fun d(logTag: LogTag, msg: String) {
        Log.d(logTag.name, msg)
    }

    fun e(logTag: LogTag, msg: String) {
        Log.e(logTag.name, msg)
    }
}