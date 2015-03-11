package com.tranware.cordova;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

public class TestPlugin extends CordovaPlugin {
	public static final String TAG = TestPlugin.class.getSimpleName();
	public static final String ACTION_FNORD = "fnord";
	private boolean awesome;
	
	{
		Log.d(TAG, "plugin created in " + Thread.currentThread().getName());
	}

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callback) throws JSONException {

		Log.d(TAG, "execute called in " + Thread.currentThread().getName());
		
		if(ACTION_FNORD.equals(action)) {
			awesome = !awesome;
			if(awesome) {
				callback.success("A WINNER IS YOU!");
			}
			else {
				callback.error("epic fail");
			}
		}
		
		return false;
	}

}