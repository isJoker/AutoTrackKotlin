package cn.com.track_library

import android.view.View
import android.widget.ExpandableListView

/**
 * Created by JokerWan on 2019-06-03.
 * Function:
 */
internal class WrapperOnChildClickListener(private val source: ExpandableListView.OnChildClickListener?) :
    ExpandableListView.OnChildClickListener {

    override fun onChildClick(
        expandableListView: ExpandableListView,
        view: View,
        groupPosition: Int,
        childPosition: Int,
        id: Long
    ): Boolean {
        TrackDataPrivate.trackAdapterView(expandableListView, view, groupPosition, childPosition)
        return source?.onChildClick(expandableListView, view, groupPosition, childPosition, id) ?: false
    }
}
