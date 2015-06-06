package com.asgj.android.appusage.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.asgj.android.appusage.R;
import com.asgj.android.appusage.Utility.UsageInfo;
import com.asgj.android.appusage.Utility.UsageSharedPrefernceHelper;
import com.asgj.android.appusage.Utility.Utils;
import com.asgj.android.appusage.database.PhoneUsageDatabase;
import com.asgj.android.appusage.database.PhoneUsageDbHelper;

/**
 * Service to track application usage time for different apps being used by the user.
 * It'll track usage for foreground apps as well as background apps (Music, Call).
 * @author jain.g
 */
public class UsageTrackingService extends Service {

    public interface provideData {
        public void provideMap(HashMap<String, Long> map);
    }

    private final LocalBinder mBinder = new LocalBinder();
    private static final String LOG_TAG = UsageTrackingService.class.getSimpleName();
    private PhoneUsageDatabase mDatabase;
    private Timer mTimer;

    // Hash-map to hold time values for foreground and background activity time values.
    public HashMap<String, Long> mForegroundActivityMap;
    public HashMap<String, Integer> mCallDetailsMap;
    public ArrayList<UsageInfo> mListMusicPlayTimes;

    int mIndex = 0;
    BackgroundTrackingTask mBgTrackingTask;

    private boolean mIsRunningForegroundAppsThread = false,
            mIsRunningBackgroundApps = false,
            mIsFirstTimeStartForgroundAppService = false,
            mIsMusicPlaying = false,
            mIsMusicStarted = false,
            mIsMusicPlayingAtStart = false,
            mIsScreenOff = false,
            mIsEndTracking = false;

    private KeyguardManager mKeyguardManager;
    private ActivityManager mActivityManager;
    private TelephonyManager mTelephonyManager;

    private long mPreviousStartTime;
    private String mPackageName;
    private String mCurrentAppName, mPreviousAppName;

    private Context mContext = null;
    private long mStartTime, mUsedTime, mStartTimestamp, mEndTimestamp;
    private long mMusicListenTime;
    private long mMusicStartTime, mMusicStopTime;
    private long mPreviousAppStartTimeStamp, mPreviousAppExitTimeStamp, mMusicStartTimeStamp, mMusicStopTimeStamp;

    // Broadcast receiver to receive screen wake up events.
    public BroadcastReceiver dataProvideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals("com.android.asgj.appusage.action.DATA_PROVIDE")) {
                Log.v(LOG_TAG, "Provide data to activity.");

                if (mBgTrackingTask != null && mBgTrackingTask.foregroundMap != null
                        && mIsScreenOff == false) {
                    long time = System.nanoTime();
                    long duration = Utils.getTimeInSecFromNano(time - mPreviousStartTime);

                    doHandlingOnApplicationClose();
                    if (duration > 0L) {
                        if (mBgTrackingTask.foregroundMap.containsKey(mPreviousAppName)) {
                            mBgTrackingTask.foregroundMap
                                    .put(mPreviousAppName,
                                            mBgTrackingTask.foregroundMap.get(mPreviousAppName)
                                                    + duration);
                        } else {
                            mBgTrackingTask.foregroundMap.put(mPreviousAppName,
                                    duration);
                        }
                    }
                    doHandlingForApplicationStarted();
                    if (mBinder.interfaceMap != null) {
                        mBinder.interfaceMap.provideMap(mBgTrackingTask.foregroundMap);
                    }
                }
            }
        }
    };

    // Broadcast receiver to receive screen wake up events.
    private BroadcastReceiver screenWakeUpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
                Log.v(LOG_TAG, "Screen is on");

                // Check whether key-guard is locked or not.
                if (mKeyguardManager.isKeyguardLocked()) {
                 // Bypass to screenUserPresent receiver.
                } else {
                    
                    mIsRunningForegroundAppsThread = true;
                    
                    // Update mPreviousStartTime and start timestamp.
                    mPreviousStartTime = System.nanoTime();
                    mPreviousAppStartTimeStamp = System.currentTimeMillis();
                    
                    // If thread isn't already running. Start it again.
                    if (mIsRunningBackgroundApps == false) {
                        startThread();
                    }
                    mIsScreenOff = false;
                }

            }
        }
    };

    // Broadcast receiver to receive screen wake up events.
    private BroadcastReceiver timeTickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals("android.intent.action.TIME_TICK")) {
                
                // Check whether present time is 00:00, dump data and clear preference at that time.
                java.text.DateFormat dateFormat = SimpleDateFormat.getTimeInstance();
                String time = dateFormat.format(new Date(System.currentTimeMillis()));
                Log.v(LOG_TAG, "Time is: " + time);
                
                String timeToCompare12Hour = "12:00:00 AM";
                String timeToCompare24Hour = "00:00:00";
                
                if (time.equals(timeToCompare12Hour) || time.equals(timeToCompare24Hour))
                {
                    // APPS DATA.
                    Log.v(LOG_TAG, "It's midnight, dump data to DB.");
                    UsageSharedPrefernceHelper.clearPreference(mContext);
                    long currentTime = System.nanoTime();
                    
                    UsageInfo usageInfo = new UsageInfo();
                    usageInfo.setmIntervalStartTime(mPreviousAppStartTimeStamp);
                    usageInfo.setmIntervalEndTime(mPreviousAppExitTimeStamp);
                    usageInfo.setmIntervalDuration(Utils.getTimeInSecFromNano(currentTime - mPreviousStartTime));
                    mDatabase.insertApplicationEntry(mPreviousAppName, usageInfo);
                    
                    mPreviousStartTime = currentTime;
                    mPreviousAppStartTimeStamp = System.currentTimeMillis();
                    initializeMap(mBgTrackingTask.foregroundMap);
                    
                    // MUSIC DATA.
                    if (isMusicPlaying()) {
                        doHandleForMusicClose();
                        
                        mIsMusicStarted = true;
                        mMusicStartTime = System.nanoTime();
                        mMusicStartTimeStamp = System.currentTimeMillis();
                        
                        mListMusicPlayTimes.clear();
                    }
                }
            }
        }
    };
    
    // Broadcast receiver to receive screen wake up events.
    private BroadcastReceiver screenUserPresentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals("android.intent.action.USER_PRESENT")) {
                Log.v(LOG_TAG, "Screen user present");
                mIsRunningForegroundAppsThread = true;
                
                // Update mPrevioustime and start time-stamp.
                mPreviousStartTime = System.nanoTime();
                mPreviousAppStartTimeStamp = System.currentTimeMillis();
                
                // If thread isn't already running. Start it again.
                if (mIsRunningBackgroundApps == false) {
                    startThread();
                }
                mIsScreenOff = false;
            }
        }
    };

    // Broadcast receiver to catch screen dim event (Means user not using phone other than attending a call or listening music.)
    private BroadcastReceiver screenDimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                Log.v(LOG_TAG, "SCREEN IS OFF");
                
                mIsScreenOff = true;
                // Update data, only if we're getting screen dim state from foreground apps running state.
                // Corner case - Screen on and locked, again screen turns dim. Avoid data update for this.
                if (mIsRunningForegroundAppsThread == true) {
                    doHandlingOnApplicationClose();
                    storeMap(mBgTrackingTask.foregroundMap);
                    
                    // Reinitialize map.
                    initializeMap(mBgTrackingTask.foregroundMap);
                    
                    Log.v (LOG_TAG, "screen Dim -- mForegroundMap after reinitializtion is : " + mBgTrackingTask.foregroundMap);
                    
                    long time = System.nanoTime();
                    long duration = Utils.getTimeInSecFromNano(time - mPreviousStartTime);

                    if (duration > 0L) {
                        if (mForegroundActivityMap.containsKey(mPreviousAppName)) {
                            mForegroundActivityMap.put(mPreviousAppName, mForegroundActivityMap.get(mPreviousAppName) + duration);
                        } else {
                            mForegroundActivityMap.put(mPreviousAppName, duration);
                        }
                    }
                    
                    Log.v (LOG_TAG, "screen Dim -- mForeground activity Map after updation is : " + mForegroundActivityMap);
                }
                
                // If screen dim, and user isn't listening to songs or talking, then update boolean variables.
                if (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE && !isMusicPlaying()) {
                    mIsRunningBackgroundApps = false;
                    mIsMusicStarted = false;
                } else if (!isMusicPlaying()) {
                    mIsMusicStarted = false;
                }
                mIsRunningForegroundAppsThread = false;
                
            }
        }
    };

    /**
     * Perform clean up tasks in this method, as activity will restart after this.
     */
    public void onTaskRemoved(Intent rootIntent) {
        onDestroy();
    };

    /**
     * Method to get current tracking map for applications for displaying in main screen.
     * @return Map of entries, with pkg name as key and duration as value. 
     */
    public HashMap<String, Long> getCurrentMap(Calendar calendar) {
        HashMap<String, Long> currentDataForToday = new HashMap<>();
		if (!UsageSharedPrefernceHelper.getShowByType(mContext).equals(
                mContext.getString(R.string.string_Today))) {
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.add(Calendar.DATE, -1);
            
            HashMap<String, Long> mPreviousDaysData; 
            
            if (!UsageSharedPrefernceHelper.getShowByType(mContext).equals(mContext.getString(R.string.string_Custom))) {
		            mPreviousDaysData = mDatabase
                    .getApplicationEntryForMentionedTimeBeforeToday(mContext,
                            UsageSharedPrefernceHelper.getCalendarByShowType(mContext), endCalendar);
            } else {
                mPreviousDaysData = mDatabase.getApplicationEntryForMentionedTimeBeforeToday(mContext, calendar, endCalendar);
            }
            for (Map.Entry<String, Long> dataEntry : mPreviousDaysData
                    .entrySet()) {
                String key = dataEntry.getKey();

                if (dataEntry.getValue() > 0L) {
                    if (currentDataForToday.containsKey(key)) {
                        currentDataForToday.put(key, dataEntry.getValue()
                                + currentDataForToday.get(key));
                    } else {
                        currentDataForToday.put(key, dataEntry.getValue());
                    }
                }
            }
        }
        
        /*if (mBgTrackingTask != null) {
            if (mBgTrackingTask.foregroundMap != null) {
                Log.v ("gaurav", "Foreground Map in calculation: " + mBgTrackingTask.foregroundMap);
            for (Map.Entry<String, Long> dataEntry : mBgTrackingTask.foregroundMap.entrySet()) {
                String key = dataEntry.getKey();
                Log.v ("gaurav", "Inserting entry");
                if (currentDataForToday.containsKey(key)) {
                    currentDataForToday.put(key, dataEntry.getValue() + currentDataForToday.get(key));
                } else {
                    currentDataForToday.put(key, dataEntry.getValue());
                }
            }
            }
        }
*/
        for (Map.Entry<String, Long> dataEntry : mForegroundActivityMap.entrySet()) {
            String key = dataEntry.getKey();

            if (dataEntry.getValue() > 0) {
                if (currentDataForToday.containsKey(key)) {
                    currentDataForToday.put(key, dataEntry.getValue() + currentDataForToday.get(key));
                } else {
                    currentDataForToday.put(key, dataEntry.getValue());
                }
            }
        }
        
        HashMap<String, Long> tempMap = new HashMap<>();
        tempMap = UsageSharedPrefernceHelper.getAllKeyValuePairsApp(mContext);
        
        for (Map.Entry<String, Long> dataEntry : tempMap.entrySet()) {
            String key = dataEntry.getKey();
            
            if (dataEntry.getValue() > 0) {
                if (currentDataForToday.containsKey(key)) {
                    currentDataForToday.put(key, dataEntry.getValue() + currentDataForToday.get(key));
                } else {
                    currentDataForToday.put(key, dataEntry.getValue());
                }
            }
        }
    
        return currentDataForToday;
    }

    /**
     * Method to return current data for music (for today) for displaying in Music tab.
     * @return ArrayList containing objects of {@code UsageInfo} types.
     */
    public ArrayList<UsageInfo> getCurrentDataForMusic() {
        
        ArrayList<UsageInfo> currentDataForMusic = new ArrayList<UsageInfo>();
        currentDataForMusic = UsageSharedPrefernceHelper.getTotalInfoOfMusic(mContext);

        currentDataForMusic.addAll(mListMusicPlayTimes);
        
        if (UsageSharedPrefernceHelper.isServiceRunning(mContext) && isMusicPlaying()) {
            
            // Add an entry from start time of music to present time.
            long time = System.nanoTime();
            
            // Add this interval to list.
            UsageInfo info = new UsageInfo();
            info.setmIntervalStartTime(mMusicStartTimeStamp);
            info.setmIntervalEndTime(System.currentTimeMillis());
            info.setmIntervalDuration(Utils.getTimeInSecFromNano(time - mMusicStartTime));
            currentDataForMusic.add(info);
        }
        
        if (!UsageSharedPrefernceHelper.getShowByType(mContext).equals(
                mContext.getString(R.string.string_Today)) && !UsageSharedPrefernceHelper.getShowByType(mContext).equals(mContext.getString(R.string.string_Custom))) {
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.add(Calendar.DATE, -1);
            
            currentDataForMusic
                    .addAll(mDatabase.getMusicEntryForMentionedTimeBeforeToday(
                            mContext,
                            UsageSharedPrefernceHelper.getCalendarByShowType(mContext), endCalendar));
        }
        return currentDataForMusic;
    }
    
    /**
     * Method to check whether music is playing.
     * @return true, if music is playing, false otherwise.
     */
    public boolean isMusicPlaying() {
        // Have to check time when screen off but music playing / call being taken.
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mIsMusicPlaying = audioManager.isMusicActive();
        return mIsMusicPlaying;
    }

    /**
     * Calculate total time for which phone is used.
     */
    public long phoneUsedTime() {
        Log.v ("gaurav", "Map is: " + mForegroundActivityMap);
        mUsedTime = 0;
        for (Map.Entry<String, Long> entry : mForegroundActivityMap.entrySet()) {
            mUsedTime += entry.getValue();
        }
        return mUsedTime;
    }

    /**
     * Local binder class to return an instance of this service for interaction with activity.
     */
    public class LocalBinder extends Binder {
         public provideData interfaceMap;

        // Return service instance from this class.
        public UsageTrackingService getInstance() {
            return UsageTrackingService.this;
        }

        public void setInterface (provideData data) {
            interfaceMap = data;
        }
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        mContext = this;

        // Set up broadcast receivers that this service uses.
        setUpReceivers(true);

        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        
        // Initialize hash-maps to hold time values.
        mForegroundActivityMap = new HashMap<String, Long>();
        mCallDetailsMap = new HashMap<String, Integer>();
        mListMusicPlayTimes = new ArrayList<UsageInfo>();

        // Starting time from which calculation needs to be done.
        mStartTime = System.nanoTime();
        mPreviousAppStartTimeStamp = System.currentTimeMillis();
        
        // Here you bind to the service.
        /*Notification noti = new Notification.Builder(mContext)
        .setContentTitle("App Usage")
        .setContentText("Tracking in progress")
        .setSmallIcon(R.drawable.ic_launcher)
        .build();
        Intent notificationIntent = new Intent(this, UsageTrackingService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        noti.setLatestEventInfo(this, getText(R.string.action_settings),
                getText(R.string.hello_world), pendingIntent);*/
       // startForeground(1, noti);

        // Initialize thread to set up default values.
        initThread(true);
        UsageSharedPrefernceHelper.setServiceRunning(mContext, true);
        Log.v(LOG_TAG, "Service 1 created");
    }

    private void setUpReceivers(boolean register) {
        if (register) {
            IntentFilter wakeUpFilter = new IntentFilter("android.intent.action.SCREEN_ON");
            IntentFilter dimFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
            IntentFilter userPresentFilter = new IntentFilter("android.intent.action.USER_PRESENT");
            IntentFilter timeTickFilter = new IntentFilter("android.intent.action.TIME_TICK");
            
            IntentFilter dataProvideFilter = new IntentFilter("com.android.asgj.appusage.action.DATA_PROVIDE");
            // Register receivers.
            registerReceiver(screenWakeUpReceiver, wakeUpFilter);
            registerReceiver(screenDimReceiver, dimFilter);
            registerReceiver(screenUserPresentReceiver, userPresentFilter);
            registerReceiver(timeTickReceiver, timeTickFilter);
            registerReceiver(dataProvideReceiver, dataProvideFilter);
        } else {
            unregisterReceiver(screenUserPresentReceiver);
            unregisterReceiver(screenWakeUpReceiver);
            unregisterReceiver(screenDimReceiver);
            unregisterReceiver(timeTickReceiver);
            unregisterReceiver(dataProvideReceiver);
        }
    }

    private void doHandlingOnApplicationClose(){
    	 mPreviousAppExitTimeStamp = System.currentTimeMillis();
         long time = System.nanoTime();
         long duration = Utils.getTimeInSecFromNano(time - mPreviousStartTime);

         // In case application usage duration is 0 seconds, just return.
         if (duration == 0) {
             return;
         }

         // As application has changed, we need to dump data to DB.
         UsageInfo usageInfoApp = new UsageInfo();
         usageInfoApp.setmIntervalStartTime(mPreviousAppStartTimeStamp);
         usageInfoApp.setmIntervalEndTime(mPreviousAppExitTimeStamp);
         usageInfoApp.setmIntervalDuration(duration);

         // Insert data to database for previous application.
         mDatabase.insertApplicationEntry(mPreviousAppName, usageInfoApp);
    }

    private void doHandlingForApplicationStarted() {
        long time = System.nanoTime();
        mPreviousAppStartTimeStamp = System.currentTimeMillis();
        Log.v(LOG_TAG, "I AM CALLED APP NAME CHANGED");
        mPreviousStartTime = time;
    }
    
    @SuppressWarnings("deprecation")
    private boolean isTopApplicationchange() {
          mActivityManager = (ActivityManager) mContext.getSystemService(ACTIVITY_SERVICE);
          mPackageName = mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName();

          mCurrentAppName = mPackageName;
          return !mCurrentAppName.equals(mPreviousAppName);

    }
    
    private boolean isNeedToHandleMusicClose(){
    	return !isMusicPlaying() && mIsMusicStarted;
    }
    
    private void doHandleForMusicClose(){
          mMusicStopTime = System.nanoTime();
          mMusicStopTimeStamp = System.currentTimeMillis();

          
          mMusicListenTime += (Utils.getTimeInSecFromNano(mMusicStopTime - mMusicStartTime));
          mIsMusicStarted = false;
          mIsMusicPlayingAtStart = false;

          long duration = Utils.getTimeInSecFromNano(mMusicStopTime - mMusicStartTime);
          
          if (duration > 0) {
              mMusicListenTime += (duration);

              // As music has been stopped add resulting interval to list.
              UsageInfo usageInfoMusic = new UsageInfo();
              usageInfoMusic.setmIntervalStartTime(mMusicStartTimeStamp);
              usageInfoMusic.setmIntervalEndTime(mMusicStopTimeStamp);
              usageInfoMusic.setmIntervalDuration(duration);

              mListMusicPlayTimes.add(usageInfoMusic);

              // Insert data to database for previous application.
              mDatabase.insertMusicEntry(usageInfoMusic);
          }
    }
    
    private void initializeMap( HashMap<String, Long> foregroundMap) {
        for (Map.Entry<String, Long> entry : foregroundMap.entrySet()) {
            entry.setValue(0L);
        }
    }

    private void initLocalMapForThread( HashMap<String, Long> foregroundMap){
    	     initializeMap(foregroundMap);
             mPreviousStartTime = mStartTime;
             Log.v (LOG_TAG, "mPreviousStartTime: " + mPreviousStartTime);

             // Initially, when service is started, application name would be Phone Use.
             mPreviousAppName = mContext.getPackageName();
           //  foregroundMap.put(mPreviousAppName, 0L);
             mIsFirstTimeStartForgroundAppService = false;
    }
    

    private final class BackgroundTrackingTask {

        HashMap<String, Long> foregroundMap = new HashMap<String, Long>();
        final class TimerTs extends TimerTask {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                if (mIsFirstTimeStartForgroundAppService) {
                    initLocalMapForThread(foregroundMap);
                }

            	if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                              // If the present application is different from the previous application, update the previous app time.
                if (isTopApplicationchange()) {
                    long time = System.nanoTime();
                    long duration = Utils.getTimeInSecFromNano(time - mPreviousStartTime);
                
                    doHandlingOnApplicationClose();
                    if (duration > 0) {
                        if (foregroundMap.containsKey(mPreviousAppName)) {
                            foregroundMap.put(mPreviousAppName, foregroundMap.get(mPreviousAppName) + Utils.getTimeInSecFromNano(time - mPreviousStartTime));
                        } else {
                            foregroundMap.put(mPreviousAppName, Utils.getTimeInSecFromNano(time - mPreviousStartTime));
                        }
                    }

                    // Update mPreviousAppStartTimeStamp.
                	doHandlingForApplicationStarted();
                	
                }
                mPreviousAppName = mCurrentAppName;
            	}

                // If music is not playing but it was started after tracking started, then update music time.
                if (isNeedToHandleMusicClose()) {
                  doHandleForMusicClose();

                } else if (isMusicPlaying() && mIsMusicPlayingAtStart == false && mIsMusicStarted == false) {
                    // If music has been started after tracking started.
                    mIsMusicStarted = true;
                    mMusicStartTimeStamp = System.currentTimeMillis();
                    mMusicStartTime = System.nanoTime();
                } 
            }
            // If tracking has ended, store last application.
            //doHandlingOnEndthread(foregroundMap);
        }
    }

    private void storeMap(HashMap<String, Long> foregroundMap) {
        
        // For each entry in foreground map, update the entry in mForegroundMap
        for (Map.Entry<String, Long> entry : foregroundMap.entrySet()) {
            String key = entry.getKey();

            if (entry.getValue() > 0L) {
                if (mForegroundActivityMap.containsKey(key)) {
                    mForegroundActivityMap.put(key, mForegroundActivityMap.get(key) + entry.getValue());
                } else {
                    mForegroundActivityMap.put(key, entry.getValue());
                }
            }
        }
        Log.v (LOG_TAG, "mForegroundActivityMap is : " + mForegroundActivityMap);
    }
    
    private void doHandlingOnEndthread(HashMap<String, Long> foregroundMap){
    	if (mIsEndTracking == true) {
            long time = System.nanoTime();
            if (foregroundMap.containsKey(mPreviousAppName)) {
                foregroundMap.put(mPreviousAppName, foregroundMap.get(mPreviousAppName) + Utils.getTimeInSecFromNano(time - mPreviousStartTime));
            } else {
                foregroundMap.put(mPreviousAppName, Utils.getTimeInSecFromNano(time - mPreviousStartTime));
            }
        }
    }

       /**
    /**
     * Starts a new thread to track foreground and background application time.
     */
    public void startThread() {
         mBgTrackingTask = new BackgroundTrackingTask();

         // Start timer.
         mTimer = new Timer();
         mTimer.schedule(mBgTrackingTask.new TimerTs(), 0, 1000);
    }

    /**
     * onUnbind is called only when activity is destroyed (either back-press or kill through task manager).
     */
    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub

        Log.v (LOG_TAG, "Service unbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.v (LOG_TAG, "onDestroy Service");
        
        mIsRunningForegroundAppsThread = false;
        mIsRunningBackgroundApps = false;
        mIsMusicStarted = false;
        mIsEndTracking = true;
        
        long time = System.nanoTime();
        long duration = Utils.getTimeInSecFromNano(time - mPreviousStartTime);

        Log.v (LOG_TAG, "onDestroy -- mForegroundMap in onDestroy is : " + mBgTrackingTask.foregroundMap);
        
        storeMap(mBgTrackingTask.foregroundMap);
        if (duration > 0) {
        if (mForegroundActivityMap.containsKey(mPreviousAppName)) {
            mForegroundActivityMap.put(mPreviousAppName, mForegroundActivityMap.get(mPreviousAppName) + Utils.getTimeInSecFromNano(time - mPreviousStartTime));
        } else {
            mForegroundActivityMap.put(mPreviousAppName, Utils.getTimeInSecFromNano(time - mPreviousStartTime));
            }
        }
        
        Log.v (LOG_TAG, "Destroy Service -- mForeground Map after updation is : " + mForegroundActivityMap);
        
        mEndTimestamp = System.currentTimeMillis();
        
        // Check whether music playing in background while we are stopping
        // tracking.
        if (isMusicPlaying()) {
            Log.v (LOG_TAG, "Music is playing");
          doHandleForMusicClose();
        }

        // As application has changed, we need to dump data to DB.
        doHandlingOnApplicationClose();
        // Get call details for given time-stamps.
        mCallDetailsMap = Utils.getCallDetails(mContext, mStartTimestamp, mEndTimestamp,mCallDetailsMap);
        
        // Unregister receivers.
        //setUpReceivers(false);
        
        // Display list. 
        mDatabase.exportDatabse(PhoneUsageDbHelper.getInstance(mContext).getDatabaseName());
        UsageSharedPrefernceHelper.setServiceRunning(mContext, false);
        super.onDestroy();
        
        // Dump data to xml shared preference.
        UsageSharedPrefernceHelper.updateTodayDataForApps(mContext, mForegroundActivityMap);
        
        if (!mListMusicPlayTimes.isEmpty()) {
            UsageSharedPrefernceHelper.updateTodayDataForMusic(mContext, mListMusicPlayTimes);
        }
        
        // Store current date to preferences.
        UsageSharedPrefernceHelper.setCurrentDate(mContext);
        
        Toast.makeText(mContext, "Phone used for: " + phoneUsedTime() + "seconds",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("isStartingAfterReboot")) {
            boolean isStartFromReboot = intent.getBooleanExtra("isStartingAfterReboot", false);
            if (isStartFromReboot) {
                initThread(false);
            }
        }
        // TODO Auto-generated method stub
        return START_STICKY;
    }
    
    private void initThread(boolean isFirstTime){
    	 mIsRunningForegroundAppsThread = true;
    	 if(isFirstTime)
         mIsFirstTimeStartForgroundAppService = true;
         
         mDatabase = new PhoneUsageDatabase(mContext);
         mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
         mStartTimestamp = System.currentTimeMillis();
         startThread();
         // If music is already playing when tracking started.
         if (isMusicPlaying()) {
             mIsMusicPlayingAtStart = true;
             mIsMusicStarted = true;
             mIsRunningBackgroundApps = true;
             mMusicStartTimeStamp = System.currentTimeMillis();
             mMusicStartTime = System.nanoTime();
             }
         }
    
    
    @Override
    public LocalBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "onBind Call");
               return mBinder;
    }
  }

