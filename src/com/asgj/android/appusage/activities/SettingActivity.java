package com.asgj.android.appusage.activities;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class SettingActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		if(true){
		     
		}
		getActionBar().setDisplayShowHomeEnabled(false);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		 FragmentManager mFragmentManager = getFragmentManager();
         FragmentTransaction mFragmentTransaction = mFragmentManager
                                 .beginTransaction();
         SettingPreferenceFragment mPrefsFragment = new SettingPreferenceFragment();
         mFragmentTransaction.replace(android.R.id.content, mPrefsFragment);
         mFragmentTransaction.commit();
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item != null && item.getItemId() == android.R.id.home){
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
}
