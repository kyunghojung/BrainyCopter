package com.hyunnyapp.brainycopter;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.hyunnyapp.brainycopter.modal.OSDCommon;
import com.hyunnyapp.brainycopter.modal.Transmitter;


public class MainActivity extends FragmentActivity implements
		 SettingsDialogDelegate, OnTouchListener,
		HudViewControllerDelegate
{
	private static final String TAG = MainActivity.class.getSimpleName();
	public static final int REQUEST_ENABLE_BT = 1;

	private SettingsDialog settingsDialog;
	private HudViewController hudVC;
	boolean isFirstRun = true;

	TextView ssid;
	TextView connectState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Log.d(TAG, "----onCreate");

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		hudVC = new HudViewController(this, this);
		hudVC.onCreate();
		hudVC.onResume();	

	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		if (Transmitter.sharedTransmitter().getBleConnectionManager() != null) {
			Transmitter.sharedTransmitter().transmmitSimpleCommand(
					OSDCommon.MSPCommnand.MSP_DISARM);
			Transmitter.sharedTransmitter().getBleConnectionManager().close();
		}
		hudVC.onDestroy();
		hudVC = null;

		Thread destroy = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});
		destroy.start();
	}

	@Override
	public void prepareDialog(SettingsDialog dialog) {

	}

	@Override
	public void onDismissed(SettingsDialog settingsDialog) {
		
		hudVC.setSettingsButtonEnabled(true);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return false;
	}

	@Override
	public void settingsBtnDidClick(View settingsBtn) {
		hudVC.setSettingsButtonEnabled(false);
		showSettingsDialog();
	}

	public ViewController getViewController() {
		return hudVC;
	}

	protected void showSettingsDialog() {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack(null);

		if (settingsDialog == null) {
			Log.d(TAG, "settingsDialog is null");
			settingsDialog = new SettingsDialog(this, this);
		}

		settingsDialog.show(ft, "settings");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
