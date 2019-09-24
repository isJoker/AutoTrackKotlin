package cn.com.autotrackclick

import android.app.Application
import cn.com.track_library.TrackDataManager

/**
 * Created by JokerWan on 2019-09-24.
 * Function:
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        TrackDataManager.init(this)
    }
}