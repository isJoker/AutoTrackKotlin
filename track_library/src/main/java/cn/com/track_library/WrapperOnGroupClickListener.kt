package cn.com.track_library

import android.view.View
import android.widget.ExpandableListView

/**
 * Created by JokerWan on 2019-06-03.
 * Function:
 */
internal class WrapperOnGroupClickListener(private val source: ExpandableListView.OnGroupClickListener?) :
    ExpandableListView.OnGroupClickListener {

    override fun onGroupClick(
        expandableListView: ExpandableListView,
        view: View,
        groupPosition: Int,
        id: Long
    ): Boolean {
        TrackDataPrivate.trackAdapterView(expandableListView, view, groupPosition, -1)
        source?.onGroupClick(expandableListView, view, groupPosition, id)
        return false
    }
}
