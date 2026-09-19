package com.walcker.match.core.navigation

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

public class CurrentActivityHolder : Application.ActivityLifecycleCallbacks {
    private var currentActivityRef: WeakReference<Activity>? = null
    public var application: Application? = null
        private set

    public fun currentActivity(): Activity? = currentActivityRef?.get()

    public fun setApplication(app: Application) {
        this.application = app
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
