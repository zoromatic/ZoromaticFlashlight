package com.zoromatic.flashlight;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

@SuppressWarnings("deprecation")
public class FlashlightActivity extends ThemeActionBarActivity {

    public static final int REQUEST_CODE_DANGEROUS_PERMISSIONS = 100;
    private static final int ACTIVITY_SETTINGS = 200;

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mLeftDrawerList;

    private Camera mCamera;
    private ToggleButton mButton;
    private SeekBar mSeekBar;
    private final Context mContext = this;

    private Handler mHandler = new Handler();

    private boolean mActive = false;
    private boolean mSwap = true;
    private int mDelay = 0;
    public static final String ALERT_OPEN = "alert_open";
    public static final String ALERT_ID = "alert_id";
    private boolean mAlertDialogOpen = false;
    private int mAlertDialogID = -1;
    private String mLog = "";
    public static final int CAMERA_NOT_SUPPORTED = 0;
    public static final int FLASH_NOT_SUPPORTED = 1;
    public static final int FLASH_NOT_AVAILABLE = 2;
    public static final String CAMERA_OPEN = "camera_open";
    private boolean mCameraOpen = false;
    public static final String DRAWER_OPEN = "drawer_open";
    private boolean mDrawerOpen = false;

    public static final String TOGGLE_ON = "toggle_on";
    public static final String FREQUENCY = "frequency";
    private boolean mToggleOn = false;
    private int mStrobeFrequency = 0;

    Bundle mSavedState = null;

    private static NotificationCompat.Builder notification;
    private static NotificationManager notificationManager;

    private String mInitialTheme = "dark";
    private int mInitialColorScheme = 9;
    private String mInitialOrientation = "sens";
    private String mInitialLanguage = "en";

    private final Runnable mRunnable = new Runnable() {
        public void run() {
            if (mActive) {
                try {
                    if (mCamera == null) {
                        return;
                    }

                    final Parameters p = mCamera.getParameters();
                    mCamera.startPreview();

                    if (mSwap) {
                        p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(p);

                        mSwap = false;
                        mHandler.postDelayed(mRunnable, mDelay);
                    } else {
                        p.setFlashMode(Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(p);

                        mSwap = true;
                        mHandler.postDelayed(mRunnable, mDelay);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mButton = findViewById(R.id.togglebutton);
                    if (mButton != null) {
                        mButton.setChecked(false);
                    }
                    mSeekBar.setEnabled(false);

                    mToggleOn = false;
                }
            }
        }
    };

    private void startStrobe(int frequency) {
        if (frequency == 0) {
            stopStrobe();
            return;
        }

        // reset strobe
        if (mActive) {
            stopStrobe();
        }

        mActive = true;
        mDelay = (int) ((1 / (float) frequency) * 1000 / 2);

        mHandler.post(mRunnable);
    }

    private void stopStrobe() {
        mActive = false;

        mHandler.post(mRunnable);
        mHandler.removeCallbacks(mRunnable);
    }

    @SuppressLint("SetTextI18n")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String lang = Preferences.getLanguageOptions(this);

        if (lang.equals("")) {
            String langDef = Locale.getDefault().getLanguage();

            if (!langDef.equals(""))
                lang = langDef;
            else
                lang = "en";

            Preferences.setLanguageOptions(this, lang);
        }

        // Change locale settings in the application
        Resources res = getApplicationContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.locale = new Locale(lang.toLowerCase());
        res.updateConfiguration(conf, dm);

        mSavedState = savedInstanceState;

        mInitialTheme = Preferences.getTheme(getApplicationContext());
        mInitialColorScheme = Preferences.getColorScheme(getApplicationContext());
        mInitialOrientation = Preferences.getOrientation(getApplicationContext());
        mInitialLanguage = Preferences.getLanguageOptions(getApplicationContext());

        switch (mInitialOrientation) {
            case "port":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case "land":
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                break;
        }

        setContentView(R.layout.activity_flashlight);

        initView();
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        initDrawer();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
        }

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);

        notification = new NotificationCompat.Builder(this);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notification.setColor(0);
        }

        notification.setDefaults(Notification.FLAG_NO_CLEAR);
        notification.setSmallIcon(R.drawable.ic_launcher);
        notification.setWhen(0);
        notification.setPriority(Notification.PRIORITY_HIGH);
        notification.setOngoing(true);

        notification.setContentTitle(getResources().getText(R.string.app_name));
        notification.setContentText(getResources().getText(R.string.tapopen));
        notification.setContentIntent(pendingIntent);
        notification.setVibrate(new long[]{0L});

        TextView valueFrequency = findViewById(R.id.valueFrequency);
        if (valueFrequency != null) {
            valueFrequency.setText(Integer.toString(0), TextView.BufferType.NORMAL);
        }

        mButton = findViewById(R.id.togglebutton);
        if (mButton != null) {
            mButton.setChecked(false);
            /*mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onToggleClicked(v);
                }
            });*/
        }

        mSeekBar = findViewById(R.id.seekBarStrobe);
        if (mSeekBar != null) {
            mSeekBar.setEnabled(false);
            mSeekBar.setProgress(0);
        }

        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mStrobeFrequency = progress;

                if (progress > 0) {
                    stopStrobe();
                    startStrobe(progress);
                } else {
                    stopStrobe();

                    if (mCamera != null) {
                        boolean turnedOn = mButton.isChecked();

                        if (turnedOn) {
                            final Parameters p = mCamera.getParameters();

                            p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                            mCamera.setParameters(p);
                            mCamera.startPreview();

                            seekBar.setEnabled(true);

                            mToggleOn = true;
                        } else {
                            mToggleOn = false;
                        }
                    }
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Intent permissionsIntent = new Intent(this, SetPermissionsActivity.class);
                permissionsIntent.putExtra(SetPermissionsActivity.PERMISSIONS_TYPE, SetPermissionsActivity.PERMISSIONS_REQUEST_CAMERA);
                permissionsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(permissionsIntent, REQUEST_CODE_DANGEROUS_PERMISSIONS);
            } else {
                updateControls(savedInstanceState);
            }
        } else {
            updateControls(savedInstanceState);
        }
    }

    private void updateControls(Bundle savedInstanceState) {
        try {
            final PackageManager pm = mContext.getPackageManager();

            if (!isCameraSupported(pm)) {
                mAlertDialogOpen = true;
                mAlertDialogID = CAMERA_NOT_SUPPORTED;
                openAlertDialog(mAlertDialogID);
            } else {
                if (!isFlashSupported(pm)) {
                    mAlertDialogOpen = true;
                    mAlertDialogID = FLASH_NOT_SUPPORTED;
                    openAlertDialog(mAlertDialogID);
                } else {
                    if (savedInstanceState == null) {
                        boolean turnOn = Preferences.getTurnOnOnOpen(mContext);

                        if (turnOn) {
                            mToggleOn = true;

                            mButton.setChecked(true);
                            mSeekBar.setEnabled(mToggleOn);
                        }

                        boolean keepStrobe = Preferences.getKeepStrobeFrequency(mContext);

                        if (keepStrobe) {
                            mStrobeFrequency = Preferences.getStrobeFrequency(mContext);
                            mSeekBar.setProgress(mStrobeFrequency);
                        }
                    } else {
                        Intent localIntent = getIntent();
                        Bundle extras = localIntent.getExtras();

                        if (extras != null) {
                            mToggleOn = extras.getBoolean(TOGGLE_ON);
                            mStrobeFrequency = extras.getInt(FREQUENCY);

                            mButton.setChecked(mToggleOn);
                            mSeekBar.setEnabled(mToggleOn);
                            mSeekBar.setProgress(mStrobeFrequency);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mButton = findViewById(R.id.togglebutton);
            if (mButton != null) {
                mButton.setChecked(false);
            }
            mSeekBar.setEnabled(false);

            mToggleOn = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            boolean keep = Preferences.getKeepActive(mContext);

            if (!keep && mCamera != null) {
                stopStrobe();

                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mButton = findViewById(R.id.togglebutton);
            if (mButton != null) {
                mButton.setChecked(false);
            }
            mSeekBar.setEnabled(false);

            mToggleOn = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            boolean keep = Preferences.getKeepActive(mContext);

            if (!keep && mCamera != null) {
                stopStrobe();

                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();

            mButton = findViewById(R.id.togglebutton);
            if (mButton != null) {
                mButton.setChecked(false);
            }
            mSeekBar.setEnabled(false);

            mToggleOn = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCamera != null) {
            try {
                final Parameters p = mCamera.getParameters();

                boolean turnedOn = mButton.isChecked();

                if (turnedOn) {
                    Log.i("FlashlightActivity", "Flashlight is turned on!");
                    p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(p);
                    mCamera.startPreview();

                    mSeekBar.setEnabled(true);

                    if (mSeekBar.getProgress() > 0)
                        startStrobe(mSeekBar.getProgress());

                    startNotification(true);

                    mToggleOn = true;

                } else {
                    Log.i("FlashlightActivity", "Flashlight is turned off!");
                    p.setFlashMode(Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(p);
                    mCamera.stopPreview();

                    stopStrobe();
                    mSeekBar.setEnabled(false);

                    startNotification(false);

                    mToggleOn = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                mButton = findViewById(R.id.togglebutton);
                if (mButton != null) {
                    mButton.setChecked(false);
                }
                mSeekBar.setEnabled(false);

                mToggleOn = false;
            }
        } else {
            new CameraTask().execute();
        }
    }

    private void initView() {
        mLeftDrawerList = findViewById(R.id.left_drawer);
        mToolbar = findViewById(R.id.toolbar);
        mDrawerLayout = findViewById(R.id.drawerLayout);

        List<RowItem> rowItems = new ArrayList<>();
        String theme = Preferences.getTheme(this);

        RowItem item = new RowItem(theme.compareToIgnoreCase("light") == 0 ? R.drawable.ic_settings_black_48dp : R.drawable.ic_settings_white_48dp,
                (String) getResources().getText(R.string.action_settings));
        rowItems.add(item);

        ItemAdapter adapter = new ItemAdapter(this, rowItems);
        mLeftDrawerList.setAdapter(adapter);

        mLeftDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        if (theme.compareToIgnoreCase("light") == 0)
            mLeftDrawerList.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        else
            mLeftDrawerList.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
    }

    private void initDrawer() {

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                mDrawerOpen = false;
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mDrawerOpen = true;
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        mLeftDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawers();
        mDrawerOpen = false;

        if (position == 0) { // open Settings
            Intent settingsIntent = new Intent(getApplicationContext(), FlashlightPreferenceActivity.class);
            startActivityForResult(settingsIntent, ACTIVITY_SETTINGS);
        }
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.activity_flashlight);

        initView();
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        initDrawer();

        mDrawerToggle.onConfigurationChanged(newConfig);

        if (mDrawerOpen) {
            mDrawerLayout.openDrawer(Gravity.LEFT);
            mDrawerOpen = true;
        } else {
            mDrawerLayout.closeDrawers();
            mDrawerOpen = false;
        }

        mDrawerToggle.syncState();

        mButton = findViewById(R.id.togglebutton);
        if (mButton != null) {
            mButton.setChecked(mToggleOn);
        }

        mSeekBar = findViewById(R.id.seekBarStrobe);
        if (mSeekBar != null) {
            mSeekBar.setEnabled(mToggleOn);
            mSeekBar.setProgress(mStrobeFrequency);
        }

        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mStrobeFrequency = progress;

                if (progress > 0) {
                    stopStrobe();
                    startStrobe(progress);
                } else {
                    stopStrobe();

                    if (mCamera != null) {
                        boolean turnedOn = mButton.isChecked();

                        if (turnedOn) {
                            final Parameters p = mCamera.getParameters();

                            p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                            mCamera.setParameters(p);
                            mCamera.startPreview();

                            seekBar.setEnabled(true);

                            mToggleOn = true;
                        } else {
                            mToggleOn = false;
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(ALERT_OPEN, mAlertDialogOpen);
        savedInstanceState.putInt(ALERT_ID, mAlertDialogID);
        savedInstanceState.putBoolean(CAMERA_OPEN, mCameraOpen);
        savedInstanceState.putBoolean(TOGGLE_ON, mToggleOn);
        savedInstanceState.putInt(FREQUENCY, mStrobeFrequency);
        savedInstanceState.putBoolean(DRAWER_OPEN, mDrawerOpen);

        mSavedState = savedInstanceState;

        super.onSaveInstanceState(savedInstanceState);
    }

    @SuppressLint("RtlHardcoded")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        mAlertDialogOpen = savedInstanceState.getBoolean(ALERT_OPEN);
        mAlertDialogID = savedInstanceState.getInt(ALERT_ID);
        mCameraOpen = savedInstanceState.getBoolean(CAMERA_OPEN);
        mToggleOn = savedInstanceState.getBoolean(TOGGLE_ON);
        mStrobeFrequency = savedInstanceState.getInt(FREQUENCY);
        mDrawerOpen = savedInstanceState.getBoolean(DRAWER_OPEN);

        if (mAlertDialogOpen) {
            openAlertDialog(mAlertDialogID);
            return;
        }

        if (mDrawerOpen) {
            if (mDrawerLayout != null) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
                mDrawerOpen = true;
                mDrawerToggle.syncState();
            }
        }

        mButton.setChecked(mToggleOn);
        //mSeekBar.setEnabled(mToggleOn);
        //mSeekBar.setProgress(mStrobeFrequency);
    }

    private void startNotification(boolean start) {
        if (start) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                String CHANNEL_ID = getString(R.string.notification_channel_id);
                CharSequence channelName = getString(R.string.notification_channel);
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, channelName, importance);
                notificationManager.createNotificationChannel(mChannel);

                notification.setChannelId(CHANNEL_ID);
                notificationManager.notify(R.string.app_name, notification.build());
            } else {
                notificationManager.notify(R.string.app_name, notification.build());
            }
        } else {
            notificationManager.cancel(R.string.app_name);
        }
    }

    private void openAlertDialog(int alertDialogID) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        String title, message;

        if (alertDialogID == 0) {
            title = getResources().getString(R.string.noflash);
            message = getResources().getString(R.string.flashnotsupported);
            mLog = getResources().getString(R.string.flashnotsupported);
        } else if (alertDialogID == 1) {
            title = getResources().getString(R.string.nocamera);
            message = getResources().getString(R.string.cameranotsupported);
            mLog = getResources().getString(R.string.cameranotsupported);
        } else {
            title = getResources().getString(R.string.noflash);
            message = getResources().getString(R.string.flashnotavailable);
            mLog = getResources().getString(R.string.flashnotavailable);
        }

        alertDialog
                .setTitle(title)
                .setMessage(message)
                .setOnKeyListener(new Dialog.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode,
                                         KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            try {
                                if (mCamera != null) {
                                    stopStrobe();

                                    mCamera.stopPreview();
                                    mCamera.release();
                                    mCamera = null;
                                }

                                dialog.dismiss();

                                startNotification(false);
                                finish();

                                android.os.Process.killProcess(android.os.Process.myPid());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        return true;
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int which) {
                        Log.e("FlashlightActivity", mLog);
                        try {
                            if (mCamera != null) {
                                stopStrobe();

                                mCamera.stopPreview();
                                mCamera.release();
                                mCamera = null;
                            }

                            dialog.dismiss();

                            startNotification(false);
                            finish();

                            android.os.Process.killProcess(android.os.Process.myPid());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

        alertDialog.show();
    }

    @SuppressLint("StaticFieldLeak")
    public class CameraTask extends AsyncTask<Void, Void, OpenCamera> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected OpenCamera doInBackground(Void... params) {
            OpenCamera openResult;

            try {
                if (mCamera == null)
                    mCamera = Camera.open();

                if (mCamera != null)
                    openResult = new OpenCamera(true, false);
                else
                    openResult = new OpenCamera(false, false);

            } catch (Exception e) {
                e.printStackTrace();
                openResult = new OpenCamera(false, true);
            }

            return openResult;
        }

        @Override
        protected void onPostExecute(OpenCamera found) {

            try {
                if (found != null && found.cameraResult) {

                    boolean turnedOn = mButton.isChecked();

                    if (turnedOn) {
                        final Parameters p = mCamera.getParameters();

                        p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(p);
                        mCamera.startPreview();

                        mSeekBar.setEnabled(true);

                        if (mSeekBar.getProgress() > 0)
                            startStrobe(mSeekBar.getProgress());

                        startNotification(true);

                        mToggleOn = true;

                        // hack for cases when flash is not turned on automatically
                        Thread.sleep(500);
                        onStart();
                    } else {
                        mToggleOn = false;
                    }

                    mCameraOpen = true;
                } else {
                    if (found != null && found.cameraException && !mCameraOpen) {
                        mAlertDialogOpen = true;
                        mAlertDialogID = FLASH_NOT_AVAILABLE;
                        openAlertDialog(mAlertDialogID);
                    }

                    mCameraOpen = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class OpenCamera {
        boolean cameraResult;
        boolean cameraException;

        OpenCamera(boolean result, boolean exception) {
            cameraResult = result;
            cameraException = exception;
        }
    }

    public void onToggleClicked(View view) {
        if (mCamera == null)
            return;

        try {
            PackageManager pm = mContext.getPackageManager();

            if (isFlashSupported(pm)) {
                final Parameters p = mCamera.getParameters();

                boolean turnedOn = ((ToggleButton) view).isChecked();

                if (turnedOn) {
                    Log.i("FlashlightActivity", "Flashlight is turned on!");
                    p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(p);
                    mCamera.startPreview();

                    mSeekBar.setEnabled(true);

                    if (mSeekBar.getProgress() > 0)
                        startStrobe(mSeekBar.getProgress());

                    startNotification(true);

                    mToggleOn = true;

                } else {
                    Log.i("FlashlightActivity", "Flashlight is turned off!");
                    p.setFlashMode(Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(p);
                    mCamera.stopPreview();

                    stopStrobe();
                    mSeekBar.setEnabled(false);

                    startNotification(false);

                    mToggleOn = false;
                }
            } else {
                mButton.setChecked(false);
                mSeekBar.setEnabled(false);

                mToggleOn = false;

                mAlertDialogOpen = true;
                mAlertDialogID = FLASH_NOT_SUPPORTED;
                openAlertDialog(mAlertDialogID);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mButton = findViewById(R.id.togglebutton);
            if (mButton != null) {
                mButton.setChecked(false);
            }
            mSeekBar.setEnabled(false);

            mToggleOn = false;
        }
    }

    private boolean isFlashSupported(PackageManager packageManager) {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private boolean isCameraSupported(PackageManager packageManager) {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {

            mDrawerLayout.closeDrawers();
            mDrawerOpen = false;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_flashlight, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_settings:
                mDrawerLayout.closeDrawers();
                mDrawerOpen = false;

                Intent settingsIntent = new Intent(getApplicationContext(), FlashlightPreferenceActivity.class);
                startActivityForResult(settingsIntent, ACTIVITY_SETTINGS);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE_DANGEROUS_PERMISSIONS) {
            if (resultCode == RESULT_OK) {
                Intent intentStart = getIntent();
                finish();
                startActivity(intentStart);
            } else {
                finish();
            }
        } else if (requestCode == ACTIVITY_SETTINGS) {
            String theme = Preferences.getTheme(getApplicationContext());
            int colorScheme = Preferences.getColorScheme(getApplicationContext());
            String orient = Preferences.getOrientation(getApplicationContext());
            String lang = Preferences.getLanguageOptions(getApplicationContext());

            if (!theme.equals(mInitialTheme)) {
                mInitialTheme = theme;

                startNotification(false);

                if (mCamera != null) {
                    stopStrobe();

                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }

                Intent localIntent = getIntent();
                finish();
                localIntent.putExtra(TOGGLE_ON, mToggleOn);
                localIntent.putExtra(FREQUENCY, mStrobeFrequency);
                startActivity(localIntent);
            }

            if (colorScheme != mInitialColorScheme) {
                mInitialColorScheme = colorScheme;

                startNotification(false);

                if (mCamera != null) {
                    stopStrobe();

                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }

                Intent localIntent = getIntent();
                finish();
                localIntent.putExtra(TOGGLE_ON, mToggleOn);
                localIntent.putExtra(FREQUENCY, mStrobeFrequency);
                startActivity(localIntent);
            }

            if (!lang.equals(mInitialLanguage)) {
                mInitialLanguage = lang;

                startNotification(false);

                if (mCamera != null) {
                    stopStrobe();

                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }

                Intent localIntent = getIntent();
                finish();
                localIntent.putExtra(TOGGLE_ON, mToggleOn);
                localIntent.putExtra(FREQUENCY, mStrobeFrequency);
                startActivity(localIntent);
            }

            if (!orient.equals(mInitialOrientation)) {
                mInitialOrientation = orient;

                switch (orient) {
                    case "port":
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                    case "land":
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                    default:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                        break;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!mDrawerOpen) {
            super.onBackPressed();
            startNotification(false);

            if (mCamera != null) {
                stopStrobe();

                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

            Preferences.setStrobeFrequency(mContext, mStrobeFrequency);

            finish();
        } else {
            if (mDrawerLayout != null)
                mDrawerLayout.closeDrawers();
            mDrawerOpen = false;
        }
    }
}
