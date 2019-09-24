package cn.com.track_library

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.support.annotation.Keep
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.menu.ActionMenuItemView
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by JokerWan on 2019-06-02.
 * Function:
 */
object TrackDataPrivate {

    private var mIgnoredActivities: MutableList<String> = ArrayList()
    private val mDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss" + ".SSS", Locale.CHINA)
    private lateinit var mDatabaseHelper: TrackDatabaseHelper
    private val mCountDownTimer: CountDownTimer by lazy { createCountDownTimer() }
    private var mCurrentActivity: WeakReference<Activity>? = null

    /**
     * merge 源JSONObject 到 目标JSONObject
     *
     * @param source
     * @param dest
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun mergeJSONObject(source: JSONObject, dest: JSONObject) {
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = source.get(key)
            if (value is Date) {
                synchronized<JSONObject>(mDateFormat) {
                    dest.put(key, mDateFormat.format(value))
                }
            } else {
                dest.put(key, value)
            }
        }
    }

    fun formatJson(jsonStr: String?): String {
        try {
            if (null == jsonStr || "" == jsonStr) {
                return ""
            }
            val sb = StringBuilder()
            sb.append(" \n")
            var last: Char
            var current = '\u0000'
            var indent = 0
            var isInQuotationMarks = false
            for (element in jsonStr) {
                last = current
                current = element
                when (current) {
                    '"' -> {
                        if (last != '\\') {
                            isInQuotationMarks = !isInQuotationMarks
                        }
                        sb.append(current)
                    }
                    '{', '[' -> {
                        sb.append(current)
                        if (!isInQuotationMarks) {
                            sb.append('\n')
                            indent++
                            addIndentBlank(sb, indent)
                        }
                    }
                    '}', ']' -> {
                        if (!isInQuotationMarks) {
                            sb.append('\n')
                            indent--
                            addIndentBlank(sb, indent)
                        }
                        sb.append(current)
                    }
                    ',' -> {
                        sb.append(current)
                        if (last != '\\' && !isInQuotationMarks) {
                            sb.append('\n')
                            addIndentBlank(sb, indent)
                        }
                    }
                    else -> sb.append(current)
                }
            }

            return sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }

    }

    private fun addIndentBlank(sb: StringBuilder, indent: Int) {
        try {
            for (i in 0 until indent) {
                sb.append('\t')
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * 忽略采集某个activity的页面浏览事件
     *
     * @param activity 忽略的activity
     */
    fun ignoreAutoTrackActivity(activity: Class<*>?) {
        if (activity == null) {
            return
        }
        mIgnoredActivities.add(activity.canonicalName)
    }

    /**
     * 恢复采集某个activity的页面浏览事件
     *
     * @param activity 恢复的activity
     */
    fun removeIgnoreActivity(activity: Class<*>?) {
        if (activity == null) {
            return
        }
        val canonicalName = activity.canonicalName
        if (mIgnoredActivities.contains(canonicalName)) {
            mIgnoredActivities.remove(canonicalName)
        }
    }

    /**
     * 获取 Android ID
     *
     * @param mContext Context
     * @return String
     */
    @SuppressLint("HardwareIds")
    fun getAndroidID(mContext: Context): String {
        var androidID = ""
        try {
            androidID =
                Settings.Secure.getString(mContext.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return androidID
    }

    fun getDeviceInfo(context: Context): Map<String, Any> {
        val deviceInfo = HashMap<String, Any>()
        run {
            deviceInfo["lib"] = "Android"
            deviceInfo["lib_version"] = TrackDataManager.SDK_VERSION
            deviceInfo["os"] = "Android"
            deviceInfo["os_version"] =
                if (Build.VERSION.RELEASE == null) "UNKNOWN" else Build.VERSION.RELEASE
            deviceInfo["manufacturer"] =
                if (Build.MANUFACTURER == null) "UNKNOWN" else Build.MANUFACTURER
            if (TextUtils.isEmpty(Build.MODEL)) {
                deviceInfo["model"] = "UNKNOWN"
            } else {
                deviceInfo["model"] = Build.MODEL.trim { it <= ' ' }
            }

            try {
                val manager = context.packageManager
                val packageInfo = manager.getPackageInfo(context.packageName, 0)
                deviceInfo["app_version"] = packageInfo.versionName

                val labelRes = packageInfo.applicationInfo.labelRes
                deviceInfo["app_name"] = context.resources.getString(labelRes)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val displayMetrics = context.resources.displayMetrics
            deviceInfo["screen_height"] = displayMetrics.heightPixels
            deviceInfo["screen_width"] = displayMetrics.widthPixels

            // 返回只读的map
            return Collections.unmodifiableMap(deviceInfo)
        }
    }

    /**
     * 注册全局Activity生命周期回调
     *
     * @param application
     */
    fun registerActivityLifecycleCallbacks(application: Application) {

        mDatabaseHelper = TrackDatabaseHelper(application, application.packageName)

        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                // 添加ViewTreeObserver.OnGlobalLayoutListener
                val rootView = getRootViewFromActivity(activity, true)
                onGlobalLayoutListener =
                    ViewTreeObserver.OnGlobalLayoutListener {
                        delegateViewsOnClickListener(
                            activity,
                            rootView
                        )
                    }
            }

            override fun onActivityStarted(activity: Activity) {
                mDatabaseHelper.commitAppStart(true)
                val timeDiff = System.currentTimeMillis() - mDatabaseHelper.appPausedTime
                if (timeDiff > SESSION_INTERVAL_TIME) {
                    // 若APP被杀死或者异常退出，导致没有收到app end时间，则重新发送app end事件
                    if (!mDatabaseHelper.appEndEventState) {
                        trackAppEnd(activity)
                    }
                }

                if (mDatabaseHelper.appEndEventState) {
                    mDatabaseHelper.commitAppEndEventState(false)
                    trackAppStart(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                trackAppViewScreen(activity)
                val rootView = getRootViewFromActivity(activity, true)
                onGlobalLayoutListener?.let {
                    rootView.viewTreeObserver.addOnGlobalLayoutListener(it)
                }
            }

            override fun onActivityPaused(activity: Activity) {
                mCurrentActivity = WeakReference(activity)
                mCountDownTimer.start()
                mDatabaseHelper.commitAppPausedTime(System.currentTimeMillis())
            }

            @SuppressLint("ObsoleteSdkInt")
            override fun onActivityStopped(activity: Activity) {
                // 移除ViewTreeObserver.OnGlobalLayoutListener
                if (Build.VERSION.SDK_INT >= 16) {
                    val rootView = getRootViewFromActivity(activity, true)
                    onGlobalLayoutListener?.let {
                        rootView.viewTreeObserver.removeOnGlobalLayoutListener(it)
                    }

                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {

            }

            override fun onActivityDestroyed(activity: Activity) {

            }
        })
    }

    private fun getRootViewFromActivity(activity: Activity, decorView: Boolean): ViewGroup {
        return if (decorView) {
            activity.window.decorView as ViewGroup
        } else {
            activity.findViewById(android.R.id.content)
        }
    }

    /**
     * track 页面浏览事件
     *
     * @param activity 浏览的activity
     */
    private fun trackAppViewScreen(activity: Activity?) {
        if (activity == null) {
            return
        }
        val canonicalName = activity.javaClass.canonicalName
        if (mIgnoredActivities.contains(canonicalName)) {
            return
        }

        try {
            val properties = JSONObject()
            properties.put("activity", canonicalName)
            properties.put("title", getActivityTitle(activity))
            TrackDataManager.instance.track("AppViewScreen", properties)
        } catch (e: JSONException) {
            e.printStackTrace()
        }


    }

    /**
     * Track AppStart 事件
     */
    private fun trackAppStart(activity: Activity?) {
        try {
            if (activity == null) {
                return
            }
            val properties = JSONObject()
            properties.put("activity", activity.javaClass.canonicalName)
            properties.put("title", getActivityTitle(activity))
            TrackDataManager.instance.track("AppStart", properties)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Track AppEnd 事件
     */
    private fun trackAppEnd(activity: Activity?) {
        try {
            activity?.let {
                val properties = JSONObject()
                properties.put("activity", activity.javaClass.canonicalName)
                properties.put("title", getActivityTitle(activity))
                TrackDataManager.instance.track("AppEnd", properties)
                mDatabaseHelper.commitAppEndEventState(true)
                mCurrentActivity = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * 获取指定activity title
     *
     * @param activity 指定的activity
     * @return
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun getActivityTitle(activity: Activity?): String? {
        var activityTitle: String? = null

        if (activity == null) {
            return null
        }

        try {
            activityTitle = activity.title.toString()

            if (Build.VERSION.SDK_INT >= 11) {
                val toolbarTitle = getToolbarTitle(activity)
                if (!TextUtils.isEmpty(toolbarTitle)) {
                    activityTitle = toolbarTitle
                }
            }

            if (TextUtils.isEmpty(activityTitle)) {
                val packageManager = activity.packageManager
                if (packageManager != null) {
                    val activityInfo = packageManager.getActivityInfo(activity.componentName, 0)
                    if (activityInfo != null) {
                        activityTitle = activityInfo.loadLabel(packageManager).toString()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return activityTitle
    }

    @TargetApi(11)
    private fun getToolbarTitle(activity: Activity): String? {
        try {
            val actionBar = activity.actionBar
            if (actionBar != null) {
                if (!TextUtils.isEmpty(actionBar.title)) {
                    return actionBar.title.toString()
                }
            } else {
                if (activity is AppCompatActivity) {
                    val supportActionBar = activity.supportActionBar
                    if (supportActionBar != null) {
                        if (!TextUtils.isEmpty(supportActionBar.title)) {
                            return supportActionBar.title!!.toString()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun formatMsToData(ms: Long): String {
        return mDateFormat.format(ms)
    }

    /**
     * 注册 AppStart 的监听
     */
    fun registerActivityStateObserver(application: Application) {
        val appStartUri = mDatabaseHelper.appStartUri
        application.contentResolver.registerContentObserver(
            appStartUri,
            false,
            object : ContentObserver(Handler()) {
                override fun onChange(selfChange: Boolean, uri: Uri) {
                    if (appStartUri == uri) {
                        mCountDownTimer.cancel()
                    }
                }
            })
    }

    private fun createCountDownTimer(): CountDownTimer {
        return object : CountDownTimer(SESSION_INTERVAL_TIME.toLong(), (10 * 1000).toLong()) {
            override fun onTick(l: Long) {

            }

            override fun onFinish() {
                trackAppEnd(mCurrentActivity?.get())
            }
        }
    }

    fun delegateViewsOnClickListener(context: Activity, view: View) {
        if (view is AdapterView<*>) {
            if (view is Spinner) {
                val onItemSelectedListener = view.onItemSelectedListener
                onItemSelectedListener?.run {
                    if (onItemSelectedListener !is WrapperAdapterViewOnItemSelectedListener) {
                        view.onItemSelectedListener =
                            WrapperAdapterViewOnItemSelectedListener(onItemSelectedListener)
                    }
                }
            } else if (view is ExpandableListView) {
                try {
                    val viewClazz = Class.forName("android.widget.ExpandableListView")
                    //Child
                    val mOnChildClickListenerField =
                        viewClazz.getDeclaredField("mOnChildClickListener")
                    if (!mOnChildClickListenerField.isAccessible) {
                        mOnChildClickListenerField.isAccessible = true
                    }
                    val onChildClickListener =
                        mOnChildClickListenerField.get(view) as ExpandableListView.OnChildClickListener
                    if (onChildClickListener !is WrapperOnChildClickListener) {
                        view.setOnChildClickListener(
                            WrapperOnChildClickListener(onChildClickListener)
                        )
                    }

                    //Group
                    val mOnGroupClickListenerField =
                        viewClazz.getDeclaredField("mOnGroupClickListener")
                    if (!mOnGroupClickListenerField.isAccessible) {
                        mOnGroupClickListenerField.isAccessible = true
                    }
                    val onGroupClickListener =
                        mOnGroupClickListenerField.get(view) as ExpandableListView.OnGroupClickListener
                    if (onGroupClickListener !is WrapperOnGroupClickListener) {
                        view.setOnGroupClickListener(
                            WrapperOnGroupClickListener(onGroupClickListener)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else if (view is ListView || view is GridView) {
                val onItemClickListener = view.onItemClickListener
                onItemClickListener?.run {
                    if (onItemClickListener !is WrapperAdapterViewOnItemClick) {
                        view.onItemClickListener =
                            WrapperAdapterViewOnItemClick(onItemClickListener)
                    }
                }
            }
        } else {
            //获取当前 view 设置的 OnClickListener
            val listener = getOnClickListener(view)

            //判断已设置的 OnClickListener 类型，如果是自定义的 WrapperOnClickListener，说明已经被 hook 过，防止重复 hook
            if (listener != null && listener !is WrapperOnClickListener) {
                //替换成自定义的 WrapperOnClickListener
                view.setOnClickListener(WrapperOnClickListener(listener))
            } else if (view is CompoundButton) {
                val onCheckedChangeListener = getOnCheckedChangeListener(view)
                if (onCheckedChangeListener != null && onCheckedChangeListener !is WrapperOnCheckedChangeListener) {
                    view.setOnCheckedChangeListener(
                        WrapperOnCheckedChangeListener(onCheckedChangeListener)
                    )
                }
            } else if (view is RadioGroup) {
                val radioOnCheckedChangeListener = getRadioGroupOnCheckedChangeListener(view)
                if (radioOnCheckedChangeListener != null && radioOnCheckedChangeListener !is WrapperRadioGroupOnCheckedChangeListener) {
                    view.setOnCheckedChangeListener(
                        WrapperRadioGroupOnCheckedChangeListener(radioOnCheckedChangeListener)
                    )
                }
            } else if (view is RatingBar) {
                val onRatingBarChangeListener = view.onRatingBarChangeListener
                if (onRatingBarChangeListener != null && onRatingBarChangeListener !is WrapperOnRatingBarChangeListener) {
                    view.onRatingBarChangeListener =
                        WrapperOnRatingBarChangeListener(onRatingBarChangeListener)
                }
            } else if (view is SeekBar) {
                val onSeekBarChangeListener = getOnSeekBarChangeListener(view)
                if (onSeekBarChangeListener != null && onSeekBarChangeListener !is WrapperOnSeekBarChangeListener) {
                    view.setOnSeekBarChangeListener(
                        WrapperOnSeekBarChangeListener(onSeekBarChangeListener)
                    )
                }
            }
        }

        //如果 view 是 ViewGroup，需要递归遍历子 View 并 hook
        if (view is ViewGroup) {
            val childCount = view.childCount
            if (childCount > 0) {
                for (i in 0 until childCount) {
                    val childView = view.getChildAt(i)
                    //递归
                    delegateViewsOnClickListener(context, childView)
                }
            }
        }
    }


    /**
     * session间隔时间为30s，即30s之内没有新的页面打开，则认为app处于后台（触发app end事件），
     * 当一个页面显示出来了，与上一个页面的退出时间间隔超过了30s，就认为app重新处于前台了（触发app start）
     */
    private const val SESSION_INTERVAL_TIME = 30 * 1000

    /**
     * View 被点击，自动埋点
     *
     * @param view View
     */
    @Keep
    fun trackViewOnClick(view: View) {
        try {
            val jsonObject = JSONObject()
            jsonObject.run {
                put("element_type", view.javaClass.canonicalName)
                put("element_id", getViewId(view))
                put("element_content", getElementContent(view))

                val activity = getActivityFromView(view)
                put("activity", activity?.javaClass?.canonicalName)

                TrackDataManager.instance.track("AppClick", jsonObject)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * 获取 view 的 android:id 对应的字符串
     *
     * @param view View
     * @return String
     */
    private fun getViewId(view: View): String? {
        var idString: String? = null
        try {
            if (view.id != View.NO_ID) {
                idString = view.context.resources.getResourceEntryName(view.id)
            }
        } catch (e: Exception) {
            //ignore
        }
        return idString
    }

    /**
     * 获取 View 上显示的文本
     *
     * @param view View
     * @return String
     */
    private fun getElementContent(view: View?): String? {
        var content: String? = null
        view?.run {
            when (this) {
                is Button -> content = text.toString()
                is ActionMenuItemView -> content = text.toString()
                is TextView -> content = text.toString()
                is ImageView -> content = contentDescription.toString()
                is RadioGroup -> try {
                    val radioGroup = view as RadioGroup?
                    val activity = getActivityFromView(view)
                    if (activity != null) {
                        val checkedRadioButtonId = radioGroup!!.checkedRadioButtonId
                        val radioButton = activity.findViewById<RadioButton>(checkedRadioButtonId)
                        radioButton?.run {
                            content = text.toString()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                is RatingBar -> content = rating.toString()
                is SeekBar -> content = progress.toString()
                is ViewGroup -> content = traverseViewContent(StringBuilder(), this)
            }
        }

        return content
    }

    private fun traverseViewContent(stringBuilder: StringBuilder, root: View?): String {
        try {
            if (root == null) {
                return stringBuilder.toString()
            }

            if (root is ViewGroup) {
                val childCount = root.childCount
                for (i in 0 until childCount) {
                    val child = root.getChildAt(i)

                    if (child.visibility != View.VISIBLE) {
                        continue
                    }
                    if (child is ViewGroup) {
                        traverseViewContent(stringBuilder, child)
                    } else {
                        val viewText = getElementContent(child)
                        if (!TextUtils.isEmpty(viewText)) {
                            stringBuilder.append(viewText)
                        }
                    }
                }
            } else {
                stringBuilder.append(getElementContent(root))
            }

            return stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return stringBuilder.toString()
        }

    }

    /**
     * 获取 View 所属 Activity
     *
     * @param view View
     * @return Activity
     */
    private fun getActivityFromView(view: View?): Activity? {
        var activity: Activity? = null
        view?.run {
            try {
                var context: Context? = view.context
                context?.run {
                    if (this is Activity) {
                        activity = this
                    } else if (this is ContextWrapper) {
                        while (this !is Activity) {
                            context = this.baseContext
                        }
                        activity = this
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return activity
    }

    fun trackAdapterView(
        adapterView: AdapterView<*>,
        view: View,
        groupPosition: Int,
        childPosition: Int
    ) {
        try {
            JSONObject().run {
                put("element_type", adapterView.javaClass.canonicalName)
                put("element_id", getViewId(adapterView))
                if (childPosition > -1) {
                    put(
                        "element_position",
                        String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition)
                    )
                } else {
                    put("element_position", String.format(Locale.CHINA, "%d", groupPosition))
                }
                val stringBuilder = StringBuilder()
                val viewText = traverseViewContent(stringBuilder, view)
                if (!TextUtils.isEmpty(viewText)) {
                    put("element_element", viewText)
                }
                val activity = getActivityFromView(adapterView)
                if (activity != null) {
                    put("activity", activity.javaClass.canonicalName)
                }

                TrackDataManager.instance.track("AppClick", this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun trackAdapterView(adapterView: AdapterView<*>, view: View, position: Int) {
        try {
            JSONObject().run {
                put("element_type", adapterView.javaClass.canonicalName)
                put("element_id", getViewId(adapterView))
                put("element_position", position.toString())
                val stringBuilder = StringBuilder()
                val viewText = traverseViewContent(stringBuilder, view)
                if (!TextUtils.isEmpty(viewText)) {
                    put("element_element", viewText)
                }
                val activity = getActivityFromView(adapterView)
                if (activity != null) {
                    put("activity", activity.javaClass.canonicalName)
                }

                TrackDataManager.instance.track("AppClick", this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * 获取 View 当前设置的 OnClickListener
     *
     * @param view View
     * @return View.OnClickListener
     */
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    @TargetApi(15)
    private fun getOnClickListener(view: View): View.OnClickListener? {
        val hasOnClick = view.hasOnClickListeners()
        if (hasOnClick) {
            try {
                val viewClazz = Class.forName("android.view.View")
                val listenerInfoMethod = viewClazz.getDeclaredMethod("getListenerInfo")
                if (!listenerInfoMethod.isAccessible) {
                    listenerInfoMethod.isAccessible = true
                }
                val listenerInfoObj = listenerInfoMethod.invoke(view)
                val listenerInfoClazz = Class.forName("android.view.View\$ListenerInfo")
                val onClickListenerField = listenerInfoClazz.getDeclaredField("mOnClickListener")
                if (!onClickListenerField.isAccessible) {
                    onClickListenerField.isAccessible = true
                }
                return onClickListenerField.get(listenerInfoObj) as View.OnClickListener
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            }

        }
        return null
    }

    /**
     * 获取 CheckBox 设置的 OnCheckedChangeListener
     *
     * @param view
     * @return
     */
    private fun getOnCheckedChangeListener(view: View): CompoundButton.OnCheckedChangeListener? {
        try {
            val viewClazz = Class.forName("android.widget.CompoundButton")
            val mOnCheckedChangeListenerField =
                viewClazz.getDeclaredField("mOnCheckedChangeListener")
            if (!mOnCheckedChangeListenerField.isAccessible) {
                mOnCheckedChangeListenerField.isAccessible = true
            }
            return mOnCheckedChangeListenerField.get(view) as CompoundButton.OnCheckedChangeListener
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        return null
    }

    private fun getOnSeekBarChangeListener(view: View): SeekBar.OnSeekBarChangeListener? {
        try {
            val viewClazz = Class.forName("android.widget.SeekBar")
            val mOnCheckedChangeListenerField =
                viewClazz.getDeclaredField("mOnSeekBarChangeListener")
            if (!mOnCheckedChangeListenerField.isAccessible) {
                mOnCheckedChangeListenerField.isAccessible = true
            }
            return mOnCheckedChangeListenerField.get(view) as SeekBar.OnSeekBarChangeListener
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        return null
    }

    private fun getRadioGroupOnCheckedChangeListener(view: View): RadioGroup.OnCheckedChangeListener? {
        try {
            val viewClazz = Class.forName("android.widget.RadioGroup")
            val mOnCheckedChangeListenerField =
                viewClazz.getDeclaredField("mOnCheckedChangeListener")
            if (!mOnCheckedChangeListenerField.isAccessible) {
                mOnCheckedChangeListenerField.isAccessible = true
            }
            return mOnCheckedChangeListenerField.get(view) as RadioGroup.OnCheckedChangeListener
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        return null
    }

}
