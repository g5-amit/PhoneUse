package com.asgj.android.appusage.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.asgj.android.appusage.Utility.UsageSharedPrefernceHelper;
import com.asgj.android.appusage.service.UsageTrackingService;

public class DeviceRebootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action == null)
			return;
		if (action.equals(Intent.ACTION_SHUTDOWN)) {
			if (UsageSharedPrefernceHelper.isServiceRunning(context)) {
				UsageSharedPrefernceHelper.setServiceRunningWhileShutDown(
						context, true);
			}
		}
		//else case is for reboot
		else {
			if (UsageSharedPrefernceHelper.isNeedToServiceOnReboot(context)) {
				Intent startServiceIntent = new Intent();
				startServiceIntent
						.setClass(context, UsageTrackingService.class);
				startServiceIntent.setComponent(new ComponentName(context,
						UsageTrackingService.class));
				startServiceIntent.putExtra("isStartingAfterReboot", true);
				context.startService(startServiceIntent);
			}
		}

	}

}