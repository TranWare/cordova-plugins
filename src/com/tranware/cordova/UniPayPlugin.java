package com.tranware.cordova;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.idtechproducts.unipay.StructConfigParameters;
import com.idtechproducts.unipay.UniPayReader;

/**
 * Cordova plugin wrapper for the UniPay SDK.  Attempts to work around the
 * SDK's triple whammy of poor design, incomplete and sometimes incorrect
 * documentation, and numerous bugs.  Some of the bugs are worked around in
 * such a way that if they're ever fixed, this wrapper will stop working.
 *
 * @author Kevin Krumwiede
 */
public class UniPayPlugin extends CordovaPlugin {
	private static final String TAG = UniPayPlugin.class.getSimpleName();
	
	private static final String ACTION_DETECT_READER = "ACTION_DETECT_READER";
	private static final String RESULT_DETECTED = "RESULT_DETECTED";
	private static final String ERROR_NOT_DETECTED = "ERROR_NOT_DETECTED";
	
	private static final String ACTION_GET_SWIPE = "ACTION_GET_SWIPE";
	private static final String ERROR_TIMEOUT = "ERROR_TIMEOUT";
	private static final String ERROR_NO_TRACK_2 = "ERROR_NO_TRACK_2";
	private static final String ERROR_UNKNOWN = "ERROR_UNKNOWN";
	
	
	private UniPayReader mReader;	
	private BroadcastReceiver mHeadsetReceiver;
	// written in main thread, read in Cordova thread
	private volatile boolean mHeadsetPlugged;
	// written in UniPay callback, read in Cordova thread
	private volatile boolean mDetected;
	// written in Cordova thread, read in UniPay callback
	private volatile CallbackContext mCordovaCallback;

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
		else if(ACTION_GET_SWIPE.equals(action)) {
			if(mDetected) {
				mReader.sendCommandEnableSwipingMSRCard();
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
			/* This is never actually called for a swipe timeout, only when
			 * handshaking fails.
			 */
			Log.d(TAG, "onReceiveMsgTimeout(\"" + message + "\")");
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
			/* This ensures that Cordova gets a callback if some action is
			 * interrupted by the device being unplugged.  This comes into
			 * play if the reader tries to handshake with headphones and the
			 * user unplugs them to save their ears.  Probably also if the
			 * user unplugs the reader during a swipe attempt.
			 */
			error(ERROR_NOT_DETECTED);
		}
		
		@Override
		public void onReceiveMsgCommandResult(int command, byte[] response) {
			/* Example of response bytes indicating success: 2 1 0 6 6 6 3
			 * There are more conditions in the sample code, but this seems to
			 * cover it for our purposes.  See UniPayDemo line 2080.  (!!!)
			 */
			if(command == cmdEnableSwipingMSRCard) {
				if(response.length > 3 && response[3] == 6) {					
					mReader.startSwipeCard();
				}
				else {
					StringBuilder sb = new StringBuilder();
					sb.append("unexpected result bytes (decimal):");
					for(byte b : response) {
						sb.append(' ').append(Integer.toString(b));
					}
					Log.w(TAG, sb.toString());
					error(ERROR_UNKNOWN);
				}
			}
			else if(command == cmdCancelSwipingMSRCard) {
				// not unexpected
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
					success(matcher.getCard(), matcher.getExpMMYY(), trackData);
				}
				else {
					error(ERROR_NO_TRACK_2);
				}
			}
			else {
				Log.d(TAG, "received garbage swipe - probably bogus timeout");				
				error(ERROR_TIMEOUT);
			}
		}

		@Override
		public void onReceiveMsgToSwipeCard() {
			/* If we call back for this, we won't be able to call back with
			 * the card data.  Cordova complains about the callback being
			 * reused.
			 */
			Log.d(TAG, "onReceiveMsgToSwipeCard()");
		}		

	}
	
	private void initReader() {
		/* If detection ever fails, the reader can get into an unrecoverable
		 * state that requires force closing the app to make it work again.
		 * To ensure reliability, we destroy and recreate the reader each time
		 * it's used.
		 */		
		destroyReader();
		mReader = new UniPayReader(new ReaderCallback(), cordova.getActivity());
		/* Force the reader to use the profile called template_9600_1.  This
		 * is far simpler than hassling with config files, and actually
		 * supports some devices that are not supported in the config file.
		 * Every device we've tested on which autoconfig works at all is
		 * configured with this template.
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
		/* This method is also completely undocumented.  It has the side
		 * effect of trying to handshake with the reader, and it always logs a
		 * warning because the SDK doesn't know the reader is connected until
		 * we've called registerListen.
		 */
		mReader.connectWithProfile(config);
		/* We never receive a connected callback if we call this before
		 * connectWithProfile.  (Oddly enough, the disconnected callback
		 * always works.)  Once it's getting callbacks, the reader will
		 * attempt to handshake with anything that's plugged in.  This will
		 * result in a callback to onReceiveMsgConnected or
		 * onReceiveMsgTimeout.  (But see the comment in
		 * onReceiveMsgDisconnected.)
		 */
		mReader.registerListen();
	}
	
	private void destroyReader() {
		if(mReader != null) {
			/* Calling this when the reader is not listening seems to have no
			 * ill effect.
			 */
			mReader.unregisterListen();
			/* The docs say to call this when the reader is disconnected, but
			 * doing so causes it to never recognize when it is connected
			 * again.  The demo app never calls this.  Calling it here ensures
			 * that the record thread is stopped.  Calling it when the record
			 * thread is not running seems to have no ill effect.
			 */
			mReader.release();
			mReader = null;
			mDetected = false;
		}
	}
	
	/**
	 * Delivers a success callback.  Does not destroy the reader.
	 * 
	 * @param message the success message
	 */
	private void success(String message) {
		if(mCordovaCallback != null) {
			mCordovaCallback.success(message);
			mCordovaCallback = null;
		}
		else {
			// shouldn't happen, but we'd like to know if it does
			Log.w(TAG, "success not delivered - callback was null");
		}
	}
	
	/**
	 * Delivers a card data callback.  Has the side effect of destroying the
	 * reader.
	 * 
	 * @param card the card number
	 * @param exp the expiration date in MMYY format
	 * @param trackData the raw track data
	 */
	private void success(final String card, final String exp, final String trackData) {
		if(mCordovaCallback != null) {
			JSONObject cardData = new JSONObject();
			try {
				cardData.put("card", card);
				cardData.put("exp", exp);
				cardData.put("raw", trackData);
				mCordovaCallback.success(cardData);
			} catch (JSONException e) {
				Log.w(TAG, "\"impossible\" exception", e);
				mCordovaCallback.error(ERROR_UNKNOWN);
			}
			mCordovaCallback = null;
		}
		else {
			// shouldn't happen, but we'd like to know if it does
			Log.w(TAG, "success not delivered - callback was null");
		}
		destroyReader();
	}
	
	/**
	 * Delivers an error callback.  Has the side effect of destroying the
	 * reader.
	 * 
	 * @param message the error message
	 */
	private void error(String message) {
		if(mCordovaCallback != null) {
			mCordovaCallback.error(message);
			mCordovaCallback = null;
		}
		else {
			// shouldn't happen, but we'd like to know if it does
			Log.w(TAG, "error not delivered - callback was null");
		}
		destroyReader();
	}
	
}
