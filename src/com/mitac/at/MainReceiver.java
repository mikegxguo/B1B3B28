package com.mitac.at;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MainReceiver extends BroadcastReceiver {
	private static String TAG = "3GReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.d(TAG, "Receive : action = " + action);
      if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
//          Intent service = new Intent(context, ATService.class);
//          service.putExtra(ATService.EXTRA_EVENT, ATService.EVENT_BOOT_COMPLETED);
//          context.startService(service);
          Intent service = new Intent(context, MainService.class);
          service.putExtra(MainService.EXTRA_EVENT, MainService.EVENT_BOOT_COMPLETED);
          context.startService(service);
      }
  }

}
