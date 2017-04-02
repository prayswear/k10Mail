package com.c0124.k9.c0124.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.c0124.k9.K9;
import com.c0124.k9.c0124.ClientHelper;

/**
 * Created by xinqian on 8/8/15.
 */
// http://stackoverflow.com/questions/3667022/checking-if-an-android-application-is-running-in-the-background
public class LifecycleHandler implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        ++resumed;
        ClientHelper.i(K9.LOG_TAG, "onActivityPaused resumed:" + resumed + ", paused: " + paused + ", inForeground:" + isApplicationInForeground());
    }

    @Override
    public void onActivityPaused(Activity activity) {
        ++paused;
        ClientHelper.i(K9.LOG_TAG, "onActivityPaused resumed:" + resumed + ", paused: " + paused + ", inForeground:" + isApplicationInForeground());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        ++started;
        ClientHelper.i(K9.LOG_TAG, "onActivityStarted started:" + started + ", stopped:" + stopped + ", visible:" + isApplicationVisible());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        ++stopped;
        ClientHelper.i(K9.LOG_TAG, "onActivityStopped started:" + started + ", stopped:" + stopped + ", visible:" + isApplicationVisible());
    }

    private static int resumed;
    private static int paused;
    private static int started;
    private static int stopped;

    // And these two public static functions
    public static boolean isApplicationVisible() {
        return started > stopped;
    }

    public static boolean isApplicationInForeground() {
        return resumed > paused;
    }
}