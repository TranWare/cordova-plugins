package com.tranware.cordova;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.idtechproducts.unipay.StructConfigParameters;
import com.idtechproducts.unipay.UniPayReader;

public class UniPayPlugin extends CordovaPlugin {
	private static final String TAG = UniPayPlugin.class.getSimpleName();
	
	private static final String ACTION_DETECT_READER = "ACTION_DETECT_READER";
	private static final String RESULT_DETECTED = "RESULT_DETECTED";
	private static final String ERROR_NOT_DETECTED = "ERROR_NOT_DETECTED";
	
	private UniPayReader mReader;	
	private BroadcastReceiver mHeadsetReceiver;
	// written in main thread, read in Cordova thread
	private volatile boolean mHeadsetPlugged;
	// written in UniPay callback, read in Cordova thread
	private volatile boolean mDetected;
	private CallbackContext mCordovaCallback;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		
		mHeadsetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
					Log.d(TAG, "mHeadsetReceiver: ACTION_HEADSET_PLUG");
					if(intent.getIntExtra("state", 0) == 1) {
						// definitely not a UniPay if there's no mic
						mHeadsetPlugged = intent.getIntExtra("microphone", 0) == 1;
					}
					else {
						mHeadsetPlugged = false;
					}
					Log.d(TAG, "mHeadsetPlugged = " + mHeadsetPlugged);
				}
			}			
		};
		cordova.getActivity().registerReceiver(mHeadsetReceiver,
				new IntentFilter(Intent.ACTION_HEADSET_PLUG));
	}

	@Override
	public void onDestroy() {
		cordova.getActivity().unregisterReceiver(mHeadsetReceiver);
		destroyReader();
	}
	
	@Override
	public boolean execute(String action, JSONArray unused, CallbackContext callback) throws JSONException {
		mCordovaCallback = callback;
		
		if(ACTION_DETECT_READER.equals(action)) {
			if(mDetected) {
				success(RESULT_DETECTED);
			}
			else if(mHeadsetPlugged) {
				initReader();
			}
			else {
				error(ERROR_NOT_DETECTED);
			}			
			return true;
		}
		
		return false;
	}

	private class ReaderCallback extends UniPayReaderMsgAdapter {
		
		@Override
		public boolean getUserGrant(int type, String message) {
			if(type == typeToUpdateXML || type == typeToReportToIdtech) {
				return false;
			}
			else return true;
		}
		
		@Override
		public void onReceiveMsgTimeout(String message) {
			Log.d(TAG, "onReceiveMsgTimeout(\"" + message + "\")");
			destroyReader();
			error(ERROR_NOT_DETECTED);
		}

		@Override
		public void onReceiveMsgConnected() {
			Log.d(TAG, "onReceiveMsgConnected()");
			mDetected = true;
			success(RESULT_DETECTED);
		}
		
		@Override
		public void onReceiveMsgToConnect() {
			Log.d(TAG, "onReceiveMsgToConnect()");
		}

		@Override
		public void onReceiveMsgDisconnected() {
			Log.d(TAG, "onReceiveMsgDisconnected()");
			destroyReader();
			
			/* This ensures that Cordova gets a callback if some action is
			 * interrupted by the device being unplugged.  This comes into
			 * play if the app tries to handshake with headphones and the user
			 * unplugs them to save their ears.  Probably also if the user
			 * unplugs the reader during a swipe attempt.
			 */
			error(ERROR_NOT_DETECTED);
			
			mDetected = false;
		}

	}
	
	private void initReader() {
		destroyReader();
		mReader = new UniPayReader(new ReaderCallback(), cordova.getActivity());

		StructConfigParameters config = new StructConfigParameters();
		config.setDirectionOutputWave((short) 1);
		config.setFrequenceInput(48000);
		config.setFrequenceOutput(48000);
		config.setRecordBufferSize(8192);
		config.setRecordReadBufferSize(163840);
		config.setWaveDirection(1);
		config.set_Low((short) -10000);
		config.set_High((short) 10000);
		config.set__Low((short) -2000);
		config.set__High((short) 2000);
		config.sethighThreshold((short) 4000);
		config.setlowThreshold((short) -4000);
		config.setdevice_Apm_Base(25000);
		config.setMin((short) 2);
		config.setMax((short) 8);
		config.setBaudRate(9600);
		config.setPreAmbleFactor((short) 2);	    	
		config.setShuttleChannel((byte) 48);
		config.setForceHeadsetPlug((short) 0);	        	
		config.setUseVoiceRecognition((short) 1);	        	
		config.setVolumeLevelAdjust((short) 0);	
		
		mReader.connectWithProfile(config);
		// this should result in callback to onReceiveMsgConnected or onReceiveMsgTimeout
		mReader.registerListen();
	}
	
	private void destroyReader() {
		if(mReader != null) {
			mReader.unregisterListen();
			mReader.release();
			mReader = null;
		}
	}
	
	private void success(String message) {
		if(mCordovaCallback != null) {
			mCordovaCallback.success(message);
			mCordovaCallback = null;
		}
	}
	
	private void error(String message) {
		if(mCordovaCallback != null) {
			mCordovaCallback.error(message);
			mCordovaCallback = null;
		}
	}
	
}
