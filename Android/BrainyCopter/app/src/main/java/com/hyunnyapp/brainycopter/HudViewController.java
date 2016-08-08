package com.hyunnyapp.brainycopter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;

import com.hyunnyapp.brainycopter.BrainyCopterApplication.AppStage;
import com.hyunnyapp.brainycopter.ble.BleConnectinManager;
import com.hyunnyapp.brainycopter.gestures.EnhancedGestureDetector;
import com.hyunnyapp.brainycopter.ipc.activity.GalleryActivity;
import com.hyunnyapp.brainycopter.modal.ApplicationSettings;
import com.hyunnyapp.brainycopter.modal.Channel;
import com.hyunnyapp.brainycopter.modal.OSDCommon;
import com.hyunnyapp.brainycopter.modal.OSDData;
import com.hyunnyapp.brainycopter.modal.Transmitter;
import com.hyunnyapp.brainycopter.sensors.DeviceOrientationChangeDelegate;
import com.hyunnyapp.brainycopter.sensors.DeviceOrientationManager;
import com.hyunnyapp.brainycopter.sensors.DeviceSensorManagerWrapper;
import com.hyunnyapp.brainycopter.ui.AnimationIndicator;
import com.hyunnyapp.brainycopter.ui.AttitudeIndicator;
import com.hyunnyapp.brainycopter.ui.Button;
import com.hyunnyapp.brainycopter.ui.Image;
import com.hyunnyapp.brainycopter.ui.Image.SizeParams;
import com.hyunnyapp.brainycopter.ui.Indicator;
import com.hyunnyapp.brainycopter.ui.Sprite;
import com.hyunnyapp.brainycopter.ui.Sprite.Align;
import com.hyunnyapp.brainycopter.ui.Text;
import com.hyunnyapp.brainycopter.ui.ToggleButton;
import com.hyunnyapp.brainycopter.ui.UIRenderer;
import com.hyunnyapp.brainycopter.ui.joystick.AcceleratorJoystick;
import com.hyunnyapp.brainycopter.ui.joystick.AnalogueJoystick;
import com.hyunnyapp.brainycopter.ui.joystick.JoystickBase;
import com.hyunnyapp.brainycopter.ui.joystick.JoystickFactory;
import com.hyunnyapp.brainycopter.ui.joystick.JoystickFactory.JoystickType;
import com.hyunnyapp.brainycopter.ui.joystick.JoystickListener;
import com.hyunnyapp.brainycopter.util.DebugHandler;
import com.hyunnyapp.brainycopter.util.FontUtils;
import com.vmc.ipc.config.VmcConfig;
import com.vmc.ipc.proxy.IpcProxy;
import com.vmc.ipc.proxy.IpcProxy.OnRecordCompleteListener;
import com.vmc.ipc.service.ConnectStateManager;
import com.vmc.ipc.service.IpcControlService;
import com.vmc.ipc.service.OnIpcConnectChangedListener;
import com.vmc.ipc.util.MediaUtil;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public class HudViewController extends ViewController
        implements OnTouchListener,
        OnGestureListener,
        SettingsViewControllerDelegate,
        DeviceOrientationChangeDelegate {
    private static final String TAG = HudViewController.class.getSimpleName();

    public final static String ACTION_RESTART_PREVIEW = "action_restart_preview";

    private static final int JOY_ID_LEFT = 1;
    private static final int JOY_ID_RIGHT = 2;
    private static final int MIDLLE_BG_ID = 3;
    private static final int TAKE_OFF_TOGGLE_BTN_ID = 4;
    private static final int SETTINGS_BTN_ID = 5;
    private static final int ALT_HOLD_TOGGLE_BTN = 6;
    private static final int STATE_TEXT_VIEW = 7;
    private static final int BATTERY_INDICATOR_ID = 8;
    private static final int HELP_BTN = 9;
    private static final int LOGO = 10;
    private static final int STATUS_BAR = 11;
    private static final int DEVICE_BATTERY_INDICATOR = 12;
    private static final int GALLERY_BTN = 13;
    private static final int RECORD_BTN = 14;
    private static final int CAPTURE_BTN = 15;
    private static final int WIFI_INDICATOR_ID = 16;
    private static final int RECORDING_INDICATOR = 17;
    private static final int BLE_INDICATOR = 18;
    private static final int WEB_ADDRESS = 19;
    private static final int DEBUG_TEXT_VIEW = 20;
    private static final int WARNING_TEXT_VIEW = 21;
    private static final int ATTITUDE_INDICATOR = 22;
    private static final int PITCH_ICON = 23;
    private static final int ROLL_ICON = 24;
    private static final int YAW_ICON = 25;
    private static final int THROTTLE_TEXT = 26;
    private static final int ROLL_TEXT = 27;
    private static final int PITCH_TEXT = 28;
    private static final int YAW_TEXT = 39;

    private final float BEGINNER_ELEVATOR_CHANNEL_RATIO = 0.5f;
    private final float BEGINNER_AILERON_CHANNEL_RATIO = 0.5f;
    private final float BEGINNER_RUDDER_CHANNEL_RATIO = 0.3f;
    private final float BEGINNER_THROTTLE_CHANNEL_RATIO = 0.8f;

    private final float AUTO_ALT_HOLD_MIN_THROTTLE = -0.6f;
    private final float AUTO_ALT_HOLD_MAX_THROTTLE = 0.4f;

    private Button settingsBtn;
    private ToggleButton takeOffToggleBtn;
    private ToggleButton altHoldToggleBtn;

    private Button galleryBtn;
    private Button captureBtn;
    private Button recordBtn;

    private static boolean isArmed;

    private boolean isAltHoldMode;
    private boolean isAccMode;

    private Button[] buttons;

    private Indicator batteryIndicator;
    private Indicator deviceBatteryIndicator;
    private Indicator wifiIndicator;
    private Indicator bleIndicator;
    private AnimationIndicator recordingIndicator;

    private Text txtBatteryStatus;
    private GLSurfaceView glView;

    private JoystickBase[] joysticks;   //[0]roll and pitch, [1]rudder and throttle
    private float joypadOpacity;
    private GestureDetector gestureDetector;

    private UIRenderer renderer;

    private HudViewControllerDelegate delegate;

    private boolean isLeftHanded;
    private JoystickListener rollPitchListener;
    private JoystickListener rudderThrottleListener;

    private ApplicationSettings settings;

    private Channel aileronChannel;
    private Channel elevatorChannel;
    private Channel rudderChannel;
    private Channel throttleChannel;
    private Channel aux1Channel;
    private Channel aux2Channel;
    private Channel aux3Channel;
    private Channel aux4Channel;

    private DeviceOrientationManager deviceOrientationManager;
    private static final float ACCELERO_TRESHOLD = (float) Math.PI / 180.0f * 2.0f;
    private static final int PITCH = 1;
    private static final int ROLL = 2;
    private float pitchBase;
    private float rollBase;
    private boolean rollAndPitchJoystickPressed;
    private IpcControlService controlService;

    private GLSurfaceView videoStageSoft = null;
    private SurfaceView videoStageHard = null;

    private LocalBroadcastManager mLocalBroadcastManager;

    private IpcProxy ipcProxy;

    private boolean isStartRecord = false;
    final CustomOnRecordCompleteListener mCustomOnRecordCompleteListener = new CustomOnRecordCompleteListener();

    private boolean isAcPlugin = false;
    private SoundPool mSoundPool;
    private int camera_click_sound;
    private int video_record_sound;
    private boolean canRefreshUI = false;
    private Image middleBg;

    private Text debugTextView;
    private Text warnningTextView;

    private AttitudeIndicator attitudeIndicator;
    private Image pitch_icon;
    private Image roll_icon;
    private Image yaw_icon;

    private Text throttleText;
    private Text rollText;
    private Text pitchText;
    private Text yawText;

    private Handler handler;

    private boolean isConnected;

    private void setCurrentDecodeMode() {
        int decodeMode = VmcConfig.getInstance().getDecodeMode();
        if (decodeMode == -1) {
            decodeMode = IpcProxy.DEFAULT_DECODE_MODE;
        }
        setDecodeMode(decodeMode);
    }

    private void setVideoEnv() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this.context);

        setCurrentDecodeMode();

        Intent intent = new Intent();
        intent.setClass(this.context, IpcControlService.class);
        this.context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this.context);
        registerAllBroadcastReceiver();
    }


    public HudViewController(Activity context, HudViewControllerDelegate delegate) {
        this.delegate = delegate;
        this.context = context;

        Transmitter.sharedTransmitter().setBleConnectionManager(new BleConnectinManager(context));
        settings = ((BrainyCopterApplication) context.getApplication()).getAppSettings();

        joypadOpacity = settings.getInterfaceOpacity();
        isLeftHanded = settings.isLeftHanded();

        gestureDetector = new EnhancedGestureDetector(context, this);

        joysticks = new JoystickBase[2];

        glView = new GLSurfaceView(context);
        glView.setEGLContextClientVersion(2);

        context.setContentView(R.layout.hud_view_controller_framelayout);

        FrameLayout mainFrameLayout = (FrameLayout) context.findViewById(R.id.mainFrameLaytout);


        glView.setZOrderOnTop(true);
        glView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mainFrameLayout.addView(glView);

        ConnectStateManager mConnectStateManager = ConnectStateManager
                .getInstance(BrainyCopterApplication.sharedApplicaion());
        ipcProxy = mConnectStateManager.getIpcProxy();

        videoStageSoft = (GLSurfaceView) context.findViewById(R.id.video_bg_soft2);
        videoStageHard = (SurfaceView) context.findViewById(R.id.video_bg_hard2);


        setVideoEnv();

        renderer = new UIRenderer(context, null);

        initGLSurfaceView();

        Resources res = context.getResources();

        middleBg = new Image(res, R.drawable.main_background, Align.CENTER);
        //middleBg.setAlpha(0.5f);
        middleBg.setVisible(true);
        middleBg.setSizeParams(SizeParams.REPEATED, SizeParams.REPEATED);
        middleBg.setAlphaEnabled(true);

        Image logo = new Image(res, R.drawable.brainycopter_logo, Align.BOTTOM_LEFT);
        logo.setMargin(0, 0, (int) res.getDimension(R.dimen.main_logo_margin_bottom), (int) res.getDimension(R.dimen.main_logo_margin_left));

		/*
        Image web_address = new Image(res, R.drawable.web_address, Align.BOTTOM_RIGHT);
		web_address.setMargin(0, (int)res.getDimension(R.dimen.main_web_address_margin_right), (int)res.getDimension(R.dimen.main_web_address_margin_bottom), 0);
		*/

        galleryBtn = new Button(res, R.drawable.btn_gallery_normal, R.drawable.btn_gallery_press, Align.TOP_LEFT);
        galleryBtn.setMargin((int) res.getDimension(R.dimen.main_btn_gallery_margin_top), 0, 0, (int) res.getDimension(R.dimen.main_btn_gallery_margin_left));

        captureBtn = new Button(res, R.drawable.btn_capture_normal, R.drawable.btn_capture_press, Align.TOP_LEFT);
        captureBtn.setMargin((int) res.getDimension(R.dimen.main_btn_capture_margin_top), 0, 0, (int) res.getDimension(R.dimen.main_btn_capture_margin_left));

        recordBtn = new Button(res, R.drawable.btn_record_video_normal, R.drawable.btn_record_video_press, Align.TOP_LEFT);
        recordBtn.setMargin((int) res.getDimension(R.dimen.main_btn_record_margin_top), 0, 0, (int) res.getDimension(R.dimen.main_btn_record_margin_left));

        int recordingIndicatorRes[] = {
                R.drawable.btn_record_video_press,
                R.drawable.btn_record_video_status
        };

        recordingIndicator = new AnimationIndicator(res, recordingIndicatorRes, Align.TOP_LEFT);
        recordingIndicator.setMargin((int) res.getDimension(R.dimen.main_btn_record_margin_top), 0, 0, (int) res.getDimension(R.dimen.main_btn_record_margin_left));
        recordingIndicator.setAlphaEnabled(true);
        recordingIndicator.setVisible(false);

        takeOffToggleBtn = new ToggleButton(res, R.drawable.btn_unlock_normal, R.drawable.btn_unlock_press,
                R.drawable.btn_lock_normal, R.drawable.btn_unlock_press,
                R.drawable.btn_lock_normal, Align.BOTTOM_CENTER);
        takeOffToggleBtn.setMargin(0, 0, (int) res.getDimension(R.dimen.hud_take_off_btn_margin_bottom), (int) res.getDimension(R.dimen.hud_take_off_btn_margin_left));
        isArmed = false;
        takeOffToggleBtn.setChecked(isArmed);
        takeOffToggleBtn.setVisible(true);
        takeOffToggleBtn.setAlphaEnabled(true);

        settingsBtn = new Button(res, R.drawable.btn_settings_normal, R.drawable.btn_settings_normal_press, Align.TOP_RIGHT);
        settingsBtn.setMargin((int) res.getDimension(R.dimen.main_btn_settings_margin_top), (int) res.getDimension(R.dimen.main_btn_settings_margin_right), 0, 0);

        altHoldToggleBtn = new ToggleButton(res, R.drawable.alt_hold_off, R.drawable.alt_hold_off,
                R.drawable.alt_hold_on, R.drawable.alt_hold_on,
                R.drawable.alt_hold_on, Align.TOP_CENTER);

        altHoldToggleBtn.setMargin(res.getDimensionPixelOffset(R.dimen.hud_alt_hold_toggle_btn_margin_top), 0, 0, res.getDimensionPixelOffset(R.dimen.hud_alt_hold_toggle_btn_margin_left));
        altHoldToggleBtn.setChecked(settings.isAltHoldMode());
        altHoldToggleBtn.setVisible(true);
        altHoldToggleBtn.setAlphaEnabled(true);

        buttons = new Button[6];
        buttons[0] = settingsBtn;
        buttons[1] = captureBtn;
        buttons[2] = recordBtn;
        buttons[3] = galleryBtn;
        buttons[4] = altHoldToggleBtn;
        buttons[5] = takeOffToggleBtn;

        int batteryIndicatorRes[] = {R.drawable.btn_battery_0,
                R.drawable.device_battery_0,
                R.drawable.device_battery_1,
                R.drawable.device_battery_2,
                R.drawable.device_battery_3
        };

        batteryIndicator = new Indicator(res, batteryIndicatorRes, Align.TOP_RIGHT);
        batteryIndicator.setMargin((int) res.getDimension(R.dimen.main_device_battery_margin_top), (int) res.getDimension(R.dimen.main_device_battery_margin_right), 0, 0);


        int wifiIndicatorRes[] = {
                R.drawable.wifi_indicator_1,
                R.drawable.wifi_indicator_2,
                R.drawable.wifi_indicator_3,
                R.drawable.wifi_indicator_4
        };

        wifiIndicator = new Indicator(res, wifiIndicatorRes, Align.TOP_RIGHT);
        wifiIndicator.setMargin((int) res.getDimension(R.dimen.main_wifi_margin_top), (int) res.getDimension(R.dimen.main_wifi_margin_right), 0, 0);

        int bleIndicatorRes[] = {
                R.drawable.ble_indicator_opened,
                R.drawable.ble_indicator_closed
        };
        bleIndicator = new Indicator(res, bleIndicatorRes, Align.TOP_RIGHT);
        bleIndicator.setMargin((int) res.getDimension(R.dimen.main_ble_margin_top), (int) res.getDimension(R.dimen.main_ble_margin_right), 0, 0);
        bleIndicator.setValue(1);

        attitudeIndicator = new AttitudeIndicator(context, 300, 300, Align.TOP_LEFT);
        attitudeIndicator.setMargin((int) res.getDimension(R.dimen.hud_attitude_indicator_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_attitude_indicator_margin_left));
        attitudeIndicator.setAlphaEnabled(true);
        attitudeIndicator.setVisible(true);
        BrainyCopterApplication.sharedApplicaion().setAttitudeIndicator(attitudeIndicator);

        yaw_icon = new Image(res, R.drawable.attitude_icon_yaw, Align.TOP_LEFT);
        yaw_icon.setMargin((int) res.getDimension(R.dimen.hud_yaw_icon_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_yaw_icon_margin_left));
        yaw_icon.setAlphaEnabled(true);
        yaw_icon.setVisible(true);

        pitch_icon = new Image(res, R.drawable.attitude_icon_pitch, Align.TOP_LEFT);
        pitch_icon.setMargin((int) res.getDimension(R.dimen.hud_pitch_icon_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_pitch_icon_margin_left));
        pitch_icon.setAlphaEnabled(true);
        pitch_icon.setVisible(true);

        roll_icon = new Image(res, R.drawable.attitude_icon_roll, Align.TOP_LEFT);
        roll_icon.setMargin((int) res.getDimension(R.dimen.hud_roll_icon_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_roll_icon_margin_left));
        roll_icon.setAlphaEnabled(true);
        roll_icon.setVisible(true);

        yawText = new Text(context, "0", Align.TOP_LEFT);
        yawText.setMargin((int) res.getDimension(R.dimen.hud_yaw_text_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_yaw_text_margin_left));
        yawText.setTextColor(Color.WHITE);
        yawText.setTypeface(FontUtils.TYPEFACE.Helvetica(context));
        yawText.setTextSize(res.getDimensionPixelSize(R.dimen.hud_attitude_text_size));
        BrainyCopterApplication.sharedApplicaion().setYawTextView(yawText);

        rollText = new Text(context, "0", Align.TOP_LEFT);
        rollText.setMargin((int) res.getDimension(R.dimen.hud_roll_text_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_roll_text_margin_left));
        rollText.setTextColor(Color.WHITE);
        rollText.setTypeface(FontUtils.TYPEFACE.Helvetica(context));
        rollText.setTextSize(res.getDimensionPixelSize(R.dimen.hud_attitude_text_size));
        BrainyCopterApplication.sharedApplicaion().setRollTextView(rollText);

        pitchText = new Text(context, "0", Align.TOP_LEFT);
        pitchText.setMargin((int) res.getDimension(R.dimen.hud_pitch_text_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_pitch_text_margin_left));
        pitchText.setTextColor(Color.WHITE);
        pitchText.setTypeface(FontUtils.TYPEFACE.Helvetica(context));
        pitchText.setTextSize(res.getDimensionPixelSize(R.dimen.hud_attitude_text_size));
        BrainyCopterApplication.sharedApplicaion().setPitchTextView(pitchText);

        throttleText = new Text(context, context.getString(R.string.info_view_throttle_0), Align.TOP_LEFT);
        throttleText.setMargin((int) res.getDimension(R.dimen.hud_info_panel_throttle_text_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_info_panel_throttle_text_margin_left));
        throttleText.setTextColor(Color.WHITE);
        throttleText.setTypeface(FontUtils.TYPEFACE.Helvetica(context));
        throttleText.setTextSize(res.getDimensionPixelSize(R.dimen.hud_info_panel_text_size));
        //throttleText.setVisibility(View.VISIBLE);

        warnningTextView = new Text(context, context.getString(R.string.info_view_warnning_not_connected), Align.TOP_LEFT);
        warnningTextView.setMargin((int) res.getDimension(R.dimen.hud_info_panel_warning_text_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_info_panel_warning_text_margin_left));
        warnningTextView.setTextColor(Color.RED);
        warnningTextView.setTypeface(FontUtils.TYPEFACE.Helvetica(context));
        warnningTextView.setTextSize(res.getDimensionPixelSize(R.dimen.hud_info_panel_text_size));
        //warnningTextView.setVisibility(View.VISIBLE);

        String debugStr = "";
        debugTextView = new Text(context, debugStr, Align.TOP_LEFT);
        debugTextView.setMargin((int) res.getDimension(R.dimen.hud_info_panel_debug_text_margin_top), 0, 0, (int) res.getDimension(R.dimen.hud_info_panel_debug_text_margin_left));
        debugTextView.setTextColor(Color.WHITE);
        debugTextView.setTypeface(FontUtils.TYPEFACE.Helvetica(context));
        debugTextView.setTextSize(res.getDimensionPixelSize(R.dimen.hud_state_text_size) * 2 / 3);
        debugTextView.setVisibility(View.INVISIBLE);
        BrainyCopterApplication.sharedApplicaion().setDebugTextView(debugTextView);

        renderer.addSprite(MIDLLE_BG_ID, middleBg);
        renderer.addSprite(LOGO, logo);
        renderer.addSprite(BATTERY_INDICATOR_ID, batteryIndicator);
        renderer.addSprite(TAKE_OFF_TOGGLE_BTN_ID, takeOffToggleBtn);
        renderer.addSprite(SETTINGS_BTN_ID, settingsBtn);
        renderer.addSprite(BLE_INDICATOR, bleIndicator);
        renderer.addSprite(ALT_HOLD_TOGGLE_BTN, altHoldToggleBtn);
        renderer.addSprite(GALLERY_BTN, galleryBtn);
        renderer.addSprite(CAPTURE_BTN, captureBtn);
        renderer.addSprite(RECORD_BTN, recordBtn);
        renderer.addSprite(WIFI_INDICATOR_ID, wifiIndicator);
        renderer.addSprite(RECORDING_INDICATOR, recordingIndicator);
        renderer.addSprite(WARNING_TEXT_VIEW, warnningTextView);
        renderer.addSprite(THROTTLE_TEXT, throttleText);
        renderer.addSprite(ROLL_TEXT, rollText);
        renderer.addSprite(PITCH_TEXT, pitchText);
        renderer.addSprite(YAW_TEXT, yawText);
        renderer.addSprite(DEBUG_TEXT_VIEW, debugTextView);
        renderer.addSprite(ATTITUDE_INDICATOR, attitudeIndicator);
        renderer.addSprite(PITCH_ICON, pitch_icon);
        renderer.addSprite(ROLL_ICON, roll_icon);
        renderer.addSprite(YAW_ICON, yaw_icon);

        isAccMode = settings.isAccMode();
        deviceOrientationManager = new DeviceOrientationManager(new DeviceSensorManagerWrapper(this.context), this);
        deviceOrientationManager.onCreate();

        initJoystickListeners();

        if (isAccMode) {
            initJoysticks(JoystickType.ACCELERO);
        } else {
            initJoysticks(JoystickType.ANALOGUE);
        }

        initListeners();

        initChannels();

        if (settings.isHeadFreeMode()) {
            aux1Channel.setValue(1);
        } else {
            aux1Channel.setValue(-1);
        }

        if (settings.isAltHoldMode()) {
            aux2Channel.setValue(1);
        } else {
            aux2Channel.setValue(-1);
        }

        handler = new Handler();
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        updateUI();
                    }
                });
            }
        }, 0, 100);

        isConnected = false;
    }

    private void initUiControlShow() {
        if (controlService == null) {
            wifiIndicator.setVisible(false);
            captureBtn.setEnabled(false);
            recordBtn.setEnabled(false);
        } else {
            int state = controlService.getConnectStateManager().getState();
            if (state == ConnectStateManager.CONNECTING || state == ConnectStateManager.DISCONNECTED) {
                wifiIndicator.setVisible(false);
                captureBtn.setEnabled(false);
                recordBtn.setEnabled(false);
            } else {
                wifiIndicator.setVisible(true);
                captureBtn.setEnabled(true);
                recordBtn.setEnabled(true);
                canRefreshUI = true;
            }
        }
    }

    private void initChannels() {
        aileronChannel = settings.getChannel(Channel.CHANNEL_NAME_AILERON);
        elevatorChannel = settings.getChannel(Channel.CHANNEL_NAME_ELEVATOR);
        rudderChannel = settings.getChannel(Channel.CHANNEL_NAME_RUDDER);
        throttleChannel = settings.getChannel(Channel.CHANNEL_NAME_THROTTLE);
        aux1Channel = settings.getChannel(Channel.CHANNEL_NAME_AUX1);
        aux2Channel = settings.getChannel(Channel.CHANNEL_NAME_AUX2);
        aux3Channel = settings.getChannel(Channel.CHANNEL_NAME_AUX3);
        aux4Channel = settings.getChannel(Channel.CHANNEL_NAME_AUX4);

        aileronChannel.setValue(0.0f);
        elevatorChannel.setValue(0.0f);
        rudderChannel.setValue(0.0f);
        throttleChannel.setValue(-1);
    }

    private void setAltHoldMode(Boolean isAltHoldMode) {
        if (isAltHoldMode) {
            if ((((int) aux2Channel.getValue()) != 1)) {
                aux2Channel.setValue(1);
            }
        } else {
            if ((((int) aux2Channel.getValue()) != -1)) {
                aux2Channel.setValue(-1);
            }
        }
    }

    private void initJoystickListeners() {
        rollPitchListener = new JoystickListener() {
            public void onChanged(JoystickBase joy, float x, float y) {
                if (BrainyCopterApplication.sharedApplicaion().getAppStage() == AppStage.SETTINGS
                        || BrainyCopterApplication.sharedApplicaion().getAppStage() == AppStage.UNKNOWN) {
                    Log.w(TAG, "AppStage.SETTINGS ignore rollPitchListener onChanged");

                    return;
                }

                if (isAccMode == false && rollAndPitchJoystickPressed == true) {
                    Log.v(TAG, "rollPitchListener onChanged x:" + x + "y:" + y);
                    if (settings.isBeginnerMode()) {
                        aileronChannel.setValue(x * BEGINNER_AILERON_CHANNEL_RATIO);
                        elevatorChannel.setValue(y * BEGINNER_ELEVATOR_CHANNEL_RATIO);
                    } else {
                        aileronChannel.setValue(x);
                        elevatorChannel.setValue(y);
                    }
                }
            }

            @Override
            public void onPressed(JoystickBase joy) {
                rollAndPitchJoystickPressed = true;
            }

            @Override
            public void onReleased(JoystickBase joy) {
                rollAndPitchJoystickPressed = false;

                aileronChannel.setValue(0.0f);
                elevatorChannel.setValue(0.0f);
            }
        };

        rudderThrottleListener = new JoystickListener() {
            public void onChanged(JoystickBase joy, float x, float y) {
                if (BrainyCopterApplication.sharedApplicaion().getAppStage() == AppStage.SETTINGS) {
                    Log.w(TAG, "AppStage.SETTINGS ignore rudderThrottleListener onChanged");
                    return;
                }

                Log.v(TAG, "rudderThrottleListener onChanged x:" + x + ", y:" + y);

                if (settings.isBeginnerMode()) {
                    rudderChannel.setValue(x * BEGINNER_RUDDER_CHANNEL_RATIO);
                    throttleChannel.setValue((BEGINNER_THROTTLE_CHANNEL_RATIO - 1) + y * BEGINNER_THROTTLE_CHANNEL_RATIO);

                } else {
                    rudderChannel.setValue(x);
                    throttleChannel.setValue(y);
                }

                throttleText.setText(
                        (new StringBuilder(String.valueOf(context.getString(R.string.info_view_throttle_title))))
                                .append(" ")
                                .append((int) (((throttleChannel.getValue() + 1) / 2) * 100))
                                .append("%").toString());
            }

            @Override
            public void onPressed(JoystickBase joy) {

            }

            @Override
            public void onReleased(JoystickBase joy) {
                rudderChannel.setValue(0.0f);
                Log.v(TAG, "rudderThrottleListener onReleased" + joy.getYValue());

                throttleChannel.setValue(joy.getYValue());

                throttleText.setText(
                        (new StringBuilder(String.valueOf(context.getString(R.string.info_view_throttle_title))))
                                .append(" ")
                                .append((int) (((throttleChannel.getValue() + 1) / 2) * 100))
                                .append("%").toString());
            }
        };
    }

    private void initListeners() {
        settingsBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (delegate != null) {
                    delegate.settingsBtnDidClick(arg0);
                }

            }
        });

        takeOffToggleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isArmed == false) {
                    throttleChannel.setValue(-1);
                    getRudderAndThrottleJoystick().setYValue(-1);
                    Transmitter.sharedTransmitter().transmmitSimpleCommand(OSDCommon.MSPCommnand.MSP_ARM);
                    isArmed = true;
                }
                else {
                    Transmitter.sharedTransmitter().transmmitSimpleCommand(OSDCommon.MSPCommnand.MSP_DISARM);
                    isArmed = false;
                }
                takeOffToggleBtn.setChecked(isArmed);
            }
        });

        altHoldToggleBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                isAltHoldMode = !isAltHoldMode;
                settings.setIsAltHoldMode(isAltHoldMode);
                settings.save();

                altHoldToggleBtn.setChecked(isAltHoldMode);

                if (isAltHoldMode) {
                    aux2Channel.setValue(1);
                } else {
                    aux2Channel.setValue(-1);
                }
            }
        });


        initVideoListener();
    }

    private void initVideoListener() {

        galleryBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HudViewController.this.context, GalleryActivity.class);
                intent.putExtra("type", MediaUtil.MEDIA_TYPE_ALL);
                intent.putExtra("browser_type", GalleryActivity.BROWSER_TYPE_REMOTE);
                HudViewController.this.context.startActivity(intent);
            }
        });


        captureBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                AsyncTask<Void, Void, Void> captureTask = new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        captureBtn.setEnabled(false);
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        playSound(camera_click_sound);
                        if (!VmcConfig.getInstance().isStoreRemote()) {
                            String dirPath = Environment
                                    .getExternalStorageDirectory()
                                    .getAbsolutePath()
                                    + MediaUtil.IPC_IMAGE_DIR;

                            String filePath = generateFileName() + ".jpg";
                            ConnectStateManager
                                    .getInstance(BrainyCopterApplication.sharedApplicaion())
                                    .getIpcProxy()
                                    .doTakePhoto(dirPath, filePath, false);
                            MediaUtil.scanIpcMediaFile(HudViewController.this.context,
                                    dirPath + filePath);
                        } else {
                            ipcProxy.takePhotoRemote(false);
                        }
                        return null;
                    }

                    protected void onPostExecute(Void result) {
                        captureBtn.setEnabled(true);
                    }
                };
                captureTask.execute();

            }
        });

        recordBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                settingsBtn.setEnabled(false);
                galleryBtn.setEnabled(false);
                if (!isStartRecord) {
                    playSound(video_record_sound);
                    AsyncTask<Void, Void, Void> startRecordTask = new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected Void doInBackground(Void... params) {
                            if (!VmcConfig.getInstance().isStoreRemote()) {
                                String dirPath = Environment
                                        .getExternalStorageDirectory()
                                        .getAbsolutePath()
                                        + MediaUtil.IPC_VIDEO_DIR;
                                String filePath = generateFileName() + ".mp4";
                                mCustomOnRecordCompleteListener.setPath(dirPath
                                        + filePath);
                                ipcProxy.addOnRecordCompleteListener(mCustomOnRecordCompleteListener);
                                ipcProxy.doStartRecord(dirPath, null, filePath,
                                        false);
                            } else {
                                ipcProxy.startRecordRemote(false);
                            }
                            ((Activity) HudViewController.this.context)
                                    .runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            recordingIndicator.setVisible(true);
                                            recordingIndicator.start(0.6f);
                                            recordingIndicator.setAlpha(1);
                                        }
                                    });
                            isStartRecord = true;
                            return null;
                        }
                    };
                    startRecordTask.execute();
                } else {
                    stopRecord();
                }
            }
        });
    }

    private void stopRecord() {
        playSound(video_record_sound);
        AsyncTask<Void, Void, Void> stopRecordTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                // TODO Auto-generated method stub
                if (!VmcConfig.getInstance().isStoreRemote()) {
                    ipcProxy.doStopRecord();
                    ipcProxy.onRecordComplete(true);
                    ipcProxy.removeOnRecordCompleteListener(mCustomOnRecordCompleteListener);
                } else {
                    ipcProxy.stopRecordRemote();
                }
                isStartRecord = false;
                return null;
            }

            protected void onPostExecute(Void result) {
                recordingIndicator.stop();
                recordingIndicator.setVisible(false);
                recordingIndicator.setAlpha(0);
                settingsBtn.setEnabled(true);
                galleryBtn.setEnabled(true);
            }
        };
        stopRecordTask.execute();

    }

    private String generateFileName() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
        return sDateFormat.format(new java.util.Date());
    }


    private void initGLSurfaceView() {
        if (glView != null) {
            glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            glView.setRenderer(renderer);
            glView.setOnTouchListener(this);
        }
    }

    private void initJoysticks(JoystickType rollAndPitchType) {
        JoystickBase rollAndPitchJoystick = getRollAndPitchJoystick();
        JoystickBase rudderAndThrottleJoystick = getRudderAndThrottleJoystick();

        if (rollAndPitchType == JoystickType.ANALOGUE) {
            if (rollAndPitchJoystick == null || !(rollAndPitchJoystick instanceof AnalogueJoystick)) {
                rollAndPitchJoystick = JoystickFactory.createAnalogueJoystick(this.getContext(), false, rollPitchListener, true);
                rollAndPitchJoystick.setXDeadBand(settings.getAileronDeadBand());
                rollAndPitchJoystick.setYDeadBand(settings.getElevatorDeadBand());
            } else {
                rollAndPitchJoystick.setOnAnalogueChangedListener(rollPitchListener);
            }
        } else if (rollAndPitchType == JoystickType.ACCELERO) {
            if (rollAndPitchJoystick == null || !(rollAndPitchJoystick instanceof AcceleratorJoystick)) {
                rollAndPitchJoystick = JoystickFactory.createAcceleroJoystick(this.getContext(), false, rollPitchListener, true);
            } else {
                rollAndPitchJoystick.setOnAnalogueChangedListener(rollPitchListener);
            }
        }

        if (rudderAndThrottleJoystick == null || !(rudderAndThrottleJoystick instanceof AnalogueJoystick)) {
            rudderAndThrottleJoystick = JoystickFactory.createAnalogueJoystick(this.getContext(), false, rudderThrottleListener, false);
            rudderAndThrottleJoystick.setXDeadBand(settings.getRudderDeadBand());
        } else {
            rudderAndThrottleJoystick.setOnAnalogueChangedListener(rudderThrottleListener);
        }

        rollAndPitchJoystick.setIsRollPitchJoystick(true);
        rudderAndThrottleJoystick.setIsRollPitchJoystick(false);

        joysticks[0] = rollAndPitchJoystick;
        joysticks[1] = rudderAndThrottleJoystick;

        setJoysticks();

        getRudderAndThrottleJoystick().setYValue(-1);
    }

    public void setJoysticks() {
        JoystickBase rollAndPitchJoystick = joysticks[0];
        JoystickBase rudderAndThrottleJoystick = joysticks[1];

        if (rollAndPitchJoystick != null) {
            if (isLeftHanded) {
                joysticks[0].setAlign(Align.BOTTOM_RIGHT);
                joysticks[0].setAlpha(joypadOpacity);
            } else {
                joysticks[0].setAlign(Align.BOTTOM_LEFT);
                joysticks[0].setAlpha(joypadOpacity);
            }

            rollAndPitchJoystick.setNeedsUpdate();
        }

        if (rudderAndThrottleJoystick != null) {
            if (isLeftHanded) {
                joysticks[1].setAlign(Align.BOTTOM_LEFT);
                joysticks[1].setAlpha(joypadOpacity);
            } else {
                joysticks[1].setAlign(Align.BOTTOM_RIGHT);
                joysticks[1].setAlpha(joypadOpacity);
            }

            rudderAndThrottleJoystick.setNeedsUpdate();
        }

        for (int i = 0; i < joysticks.length; ++i) {
            JoystickBase joystick = joysticks[i];

            if (joystick != null) {
                joystick.setInverseYWhenDraw(true);

                int margin = context.getResources().getDimensionPixelSize(R.dimen.hud_joy_margin);

                joystick.setMargin(0, margin, 48 + margin, margin);
            }
        }

        renderer.removeSprite(JOY_ID_LEFT);
        renderer.removeSprite(JOY_ID_RIGHT);

        if (rollAndPitchJoystick != null) {
            if (isLeftHanded) {
                renderer.addSprite(JOY_ID_RIGHT, rollAndPitchJoystick);
            } else {
                renderer.addSprite(JOY_ID_LEFT, rollAndPitchJoystick);
            }
        }

        if (rudderAndThrottleJoystick != null) {
            if (isLeftHanded) {
                renderer.addSprite(JOY_ID_LEFT, rudderAndThrottleJoystick);
            } else {
                renderer.addSprite(JOY_ID_RIGHT, rudderAndThrottleJoystick);
            }
        }
    }

    public JoystickBase getRollAndPitchJoystick() {
        return joysticks[0];
    }

    public JoystickBase getRudderAndThrottleJoystick() {
        return joysticks[1];
    }

    public void setInterfaceOpacity(float opacity) {
        if (opacity < 0 || opacity > 100.0f) {
            Log.w(TAG, "Can't set interface opacity. Invalid value: " + opacity);
            return;
        }

        joypadOpacity = opacity / 100f;

        Sprite joystick = renderer.getSprite(JOY_ID_LEFT);
        joystick.setAlpha(joypadOpacity);

        joystick = renderer.getSprite(JOY_ID_RIGHT);
        joystick.setAlpha(joypadOpacity);
    }

    public void setBatteryValue(final int percent) {
        if (percent > 100 || percent < 0) {
            Log.w(TAG, "Can't set battery value. Invalid value " + percent);
            return;
        }

        int imgNum = Math.round((float) percent / 100.0f * 4.0f);

        if (imgNum < 0)
            imgNum = 0;

        if (imgNum > 4)
            imgNum = 4;

        if (batteryIndicator != null) {
            batteryIndicator.setValue(imgNum);
        }
    }

    public void setSettingsButtonEnabled(boolean enabled) {
        settingsBtn.setEnabled(enabled);
    }

    public void setDoubleTapClickListener(GestureDetector.OnDoubleTapListener listener) {
        gestureDetector.setOnDoubleTapListener(listener);
    }

    public void onPause() {
        if (glView != null) {
            glView.onPause();
        }

        deviceOrientationManager.pause();
    }

    public void onResume() {
        if (glView != null) {
            glView.onResume();
        }

        deviceOrientationManager.resume();

        if (ipcProxy != null)
            ipcProxy.doStartPreview();
    }

    //glView onTouch Event handler
    public boolean onTouch(View v, MotionEvent event) {
        boolean result = false;

        for (int i = 0; i < buttons.length; ++i) {
            if (buttons[i].processTouch(v, event)) {
                result = true;
                break;
            }
        }

        if (result != true) {
            gestureDetector.onTouchEvent(event);

            for (int i = 0; i < joysticks.length; ++i) {
                JoystickBase joy = joysticks[i];
                if (joy != null) {
                    if (joy.processTouch(v, event)) {
                        result = true;
                    }
                }
            }
        }

        return result;
    }

    public void onDestroy() {
        renderer.clearSprites();
        deviceOrientationManager.destroy();
        unregisterAllBroadcastReceiver();
        this.context.unbindService(mConnection);
    }

    public boolean onDown(MotionEvent e) {
        return false;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        return false;
    }

    public void onLongPress(MotionEvent e) {
        // Left unimplemented
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        return false;
    }

    public void onShowPress(MotionEvent e) {
        // Left unimplemented
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    public View getRootView() {
        if (glView != null) {
            return glView;
        }

        Log.w(TAG, "Can't find root view");
        return null;
    }

    @Override
    public void interfaceOpacityValueDidChange(float newValue) {
        setInterfaceOpacity(newValue);
    }

    @Override
    public void leftHandedValueDidChange(boolean isLeftHanded) {
        this.isLeftHanded = isLeftHanded;

        setJoysticks();

        Log.e(TAG, "THRO:" + throttleChannel.getValue());

        getRudderAndThrottleJoystick().setYValue(throttleChannel.getValue());
    }

    @Override
    public void accModeValueDidChange(boolean isAccMode) {
        this.isAccMode = isAccMode;

        initJoystickListeners();

        if (isAccMode) {
            initJoysticks(JoystickType.ACCELERO);
        } else {
            initJoysticks(JoystickType.ANALOGUE);
        }
    }

    @Override
    public void headfreeModeValueDidChange(boolean isHeadfree) {
        if (settings.isHeadFreeMode()) {
            aux1Channel.setValue(1);
        } else {
            aux1Channel.setValue(-1);
        }
    }

    @Override
    public void autoAltHoldModeValueDidChange(boolean isAutoAltHoldMode) {

    }

    @Override
    public void aileronAndElevatorDeadBandValueDidChange(float newValue) {
        JoystickBase rollAndPitchJoyStick = getRollAndPitchJoystick();

        rollAndPitchJoyStick.setXDeadBand(newValue);
        rollAndPitchJoyStick.setYDeadBand(newValue);
    }

    @Override
    public void rudderDeadBandValueDidChange(float newValue) {
        JoystickBase rudderAndThrottleStick = getRudderAndThrottleJoystick();

        rudderAndThrottleStick.setXDeadBand(newValue);
    }

    @Override
    public void onDeviceOrientationChanged(float[] orientation,
                                           float magneticHeading, int magnetoAccuracy) {
        if (rollAndPitchJoystickPressed == false) {
            pitchBase = orientation[PITCH];
            rollBase = orientation[ROLL];
            aileronChannel.setValue(0.0f);
            elevatorChannel.setValue(0.0f);

            //Log.v(TAG, "before pressed ROLL:" + rollBase + ",PITCH:" + pitchBase);
        } else {
            float x = (orientation[PITCH] - pitchBase);
            float y = (orientation[ROLL] - rollBase);

            if (isAccMode) {
                Log.v(TAG, "ROLL:" + (-x) + ",PITCH:" + y);

                if (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD) {
                    if (settings.isBeginnerMode()) {
                        aileronChannel.setValue(-x * BEGINNER_AILERON_CHANNEL_RATIO);
                        elevatorChannel.setValue(y * BEGINNER_ELEVATOR_CHANNEL_RATIO);
                    } else {
                        aileronChannel.setValue(-x);
                        elevatorChannel.setValue(y);
                    }
                }
            }
        }
    }

    @Override
    public void didConnect() {
        bleIndicator.setValue(0);
        isConnected = true;
    }

    @Override
    public void didDisconnect() {
        bleIndicator.setValue(1);
        isConnected = false;
    }

    @Override
    public void didFailToConnect() {
        bleIndicator.setValue(1);
        isConnected = false;
    }

    @Override
    public void beginnerModeValueDidChange(boolean isBeginnerMode) {

    }


    @Override
    public void tryingToConnect(String target) {
        ApplicationSettings settings = BrainyCopterApplication.sharedApplicaion().getAppSettings();

        if (target.equals("FlexBLE")) {
            if (settings.getFlexbotVersion().equals("1.5.0") == false) {
                settings.setFlexbotVersion("1.5.0");
                settings.save();
            }

            BrainyCopterApplication.sharedApplicaion().setFullDuplex(true);
        } else {
            settings.getFlexbotVersion().equals("1.0.0");
            settings.save();
            BrainyCopterApplication.sharedApplicaion().setFullDuplex(false);

        }
    }

    private void updateUI() {
        OSDData osddata = Transmitter.sharedTransmitter().getOsdData();
        if (osddata != null) {
            Resources res = context.getResources();

            if (osddata.getIsLocked()) {
                isArmed = false;
                takeOffToggleBtn.setChecked(false);
            } else {
                isArmed = true;
                takeOffToggleBtn.setChecked(true);
            }
        }
        if (!isConnected) {
            warnningTextView.setText(context.getString(R.string.info_view_warnning_not_connected));
        } else {
            warnningTextView.setText(context.getString(R.string.info_view_warnning_connected));
        }

/*
        float throttleValue = throttleChannel.getValue();

        OSDData osddata = Transmitter.sharedTransmitter().getOsdData();

        if ((double)throttleValue < -0.999D && isConnected)
        {
            initialVBat = osddata.getvBat();
        }

        label0:
        {
            label1:
            {
                refreshWifiLevel();
                float f = throttleChannel.getValue();
                OSDData osddata = Transmitter.sharedTransmitter().getOsdData();
                if ((double)f < -0.999D && isConnected)
                {
                    initialVBat = osddata.getvBat();
                }

                f = (float)(((double)(throttleChannel.getValue() + 1.0F) / 2D) * 0.40000000000000002D);
                float f1 = initialVBat + f;
                f = f1;
                if (f1 > initialVBat)
                {
                    f = initialVBat;
                }

                if (osddata != null)
                {
                    DecimalFormat decimalformat;

                    if ((double)f > 3.5D)
                    {
                        float f2 = (float)(((double)f - 3.5D) / 1.0D);
                        f = f2;
                        if ((double)f2 > 0.93000000000000005D)
                        {
                            f = 1.0F;
                        }
                    } else
                    {
                        f = 0.0F;
                    }
                    if ((double)f < 0.14999999999999999D)
                    {
                        batteryTextViewText.setTextColor(0xffff0000);
                        powerIsLow = true;
                    } else
                    {
                        batteryTextViewText.setTextColor(-256);
                        powerIsLow = false;
                    }
                    decimalformat = new DecimalFormat("0.0");
                    batteryTextViewText.setText((new StringBuilder(String.valueOf(context.getString(0x7f07003e)))).append(" ").append(decimalformat.format(osddata.getvBat())).append("v").toString());
                    if (!isConnected)
                    {
                        break label0;
                    }
                    if (!signalIsWeek)
                    {
                        break label1;
                    }
                    warnningTextViewText.setText(context.getString(0x7f070054));
                }
                return;
            }
            warnningTextViewText.setText("");
            return;
        }
        warnningTextViewText.setText(context.getString(0x7f070052));
        */
    }


    private void registerAllBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        this.context.registerReceiver(receiver, filter);
        IntentFilter decodeFilter = new IntentFilter();
        decodeFilter.addAction(IpcControlService.ACTION_NAVDATA_BATTERYSTATECHANGED);
        decodeFilter.addAction(IpcProxy.ACTION_DECODEMODE_CHANGED);
        decodeFilter.addAction(IpcProxy.ACTION_CONNECT_QUALITY_CHANGED);
        decodeFilter.addAction(ACTION_RESTART_PREVIEW);
        decodeFilter.addAction(IpcProxy.ACTION_REFRESH_DEBUG);
        //decodeFilter.addAction(VideoSettingView.ACTION_DEBUG_PRIVEW);
        mLocalBroadcastManager.registerReceiver(receiver, decodeFilter);
    }


    private void unregisterAllBroadcastReceiver() {
        this.context.unregisterReceiver(receiver);
        mLocalBroadcastManager.unregisterReceiver(receiver);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
//	    if (action.equals(Intent.ACTION_TIME_CHANGED)) {
//	    	//*text_time.setText(SystemUtil.getCurrentFormatTime());
//	    }
//	    else if (action.equals(Intent.ACTION_TIME_TICK)) {
//	    	//*text_time.setText(SystemUtil.getCurrentFormatTime());
//	    }

            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                final int level = intent.getIntExtra(
                        BatteryManager.EXTRA_LEVEL, 0);
                final int scale = intent.getIntExtra(
                        BatteryManager.EXTRA_SCALE, 0);
                final int status = intent.getIntExtra(
                        BatteryManager.EXTRA_STATUS, 0);
                setBatteryValue(level);

                //*battery_phone.setImageLevel(level / 25);
                //*battery_phone_text.setText(level + "%");
            } else if (action
                    .equals(IpcControlService.ACTION_NAVDATA_BATTERYSTATECHANGED)) {
                final String str = intent
                        .getStringExtra(IpcControlService.EXTRA_BATTERY_LEVEL);
                // Log.e(TAG, String.format("device level=%s", level));
                if (str == null)
                    return;
                String[] infos = str.trim().split(",");
                int level = Integer.parseInt(infos[0]);
                boolean plugin = false;

                if (infos.length > 1) {
                    plugin = Integer.parseInt(infos[1]) > 0 ? true : false;
                }
                //if (isAcPlugin != plugin) {
                /**
                 if (plugin) {
                 battery_device
                 .setImageResource(R.drawable.indication_ac_plugin);
                 AnimationDrawable animation = (AnimationDrawable) battery_device
                 .getDrawable();
                 animation.start();
                 } else {
                 battery_device
                 .setImageResource(R.drawable.device_battery_level);
                 }
                 */
                //}
                //if (!plugin) {
                // deviceBatteryIndicator.setValue(Math.min(level / 25, 3));
                //}
                isAcPlugin = plugin;
                //*battery_device_text.setText(level + "%");
                if (level < 10) {
                    //*showWarningMessage(getResources().getString(R.string.BATTERY_LOW_ALERT));
                } else {
                    //*hideWarningMessage();
                }
            } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                refreshWifiLevel();
            } else if (action.equals(IpcProxy.ACTION_DECODEMODE_CHANGED)) {
                Log.d(TAG, "IpcProxy.ACTION_DECODEMODE_CHANGED");
                onDecodeModeChanged(intent
                        .getStringExtra(IpcProxy.EXTRA_DECODE_MODE));
            } else if (action.equals(IpcProxy.ACTION_CONNECT_QUALITY_CHANGED)) {
                int state = intent.getIntExtra(IpcProxy.EXTRA_CONNECT_QUALITY,
                        0);
                if (state == 1) {
                    //*hideWarningMessage();
                } else if (state == -1) {
                    //*showWarningMessage(getResources().getString(R.string.VIDEO_CONNECTION_ALERT));
                }
            } else if (action.equals(ACTION_RESTART_PREVIEW)) {
                int mode = intent.getIntExtra("decodemode", 1);
                Log.d(TAG, action + "---" + mode);
                ipcProxy.stopPreview();
                setDecodeMode(mode);
                ipcProxy.startPreview();
            } else if (action.equals(IpcProxy.ACTION_REFRESH_DEBUG)) {
                String info = intent.getStringExtra(IpcProxy.EXTRA_DEBUG_INFO);
                // DebugHandler.logd(TAG, "info:"+info);
                //*if (debugInfo != null && info != null && info.length() > 0)
                //*    debugInfo.setText(info);
                //} else if (action.equals(VideoSettingView.ACTION_DEBUG_PRIVEW)) {
                //*debugSwitch.setVisibility(View.VISIBLE);
            }
        }
    };

    public void setDecodeMode(int decodeMode) {
        Log.d(TAG, "decodeMode is " + decodeMode);
        ipcProxy.setIpcDecMode(decodeMode);
        switch (decodeMode) {
            case 0: {
                videoStageHard.setVisibility(View.GONE);
                videoStageSoft.setVisibility(View.GONE);
                videoStageHard.setVisibility(View.VISIBLE);
                break;
            }
            case 1: {
                videoStageSoft.setVisibility(View.VISIBLE);
                videoStageHard.setVisibility(View.GONE);
                break;
            }
            case 2: {
                videoStageHard.setVisibility(View.GONE);
                videoStageSoft.setVisibility(View.GONE);
                videoStageHard.setVisibility(View.VISIBLE);
                break;
            }
        }
        VmcConfig.getInstance().setDecodeMode(decodeMode);
    }


    private class CustomOnRecordCompleteListener implements
            OnRecordCompleteListener {

        String filePath;

        public CustomOnRecordCompleteListener() {
        }

        public CustomOnRecordCompleteListener(String path) {
            filePath = path;
        }

        public void setPath(String path) {
            filePath = path;
        }

        @Override
        public void onRecordComplete(boolean isSuccess) {
            // TODO Auto-generated method stub
            if (isSuccess)
                MediaUtil.scanIpcMediaFile(HudViewController.this.context, filePath);
            else {
                DebugHandler.logWithToast(HudViewController.this.context,
                        "Sorry!Record fail.", 2000);
            }
            ipcProxy.removeOnRecordCompleteListener(this);
        }

    }

    ;

    private void onDecodeModeChanged(String mode) {
        DebugHandler.logd(TAG, "onDecodeModeChanged:" + mode);
        if (mode.equals("softdec")) {
            videoStageHard.setVisibility(View.GONE);
            videoStageSoft.setVisibility(View.VISIBLE);
        } else {
            videoStageHard.setVisibility(View.VISIBLE);
            videoStageSoft.setVisibility(View.GONE);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            controlService = ((IpcControlService.LocalBinder) service).getService();
            controlService.getConnectStateManager().addConnectChangedListener(
                    mOnIpcConnectChangedListener);
            // onDroneServiceConnected();
            initUiControlShow();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            controlService.getConnectStateManager()
                    .removeConnectChangedListener(mOnIpcConnectChangedListener);
            controlService = null;
        }
    };

    private OnIpcConnectChangedListener mOnIpcConnectChangedListener = new OnIpcConnectChangedListener() {

        @Override
        public void OnIpcConnected() {
            wifiIndicator.setVisible(true);
            captureBtn.setEnabled(true);
            recordBtn.setEnabled(true);
            canRefreshUI = true;
            setCurrentDecodeMode();
        }

        @Override
        public void OnIpcDisConnected() {
            wifiIndicator.setVisible(false);
            captureBtn.setEnabled(false);
            recordBtn.setEnabled(false);

            if (isStartRecord) {
                stopRecord();
            }

            if (canRefreshUI) {
                //glView.invalidate();
                videoStageHard.setVisibility(View.GONE);
                videoStageSoft.setVisibility(View.GONE);
            }

            canRefreshUI = false;
        }

        @Override
        public void onIpcPaused() {

        }

        @Override
        public void onIpcResumed() {
        }
    };


    private void refreshWifiLevel() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info.getBSSID() != null) {
            int strength = WifiManager.calculateSignalLevel(info.getRssi(), 4);
            Log.d(TAG, String.format("strength=%d", strength));

            int imgNum = strength;

            //txtBatteryStatus.setText(percent + "%");

            if (imgNum < 0)
                imgNum = 0;

            if (imgNum > 3)
                imgNum = 3;

            if (wifiIndicator != null) {
                wifiIndicator.setValue(imgNum);
            }
        }
    }


    @Override
    public void viewWillAppear() {
        // TODO Auto-generated method stub
        super.viewWillAppear();
    }


    @Override
    public void viewWillDisappear() {
        // TODO Auto-generated method stub
        super.viewWillDisappear();

        // TODO Auto-generated method stub
        if (isStartRecord) {
            AsyncTask<Void, Void, Void> stopRecordTask = new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    // TODO Auto-generated method stub
                    if (!VmcConfig.getInstance().isStoreRemote()) {
                        ipcProxy.doStopRecord();
                        ipcProxy.onRecordComplete(true);
                        ipcProxy.removeOnRecordCompleteListener(mCustomOnRecordCompleteListener);
                    } else {
                        ipcProxy.stopRecordRemote();
                    }
                    isStartRecord = false;
                    ipcProxy.doStopPreview();
                    return null;
                }
            };
            stopRecordTask.execute();
        } else {
            ipcProxy.doStopPreview();
        }


        if (mSoundPool != null)
            mSoundPool.release();
        //super.onStop();

    }

    private void initSound() {
        if (mSoundPool == null) {
            mSoundPool = new SoundPool(2, AudioManager.STREAM_SYSTEM, 0);
        }
        camera_click_sound = mSoundPool.load(context, R.raw.camera_click, 1);
        video_record_sound = mSoundPool.load(context, R.raw.video_record, 1);
    }

    private void playSound(int soundId) {
        if (mSoundPool != null) mSoundPool.play(soundId, 1, 1, 0, 0, 1);
    }
}
