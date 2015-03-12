package com.tranware.cordova;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.idtechproducts.unipay.StructConfigParameters;
import com.idtechproducts.unipay.UniPayReader;

/**
 * Cordova/PhoneGap plugin wrapper for the IDTech UniPay API.
 * <p>
 * Plugins are instantiated on a Looper thread named JavaBridge.  Their
 * <tt>execute</tt> methods are also called in that thread.  Callbacks can
 * apparently occur in any thread, since they cause messages to be placed in a
 * synchronized queue.
 *
 * @author Kevin Krumwiede
 */
public class UniPayPlugin extends CordovaPlugin {
	private static final String TAG = UniPayPlugin.class.getSimpleName();
	
	private static final String ACTION_ENABLE_READER = "enableReader";
	private static final String ACTION_GET_SWIPE = "getSwipe";
	private static final String ACTION_CANCEL_SWIPE = "cancelSwipe";
	private static final String ACTION_DISABLE_READER = "disableReader";
	
	private static final String ERROR_CANCEL = "cancel";	
	private static final String ERROR_TIMEOUT = "timeout";
	private static final String ERROR_NO_TRACK2 = "noTrack2";
	private static final String ERROR_UNKNOWN = "unknown";

	/* Can't initialize this at construction because it needs a Context from
	 * cordova.getActivity(), and cordova won't have been initialized yet.
	 */ 
	private UniPayReader mReader;
	
	private CallbackContext mCordovaCallback;	

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		Log.d(TAG, "initialize");
		super.initialize(cordova, webView);
		initReader();
		mReader.registerListen();
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		mReader.unregisterListen();
		mReader = null;
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callback) throws JSONException {
		if(ACTION_ENABLE_READER.equals(action)) {
			mCordovaCallback = callback;
			mReader.sendCommandEnableSwipingMSRCard();
			// callback occurs when command result is received
			return true;
		}
		else if(ACTION_GET_SWIPE.equals(action)) {
			mCordovaCallback = callback;
			mReader.startSwipeCard();
			// callback occurs when swipe is received or times out
			return true;
		}
		else if(ACTION_CANCEL_SWIPE.equals(action)) {
			/* Docs are ambiguous, but I think this is the correct method to
			 * cancel an ongoing swipe, as opposed to
			 * UniPayReader#sendCommandCancelSwipingMSRCard().
			 */
			mReader.stopSwipeCard();
			// no callback, but send error to original swipe callback
			mCordovaCallback.error(ERROR_CANCEL);
			mCordovaCallback = null;
			return true;
		}
		else if(ACTION_DISABLE_READER.equals(action)) {
			mReader.sendCommandCancelSwipingMSRCard();
			callback.success();
			mCordovaCallback = null;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Initializes the reader.
	 */
	private void initReader() {
		Log.d(TAG, "initializing reader");
		mReader = new UniPayReader(new ReaderCallback(),
				cordova.getActivity().getApplicationContext());

		/* This is the config template that autoconfig selects for every
		 * device we tested that works at all.
		 * 
		 * <template_9600_1>
		 *    <support_status val=""> </support_status>
		 *    <directionOutputWave>1</directionOutputWave>
		 *    <InputFreq>48000</InputFreq>
		 *    <OutputFreq>48000</OutputFreq>
		 *    <RecBuffSize>8192</RecBuffSize>
		 *    <ReadRecBuffSize>163840</ReadRecBuffSize>
		 *    <WaveDirct>1</WaveDirct>
		 *    <_Low>-10000</_Low>
		 *    <_High>10000</_High>
		 *    <__Low>-2000</__Low>
		 *    <__High>2000</__High>
		 *    <highThreshold>4000</highThreshold>
		 *    <lowThreshold>-4000</lowThreshold>
		 *    <device_Apm_Base>25000</device_Apm_Base>
		 *    <min>2</min>
		 *    <max>8</max>
		 *    <baudRate>9600</baudRate>
		 *    <preAmbleFactor>2</preAmbleFactor>
		 * </template_9600_1>
		 */

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

		/* There is no documentation for StructConfigParameters, and these
		 * values are not in the config file.  I discovered the expected
		 * values by instrumenting the demo app to log them after running
		 * autoconfig.
		 */
		config.setShuttleChannel((byte) 48);
		config.setForceHeadsetPlug((short) 0);	        	
		config.setUseVoiceRecognition((short) 1);	        	
		config.setVolumeLevelAdjust((short) 0);

		/* This method is also completely undocumented. */
		mReader.connectWithProfile(config);
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
		public void onReceiveMsgCommandResult(int command, byte[] response) {
			/* Example of response bytes indicating success: 2 1 0 6 6 6 3
			 * There are more conditions in the sample code, but this seems to
			 * cover it for our purposes.  See UniPayDemo line 2080.  (!!!)
			 */
			if(command == cmdEnableSwipingMSRCard) {
				if(response.length > 3 && response[3] == 6) {
					mCordovaCallback.success();
					mCordovaCallback = null;
				}
				else {
					StringBuilder sb = new StringBuilder();
					sb.append("unexpected result bytes (decimal):");
					for(byte b : response) {
						sb.append(' ').append(Integer.toString(b));
					}
					Log.w(TAG, sb.toString());
					mCordovaCallback.error(ERROR_UNKNOWN);
					mCordovaCallback = null;
				}
			}
			else {
				Log.w(TAG, "unexpected command result: " + command);
			}
		}

		@Override
		public void onReceiveMsgCardData(byte flags, byte[] data) {
			if(flags == 0) {
				Log.d(TAG, "received swipe");
				final String trackData = new String(data);				
				Track2Matcher matcher = new Track2Matcher();
				if(matcher.find(trackData)) {
					JSONObject result = new JSONObject();
					try {
						result.put("card", matcher.getCard());
						result.put("exp", matcher.getExpMMYY());
						result.put("raw", trackData);
						mCordovaCallback.success(result);
						mCordovaCallback = null;
					} catch (JSONException e) {
						Log.w(TAG, "\"impossible\" exception", e);
						mCordovaCallback.error(ERROR_UNKNOWN);
						mCordovaCallback = null;
					}
				}
				else {
					mCordovaCallback.error(ERROR_NO_TRACK2);
					mCordovaCallback = null;
				}
			}
			else {
				Log.d(TAG, "received garbage swipe - probably bogus timeout");				
				mCordovaCallback.error(ERROR_TIMEOUT);
				mCordovaCallback = null;
			}
		}

		@Override
		public void onReceiveMsgTimeout(String message) {
			// including this in case they ever fix it
			Log.d(TAG, "swipe timed out");
			mCordovaCallback.error(ERROR_TIMEOUT);
			mCordovaCallback = null;
		}
	}	
}
