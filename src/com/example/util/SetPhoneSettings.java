package com.example.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;


public class SetPhoneSettings {
	
	private static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
    private static final int MESSAGE_SET_2G = 1;
    private static final int MESSAGE_SET_3G = 2;
    private static final int MESSAGE_SET_AFTER_GET_2G = 3;
    private static final int MESSAGE_SET_AFTER_GET_3G = 4;
    
    private static final int MESSAGE_AFTER_SET_CUSTOM = 5;
    private static final int MESSAGE_TO_SET_CUSTOM = 6;

	public static final String TAG = "PhoneSettings";
	
	SetHandler setHandler = new SetHandler();
    
	TelephonyManager telephonyManager;
	Context context;
	
    Object mPhone;
    Method setPreferredNetworkType;
    Method getPreferredNetworkType;
    
    int currentNetwork = -1;
    int customNetwork = -1;
    
    int network2G = 1;
    int network3G = 0;
    
    enum setG {
        set2g, set3g, custom
    }
    
    setG settingG = null;
    
    public SetPhoneSettings(Context context) {
        this.context = context;
        
        mPhone = loadPhoneObject();
        try {
 
            setPreferredNetworkType = mPhone.getClass().getMethod("setPreferredNetworkType", new Class[] { int.class, Message.class });
            getPreferredNetworkType = mPhone.getClass().getMethod("getPreferredNetworkType", new Class[] { Message.class });

            getNetwork();
        }
        catch (Exception e) {
            Log.e(TAG, "Error!", e);
        }

        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }
    /**
     * Instantiate the Phonefactory class
     * @return getDefaultPhone
     */
    static Object loadPhoneObject() {
        try {
        	Class<?> phoneFactory = Class.forName("com.android.internal.telephony.PhoneFactory");
            Method getDefaultPhone = phoneFactory.getMethod("getDefaultPhone", new Class[] {});
            return getDefaultPhone.invoke(null, new Object[] {});
        }
        catch (Exception e) {
            Log.e(TAG, "Error!", e);
        }
        return null;
    }
    /**
     * Get the preferred network on Phone.apk, response will be deliver on a message
     */
    void getNetwork() {
        try {
            getPreferredNetworkType.invoke(mPhone, new Object[] { setHandler.obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE) });
        }
        catch (Exception e) {
            Log.e(TAG, "Error!", e);
        }
    }
    
    public void setNetworkNow(int net) {
        if (settingG != setG.custom || customNetwork != net) {
            settingG = setG.custom;
            customNetwork = net;
            Looper.myQueue().addIdleHandler(setHandler);
        }
    }
    
    public void setCustomNow() {
        if (currentNetwork != customNetwork) {
            Log.i(TAG, "set custom network: " + customNetwork);
            if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                try {
                    getPreferredNetworkType.invoke(mPhone, new Object[] { setHandler.obtainMessage(MESSAGE_TO_SET_CUSTOM) });
                }
                catch (Exception e) {
                    Log.e(TAG, "Error!", e);
                }
            }
            else {
                Log.i(TAG, "custom not set, phone in use");
            }
        }
    }
    
    /**
     * handler to call the desired PhoneFactory methods
     */
    private class SetHandler extends Handler implements IdleHandler {

		@Override
		public boolean queueIdle() {
            Log.d(TAG, "Idle Queue");
            if (settingG == setG.custom) {
                setCustomNow();
            }
            return false;
        }

		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            
            case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
            	handleGetPreferredNetworkTypeResponse(msg);
            break;
            
            case MESSAGE_TO_SET_CUSTOM:
            	Log.d(TAG, "switching from " + currentNetwork + " to " + customNetwork);
            	if ((currentNetwork != customNetwork) && 
            			(telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE)) {
            		try{
            			int delay = 500;
            			int timeout = (int) ((SystemClock.uptimeMillis() + delay ) / 1000);
            			Log.d(TAG, "start timeout = " + timeout );
                    
            			setPreferredNetworkType.invoke(mPhone, new Object[] { customNetwork, setHandler.obtainMessage(MESSAGE_AFTER_SET_CUSTOM, customNetwork, timeout) });
            		}
            		catch (Exception e) {
            			Log.e(TAG, "Error!", e);
            		}
            	}
            	else {
            		Log.d(TAG, "Custom ("+ customNetwork + ") not set: already in that network or phone in use");
            	}
            break;
            
        	case MESSAGE_AFTER_SET_CUSTOM:
        		if (handleSetPreferredNetworkTypeResponse(msg, setG.custom)) {
        			if (settingG == setG.custom) {
        				settingG = null;
        			}
        		}
            break;
            }
		}
		
		private void handleGetPreferredNetworkTypeResponse(Message msg) {
			try {
				Field declaredField = msg.obj.getClass().getDeclaredField("exception");
				Object exception = declaredField.get(msg.obj);
				if (exception != null) {
					Log.e(TAG, "Error Setting: " + declaredField.get(msg.obj));
				}
				else {
					declaredField = msg.obj.getClass().getDeclaredField("result");
					Object result = declaredField.get(msg.obj);
					int type = ((int[]) result)[0];

					currentNetwork = type;
					Log.d(TAG, "currentNetwork (" + type + ")");
					//Toggle2GService.showNotification(context, is2g());
				}
			}
			catch (Exception e) {
				Log.e(TAG, "Error!", e);
			}
		}
		
		private boolean handleSetPreferredNetworkTypeResponse(Message msg, setG set) {
            try {
                Field declaredField = msg.obj.getClass().getDeclaredField("exception");
                Object exception = declaredField.get(msg.obj);
                
                if (exception != null) {
                    Log.e(TAG, "Error Setting: " + exception);
                    // try again! new timeout
                    long timeout = msg.arg2 * 1000;

                    Thread.sleep(500);
                    if ( ( settingG == null || set == settingG ) && SystemClock.uptimeMillis() < timeout) {
                        Log.i(TAG, "retry timeout left = " + (timeout - SystemClock.uptimeMillis()));
                        setPreferredNetworkType.invoke(mPhone, new Object[] { msg.arg1, setHandler.obtainMessage(msg.what, msg.arg1, msg.arg2) });
                    }
                    else {
                        Log.i(TAG, "retry timeout over, giving up");
                    }  
                }
                else {
                    getNetwork();
                    return true;
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Error!", e);
            }
            
            return false;
        }

    	
    }
    
}