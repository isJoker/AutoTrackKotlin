package cn.com.track_library

/**
 * Created by JokerWan on 2019-06-02.
 * Function:
 */
sealed class TrackDataTable {
    class AppStarted(val name: String = "app_started") : TrackDataTable()
    class AppPausedTime(val name: String = "app_paused_time") : TrackDataTable()
    class AppEndState(val name: String = "app_end_state") : TrackDataTable()
}
