package com.asgj.android.appusage.ui.widgets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.asgj.android.appusage.R;
import com.asgj.android.appusage.Utility.PackageInfo;
import com.asgj.android.appusage.Utility.UsageSharedPrefernceHelper;
import com.asgj.android.appusage.Utility.Utils;

public class PreferenceListAdapter extends BaseAdapter implements
		View.OnClickListener,OnSeekBarChangeListener {

	private ArrayList<PackageInfo> mPackageList = null;
	private Context mContext = null;
	private int mSelectedCount = 0;
	private static int MAXIMUN_APPLICATIONS = 5;

	PreferenceListAdapter(ArrayList<PackageInfo> packageList, Context context) {
		mPackageList = packageList;
		mContext = context;
		Set<String> alreadySelectedList = UsageSharedPrefernceHelper
				.getSelectedApplicationForTracking(mContext);
		//TODO : need to get time also from prefernce to show on seekbar.
		if (alreadySelectedList != null && alreadySelectedList.size() > 0) {
			mSelectedCount = alreadySelectedList.size();
			String[] selectionlist = new String[mSelectedCount];
			selectionlist = alreadySelectedList.toArray(selectionlist);
			
			for (int j =0; j< mSelectedCount ; j++) {
				for(int i = 0; i< mPackageList.size() ; i++){
					if(mPackageList.get(i).getmApplicationName().equals(selectionlist[j])){
						mPackageList.get(i).setChecked(true);
					}
				}
			}
			this.notifyDataSetChanged();
		}

	}

	public ArrayList<PackageInfo> getSelectedPackages() {
		return mPackageList;
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mPackageList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			convertView = inflater.inflate(R.layout.prefernce_list_item_layout,
					null);
		}

		CheckBox checkbox = (CheckBox) convertView
				.findViewById(R.id.checkBox_package);
		checkbox.setTag(position);
		checkbox.setChecked(mPackageList.get(position).isChecked());
		SeekBar seekbar = (SeekBar)convertView.findViewById(R.id.seekBar1);
		seekbar.setOnSeekBarChangeListener(this);
		seekbar.setTag(position);
		if(mPackageList.get(position).isChecked()){
			seekbar.setProgress(mPackageList.get(position).getmInputtime());
			seekbar.setVisibility(View.VISIBLE);
		}else{
			seekbar.setVisibility(View.GONE);
		}
		TextView textview = (TextView)convertView.findViewById(R.id.title_package);
		textview.setText(Utils.getApplicationLabelName(mContext, mPackageList
				.get(position).getmApplicationName()));
		textview.setOnClickListener(this);
		textview.setTag(position);
		checkbox.setOnClickListener(this);
		return convertView;
	}

	@Override
	public void onClick(View v) {
		if (v instanceof CheckBox) {
			int pos = (int)v.getTag();
			if (!mPackageList.get(pos).isChecked()) {
				mPackageList.get(pos).setChecked(true);
				if (mSelectedCount < MAXIMUN_APPLICATIONS) {
					mSelectedCount++;

				} else {
					((CheckBox) v).setChecked(false);
					Toast.makeText(
							mContext,
							mContext.getString(R.string.string_select_packages_maximum_app_toast),
							Toast.LENGTH_LONG).show();
				}
			} else {
				mPackageList.get(pos).setChecked(false);
				mSelectedCount--;
			}
			this.notifyDataSetChanged();
		}

	}
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		int position = (int)seekBar.getTag();
		mPackageList.get(position).setmInputtime(progress);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		
	}
}
