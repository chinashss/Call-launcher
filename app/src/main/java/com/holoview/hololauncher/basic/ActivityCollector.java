package com.holoview.hololauncher.basic;

import android.app.Activity;

import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Created by Mr.kk on 2019/4/4.
 * This Project is android-glass-launcher
 */
public class ActivityCollector {
    public static LinkedHashMap<String, Activity> activities = new LinkedHashMap<String, Activity>();

    public static void addActivity(Activity activity) {
        activities.put(activity.getClass().getSimpleName(), activity);
    }

    public static void removeActivity(Activity activity) {
        activities.remove(activity.getClass().getSimpleName());
    }

    public static void closeActivity(Class cls) {
        Activity removeActivity = activities.get(cls.getSimpleName());
        removeActivity.finish();
    }

    public static void finishAll() {
        for (String key : activities.keySet()) {
            removeActivity(activities.get(key));
        }
    }
}
