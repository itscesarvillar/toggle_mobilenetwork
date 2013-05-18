package com.example.mobileinterface;

import com.example.mobileinterface.R;
import com.example.util.MobileManager;
import com.example.util.SetPhoneSettings;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Switch;

public class Main extends Activity {
	
	private MobileManager iw;
	private SetPhoneSettings mPhone;
	private ConnectivityManager cm;
	private final String TAG = "MD";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		iw = new MobileManager(this);
		cm = ((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE));
		
		ContentResolver cr = getContentResolver();
		try{
			int value = Secure.getInt(cr, "preferred_network_mode");
			Log.d(TAG,"starred network_cr:" + value);
		} catch(SettingNotFoundException e) {
			Log.e(TAG,"error getting starred network:", e);
		}

		mPhone = new SetPhoneSettings(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		boolean is3GEnabled = !(cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.DISCONNECTED
                && cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getReason().equals("dataDisabled"));
		Log.d(TAG, is3GEnabled?"3g up":"3g down");
		
		Switch switch_mobile = (Switch) findViewById(R.id.mobileData);
		switch_mobile.setChecked(is3GEnabled);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	public void handleMobileInterface (View view) {
		boolean on = ((Switch) view).isChecked();
		
		if (on) {
			//enable mobile interface
			Log.d(TAG, "turn on 3g");
			iw.setMobileDataEnabled(true);
		} else {
			//disable mobile interface
			Log.d(TAG, "turn off 3g");
			iw.setMobileDataEnabled(false);
		}
	}
	
	public void showTestingMenu (View view){
	    Intent in = new Intent(Intent.ACTION_MAIN);
	    in.setClassName("com.android.settings", "com.android.settings.TestingSettings");
	    startActivity(in);
	}
	
	//set preferred network GSM_ONLY (1)
	public void set2g(View view){
		Log.d(TAG, "switch to gsm");
		mPhone.setNetworkNow(1);
	}
	
	//set preferred network WCDMA_PREFERRED (0)
	public void set3g(View view){
		Log.d(TAG, "switch to umts");
		mPhone.setNetworkNow(0);
	}
}
