package com.asgj.android.appusage.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.asgj.android.appusage.R;
import com.asgj.android.appusage.Utility.UsageInfo;
import com.asgj.android.appusage.Utility.Utils;
import com.asgj.android.appusage.database.PhoneUsageDbHelper.Columns;
import com.asgj.android.appusage.database.PhoneUsageDbHelper.Table;

public class PhoneUsageDatabase {

	public static final String MUSIC_PACKAGE_NAME = "music_package";
	
	private Context mContext;
	PhoneUsageDbHelper mDbHelper = null;
	SQLiteDatabase mDatabase = null;

	public PhoneUsageDatabase(Context context) {
		mContext = context;
		mDbHelper = PhoneUsageDbHelper.getInstance(mContext);
		mDatabase = mDbHelper.getWritableDatabase();
	}

	
	public void exportDatabse(String databaseName) {
		try {
			File sd = Environment.getExternalStorageDirectory();
			File data = Environment.getDataDirectory();

			if (sd.canWrite()) {
				String currentDBPath = "//data//" + "com.asgj.android.appusage"
						+ "//databases//" + databaseName + "";
				String backupDBPath = "backupname.db";
				File currentDB = new File(data, currentDBPath);
				File backupDB = new File(sd, backupDBPath);

				if (currentDB.exists()) {
					FileInputStream fis = new FileInputStream(currentDB);
					FileChannel src = fis.getChannel();
					FileOutputStream fos = new FileOutputStream(backupDB);
					FileChannel dst = fos.getChannel();
					dst.transferFrom(src, 0, src.size());
					src.close();
					dst.close();
					fis.close();
					fos.close();
				}
			}
		} catch (Exception e) {

		}
	}

	public void insertMusicEntry(UsageInfo mValues) {
		insertApplicationEntry(MUSIC_PACKAGE_NAME, mValues);
	}

	public ArrayList<UsageInfo> getMusicEntryInInterval(
			String date, long start_time,
			long end_time, boolean isShowExtendDurationIntervals) {
		return getApplicationEntryInInterval(MUSIC_PACKAGE_NAME,
				date, start_time, end_time, isShowExtendDurationIntervals);
	}

	public ArrayList<UsageInfo> getApplicationEntryInInterval(
            String packageName, String date,
            long start_time, long end_time,
            boolean isShowExtendDurationIntervals) {
        String selection = Columns.COLUMN_APP_NAME + "= '" + packageName
                + "'  AND " + Columns.COLUMN_DATE + "= '" + date + "' AND "
                + Columns.COLUMN_START_INTERVAL_TIME + ">" + start_time;
        if (!isShowExtendDurationIntervals)
            selection = selection + " AND " + Columns.COLUMN_END_INTERVAL_TIME
            + "<" + end_time;
        Cursor cursor = mDatabase.query(Table.TABLE_NAME, null, selection, null,
                null, null, null);
        ArrayList<UsageInfo> mInfoList = new ArrayList<UsageInfo>();
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                UsageInfo info = new UsageInfo();
                info.setmIntervalStartTime(cursor.getLong(cursor
                        .getColumnIndex(Columns.COLUMN_START_INTERVAL_TIME)));
                info.setmIntervalEndTime(cursor.getLong(cursor
                        .getColumnIndex(Columns.COLUMN_END_INTERVAL_TIME)));
                info.setmIntervalDuration(cursor.getLong(cursor
                        .getColumnIndex(Columns.COLUMN_INTERVAL_DURATION)));
                mInfoList.add(info);
            } while (cursor.moveToNext());
        }
        return mInfoList;

    }
    public void insertApplicationEntry(String pkgname, UsageInfo mValues) {
        ContentValues cv = new ContentValues();
        cv.put(Columns.COLUMN_APP_NAME, pkgname);
        String date = Utils.getDateFromMiliSeconds(mValues.getmIntervalStartTime());
        cv.put(Columns.COLUMN_DATE, date);
        cv.put(Columns.COLUMN_START_INTERVAL_TIME, mValues.getmIntervalStartTime());
        cv.put(Columns.COLUMN_END_INTERVAL_TIME, mValues.getmIntervalEndTime());
        cv.put(Columns.COLUMN_INTERVAL_DURATION, mValues.getmIntervalDuration());
        mDatabase.insert(Table.TABLE_NAME, null, cv);
    }

    public HashMap<String, Long> getApplicationEntryForMentionedTimeBeforeToday(Context context,
            Calendar startCalendar, Calendar endCalendar) {
        // long currentTime = System.currentTimeMillis();

        String startDate = Utils.getDateFromMiliSeconds(startCalendar.getTimeInMillis());
        String endDate = Utils.getDateFromMiliSeconds(endCalendar.getTimeInMillis());
        
        String selection = Columns.COLUMN_APP_NAME + "<> '" + MUSIC_PACKAGE_NAME + "' AND "
                + Columns.COLUMN_DATE + " BETWEEN '" + startDate + "'  AND '" + endDate + "'";

        Cursor cursor = mDatabase.query(Table.TABLE_NAME, null, selection, null, null, null, null);
        HashMap<String, Long> map = new HashMap<String, Long>();

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                String pkgName = cursor.getString(cursor.getColumnIndex(Columns.COLUMN_APP_NAME));
                long duration = cursor.getLong(cursor
                        .getColumnIndex(Columns.COLUMN_INTERVAL_DURATION));
                if (map.containsKey(pkgName)) {
                    duration = duration + map.get(pkgName);
                }
                map.put(pkgName, duration);
            } while (cursor.moveToNext());
        }
        return map;

    }

    public ArrayList<UsageInfo> getMusicEntryForMentionedTimeBeforeToday(Context context,
            Calendar startCalendar, Calendar endCalendar) {
        // long currentTime = System.currentTimeMillis();
        // String yesterdayDate = Utils.getDateFromMiliSeconds(currentTime -
        // (24*3600*1000));
        // String startDate = null;

        Calendar calendarStart = Calendar.getInstance();
        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.add(Calendar.DATE, -1);

        String startDate = Utils.getDateFromMiliSeconds(startCalendar.getTimeInMillis());
        String endDate = Utils.getDateFromMiliSeconds(endCalendar.getTimeInMillis());

        String selection = Columns.COLUMN_APP_NAME + "= '" + MUSIC_PACKAGE_NAME + "' AND "
                + Columns.COLUMN_DATE + " BETWEEN '" + startDate + "'  AND '" + endDate + "'";

        Cursor cursor = mDatabase.query(Table.TABLE_NAME, null, selection, null, null, null, null);
        ArrayList<UsageInfo> mInfoList = new ArrayList<UsageInfo>();
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                UsageInfo info = new UsageInfo();
                info.setmIntervalStartTime(cursor.getLong(cursor
                        .getColumnIndex(Columns.COLUMN_START_INTERVAL_TIME)));
                info.setmIntervalEndTime(cursor.getLong(cursor
                        .getColumnIndex(Columns.COLUMN_END_INTERVAL_TIME)));
                info.setmIntervalDuration(cursor.getLong(cursor
                        .getColumnIndex(Columns.COLUMN_INTERVAL_DURATION)));
                mInfoList.add(info);
            } while (cursor.moveToNext());
        }
        return mInfoList;

	}

	public long getTotalDurationOfAppInInternal(String packageName, String date, long start_time, long end_time,
			boolean isShowExtendDurationIntervals) {
		String selection = Columns.COLUMN_APP_NAME + "=" + packageName
				+ " AND " + Columns.COLUMN_DATE + "=" + date + " AND "
				+ Columns.COLUMN_START_INTERVAL_TIME + ">" + start_time;
		if (!isShowExtendDurationIntervals)
			selection = selection + " AND " + Columns.COLUMN_END_INTERVAL_TIME
					+ "<" + end_time;
		Cursor cursor = mDatabase.query(Table.TABLE_NAME, null, selection, null,
				null, null, null);
		long total_time = 0;
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				total_time = total_time
						+ cursor.getLong(cursor
								.getColumnIndex(Columns.COLUMN_INTERVAL_DURATION));
			} while (cursor.moveToNext());
		}
		return total_time;

	}
	public long getTotalDurationOfAllAppsByDate(String date){
		long time_start = Utils.getMiliSecFromDate(date);
		long time_end = time_start + (24 * 3600 * 1000);
		String selection = Columns.COLUMN_DATE + "=" + date + " AND "
				+ Columns.COLUMN_START_INTERVAL_TIME + ">" + time_start +
				 " AND " + Columns.COLUMN_START_INTERVAL_TIME
					+ "<" + time_end;
		Cursor cursor = mDatabase.query(Table.TABLE_NAME, null, selection,
				null, null, null, null);
		long total_time = 0;
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				total_time = total_time
						+ cursor.getLong(cursor
								.getColumnIndex(Columns.COLUMN_INTERVAL_DURATION));
			} while (cursor.moveToNext());
		}
		return total_time;

	}

	public long getTotalDurationOfApplicationOfAppByDate(String packageName,
			String date) {
		long time_start = Utils.getMiliSecFromDate(date);
		long time_end = time_start + (24 * 3600 * 1000);
		return getTotalDurationOfAppInInternal(packageName, date, time_start,
				time_end, false);
	}
	
	public ArrayList<String> getAllPackagesUsedByDate(String date){
		long time_start = Utils.getMiliSecFromDate(date);
		long time_end = time_start + (24 * 3600 * 1000);
		String selection = Columns.COLUMN_DATE + "=" + date + " AND "
				+ Columns.COLUMN_START_INTERVAL_TIME + ">" + time_start +
				 " AND " + Columns.COLUMN_START_INTERVAL_TIME
					+ "<" + time_end;
		Cursor cursor = mDatabase.query(Table.TABLE_NAME, null, selection,
				null, null, null, null);
		ArrayList<String> mPackList = new ArrayList<>();
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				mPackList.add(cursor.getString(cursor
								.getColumnIndex(Columns.COLUMN_APP_NAME)));
			} while (cursor.moveToNext());
		}
		return mPackList;
	}
}
