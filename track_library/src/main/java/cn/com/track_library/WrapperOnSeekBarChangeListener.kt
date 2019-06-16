package cn.com.track_library

import android.widget.SeekBar

/**
 * Created by JokerWan on 2019-06-03.
 * Function:
 */
internal class WrapperOnSeekBarChangeListener(private val source: SeekBar.OnSeekBarChangeListener?) :
    SeekBar.OnSeekBarChangeListener {

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        source?.onStopTrackingTouch(seekBar)

        TrackDataPrivate.trackViewOnClick(seekBar)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        source?.onStartTrackingTouch(seekBar)
    }

    override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
        source?.onProgressChanged(seekBar, i, b)
    }
}
