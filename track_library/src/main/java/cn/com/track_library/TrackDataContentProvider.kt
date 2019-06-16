package cn.com.track_library

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Created by JokerWan on 2019-06-03.
 * Function:
 */
class TrackDataContentProvider : ContentProvider() {
    private val mContentResolver by lazy { context?.contentResolver }
    private val mSharedPreferences by lazy {
        context?.getSharedPreferences(
            "cn.com.track_library.TrackDataSP",
            Context.MODE_PRIVATE
        )
    }
    private var mEditor: SharedPreferences.Editor? = null

    override fun onCreate(): Boolean {
        context?.run {
            uriMatcher.addURI("$packageName.TrackDataContentProvider", TrackDataTable.AppStarted().name, APP_START)
            uriMatcher.addURI(
                "$packageName.TrackDataContentProvider",
                TrackDataTable.AppEndState().name,
                APP_END_STATE
            )
            uriMatcher.addURI(
                "$packageName.TrackDataContentProvider",
                TrackDataTable.AppPausedTime().name,
                APP_PAUSED_TIME
            )
            mSharedPreferences?.run {
                mEditor = edit()
                mEditor?.apply()
            }
        }
        return false
    }

    override fun query(uri: Uri, strings: Array<String>?, s: String?, strings1: Array<String>?, s1: String?): Cursor? {
        val match = uriMatcher.match(uri)
        var matrixCursor: MatrixCursor? = null
        when (match) {
            APP_START -> {
                val appStart = if (mSharedPreferences!!.getBoolean(TrackDatabaseHelper.APP_STARTED, true)) 1 else 0
                matrixCursor = MatrixCursor(arrayOf(TrackDatabaseHelper.APP_STARTED))
                matrixCursor.addRow(arrayOf<Any>(appStart))
            }
            APP_END_STATE -> {
                val appEnd = if (mSharedPreferences!!.getBoolean(TrackDatabaseHelper.APP_END_STATE, true)) 1 else 0
                matrixCursor = MatrixCursor(arrayOf(TrackDatabaseHelper.APP_END_STATE))
                matrixCursor.addRow(arrayOf<Any>(appEnd))
            }
            APP_PAUSED_TIME -> {
                val pausedTime = mSharedPreferences!!.getLong(TrackDatabaseHelper.APP_PAUSED_TIME, 0)
                matrixCursor = MatrixCursor(arrayOf(TrackDatabaseHelper.APP_PAUSED_TIME))
                matrixCursor.addRow(arrayOf<Any>(pausedTime))
            }
        }
        return matrixCursor
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        if (contentValues == null) {
            return uri
        }
        when (uriMatcher.match(uri)) {
            APP_START -> {
                val appStart = contentValues.getAsBoolean(TrackDatabaseHelper.APP_STARTED)!!
                mEditor?.putBoolean(TrackDatabaseHelper.APP_STARTED, appStart)
                mContentResolver?.notifyChange(uri, null)
            }
            APP_END_STATE -> {
                val appEnd = contentValues.getAsBoolean(TrackDatabaseHelper.APP_END_STATE)!!
                mEditor?.putBoolean(TrackDatabaseHelper.APP_END_STATE, appEnd)
            }
            APP_PAUSED_TIME -> {
                val pausedTime = contentValues.getAsLong(TrackDatabaseHelper.APP_PAUSED_TIME)!!
                mEditor?.putLong(TrackDatabaseHelper.APP_PAUSED_TIME, pausedTime)
            }
        }
        mEditor?.commit()
        return uri
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        return 0
    }

    override fun update(uri: Uri, contentValues: ContentValues?, s: String?, strings: Array<String>?): Int {
        return 0
    }

    companion object {

        private const val APP_START = 1
        private const val APP_END_STATE = 2
        private const val APP_PAUSED_TIME = 3

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
    }
}
