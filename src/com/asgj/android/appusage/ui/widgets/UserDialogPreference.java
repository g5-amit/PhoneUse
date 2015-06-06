package com.asgj.android.appusage.ui.widgets;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

import com.asgj.android.appusage.R;
import com.asgj.android.appusage.Utility.PackageInfo;
import com.asgj.android.appusage.Utility.UsageSharedPrefernceHelper;


public class UserDialogPreference extends DialogPreference implements
		DialogInterface.OnClickListener {

	Context mContext = null;
	ArrayList<PackageInfo> mInfo;

	PreferenceListAdapter mAdapter = null;

	private void initPackageList() {
		List<android.content.pm.PackageInfo> minfo = mContext
				.getPackageManager().getInstalledPackages(
						PackageManager.GET_META_DATA);
		mInfo = new ArrayList<PackageInfo>();
		
		for (android.content.pm.PackageInfo info : minfo) {
			PackageInfo infoItem = new PackageInfo();
			infoItem.setmApplicationName(info.packageName);
			mInfo.add(infoItem);
		}
		mAdapter = new PreferenceListAdapter(mInfo, mContext);

	}

	public UserDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		this.setTitle(R.string.string_select_packages_pref_title);
		this.setSummary(R.string.string_select_packages_dialog_title);
	}

	@Override
	protected void onBindView(View view) {
		
		super.onBindView(view);
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		initPackageList();
		builder.setAdapter(mAdapter, this);
		builder.setTitle(R.string.string_select_packages_dialog_title);
		builder.setPositiveButton(android.R.string.ok, this);
		builder.setNegativeButton(android.R.string.cancel, this);
		super.onPrepareDialogBuilder(builder);
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			ArrayList<PackageInfo> selectedPackages = mAdapter.getSelectedPackages();
			for (PackageInfo pkg : selectedPackages) {
				if(pkg.isChecked()){
				UsageSharedPrefernceHelper.setApplicationForTracking(mContext,
						pkg.getmApplicationName());
				//TODO :: please save time also.
				}
			}
		}
		super.onClick(dialog, which);
	}

}
