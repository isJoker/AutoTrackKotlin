package cn.com.track_library

import android.view.View

/**
 * Created by JokerWan on 2019-06-03.
 * Function:
 */
internal class WrapperOnClickListener(private val source: View.OnClickListener?) : View.OnClickListener {

    override fun onClick(view: View) {
        //调用原有的 OnClickListener
        try {
            source?.onClick(view)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //插入埋点代码
        TrackDataPrivate.trackViewOnClick(view)
    }
}
