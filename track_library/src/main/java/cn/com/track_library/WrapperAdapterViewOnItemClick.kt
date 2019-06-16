package cn.com.track_library

import android.view.View
import android.widget.AdapterView

/**
 * Created by JokerWan on 2019-06-03.
 * Function:
 */
internal class WrapperAdapterViewOnItemClick(private val source: AdapterView.OnItemClickListener?) :
    AdapterView.OnItemClickListener {

    override fun onItemClick(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        source?.onItemClick(adapterView, view, position, id)

        TrackDataPrivate.trackAdapterView(adapterView, view, position)
    }
}
