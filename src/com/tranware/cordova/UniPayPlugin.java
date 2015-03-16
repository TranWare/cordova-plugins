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
	private static final String RESULT_NOT_DETECTED = "RESULT_NOT_DETECTED";
	
	private UniPayReader mReader;	
	private BroadcastReceiver mHeadsetReceiver;
	private CallbackContext mCordovaCallback;
	// written in main thread, read in Cordova thread
	private volatile boolean mHeadsetPlugged;
	// written in UniPay callback, read in Cordova thread
	private volatile boolean mDetected;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		mReader = new UniPayReader(new ReaderCallback(), cordova.getActivity());
		configReader();
		
		mHeadsetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
					if(intent.getIntExtra("state", 0) == 1) {
						// definitely not a UniPay if there's no mic
						mHeadsetPlugged = intent.getIntExtra("microphone", 0) == 1;
					}
					else {
						mHeadsetPlugged = false;
					}
				}
			}			
		};
		cordova.getActivity().registerReceiver(mHeadsetReceiver,
				new IntentFilter(Intent.ACTION_HEADSET_PLUG));
	}

	@Override
	public void onDestroy() {
		cordova.getActivity().unregisterReceiver(mHeadsetReceiver);
		mReader.unregisterListen();
		mReader.release();
	}
	
	@Override
	public boolean execute(String action, JSONArray unused, CallbackContext callback) throws JSONException {

		if(ACTION_DETECT_READER.equals(action)) {
			if(mDetected) {
				callback.success(RESULT_DETECTED);
			}
			else if(mHeadsetPlugged) {
				// this should result in callback to onReceiveMsgConnected or onReceiveMsgTimeout
				mCordovaCallback = callback;
				mReader.registerListen();
			}
			else {
				callback.error(RESULT_NOT_DETECTED);
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
			Log.d(TAG, "no response from reader");
			mReader.unregisterListen();
			mCordovaCallback.error(RESULT_NOT_DETECTED);
		}

		@Override
		public void onReceiveMsgConnected() {
			Log.d(TAG, "card reader detected");
			mDetected = true;
			mCordovaCallback.success(RESULT_DETECTED);
		}
		
		@Override
		public void onReceiveMsgToConnect() {
			Log.d(TAG, "audio device plugged");
		}

		@Override
		public void onReceiveMsgDisconnected() {
			Log.d(TAG, "audio device unplugged");
			mDetected = false;
		}

	}
	
	private void configReader() {
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
	}
	
}
