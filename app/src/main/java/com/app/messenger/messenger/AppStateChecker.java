package com.app.messenger.messenger;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

public class AppStateChecker implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private Context context;
    public void setContext(Context context) {
        this.context = context;
    }

    private boolean isInBackground = false;

    @Override
    public void onTrimMemory(final int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            isInBackground = true;
            BackgroundNotifications.setUserStatus("absent");
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if(isInBackground){
            BackgroundNotifications.setUserStatus("");
            isInBackground = false;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) { }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) { }

    @Override
    public void onActivityStarted(Activity activity) { }

    @Override
    public void onActivityStopped(Activity activity) { }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) { }

    @Override
    public void onActivityDestroyed(Activity activity) { }

    @Override
    public void onConfigurationChanged(Configuration configuration) { }

    @Override
    public void onLowMemory() { }
}