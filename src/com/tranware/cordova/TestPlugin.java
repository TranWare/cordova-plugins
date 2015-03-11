package com.tranware.cordova;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.os.Handler;
import android.util.Log;

public class TestPlugin extends CordovaPlugin {
	public static final String TAG = TestPlugin.class.getSimpleName();
	public static final String ACTION_FNORD = "fnord";
	// MagTek example has a handler initialized like this
	private final Handler handler = new Handler();
	private boolean awesome;
	
	{
		Log.d(TAG, "plugin created in " + Thread.currentThread().getName());
	}

	@Override
	public boolean execute(String action, JSONArray args,
			final CallbackContext callback) throws JSONException {

		Log.d(TAG, "execute called in " + Thread.currentThread().getName());
		
		if(ACTION_FNORD.equals(action)) {
			awesome = !awesome;
			if(awesome) {
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						callback.success("A WINNER IS YOU!");						
					}
				}, 3000);
			}
			else {
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						callback.error("epic fail");				
					}
				}, 3000);
			}
		}
		
		return false;
	}

}
