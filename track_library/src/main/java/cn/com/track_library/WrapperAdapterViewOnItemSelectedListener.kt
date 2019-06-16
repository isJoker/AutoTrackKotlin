package cn.com.track_library

import android.view.View
import android.widget.AdapterView

/**
 * Created by JokerWan on 2019-06-03.
 * Function:
 */
internal class WrapperAdapterViewOnItemSelectedListener(private val source: AdapterView.OnItemSelectedListener?) :
    AdapterView.OnItemSelectedListener {

    override fun onItemSelected(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        source?.onItemSelected(adapterView, view, position, id)

        TrackDataPrivate.trackAdapterView(adapterView, view, position)
    }

    override fun onNothingSelected(adapterView: AdapterView<*>) {
        source?.onNothingSelected(adapterView)
    }
}
