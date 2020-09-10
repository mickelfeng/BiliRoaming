package me.iacn.biliroaming.hook

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import me.iacn.biliroaming.utils.*
import me.iacn.biliroaming.BiliBiliPackage.Companion.instance
import me.iacn.biliroaming.XposedInit

class DownloadThreadHook(classLoader: ClassLoader) : BaseHook(classLoader) {
    override fun startHook() {
        Log.d("startHook: DownloadThread")
        if(!XposedInit.sPrefs.getBoolean("custom_download_thread", false)) return
        instance.downloadThreadListenerClass?.run {
            hookBeforeAllConstructors { param ->
                val view = param.args[1] as TextView
                val visibility = if (view.tag as Int == 1) {
                    view.text = "自定义"
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
                (view.parent as ViewGroup).getChildAt(1).visibility = visibility
            }
            replaceMethod("onClick", View::class.java) { param ->
                var textViewField: String? = null
                var activityField: String? = null
                declaredFields.forEach {
                    when (it.type) {
                        instance.downloadingActivityClass -> activityField = it.name
                        TextView::class.java -> textViewField = it.name
                    }
                }
                val view = param.thisObject.getObjectFieldAs<TextView>(textViewField)
                if (view.tag as Int == 1) {
                    AlertDialog.Builder(view.context).create().run {
                        setTitle("自定义同时缓存数")
                        val numberPicker = NumberPicker(context).run {
                            minValue = 1
                            maxValue = 64
                            wrapSelectorWheel = false
                            value = param.thisObject.getObjectField(activityField)?.getIntField(instance.downloadingThread())
                                    ?: 1
                            this
                        }
                        setView(numberPicker, 50, 0, 50, 0)
                        setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
                            view.tag = numberPicker.value
                            param.invokeOriginalMethod()
                        }
                        show()
                    }
                } else {
                    param.invokeOriginalMethod()
                }
            }
        }
        instance.reportDownloadThreadClass?.replaceMethod(instance.reportDownloadThread(), Context::class.java, Int::class.javaPrimitiveType){}
    }
}