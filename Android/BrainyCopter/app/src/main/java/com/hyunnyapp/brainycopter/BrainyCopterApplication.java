package com.hyunnyapp.brainycopter;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.res.AssetManager;
import android.util.Log;

import com.hyunnyapp.brainycopter.modal.ApplicationSettings;
import com.hyunnyapp.brainycopter.ui.AttitudeIndicator;
import com.hyunnyapp.brainycopter.ui.Text;
import com.hyunnyapp.brainycopter.util.FileHelper;

import com.vmc.ipc.config.ConfigStoreHandler;
import com.vmc.ipc.config.VmcConfig;
import com.vmc.ipc.util.MediaUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class BrainyCopterApplication extends Application
{   
	private static final String TAG = BrainyCopterApplication.class.getSimpleName();
    
	private static BrainyCopterApplication instance;
	
	private ApplicationSettings settings;
	private FileHelper fileHelper;
	
	private AppStage appStage = AppStage.UNKNOWN;
	
	private boolean isFullDuplex;


	public boolean isFullDuplex() {
		return isFullDuplex;
	}


	public void setFullDuplex(boolean isFullDuplex) {
		this.isFullDuplex = isFullDuplex;
	}


	static {
    	System.loadLibrary("vmcipc");
    }

    
	private Text debugTextView;
	
	
	public Text getDebugTextView() {
		return debugTextView;
	}


	public void setDebugTextView(Text debugTextView) {
		this.debugTextView = debugTextView;
	}

	private float alt;
	
	public void setCurrentAlt(float alt_){
		alt = alt_;
	}
	
	public float getCurrentAlt(){
		return alt;
	}

	private AttitudeIndicator attitudeIndicator;
	private Text rollText;
	private Text pitchText;
	private Text yawText;

	public void setAttitudeIndicator(AttitudeIndicator attitudeIndicator) {
		this.attitudeIndicator = attitudeIndicator;
	}

	public void setRollTextView(Text roll) {
		this.rollText = roll;
	}

	public void setPitchTextView(Text pitch) {
		this.pitchText = pitch;
	}

	public void setYawTextView(Text yaw) {
		this.yawText = yaw;
	}

	public void setCurrentAttitude(float roll, float pitch, float yaw) {
		attitudeIndicator.setAttitude(roll, pitch, yaw);
		attitudeIndicator.invalidate();

		yawText.setText(Integer.toString((int)yaw));
		rollText.setText(Integer.toString((int)roll));
		pitchText.setText(Integer.toString((int)pitch));
	}

	public enum AppStage{
		UNKNOWN,
		HUD,
		SETTINGS
	};
	  
	
	@SuppressLint("NewApi")
    @Override
	public void onCreate() 
	{
		super.onCreate();
		
		instance = this;

		fileHelper = new FileHelper(this);
		
		copyDefaultSettingsFileIfNeeded();
		
		settings = new ApplicationSettings(getFilesDir() + "/Settings.plist");

		MediaUtil.createIPCDir();
		VmcConfig.getInstance().setConfigStoreHandler(new ConfigStoreHandler(this));
		VmcConfig.getInstance().initNativeConfig(MediaUtil.getAppConfigDir());
	}
	
	
	@Override
	public void onTerminate() 
	{
		super.onTerminate();
	}

	
	public ApplicationSettings getAppSettings()
	{
		return settings;
	}
	
	public FileHelper getFileHelper(){
		return fileHelper;
	}
	
	
    public static BrainyCopterApplication sharedApplicaion() {
        return instance;  
    }  
    
    
    private void copyDefaultSettingsFileIfNeeded(){
		String settingsFileName        = "Settings.plist";
		String defaultSettingsFileName = "DefaultSettings.plist";

		if (fileHelper.hasDataFile(defaultSettingsFileName) == false) {
			AssetManager assetManager = getAssets();
			
			InputStream in = null;
			OutputStream out = null;
			try {
				in = assetManager.open(settingsFileName);
				out =  openFileOutput(defaultSettingsFileName, MODE_PRIVATE);

				byte[] buffer = new byte[1024];
				int read;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}

				in.close();
				in = null;
				out.flush();
				out.close();
				out = null;
			} catch (IOException e) {
				Log.e("tag", "Failed to copy asset file: " + settingsFileName, e);
			}
		}
		
		if (fileHelper.hasDataFile(settingsFileName) == false) {
			AssetManager assetManager = getAssets();
			
			InputStream in = null;
			OutputStream out = null;
			try {
				in = assetManager.open(settingsFileName);
				out =  openFileOutput(settingsFileName, MODE_PRIVATE);

				byte[] buffer = new byte[1024];
				int read;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}

				in.close();
				in = null;
				out.flush();
				out.close();
				out = null;
			} catch (IOException e) {
				Log.e("tag", "Failed to copy asset file: " + settingsFileName, e);
			}
		}
		else{
			ApplicationSettings userSettings = new ApplicationSettings(getFilesDir() + "/" + settingsFileName);
			
			if (userSettings.getSettingsVersion().equals("1.0.0")) { //old settings file, needed to be updated
				fileHelper.delDataFile(settingsFileName);
				fileHelper.delDataFile(defaultSettingsFileName);
				copyDefaultSettingsFileIfNeeded();
			}
		}
    }


	public AppStage getAppStage() {
		return appStage;
	}


	public void setAppStage(AppStage appStage) {
		this.appStage = appStage;
	}
}
