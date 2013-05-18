package com.example.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.example.mobileinterface.R;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.util.Log;


public class MobileManager {

	ConnectivityManager cm;
	TelephonyManager tm;
	NotificationManager nm;
	Context context;
	

	public MobileManager(Context context){
		this.context = context;
		cm = (ConnectivityManager) context.getSystemService(Activity.CONNECTIVITY_SERVICE);
		tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}


	final static String TAG = "mobileManager";
	
	//that would be better if was static
	
	public void setMobileDataEnabled(boolean enable) {
		Method m = getMethodFromClass(cm, "setMobileDataEnabled");
		invokeMethodClass(cm, m, enable);
	}

	
	/**
	 * getMethodFromClass
	 *  get a method given by a string from an particular class  
	 * @param obj - the class where the method is
	 * @param methodName - the name of the method
	 * @return the method object
	 */
	private Method getMethodFromClass(Object obj, String methodName) {
		final String TAG = "getMethodFromClass";
		Class<?> whichClass = null;
		try {
			whichClass = Class.forName(obj.getClass().getName());
		} catch (ClassNotFoundException e2) {
			Log.d(TAG, "class not found");
		}
		Method method = null;
		try {
			//method = whichClass.getDeclaredMethod(methodName);
			Method[] methods = whichClass.getDeclaredMethods();
			for (Method m : methods) {
				Log.d(TAG,"m:" +m);
				if (m.getName().contains(methodName)) {
					method = m;
				}
			}
		} catch (SecurityException e2) {
			Log.d(TAG, "SecurityException for " + methodName);
		}
		return method;
	}
	/**
	 * invokeMethodClass 
	 *  Run the method of a given class
	 * 
	 * @param obj - class where the method is
	 * @param method - the desire method to invoke
	 * @param argv - arguments of that method as a varags
	 * @return the returned object from invocation
	 */
	private Object invokeMethodClass(Object obj, Method method, Object... argv) {
		Object result = null;
		if (method == null) {
			Log.d(TAG,"method null");
			return result;
		}
		method.setAccessible(true);
		try {
			result = method.invoke(obj, argv);
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "IllegalArgumentException for " + method.getName());
		} catch (IllegalAccessException e) {
			Log.d(TAG, "IllegalAccessException for " + method.getName());
		} catch (InvocationTargetException e) {
			Log.d(TAG, "InvocationTargetException for " + method.getName()
					+ "; Reason: " + e.getLocalizedMessage());
		}
		return result;
	}
}