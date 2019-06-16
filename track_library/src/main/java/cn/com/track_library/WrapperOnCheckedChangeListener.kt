package cn.com.track_library

import android.widget.CompoundButton

/**
 * Created by JokerWan on 2019-06-03.
 * Function:
 */
internal class WrapperOnCheckedChangeListener(private val source: CompoundButton.OnCheckedChangeListener?) :
    CompoundButton.OnCheckedChangeListener {

    override fun onCheckedChanged(compoundButton: CompoundButton, b: Boolean) {
        //调用原有的 OnClickListener
        try {
            source?.onCheckedChanged(compoundButton, b)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //插入埋点代码
        TrackDataPrivate.trackViewOnClick(compoundButton)
    }
}
