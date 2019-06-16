package cn.com.track_library

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri

/**
 * Created by JokerWan on 2019-06-02.
 * Function: ContentProvider + SharedPreferences 来实现前后台标记位跨进程数据共享
 */
class TrackDatabaseHelper(context: Context, packageName: String) {
    private val mContentResolver: ContentResolver = context.contentResolver
    val appStartUri: Uri
    private val mAppEndState: Uri
    private val mAppPausedTime: Uri

    /**
     * 获取app paused 的时长
     *
     * @return Activity paused 时长
     */
    val appPausedTime: Long
        get() {
            var pausedTime: Long = 0
            val query = mContentResolver.query(mAppPausedTime, arrayOf(APP_PAUSED_TIME), null, null, null)
            query?.run {
                if (query.count > 0) {
                    while (query.moveToNext()) {
                        pausedTime = getLong(0)
                    }
                }
                close()
            }
            return pausedTime
        }

    /**
     * 返回app end 的状态
     *
     * @return Activity End 状态
     */
    val appEndEventState: Boolean
        get() {
            var state = true
            val cursor = mContentResolver.query(mAppEndState, arrayOf(APP_END_STATE), null, null, null)
            cursor?.run {
                if (count > 0) {
                    while (moveToNext()) {
                        state = getInt(0) > 0
                    }
                }

                close()
            }
            return state
        }

    init {
        appStartUri =
            Uri.parse("content://" + packageName + TrackDataContentProvider + TrackDataTable.AppStarted().name)
        mAppEndState =
            Uri.parse("content://" + packageName + TrackDataContentProvider + TrackDataTable.AppEndState().name)
        mAppPausedTime =
            Uri.parse("content://" + packageName + TrackDataContentProvider + TrackDataTable.AppPausedTime().name)
    }

    /**
     * 保存app start 的状态
     *
     * @param appStart 是否是start
     */
    fun commitAppStart(appStart: Boolean) {
        val contentValues = ContentValues()
        contentValues.put(APP_STARTED, appStart)
        mContentResolver.insert(appStartUri, contentValues)
    }

    /**
     * 保存app paused 的时长
     *
     * @param pausedTime Activity paused 时长
     */
    fun commitAppPausedTime(pausedTime: Long) {
        val contentValues = ContentValues()
        contentValues.put(APP_PAUSED_TIME, pausedTime)
        mContentResolver.insert(mAppPausedTime, contentValues)
    }

    /**
     * 保存app end 的状态
     *
     * @param appEndState Activity end 状态
     */
    fun commitAppEndEventState(appEndState: Boolean) {
        val contentValues = ContentValues()
        contentValues.put(APP_END_STATE, appEndState)
        mContentResolver.insert(mAppEndState, contentValues)
    }

    companion object {

        private const val TrackDataContentProvider = ".TrackDataContentProvider/"

        const val APP_STARTED = "app_started"
        const val APP_END_STATE = "app_end_state"
        const val APP_PAUSED_TIME = "app_paused_time"
    }
}
