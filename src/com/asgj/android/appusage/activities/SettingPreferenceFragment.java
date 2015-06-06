package com.asgj.android.appusage.activities;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.asgj.android.appusage.R;

public class SettingPreferenceFragment extends PreferenceFragment{
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefernces);
	}

}
