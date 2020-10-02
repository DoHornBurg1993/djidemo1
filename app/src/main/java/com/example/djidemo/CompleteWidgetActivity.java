package com.example.djidemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dji.mapkit.core.camera.DJICameraUpdateFactory;
import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJIBitmapDescriptorFactory;
import com.dji.mapkit.core.models.DJILatLng;
import com.dji.mapkit.core.models.annotations.DJIMarker;
import com.dji.mapkit.core.models.annotations.DJIMarkerOptions;
import com.dji.mapkit.core.models.annotations.DJIPolyline;
import com.dji.mapkit.core.models.annotations.DJIPolylineOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import dji.common.accessory.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.flyzone.FlyZoneCategory;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.AccessoryAggregationKey;
import dji.keysdk.CameraKey;
import dji.keysdk.DJIKey;
import dji.keysdk.KeyManager;
import dji.sdk.accessory.AccessoryAggregation;
import dji.sdk.accessory.beacon.Beacon;
import dji.sdk.accessory.speaker.AudioFileInfo;
import dji.sdk.accessory.speaker.Speaker;
import dji.sdk.accessory.speaker.TransmissionListener;
import dji.sdk.accessory.spotlight.Spotlight;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.ux.widget.FPVWidget;
import dji.ux.widget.MapWidget;
import dji.ux.widget.controls.CameraControlsWidget;

import static dji.common.camera.SettingsDefinitions.DisplayMode.MSX;
import static dji.common.camera.SettingsDefinitions.DisplayMode.THERMAL_ONLY;
import static dji.common.camera.SettingsDefinitions.DisplayMode.VISUAL_ONLY;

/**
 * @author： DuHongBo
 */
public class CompleteWidgetActivity extends Activity implements View.OnClickListener, DJIMap.OnMapClickListener {

    private MapWidget mapWidget;
    private ViewGroup parentView;
    private FPVWidget fpvWidget;
    private FPVWidget secondaryFPVWidget;
    private RelativeLayout primaryVideoView;
    private FrameLayout secondaryVideoView;
    private boolean isMapMini = true;

    private int height;
    private int width;
    private int margin;
    private int deviceWidth;
    private int deviceHeight;
    private AudioRecorderHandler audioRecoderHandler;
    private int last_rec,msx_values=0;
    private Camera camera;
    private Button spotlight,beacon,rec_speaker,play_mode,stop_record;
    private Button visual_only,thermal_only,msx,msx_plus,msx_minus;
    private TextView camera_status;
    private AccessoryAggregation accessoryAggregation;
    private Spotlight light;
    private Beacon beacon_light;
    private Speaker speaker;
    private DJIKey beaconEnabledKey;
    private DJIKey spotlightEnabledKey;
    private DJIKey playModeKey;
    protected static final String TAG = "CompleteWidgetActivity";

    private DJIMap aMap;
    private Button locate, add, clear,rtmp;
    private Button config, upload, start, stop,pause;
    private boolean isAdd = false,pause_resume=true;

    private double droneLocationLat = 181, droneLocationLng = 181;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;
    private List<DJIMarker> mark_on_map=new ArrayList<>();
    private List<DJIPolyline> line_on_map=new ArrayList<>();
    private LiveStreamManager liveStreamManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_widgets);
        IntentFilter filter = new IntentFilter();
        filter.addAction(FPVDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
        height = DensityUtil.dip2px(this, 200);
        width = DensityUtil.dip2px(this, 300);
        margin = DensityUtil.dip2px(this, 12);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;
        mapWidget = findViewById(R.id.map_widget);
        mapWidget.initAMap(new MapWidget.OnMapReadyListener() {
            @Override
            public void onMapReady(@NonNull DJIMap map) {
                map.setOnMapClickListener(new DJIMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(DJILatLng latLng) {
                        onViewClick(mapWidget);
                    }
                });
            }
        });
        mapWidget.onCreate(savedInstanceState);

        parentView = (ViewGroup) findViewById(R.id.root_view);

        fpvWidget = findViewById(R.id.fpv_widget);
        fpvWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onViewClick(fpvWidget);
            }
        });
        primaryVideoView = (RelativeLayout) findViewById(R.id.fpv_container);
        secondaryVideoView = (FrameLayout) findViewById(R.id.secondary_video_view);
        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget);
        secondaryFPVWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapVideoSource();
            }
        });
        updateSecondaryVideoVisibility();

        initUI();
        initDJIKey();
        initMapView();
        addListener();
        audioRecoderHandler = new AudioRecorderHandler(this);
    }

    private void initUI() {
        spotlight=findViewById(R.id.spotlight);
        beacon=findViewById(R.id.beacon);
        rec_speaker=findViewById(R.id.rec_speaker);
        findViewById(R.id.speaker_volume).setOnClickListener(this);
        play_mode=findViewById(R.id.play_mode);
        stop_record=findViewById(R.id.stop_record);
        findViewById(R.id.play_record).setOnClickListener(this);
        findViewById(R.id.stop_playing).setOnClickListener(this);
        visual_only=findViewById(R.id.visual_only);
        thermal_only=findViewById(R.id.thermal_only);
        msx=findViewById(R.id.msx);
        msx_plus=findViewById(R.id.msx_plus);
        msx_minus=findViewById(R.id.msx_minus);
        camera_status=findViewById(R.id.camera_status);
        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        upload = (Button) findViewById(R.id.upload);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        pause=(Button) findViewById(R.id.pause) ;
        rtmp=findViewById(R.id.rtmp);

        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        pause.setOnClickListener(this);
        spotlight.setOnClickListener(this);
        beacon.setOnClickListener(this);
        rec_speaker.setOnClickListener(this);
        play_mode.setOnClickListener(this);
        stop_record.setOnClickListener(this);
        visual_only.setOnClickListener(this);
        thermal_only.setOnClickListener(this);
        msx.setOnClickListener(this);
        msx_plus.setOnClickListener(this);
        msx_minus.setOnClickListener(this);
        rtmp.setOnClickListener(this);
    }

    private void initDJIKey() {
        beaconEnabledKey = AccessoryAggregationKey.createBeaconKey(AccessoryAggregationKey.BEACON_ENABLED);
        spotlightEnabledKey = AccessoryAggregationKey.createSpotlightKey(AccessoryAggregationKey.SPOTLIGHT_ENABLED);
        playModeKey = AccessoryAggregationKey.createSpeakerKey(AccessoryAggregationKey.PLAY_MODE);
    }

    private void initMapView() {

        if (aMap == null) {
            aMap = mapWidget.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
            mapWidget.setFlyZoneVisible(FlyZoneCategory.RESTRICTED,true);
            mapWidget.setMapCenterLock(MapWidget.MapCenterLock.NONE);
        }
    }

    private void onViewClick(View view) {
        if (view == fpvWidget && !isMapMini) {
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = true;
        } else if (view == mapWidget && isMapMini) {
            hidePanels();
            resizeFPVWidget(width, height, margin, 12);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = false;
        }
    }

    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        RelativeLayout.LayoutParams fpvParams = (RelativeLayout.LayoutParams) primaryVideoView.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.rightMargin = margin;
        fpvParams.bottomMargin = margin;
        if (isMapMini) {
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        } else {
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        }
        primaryVideoView.setLayoutParams(fpvParams);

        parentView.removeView(primaryVideoView);
        parentView.addView(primaryVideoView, fpvInsertPosition);
    }

    private void reorderCameraCapturePanel() {
        View cameraCapturePanel = findViewById(R.id.CameraCapturePanel);
        parentView.removeView(cameraCapturePanel);
        parentView.addView(cameraCapturePanel, isMapMini ? 9 : 13);
    }

    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == FPVWidget.VideoSource.SECONDARY) {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
        } else {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
        }
    }

    private void updateSecondaryVideoVisibility() {
        if (secondaryFPVWidget.getVideoSource() == null) {
            secondaryVideoView.setVisibility(View.GONE);
        } else {
            secondaryVideoView.setVisibility(View.VISIBLE);
        }
    }

    private void hidePanels() {
        //These panels appear based on keys from the drone itself.
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.HISTOGRAM_ENABLED), false, null);
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.COLOR_WAVEFORM_ENABLED), false, null);
        }

        //These panels have buttons that toggle them, so call the methods to make sure the button state is correct.
        CameraControlsWidget controlsWidget = findViewById(R.id.CameraCapturePanel);
        controlsWidget.setAdvancedPanelVisibility(false);
        controlsWidget.setExposurePanelVisibility(false);

        //These panels don't have a button state, so we can just hide them.
        findViewById(R.id.pre_flight_check_list).setVisibility(View.GONE);
        findViewById(R.id.rtk_panel).setVisibility(View.GONE);
        findViewById(R.id.spotlight_panel).setVisibility(View.GONE);
        findViewById(R.id.speaker_panel).setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 隐藏导航栏和状态栏。
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        mapWidget.onResume();
        initFlightController();
    }

    @Override
    protected void onPause() {
        mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        unregisterReceiver(mReceiver);
        removeListener();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    @Override
    public void onMapClick(DJILatLng djiLatLng) {
        if (isAdd){
            markWaypoint(djiLatLng);
            Waypoint mWaypoint = new Waypoint(djiLatLng.latitude, djiLatLng.longitude, altitude);

            int j =mark_on_map.size();
            Log.e("mark_on_map",j+"");
            if(mark_on_map.size()>1){
                DJIPolyline p = aMap.addPolyline((new DJIPolylineOptions()).add(mark_on_map.get(j - 2).getPosition(), mark_on_map.get(j - 1).getPosition()).color(Color.RED).width(5));
                line_on_map.add(p);
            }
            mWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,-70));
            mWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,3));
            mWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,0));
            mWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,6));
            mWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,-50));
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }

    private class ResizeAnimation extends Animation {

        private View mView;
        private int mToHeight;
        private int mFromHeight;

        private int mToWidth;
        private int mFromWidth;
        private int mMargin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            mToHeight = toHeight;
            mToWidth = toWidth;
            mFromHeight = fromHeight;
            mFromWidth = fromWidth;
            mView = v;
            mMargin = margin;
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = mMargin;
            p.bottomMargin = mMargin;
            mView.requestLayout();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.spotlight: {
                if (accessoryAggregation != null&light != null) {
                    Boolean isSpotlightEnabled = (Boolean) KeyManager.getInstance().getValue(spotlightEnabledKey);
                    if (isSpotlightEnabled != null) {
                        light.setEnabled(!isSpotlightEnabled, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (!isSpotlightEnabled) {
                                    setResultToToast("照明开启: " + (djiError == null ? "Success!" : djiError.getDescription()));
                                } else {
                                    setResultToToast("照明关闭: " + (djiError == null ? "Success!" : djiError.getDescription()));
                                }
                            }
                        });
                    }
                }
                break;
            }
            case R.id.beacon: {
                if (accessoryAggregation != null&beacon_light != null) {
                    Boolean isBeacoEnabled = (Boolean) KeyManager.getInstance().getValue(beaconEnabledKey);
                    if (isBeacoEnabled != null) {
                        beacon_light.setEnabled(!isBeacoEnabled, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (!isBeacoEnabled) {
                                    setResultToToast("夜航灯开启: " + (djiError == null ? "Success!" : djiError.getDescription()));
                                } else {
                                    setResultToToast("夜航灯关闭: " + (djiError == null ? "Success!" : djiError.getDescription()));
                                }
                            }
                        });
                    }
                }

                break;
            }

            case R.id.speaker_volume: {
                showSpeakerVolumeDialog();
                break;
            }

            case R.id.play_mode: {
                if (speaker != null) {
                    SettingsDefinitions.PlayMode playMode = (SettingsDefinitions.PlayMode) KeyManager.getInstance().getValue(playModeKey);
                    if (playMode == SettingsDefinitions.PlayMode.REPEAT_SINGLE) {
                        speaker.setPlayMode(SettingsDefinitions.PlayMode.SINGLE_ONCE, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError == null) {
                                    runOnUiThread(new Runnable() {
                                        @SuppressLint("SetTextI18n")
                                        @Override
                                        public void run() {
                                            play_mode.setText("SINGLE_ONCE");
                                        }
                                    });
                                } else
                                    setResultToToast("play mode change fail!");
                            }
                        });
                    } else {
                        speaker.setPlayMode(SettingsDefinitions.PlayMode.REPEAT_SINGLE, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError == null) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            play_mode.setText("REPEAT_SINGLE");
                                        }
                                    });
                                } else
                                    setResultToToast("play mode change fail!");
                            }
                        });
                    }
                } else {
                    setResultToToast("Speaker disconnected!");
                }
                break;
            }
            case R.id.rec_speaker: {
                if (speaker != null) {
                    long time = Calendar.getInstance().getTimeInMillis();
                    AudioFileInfo uploadInfo = new AudioFileInfo("" + time,
                            SettingsDefinitions.AudioStorageLocation.TEMPORARY);

                    speaker.startTransmission(uploadInfo, new TransmissionListener() {
                        @Override
                        public void onStart() {
                            setResultToToast("Transmit started");
                            startRecordUsingMic(speaker);
                        }

                        @Override
                        public void onProgress(int dataSize) {
                            setResultToToast("Transmitting:" + "Size:" + dataSize);
                        }

                        @Override
                        public void onFinish(int index) {
                            setResultToToast("Transmit finished!" + index);
                            last_rec = index;
                        }

                        @Override
                        public void onFailure(DJIError error) {
                            setResultToToast("Transmit failure:" + error.getDescription());
                        }
                    });

                } else {
                    setResultToToast("Speaker disconnected!");
                }
                break;
            }
            case R.id.stop_record:
                if (speaker != null) {
                    if (audioRecoderHandler != null) {
                        audioRecoderHandler.stopRecord();

                    }
                } else {
                    setResultToToast("Speaker disconnected!");
                }
                break;
            case R.id.play_record: {
                if (speaker != null) {
                    speaker.play(last_rec, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            setResultToToast("广播开始: " + (djiError == null ? "Success!" : djiError.getDescription()));
                        }
                    });
                } else {
                    setResultToToast("Speaker disconnected!");
                }
                break;
            }
            case R.id.stop_playing: {
                if (speaker != null) {
                    speaker.stop(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            setResultToToast("广播停止: " + (djiError == null ? "Success!" : djiError.getDescription()));
                        }
                    });
                } else {
                    setResultToToast("Speaker disconnected!");
                }
                break;
            }
            case R.id.visual_only:{
                if(camera!=null){
                    camera.setDisplayMode(VISUAL_ONLY, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            setResultToToast("Mode: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                            if (djiError == null)
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        camera_status.setText("模式：摄像机");
                                    }
                                });

                        }
                    });
                }else
                    setResultToToast("Mode:NotSupport " );
                break;
            }
            case R.id.thermal_only:{
                if(camera!=null){
                    camera.setDisplayMode(THERMAL_ONLY, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            setResultToToast("Mode: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                            if (djiError == null)
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        camera_status.setText("模式：热成像");
                                    }
                                });
                        }
                    });
                }else
                    setResultToToast("Mode:NotSupport " );
                break;
            }
            case R.id.msx:{
                if(camera!=null){
                    camera.setDisplayMode(MSX, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            setResultToToast("Mode: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                            if (djiError == null)
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        camera_status.setText("模式：混合");
                                    }
                                });
                        }
                    });
                }else
                    setResultToToast("Mode:NotSupport " );
                break;
            }
            case R.id.msx_plus:{
                if(camera!=null&msx_values<=95) {
                    msx_values += 5;
                    camera.setMSXLevel(msx_values, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            setResultToToast(djiError == null ? "Successfully" + msx_values + "-100" : djiError.getDescription());
                        }
                    });
                }
                break;
            }
            case R.id.msx_minus:{
                if(camera!=null&msx_values>=5) {
                    msx_values -= 5;
                    camera.setMSXLevel(msx_values, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            setResultToToast(djiError == null ? "Successfully" + msx_values + "-100" : djiError.getDescription());
                        }
                    });
                }
                break;
            }
            case R.id.locate:{
//                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }
            case R.id.add:{
                enableDisableAdd();
                break;
            }
            case R.id.clear: {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i=0;i<line_on_map.size();i++)
                            line_on_map.get(i).remove();
                        for(int j=0;j<mark_on_map.size();j++)
                            mark_on_map.get(j).remove();
                        line_on_map.clear();
                        mark_on_map.clear();
                    }

                });
                waypointList.clear();
                waypointMissionBuilder.waypointList(waypointList);
//                updateDroneLocation();
                break;
            }
            case R.id.config:{
                showSettingDialog();
                break;
            }
            case R.id.upload:{
                uploadWayPointMission();
                break;
            }
            case R.id.start:{
                startWaypointMission();
                break;
            }
            case R.id.stop:{
                stopWaypointMission();
                break;
            }
            case R.id.pause:{
                if(pause_resume)
                    pauseWaypointMission();
                else
                    resumeWaypointMission();
                break;
            }
            case R.id.rtmp:{
                if(liveStreamManager!=null) {
                    if (liveStreamManager.isStreaming()) {
                        AlertDialog.Builder normalDialog = new AlertDialog.Builder(CompleteWidgetActivity.this);
                        normalDialog.setMessage("停止直播?");
                        normalDialog.setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        liveStreamManager.stopStream();
                                        rtmp.setText("开始直播！");
                                    }
                                });
                        normalDialog.setNegativeButton("关闭",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });
                        // 显示
                        normalDialog.show();
                    }else{
                        final EditText editText = new EditText(CompleteWidgetActivity.this);
                        AlertDialog.Builder inputDialog =new AlertDialog.Builder(CompleteWidgetActivity.this);
                        inputDialog.setTitle("请输入直播地址：").setView(editText);
                        inputDialog.setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
//                                        liveStreamManager.setAudioStreamingEnabled(false);
                                        liveStreamManager.setLiveUrl(editText.getText().toString());
                                        liveStreamManager.startStream();
                                        rtmp.setText("直播中。。。");
                                    }
                                });
                        inputDialog.setNegativeButton("关闭",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).show();
                    }
                }else
                    setResultToToast("直播不可用！");
                break;
            }
            default:
                break;
        }
    }

    private void showSpeakerVolumeDialog(){
        LinearLayout SpeakerVolumeSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.seekbar, null);

        final SeekBar SpeakerVolumeSeekBar = SpeakerVolumeSettings.findViewById(R.id.seekBar_continuousValue);
        TextView SpeakerVolumeTV = (TextView) SpeakerVolumeSettings.findViewById(R.id.tv_progress);


        SpeakerVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SpeakerVolumeTV.setText(progress+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(SpeakerVolumeSettings)
                .setPositiveButton("确定",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {
                        if (speaker != null) {
                            speaker.setVolume(Integer.valueOf(SpeakerVolumeTV.getText().toString()), new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    setResultToToast("音量调节: " + (djiError == null ? "Success!" : djiError.getDescription()));
                                }
                            });
                        }
                    }
                })
                .create()
                .show();
    }

    private void startRecordUsingMic(final Speaker speaker) {
        if (audioRecoderHandler != null) {
            audioRecoderHandler.startRecord(new AudioRecorderHandler.AudioRecordingCallback() {
                @Override
                public void onRecording(byte[] data) {
                    if (speaker != null) {
                        speaker.paceData(data);
                    }
                }

                @Override
                public void onStopRecord(String savedPath) {
                    if (speaker != null) {
                        speaker.markEOF();
                    }
                    audioRecoderHandler.deleteLastRecordFile();
                }
            });
        }
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void initFlightController() {

        BaseProduct product = FPVDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
                accessoryAggregation=((Aircraft) product).getAccessoryAggregation();
                liveStreamManager= DJISDKManager.getInstance().getLiveStreamManager();
                if((product).getCameras().size()>1)
                    camera=((Aircraft) product).getCameras().get(1);
            }
        }

        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                        }
                    });

        }

        if(accessoryAggregation!=null){
            if(accessoryAggregation.getAccessoryAggregationState().isBeaconConnected()) {
                beacon_light=accessoryAggregation.getBeacon();
            }else if(accessoryAggregation.getAccessoryAggregationState().isSpeakerConnected()){
                speaker=accessoryAggregation.getSpeaker();
            }else if(accessoryAggregation.getAccessoryAggregationState().isSpotlightConnected()){
                light=accessoryAggregation.getSpotlight();
            }
        }
    }

    //为waypointmission操作符添加侦听器
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {
            setResultToToast(executionEvent.getProgress().targetWaypointIndex+"");
        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    private void cameraUpdate(){
        DJILatLng pos = new DJILatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        aMap.moveCamera(new DJICameraUpdateFactory.CameraPositionUpdate(pos,zoomlevel,0,0));
    }

    private void markWaypoint(DJILatLng point){
        //创建MarkerOptions对象
        DJIMarkerOptions markerOptions = new DJIMarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(DJIBitmapDescriptorFactory.defaultMarker());
        markerOptions.anchor(0.5f, 1f);
        DJIMarker marker = aMap.addMarker(markerOptions);
        mark_on_map.add(marker);
        Log.e("LatLng",point.longitude+"--"+point.latitude);
    }

    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            add.setText("Exit");
        }else{
            isAdd = false;
            add.setText("Add");
        }
    }

    private void showSettingDialog(){
        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed){
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed){
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed){
                    mSpeed = 10.0f;
                }
            }

        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone){
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                        Log.e(TAG,"altitude "+altitude);
                        Log.e(TAG,"speed "+mSpeed);
                        Log.e(TAG, "mFinishedAction "+mFinishedAction);
                        Log.e(TAG, "mHeadingMode "+mHeadingMode);
                        configWayPointMission();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }

    String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void configWayPointMission(){

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }

    }

    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void pauseWaypointMission(){

        getWaypointMissionOperator().pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error==null) {
                    setResultToToast("Mission Pause: " + "Successfully");
                    pause_resume=false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pause.setText("Resume");
                        }
                    });
                }else
                    setResultToToast("Mission Pause: " + error.getDescription());
            }
        });

    }

    private void resumeWaypointMission(){

        getWaypointMissionOperator().resumeMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error==null) {
                    setResultToToast("Mission Resume: " + "Successfully");
                    pause_resume=true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pause.setText("Pause");
                        }
                    });
                }else
                    setResultToToast("Mission Resume: " + error.getDescription());
            }
        });
    }

    private void setResultToToast(final String string){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CompleteWidgetActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
