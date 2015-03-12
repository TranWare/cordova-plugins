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
		Log.d(TAG, "action: " + action);
		
		if(ACTION_FNORD.equals(action)) {
			
			JSONArray result = new JSONArray();
			result.put("abc");
			result.put("0123456789");
			result.put("we all live in a yellow submarine");
			
			
//			awesome = !awesome;
//			if(awesome) {
//				boolean posted = handler.postDelayed(new Runnable() {
//					@Override
//					public void run() {
//						callback.success("A WINNER IS YOU!");						
//					}
//				}, 3000);
//				if(!posted) {
//					Log.w(TAG, "runnable not posted");
//				}
//			}
//			else {
//				boolean posted = handler.postDelayed(new Runnable() {
//					@Override
//					public void run() {
//						callback.error("epic fail");				
//					}
//				}, 3000);
//				if(!posted) {
//					Log.w(TAG, "runnable not posted");
//				}
//			}
			return true;
		}
		
		return false;
	}

}
