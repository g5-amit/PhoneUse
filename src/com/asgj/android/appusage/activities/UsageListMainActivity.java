package com.asgj.android.appusage.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.asgj.android.appusage.R;
import com.asgj.android.appusage.Utility.UsageInfo;
import com.asgj.android.appusage.Utility.UsageSharedPrefernceHelper;
import com.asgj.android.appusage.Utility.Utils;
import com.asgj.android.appusage.database.PhoneUsageDatabase;
import com.asgj.android.appusage.dialogs.MonthViewFragment;
import com.asgj.android.appusage.dialogs.MonthViewFragment.DateInterface;
import com.asgj.android.appusage.service.UsageTrackingService;
import com.asgj.android.appusage.service.UsageTrackingService.LocalBinder;
import com.asgj.android.appusage.service.UsageTrackingService.provideData;

public class UsageListMainActivity extends Activity implements View.OnClickListener, DateInterface {
    private Context mContext;
    private MonthViewFragment startDateFragment;
    private Calendar cal1, cal2;
    private UsageTrackingService mMainService;
    private UsageStatsManager mUsageStatsManager;
    private long mTimeStamp;
    private Handler mHandler;
    private HashMap<String, Long> mDataMap;
    private ArrayList<UsageInfo> mMusicList;
    private List<UsageStats> mQueryUsageStats;
    private boolean mIsCreated = false;
    private boolean mIsBound = false;
    private LocalBinder mBinder;
    private boolean mIsDateInPref = true;
    private PhoneUsageDatabase mDatabase;
    private SlidingTabsBasicFragment<HashMap, ArrayList, ArrayList> mFragment;
    private static final String LOG_TAG = UsageListMainActivity.class.getSimpleName();
    private String[] mShowList = null;
    private TextView mShowByOptionsMain = null;
    private TextView mShowByOptions2 = null;
    private TextView mShowByOptions3 = null;
    private TextView mShowByOptions4 = null;
    private TextView mShowByOptions5 = null;
    private float mNormalYPosition = -1f;
    private float mSecondFabPos = -1f;
    private float mThirdFabPos = -1f;
    private float mForthFabPos = -1f;
    private float mFifthFabPos = -1f;
    private boolean isFabPositionSet = false;
    private int mFabPosParameter = 50;
    private void setFabPositions(){
    	if(isFabPositionSet){
    		return;
    	}
    	DisplayMetrics metrics = getResources().getDisplayMetrics();
    	float height = metrics.heightPixels;
    	mSecondFabPos = mShowByOptions2.getY() - (height / mFabPosParameter * 3);
    	mThirdFabPos = mShowByOptions2.getY() - (height / mFabPosParameter * 6);
    	mForthFabPos = mShowByOptions2.getY() - (height / mFabPosParameter * 9);
    	mFifthFabPos = mShowByOptions2.getY() - (height / mFabPosParameter * 12);
    	mNormalYPosition = mShowByOptions2.getY();
    	isFabPositionSet = true;
    }

    // UI elements.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usage_list_main_layout);
        getActionBar().setDisplayShowHomeEnabled(false);
        mContext = this;
        mShowList = new String[]{getString(R.string.string_Today),
        		getString(R.string.string_Weekly),getString(R.string.string_Monthly)
        		,getString(R.string.string_Yearly),getString(R.string.string_Custom)};
        mDatabase = new PhoneUsageDatabase(mContext);
        initListFragment();
        mIsCreated = true;

        mHandler = new Handler();

        initFabTextView();
        
        UsageSharedPrefernceHelper.setShowByUsage(mContext, mContext.getString(R.string.string_Today));
        
        // Check whether binding is needed.
        if (UsageSharedPrefernceHelper.isServiceRunning(mContext)) {
            Log.v(LOG_TAG, "Service restrart from activity");
            Intent startServiceIntent = new Intent();
            startServiceIntent.setClass(this, UsageTrackingService.class);
            startServiceIntent.setComponent(new ComponentName(this, UsageTrackingService.class));
            //startService(startServiceIntent);
            bindService(startServiceIntent, mConnection, 0);
        }
    }
    
    private void initFabTextView(){
    	mShowByOptionsMain = (TextView)findViewById(R.id.showByOptions1);
    	mShowByOptions2 = (TextView)findViewById(R.id.showByOptions2);
    	mShowByOptions3 = (TextView)findViewById(R.id.showByOptions3);
    	mShowByOptions4 = (TextView)findViewById(R.id.showByOptions4);
    	mShowByOptions5 = (TextView)findViewById(R.id.showByOptions5);
        
    	mShowByOptionsMain.setOnClickListener(this);
    	mShowByOptions2.setOnClickListener(this);
    	mShowByOptions3.setOnClickListener(this);
    	mShowByOptions4.setOnClickListener(this);
    	mShowByOptions5.setOnClickListener(this);
    	if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
    		mShowByOptionsMain.setVisibility(View.GONE);
    	}else{
    		mShowByOptionsMain.setText(UsageSharedPrefernceHelper.getShowByType(mContext));
    		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)mShowByOptionsMain.getLayoutParams();
    		DisplayMetrics metrics = getResources().getDisplayMetrics();
        	float height = metrics.heightPixels;
        	params.bottomMargin = (int)(height / mFabPosParameter);
        	params.rightMargin = (int)(height / mFabPosParameter);
        	mShowByOptionsMain.setLayoutParams(params);
        	mShowByOptions2.setLayoutParams(params);
        	mShowByOptions3.setLayoutParams(params);
        	mShowByOptions4.setLayoutParams(params);
        	mShowByOptions5.setLayoutParams(params);
    		mShowByOptionsMain.setElevation(20f);
    		mShowByOptions2.setElevation(20f);
    		mShowByOptions3.setElevation(20f);
    		mShowByOptions4.setElevation(20f);
    		mShowByOptions5.setElevation(20f);
        }
    }

    private void initListFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        mFragment = new SlidingTabsBasicFragment<HashMap, ArrayList, ArrayList>();
        transaction.replace(R.id.usage_list_main_fragment, mFragment);
        transaction.commit();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            Log.v(LOG_TAG, "Service disconnected");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMainService = ((LocalBinder) service).getInstance();
            mBinder = (LocalBinder) service;
            mIsBound = true;

            if (mIsCreated) {
                displayData();
                mFragment.setmMusicData(mMainService.getCurrentDataForMusic());
            }
            Log.v(LOG_TAG, "Service connected, mMainService is: " + mMainService);
        }
    };
    
    /**
     * Displays data as per different scenarios and preferences.
     */
    public void displayData() {

        /**
         * 2 cases, either service is running or stopped.
         * In case of stopped, only static data is needed.
         * In case of running, we need to add dynamic data (Custom's an exception if end date is lesser than today).
         */

        // Fetch static data for today.
        mDataMap = new HashMap<>();
        mMusicList = new ArrayList<>();

        // Case 1.
        if (!UsageSharedPrefernceHelper.isServiceRunning(mContext)) {

            // Check whether custom and end day not today.
            if (UsageSharedPrefernceHelper.getShowByType(mContext).equals(
                    mContext.getString(R.string.string_Custom))
                    && Utils.compareDates(cal2, Calendar.getInstance()) != 0) {

                mDataMap = mDatabase.getApplicationEntryForMentionedTimeBeforeToday(mContext,
                        cal1, cal2);
                mMusicList = mDatabase.getMusicEntryForMentionedTimeBeforeToday(mContext,
                        cal1, cal2);
            } else {

                switch (UsageSharedPrefernceHelper.getShowByType(mContext)) {
                case "Today":
                    mDataMap = UsageSharedPrefernceHelper.getAllKeyValuePairsApp(mContext);
                    mMusicList = UsageSharedPrefernceHelper.getTotalInfoOfMusic(mContext);
                    break;
                case "Weekly":
                case "Monthly":
                case "Yearly":
                    mDataMap = mDatabase.getApplicationEntryForMentionedTimeBeforeToday(mContext,
                            UsageSharedPrefernceHelper.getCalendarByShowType(mContext),
                            Calendar.getInstance());
                    mMusicList = mDatabase.getMusicEntryForMentionedTimeBeforeToday(mContext,
                            UsageSharedPrefernceHelper.getCalendarByShowType(mContext),
                            Calendar.getInstance());
                    break;
                case "Custom":
                    mDataMap = mDatabase.getApplicationEntryForMentionedTimeBeforeToday(mContext,
                            cal1, Calendar.getInstance());
                    mMusicList = mDatabase.getMusicEntryForMentionedTimeBeforeToday(mContext,
                            cal1, Calendar.getInstance());
                    break;
                default:
                    break;
                }
            }
        } else {
            // Check whether custom and end day not today.
            if (UsageSharedPrefernceHelper.getShowByType(mContext).equals(
                    mContext.getString(R.string.string_Custom))
                    && Utils.compareDates(cal2, Calendar.getInstance()) != 0) {

                mDataMap = mDatabase.getApplicationEntryForMentionedTimeBeforeToday(mContext,
                        cal1, cal2);
                mMusicList = mDatabase.getMusicEntryForMentionedTimeBeforeToday(mContext,
                        cal1, cal2);
            } else {
                if (mMainService != null) {
                    if (!UsageSharedPrefernceHelper.getShowByType(mContext).equals(
                            mContext.getString(R.string.string_Custom))) {
                        mDataMap = mMainService.getCurrentMap(UsageSharedPrefernceHelper
                                .getCalendarByShowType(mContext));
                        mMusicList = mMainService.getCurrentDataForMusic();
                    } else {
                        mDataMap = mMainService.getCurrentMap(cal1);
                        mMusicList = mMainService.getCurrentDataForMusic();
                    }

                    // Need data from service, fire off a broadcast.
                    Intent getDataIntent = new Intent();
                    getDataIntent.setAction("com.android.asgj.appusage.action.DATA_PROVIDE");
                    sendBroadcast(getDataIntent);

                    mBinder.setInterface(new provideData() {

                        @Override
                        public void provideMap(HashMap<String, Long> map) {
                            // TODO Auto-generated method stub
                            Log.v ("gaurav", "Interface call");
                            for (Map.Entry<String, Long> dataEntry : map.entrySet()) {
                                String key = dataEntry.getKey();
                                Log.v ("gaurav", "Data map before entry: " + mDataMap);
                                if (mDataMap.containsKey(key)) {
                                    mDataMap.put(key, dataEntry.getValue() + mDataMap.get(key));
                                } else {
                                    mDataMap.put(key, dataEntry.getValue());
                                }
                            }
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // TODO Auto-generated method stub
                                    mFragment.setmUsageAppData(mDataMap);
                                }
                            });
                        }
                    });
                }
            }
        }
        mFragment.setmUsageAppData(mDataMap);
        mFragment.setmMusicData(mMusicList);
    }
    

    /**
     * onResume method to dynamically show data as tracking progresses.
     */
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        Log.v(LOG_TAG, "mainService onResume is: " + mMainService);

        // IF service not running, show data from xml.
        if (!UsageSharedPrefernceHelper.isServiceRunning(mContext)) {
           
            if (mIsDateInPref == true) {
                
                // Fetch date from preferences and compare.
                Date presentDate = new Date(System.currentTimeMillis());
                Date prefsDate = new Date(UsageSharedPrefernceHelper.getDateStoredInPref(mContext));

                Calendar c1 = Calendar.getInstance();
                c1.setTime(presentDate);
                
                Calendar c2 = Calendar.getInstance();
                c2.setTime(prefsDate);

                // Compare with present time.
                if (Utils.compareDates(c1, c2) == 1) {
                    // Clear preference.
                    UsageSharedPrefernceHelper.clearPreference(mContext);
                }
                mIsDateInPref = false;

                // Store timestamp to check if day has changed.
                mTimeStamp = System.currentTimeMillis();
            } else {
                Date presentDate = new Date(System.currentTimeMillis());
                Date prevDate = new Date(mTimeStamp);

                Calendar c1 = Calendar.getInstance();
                c1.setTime(presentDate);

                Calendar c2 = Calendar.getInstance();
                c2.setTime(prevDate);

                if (Utils.compareDates(c1, c2) == 1) {
                    // Clear prefs.
                    UsageSharedPrefernceHelper.clearPreference(mContext);
                }
                
                mTimeStamp = System.currentTimeMillis();
            }
        }
        
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mFragment.setmUsageAppData(Utils.getAppUsageFromLAndroidDb(mContext,UsageSharedPrefernceHelper.getShowByType(mContext),0,0));
        } else {
            displayData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.list_activity_menu, menu);

        if (UsageSharedPrefernceHelper.isServiceRunning(mContext)) {
            MenuItem menuItem = (MenuItem) menu.findItem(R.id.action_start);
            menuItem.setTitle(getString(R.string.string_stop));
        }
        
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
        	 MenuItem menuItem = (MenuItem) menu.findItem(R.id.action_showBy);
        	 menuItem.setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        Log.v(LOG_TAG, "onDestroy activity");

        // Unbind from service to prevent service connection leak.
        if (mIsBound) {
            unbindService(mConnection);
        }
        
        UsageSharedPrefernceHelper.setCurrentDate(mContext);
        super.onDestroy();
    }

    private void startTrackingService() {
            UsageSharedPrefernceHelper.setServiceRunning(mContext, true);

            // Here you bind to the service.
            Intent startServiceIntent = new Intent();
            startServiceIntent.setClass(this, UsageTrackingService.class);
            startServiceIntent.setComponent(new ComponentName(this, UsageTrackingService.class));
            startService(startServiceIntent);
            bindService(startServiceIntent, mConnection, 0);

            // Home Screen intent.
            Intent intentGoToHomeScreen = new Intent();
            intentGoToHomeScreen.setAction("android.intent.action.MAIN");
            intentGoToHomeScreen.addCategory("android.intent.category.HOME");

            // Test toast.
            Toast.makeText(mContext, "Your phone usage is being calculated now!", Toast.LENGTH_LONG)
                    .show();
            startActivity(intentGoToHomeScreen);

    }

    private void stopTrackingService() {
        // Destroy the service, as no longer needed.
        Intent stopServiceIntent = new Intent();
        stopServiceIntent.setClass(this, UsageTrackingService.class);
        stopServiceIntent.setComponent(new ComponentName(this, UsageTrackingService.class));
        stopService(stopServiceIntent);

       UsageSharedPrefernceHelper.setServiceRunning(mContext, false);

        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mUsageStatsManager = (UsageStatsManager) mContext.getSystemService("usagestats");

            SimpleDateFormat sdf1 = new SimpleDateFormat("MMM dd,yyyy HH:mm");

            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);

            Log.v(LOG_TAG, "CALENDAR OBJECT: " + c);
            TimeZone currentTimeZone = TimeZone.getDefault();
            int offset = currentTimeZone.getRawOffset();
            Date resultdate2 = new Date(offset);
            Log.v(LOG_TAG,
                    "Current timezone: " + currentTimeZone + " and offset : "
                            + sdf1.format(resultdate2));

            long time2 = System.currentTimeMillis();

            // Need to query usage stats for present day according to timezone.
            mQueryUsageStats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                    c.getTimeInMillis(), time2);

            Date resultdate1 = new Date(c.getTimeInMillis());

            Log.v(LOG_TAG, "Inside stop" + sdf1.format(resultdate1));

            SimpleDateFormat sdf11 = new SimpleDateFormat("MMM dd,yyyy HH:mm");
            Date resultdate11 = new Date(time2);
            Log.v(LOG_TAG, sdf11.format(resultdate11));
        }

        Log.v(LOG_TAG, "Query stats:" + mQueryUsageStats);
        // Query stats:
        if (mQueryUsageStats != null) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
            for (UsageStats usageStats : mQueryUsageStats) {
                mySortedMap.put(usageStats.getTotalTimeInForeground(), usageStats);
            }
            if (mySortedMap != null && !mySortedMap.isEmpty()) {
                for (Map.Entry<Long, UsageStats> entry : mySortedMap.entrySet()) {
                    Log.v(LOG_TAG, "Key : " + entry.getKey());
                    Log.v(LOG_TAG, "Value : " + entry.getValue());
                    String topPackageName = entry.getValue().getPackageName();
                    Log.v(LOG_TAG, "Package name: " + topPackageName);
                    Log.v(LOG_TAG, "Time spent in foreground: "
                            + entry.getValue().getTotalTimeInForeground() / 1000);
                    // Log.v (LOG_TAG, "First timestamp")

                    SimpleDateFormat sdf11 = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                    Date resultdate11 = new Date(entry.getValue().getFirstTimeStamp());
                    Log.v(LOG_TAG, "First timestamp: " + sdf11.format(resultdate11));

                    Date resultdate111 = new Date(entry.getValue().getLastTimeStamp());
                    Log.v(LOG_TAG, "Last timestamp: " + sdf11.format(resultdate111));
                }

            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
        case R.id.action_start:
            if (!UsageSharedPrefernceHelper.isServiceRunning(this)) {
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    if (!Utils.isPermissionGranted(this)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                .setTitle(R.string.string_error_title)
                                .setMessage(R.string.string_error_msg)
                                .setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(
                                                        Settings.ACTION_USAGE_ACCESS_SETTINGS);
                                                startActivity(intent);
                                            }
                                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
            	}
            	
                startTrackingService();
                item.setTitle(getString(R.string.string_stop));
                UsageSharedPrefernceHelper.setServiceRunning(mContext, true);

            } else {
                stopTrackingService();
                item.setTitle(getString(R.string.string_start));
                UsageSharedPrefernceHelper.setServiceRunning(mContext, false);
                finish();
            }

            break;
        case R.id.action_showBy:
            AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(
                    getString(R.string.string_showBy)).setAdapter(
                    new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mShowList),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            
                            if (!mShowList[which].equals(mContext.getString(R.string.string_Custom))) {
                                UsageSharedPrefernceHelper.setShowByUsage(getBaseContext(),
                                        mShowList[which]);
                                displayData();
                                return;
                            }

                            startDateFragment = new MonthViewFragment();
                            startDateFragment.show(getFragmentManager(), "startMonthViewPicker");
                        }
                        
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
            break;
        case R.id.action_setting:
        	Intent intent = new Intent(this, SettingActivity.class);
        	startActivity(intent);
        	break;

        }
        return super.onOptionsItemSelected(item);
    }
    private void showFabOptions(){
		mShowByOptions2.animate().y(mSecondFabPos).setDuration(1000).setListener(new ShowAnimationListner()).start();
		mShowByOptions3.animate().y(mThirdFabPos).setDuration(1000).setListener(new ShowAnimationListner()).start();;
		mShowByOptions4.animate().y(mForthFabPos).setDuration(1000).setListener(new ShowAnimationListner()).start();
		mShowByOptions5.animate().y(mFifthFabPos).setDuration(1000).setListener(new ShowAnimationListner()).start();
    }
    
    private void hideFabOption(){
		mShowByOptions2.animate().y(mNormalYPosition).setDuration(1000).setListener(new HideAnimationListner()).start();
		mShowByOptions3.animate().y(mNormalYPosition).setDuration(1000).setListener(new HideAnimationListner()).start();
		mShowByOptions4.animate().y(mNormalYPosition).setDuration(1000).setListener(new HideAnimationListner()).start();
		mShowByOptions5.animate().y(mNormalYPosition).setDuration(1000).setListener(new HideAnimationListner()).start();
    	
    }
    
 class ShowAnimationListner implements AnimatorListener {
		
		@Override
		public void onAnimationStart(Animator animation) {
			mShowByOptionsMain.setClickable(false);
			mShowByOptions2.setClickable(false);
			mShowByOptions3.setClickable(false);
			mShowByOptions4.setClickable(false);
			mShowByOptions5.setClickable(false);
			mShowByOptions2.setVisibility(View.VISIBLE);
			mShowByOptions3.setVisibility(View.VISIBLE);
			mShowByOptions4.setVisibility(View.VISIBLE);
			mShowByOptions5.setVisibility(View.VISIBLE);
		}
		
		@Override
		public void onAnimationRepeat(Animator animation) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onAnimationEnd(Animator animation) {
			mShowByOptionsMain.setClickable(true);
			mShowByOptions2.setClickable(true);
			mShowByOptions3.setClickable(true);
			mShowByOptions4.setClickable(true);
			mShowByOptions5.setClickable(true);
			
		}
		
		@Override
		public void onAnimationCancel(Animator animation) {
			// TODO Auto-generated method stub
			
		}
	}
    
    class HideAnimationListner implements AnimatorListener {
		
		@Override
		public void onAnimationStart(Animator animation) {
			mShowByOptionsMain.setClickable(false);
			mShowByOptions2.setClickable(false);
			mShowByOptions3.setClickable(false);
			mShowByOptions4.setClickable(false);
			mShowByOptions5.setClickable(false);
			
		}
		
		@Override
		public void onAnimationRepeat(Animator animation) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onAnimationEnd(Animator animation) {
			
			mShowByOptions2.setVisibility(View.INVISIBLE);
			mShowByOptions3.setVisibility(View.INVISIBLE);
			mShowByOptions4.setVisibility(View.INVISIBLE);
			mShowByOptions5.setVisibility(View.INVISIBLE);
			mShowByOptionsMain.setClickable(true);
			mShowByOptions2.setClickable(true);
			mShowByOptions3.setClickable(true);
			mShowByOptions4.setClickable(true);
			mShowByOptions5.setClickable(true);
			
		}
		
		@Override
		public void onAnimationCancel(Animator animation) {
			// TODO Auto-generated method stub
			
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.showByOptions1:
			setFabPositions();
			if(mShowByOptions2.getVisibility() == View.INVISIBLE){
			    showFabOptions();
			    mShowByOptionsMain.setText(mShowList[0]);
			}else{
				UsageSharedPrefernceHelper.setShowByUsage(this, mShowList[0]);
				hideFabOption();
				mShowByOptionsMain.setText(mShowList[0]);
				mFragment.setmUsageAppData(Utils.getAppUsageFromLAndroidDb(mContext,UsageSharedPrefernceHelper.getShowByType(mContext),0,0));
			}
			break;
		case R.id.showByOptions2:
			UsageSharedPrefernceHelper.setShowByUsage(this, mShowList[1]);
			hideFabOption();
			mShowByOptionsMain.setText(mShowList[1]);
			 mFragment.setmUsageAppData(Utils.getAppUsageFromLAndroidDb(mContext,UsageSharedPrefernceHelper.getShowByType(mContext),0,0));
			break;
		case R.id.showByOptions3:
			UsageSharedPrefernceHelper.setShowByUsage(this, mShowList[2]);
			hideFabOption();
			mShowByOptionsMain.setText(mShowList[2]);
			 mFragment.setmUsageAppData(Utils.getAppUsageFromLAndroidDb(mContext,UsageSharedPrefernceHelper.getShowByType(mContext),0,0));
			break;
		case R.id.showByOptions4:
			UsageSharedPrefernceHelper.setShowByUsage(this, mShowList[3]);
			hideFabOption();
			mShowByOptionsMain.setText(mShowList[3]);
			mFragment.setmUsageAppData(Utils.getAppUsageFromLAndroidDb(mContext,UsageSharedPrefernceHelper.getShowByType(mContext),0,0));
			break;
		case R.id.showByOptions5:
			startDateFragment = new MonthViewFragment();
            startDateFragment.show(getFragmentManager(), "startMonthViewPicker");
			hideFabOption();
			break;
		}
		
	}

    @Override
    public void onDateSetComplete(Calendar startCalendar, Calendar endCalendar) {
        // TODO Auto-generated method stub

        this.cal1 = startCalendar;
        this.cal2 = endCalendar;
        // Set in preference only after date from both pickers have been validated.
        UsageSharedPrefernceHelper.setShowByUsage(mContext,
                mContext.getString(R.string.string_Custom));
        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            displayData();
        }else{
        	mShowByOptionsMain.setText(mShowList[4]);
        	mFragment.setmUsageAppData(Utils.getAppUsageFromLAndroidDb(mContext,UsageSharedPrefernceHelper.getShowByType(mContext),
        			startCalendar.getTimeInMillis(),endCalendar.getTimeInMillis()));
        }

    }
}
