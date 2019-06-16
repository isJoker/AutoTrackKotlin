package cn.com.track_library

import android.widget.RatingBar

/**
 * Created by JokerWan on 2019-06-03.
 * Function:
 */
internal class WrapperOnRatingBarChangeListener(private val source: RatingBar.OnRatingBarChangeListener?) :
    RatingBar.OnRatingBarChangeListener {

    override fun onRatingChanged(ratingBar: RatingBar, v: Float, b: Boolean) {
        //调用原有的 OnClickListener
        try {
            source?.onRatingChanged(ratingBar, v, b)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //插入埋点代码
        TrackDataPrivate.trackViewOnClick(ratingBar)
    }
}
