package cn.com.track_library

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.util.Log
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by JokerWan on 2019-06-01.
 * Function: track 数据操作类
 */
class TrackDataManager private constructor(application: Application) {
    private val mDeviceId: String = TrackDataPrivate.getAndroidID(application)
    private var mDeviceInfo: Map<String, Any> = TrackDataPrivate.getDeviceInfo(application)

    init {
        TrackDataPrivate.registerActivityLifecycleCallbacks(application)
        TrackDataPrivate.registerActivityStateObserver(application)
    }

    /**
     * track 事件
     *
     * @param eventName  事件名称
     * @param properties 事件属性
     */
    fun track(eventName: String, properties: JSONObject) {

        try {
            val jsonObject = JSONObject()
            jsonObject.run {
                put("event", eventName)
                put("time", TrackDataPrivate.formatMsToData(System.currentTimeMillis()))
                put("device_id", mDeviceId)
                val sendProperties = JSONObject(mDeviceInfo)
                TrackDataPrivate.mergeJSONObject(properties, sendProperties)
                put("properties", sendProperties)

                // 打印事件信息
                Log.i(TAG, TrackDataPrivate.formatJson(toString()))
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }


    }

    /**
     * Track Dialog 的点击
     * @param activity Activity
     * @param dialog Dialog
     */
    fun trackDialog(activity: Activity, dialog: Dialog) {
        dialog.window?.run {
            decorView?.viewTreeObserver?.addOnGlobalLayoutListener {
                TrackDataPrivate.delegateViewsOnClickListener(
                    activity,
                    decorView
                )
            }
        }
    }

    /**
     * 忽略要埋点的Activity
     */
    fun ignoreAutoTrackActivity(activityClass: Class<*>) {
        TrackDataPrivate.ignoreAutoTrackActivity(activityClass)
    }

    /**
     * 恢复埋点的Activity
     */
    fun removeIgnoredActivity(activityClass: Class<*>) {
        TrackDataPrivate.removeIgnoreActivity(activityClass)
    }

    companion object {
        internal const val SDK_VERSION = "1.0.0"

        lateinit var instance: TrackDataManager

        private val TAG = TrackDataManager::class.java.simpleName

        fun init(application: Application) {
            instance = TrackDataManager(application)
        }
    }
}
