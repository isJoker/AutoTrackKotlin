package cn.com.track_library

import android.widget.RadioGroup

/**
 * Created by JokerWan on 2019-06-03.
 * Function:
 */
internal class WrapperRadioGroupOnCheckedChangeListener(private val source: RadioGroup.OnCheckedChangeListener?) :
    RadioGroup.OnCheckedChangeListener {

    override fun onCheckedChanged(radioGroup: RadioGroup, i: Int) {
        //调用原有的 OnClickListener
        try {
            source?.onCheckedChanged(radioGroup, i)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //插入埋点代码
        TrackDataPrivate.trackViewOnClick(radioGroup)
    }
}
