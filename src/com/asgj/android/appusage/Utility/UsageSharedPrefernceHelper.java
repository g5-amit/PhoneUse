package com.asgj.android.appusage.Utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.asgj.android.appusage.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class UsageSharedPrefernceHelper {
    private static String PREFERNCE_NAME = "phone.usage";
    private static final String LOG_TAG = UsageSharedPrefernceHelper.class.getSimpleName();
    private static String PREF_NAME_APP_USAGE_INFO = "phone.usage.app.info";
    private static String PREF_NAME_MUSIC_USAGE_INFO = "phone.usage.music.info";

    public static void insertTotalDurationAppInPref(Context context, String pkgName, long time) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME_APP_USAGE_INFO,
                Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(pkgName, time);
        editor.commit();
    }

    public static long getTotalDurationAppInPref(Context context, String pkgName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME_APP_USAGE_INFO,
                Context.MODE_PRIVATE);
        return prefs.getLong(pkgName, 0);
    }

    public static void setServiceRunning(Context context, boolean isServiceRunning) {
        SharedPreferences prefs = context
                .getSharedPreferences(PREFERNCE_NAME, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putBoolean("isServiceRunning", isServiceRunning);
        editor.commit();
    }
    
    public static void setShowByUsage(Context context, String isServiceRunning) {
        SharedPreferences prefs = context
                .getSharedPreferences(PREFERNCE_NAME, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString("showBy", isServiceRunning);
        editor.commit();
    }
    
    public static String getShowByType(Context context) {
        SharedPreferences prefs = context
                .getSharedPreferences(PREFERNCE_NAME, Context.MODE_PRIVATE);
        return prefs.getString("showBy", context.getString(R.string.string_Today));
    }

    public static boolean isServiceRunning(Context context) {
        SharedPreferences prefs = context
                .getSharedPreferences(PREFERNCE_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("isServiceRunning", false);
    }

    public static void setServiceRunningWhileShutDown(Context context, boolean isServiceRunning) {
        SharedPreferences prefs = context
                .getSharedPreferences(PREFERNCE_NAME, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putBoolean("isServiceRunningWhileShutDown", isServiceRunning);
        editor.commit();
    }

    public static void setCurrentDate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERNCE_NAME,
                Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong("date", System.currentTimeMillis());
        editor.commit();
    }

    public static long getDateStoredInPref(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(PREFERNCE_NAME,
                Context.MODE_PRIVATE);
        long date = prefs.getLong("date",
                System.currentTimeMillis());
        return date;

    }

    public static void clearPreference(Context context) {

        SharedPreferences appPrefs = context.getSharedPreferences(PREF_NAME_APP_USAGE_INFO,
                Context.MODE_PRIVATE);
        SharedPreferences musicPrefs = context.getSharedPreferences(PREF_NAME_MUSIC_USAGE_INFO,
                Context.MODE_PRIVATE);
        
        Log.v(LOG_TAG, "Clearing preference, preference data is: " + appPrefs.getAll());
        Editor editor = appPrefs.edit();
        editor.clear();
        editor.commit();

        editor = musicPrefs.edit();
        editor.clear();
        editor.commit();
    }

    public static HashMap<String, Long> getAllKeyValuePairsApp(Context context) {
        SharedPreferences prefs = context
                .getSharedPreferences(PREF_NAME_APP_USAGE_INFO, Context.MODE_PRIVATE);
        return (HashMap<String, Long>) prefs.getAll();
    }
    
    public static HashMap<String, HashSet<String>> getAllKeyValuePairsMusic(Context context) {
        SharedPreferences prefs = context
                .getSharedPreferences(PREF_NAME_MUSIC_USAGE_INFO, Context.MODE_PRIVATE);
        return (HashMap<String, HashSet<String>>) prefs.getAll();
    }

    public static boolean isNeedToServiceOnReboot(Context context) {
        SharedPreferences prefs = context
                .getSharedPreferences(PREFERNCE_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("isServiceRunningWhileShutDown", false);
    }

    public static ArrayList<UsageInfo> getTotalInfoOfMusic(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME_MUSIC_USAGE_INFO,
                Context.MODE_PRIVATE);
        Set<String> mInfoList = prefs.getStringSet("music", null);
        ArrayList<UsageInfo> mInfo = new ArrayList<>();
        if (mInfoList != null) {
            for (String s : mInfoList) {
                mInfo.add(getMusicInfo(s));
            }
        }
        return mInfo;

    }

    public static void setTotalIntervalsOfMusic(Context context, UsageInfo info) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME_MUSIC_USAGE_INFO,
                Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        Set<String> mInfoList = new HashSet<>();
        if (prefs.contains("music")) {
            mInfoList.addAll(prefs.getStringSet("music", null));
        }
        mInfoList.add(getFormattedStringFromMusicInfo(info));
        editor.putStringSet("music", mInfoList);
        editor.commit();
    }

    public static Set<String> getSelectedApplicationForTracking(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERNCE_NAME,
                Context.MODE_PRIVATE);
        return prefs.getStringSet("applicationtracking", null);
        

    }

    public static void setApplicationForTracking(Context context,String info) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERNCE_NAME,
                Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        Set<String> mInfoList = new HashSet<>();
        if (prefs.contains("applicationtracking")) {
            mInfoList.addAll(prefs.getStringSet("applicationtracking", null));
        }
        if(!mInfoList.contains(info)){
        mInfoList.add(info);
        editor.putStringSet("applicationtracking", mInfoList);
        editor.commit();
        }
    }
    private static String getFormattedStringFromMusicInfo(UsageInfo info) {
        return info.getmIntervalStartTime() + "_" + info.getmIntervalEndTime() + "_"
                + info.getmIntervalDuration();
    }

    private static UsageInfo getMusicInfo(String info) {
        StringTokenizer tokenizer = new StringTokenizer(info, "_");
        UsageInfo infoIns = new UsageInfo();
        infoIns.setmIntervalStartTime(Long.parseLong((String) tokenizer.nextElement()));
        infoIns.setmIntervalEndTime(Long.parseLong((String) tokenizer.nextElement()));
        infoIns.setmIntervalDuration(Long.parseLong((String) tokenizer.nextElement()));
        return infoIns;
    }

    public static void updateTodayDataForApps(Context context, HashMap<String, Long> dataMap) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME_APP_USAGE_INFO, Context.MODE_PRIVATE);

        for (Map.Entry<String, Long> dataEntry : dataMap.entrySet()) {
            insertTotalDurationAppInPref(context, dataEntry.getKey(), dataEntry.getValue()
                    + getTotalDurationAppInPref(context, dataEntry.getKey()));
        }
        Log.v(LOG_TAG, "Data in xml: " + prefs.getAll());
    }
    
    /**
     * Method to write music data for today to XML.
     * @param context Context to access application resources.
     * @param musicList List containing {@code UsageInfo} objects which hold music details.
     * @throws NullPointerException if musicList is null.
     * @throws IllegalArgumentException if musicList is empty.
     */
    public static void updateTodayDataForMusic(Context context, ArrayList<UsageInfo> musicList) throws NullPointerException, IllegalArgumentException {
        
        if (musicList == null) {
            throw new NullPointerException("Music list is null");
        } else if (musicList.isEmpty()) {
            throw new IllegalArgumentException("Music list is empty");
        } else {
            ListIterator<UsageInfo> iterator = musicList.listIterator();
            
            while (iterator.hasNext()) {
                setTotalIntervalsOfMusic(context, iterator.next());
            }
            
            Log.v (LOG_TAG, "Music data prefs: " + getTotalInfoOfMusic(context));
        }
    }
    
    /**
     * Return a calendar object based on the showType stored in preferences.
     * @param context Context to access application resources.
     * @return calendar Calendar object with given date.
     */
    public static Calendar getCalendarByShowType(Context context) {
        
        SharedPreferences prefs = context.getSharedPreferences(PREFERNCE_NAME, Context.MODE_PRIVATE);
        String showBy = prefs.getString("showBy", context.getResources().getString(R.string.string_Today));
        
        Calendar calendar = Calendar.getInstance();
        
        switch (showBy) {
        case "Today" : break;
        case "Weekly" : calendar.add(Calendar.DATE, -6);
                        break;
        case "Monthly" : calendar.add(Calendar.DATE, -29);
                        break;
        case "Yearly" : calendar.add(Calendar.DATE, -364);
                        break;
        default : break;
        }
        
        return calendar;
    }
}
