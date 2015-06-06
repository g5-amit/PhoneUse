package com.asgj.android.appusage.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

//Class for creating and upgrading DB.
public class PhoneUsageDbHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "PhoneUse.db";
	private static final int DATABASE_VERSION = 1;
	private static PhoneUsageDbHelper instance = null;

	public PhoneUsageDbHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	// Get instance of this class
	public static synchronized PhoneUsageDbHelper getInstance(Context context) {
		// Use lazy instantiation.
		if (instance == null) {
			// Instance is null, hence create new one.
			instance = new PhoneUsageDbHelper(context);
		}
		return instance;
	}

	private PhoneUsageDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	class Columns {
		public static final String COLUMN_APP_NAME = "APP_NAME";
		public static final String COLUMN_DATE = "date_interval";
		public static final String COLUMN_START_INTERVAL_TIME = "start_time_interval";
		public static final String COLUMN_END_INTERVAL_TIME = "end_time_interval";
		public static final String COLUMN_INTERVAL_DURATION = "interval_duration";

	}

	class Table {
		// Table Names.
		public static final String TABLE_NAME = "AppUsage";
	}

	class Query {
		public static final String CREATE_TABLE_APPUSAGE_QUERY = "create table "
				+ Table.TABLE_NAME
				+ "("
				+ Columns.COLUMN_APP_NAME
				+ " text not null, "
				+ Columns.COLUMN_DATE
				+ " text not null,"
				+ Columns.COLUMN_START_INTERVAL_TIME
				+ " INTEGER,"
				+ Columns.COLUMN_END_INTERVAL_TIME
				+ " INTEGER,"
				+ Columns.COLUMN_INTERVAL_DURATION + " INTEGER);";
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		// mContext = this;
		// Create Tables.
		db.execSQL(Query.CREATE_TABLE_APPUSAGE_QUERY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("DROP TABLE IF EXISTS " + Table.TABLE_NAME);
		// create new tables
		onCreate(db);
	}

}
