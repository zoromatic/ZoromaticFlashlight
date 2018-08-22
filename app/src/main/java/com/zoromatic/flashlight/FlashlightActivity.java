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

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ListView leftDrawerList;

    private Camera camera;
    private ToggleButton button;
    private SeekBar seekBar;
    private final Context context = this;

    private Handler mHandler = new Handler();

    private boolean mActive = false;
    private boolean mSwap = true;
    private int mDelay = 0;
    public static final String ALERT_OPEN = "alert_open";
    public static final String ALERT_ID = "alert_id";
    private boolean mAlertDialogOpen = false;
    private int mAlertDialogID = -1;
    private String log = "";
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

    Bundle savedState = null;

    private static NotificationCompat.Builder notification;
    private static NotificationManager notificationManager;

    private String initialTheme = "dark";
    private int initialColorScheme = 9;
    private String initialOrientation = "sens";
    private String initialLanguage = "en";

    private final Runnable mRunnable = new Runnable() {
        public void run() {
            if (mActive) {
                try {
                    if (camera == null) {
                        return;
                    }

                    final Parameters p = camera.getParameters();
                    camera.startPreview();

                    if (mSwap) {
                        p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(p);

                        mSwap = false;
                        mHandler.postDelayed(mRunnable, mDelay);
                    } else {
                        p.setFlashMode(Parameters.FLASH_MODE_OFF);
                        camera.setParameters(p);

                        mSwap = true;
                        mHandler.postDelayed(mRunnable, mDelay);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    button = findViewById(R.id.togglebutton);
                    if (button != null) {
                        button.setChecked(false);
                    }
                    seekBar.setEnabled(false);

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

        savedState = savedInstanceState;

        initialTheme = Preferences.getTheme(getApplicationContext());
        initialColorScheme = Preferences.getColorScheme(getApplicationContext());
        initialOrientation = Preferences.getOrientation(getApplicationContext());
        initialLanguage = Preferences.getLanguageOptions(getApplicationContext());

        switch (initialOrientation) {
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
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initDrawer();

        /*TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary,
                outValue,
                true);
        int primaryColor = outValue.resourceId;

        setStatusBarColor(findViewById(R.id.statusBarBackground),
                getResources().getColor(primaryColor));*/

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
        }

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
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

        button = findViewById(R.id.togglebutton);
        if (button != null) {
            button.setChecked(false);
            button.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onToggleClicked(v);
                }
            });
        }

        seekBar = findViewById(R.id.seekBarStrobe);
        if (seekBar != null) {
            seekBar.setEnabled(false);
            seekBar.setProgress(0);
        }

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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

                    if (camera != null) {
                        boolean turnedOn = button.isChecked();

                        if (turnedOn) {
                            final Parameters p = camera.getParameters();

                            p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                            camera.setParameters(p);
                            camera.startPreview();

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
            final PackageManager pm = context.getPackageManager();

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
                        boolean turnOn = Preferences.getTurnOnOnOpen(context);

                        if (turnOn) {
                            mToggleOn = true;

                            button.setChecked(true);
                            seekBar.setEnabled(mToggleOn);
                        }

                        boolean keepStrobe = Preferences.getKeepStrobeFrequency(context);

                        if (keepStrobe) {
                            mStrobeFrequency = Preferences.getStrobeFrequency(context);
                            seekBar.setProgress(mStrobeFrequency);

                            //startStrobe(seekBar.getProgress());
                        }
                    } else {
                        Intent localIntent = getIntent();
                        Bundle extras = localIntent.getExtras();

                        if (extras != null) {
                            mToggleOn = extras.getBoolean(TOGGLE_ON);
                            mStrobeFrequency = extras.getInt(FREQUENCY);

                            button.setChecked(mToggleOn);
                            seekBar.setEnabled(mToggleOn);
                            seekBar.setProgress(mStrobeFrequency);

                            //startStrobe(seekBar.getProgress());
                        }
                    }

                    //if (camera == null)
                    //    new CameraTask().execute();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            button = findViewById(R.id.togglebutton);
            if (button != null) {
                button.setChecked(false);
            }
            seekBar.setEnabled(false);

            mToggleOn = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            boolean keep = Preferences.getKeepActive(context);

            if (!keep && camera != null) {
                stopStrobe();

                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            button = findViewById(R.id.togglebutton);
            if (button != null) {
                button.setChecked(false);
            }
            seekBar.setEnabled(false);

            mToggleOn = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            boolean keep = Preferences.getKeepActive(context);

            if (!keep && camera != null) {
                stopStrobe();

                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();

            button = findViewById(R.id.togglebutton);
            if (button != null) {
                button.setChecked(false);
            }
            seekBar.setEnabled(false);

            mToggleOn = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (camera != null) {
            try {
                final Parameters p = camera.getParameters();

                boolean turnedOn = button.isChecked();

                if (turnedOn) {
                    Log.i("FlashlightActivity", "Flashlight is turned on!");
                    p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(p);
                    camera.startPreview();

                    seekBar.setEnabled(true);

                    if (seekBar.getProgress() > 0)
                        startStrobe(seekBar.getProgress());

                    startNotification(true);

                    mToggleOn = true;

                } else {
                    Log.i("FlashlightActivity", "Flashlight is turned off!");
                    p.setFlashMode(Parameters.FLASH_MODE_OFF);
                    camera.setParameters(p);
                    camera.stopPreview();

                    stopStrobe();
                    seekBar.setEnabled(false);

                    startNotification(false);

                    mToggleOn = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                button = findViewById(R.id.togglebutton);
                if (button != null) {
                    button.setChecked(false);
                }
                seekBar.setEnabled(false);

                mToggleOn = false;
            }
        } else {
            new CameraTask().execute();
        }
    }

    private void initView() {
        leftDrawerList = findViewById(R.id.left_drawer);
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);

        List<RowItem> rowItems = new ArrayList<>();
        String theme = Preferences.getTheme(this);

        RowItem item = new RowItem(theme.compareToIgnoreCase("light") == 0 ? R.drawable.ic_settings_black_48dp : R.drawable.ic_settings_white_48dp,
                (String) getResources().getText(R.string.action_settings));
        rowItems.add(item);

        ItemAdapter adapter = new ItemAdapter(this, rowItems);
        leftDrawerList.setAdapter(adapter);

        leftDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        if (theme.compareToIgnoreCase("light") == 0)
            leftDrawerList.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        else
            leftDrawerList.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
    }

    private void initDrawer() {

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {

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
        drawerLayout.setDrawerListener(drawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        leftDrawerList.setItemChecked(position, true);
        drawerLayout.closeDrawers();
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
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initDrawer();

        drawerToggle.onConfigurationChanged(newConfig);

        if (mDrawerOpen) {
            drawerLayout.openDrawer(Gravity.LEFT);
            mDrawerOpen = true;
        } else {
            drawerLayout.closeDrawers();
            mDrawerOpen = false;
        }

        drawerToggle.syncState();

        button = findViewById(R.id.togglebutton);
        if (button != null) {
            button.setChecked(mToggleOn);
        }

        seekBar = findViewById(R.id.seekBarStrobe);
        if (seekBar != null) {
            seekBar.setEnabled(mToggleOn);
            seekBar.setProgress(mStrobeFrequency);
        }

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

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

                    if (camera != null) {
                        boolean turnedOn = button.isChecked();

                        if (turnedOn) {
                            final Parameters p = camera.getParameters();

                            p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                            camera.setParameters(p);
                            camera.startPreview();

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

        savedState = savedInstanceState;

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
            if (drawerLayout != null) {
                drawerLayout.openDrawer(Gravity.LEFT);
                mDrawerOpen = true;
                drawerToggle.syncState();
            }
        }

        button.setChecked(mToggleOn);
        //seekBar.setEnabled(mToggleOn);
        //seekBar.setProgress(mStrobeFrequency);
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
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        String title, message;

        if (alertDialogID == 0) {
            title = getResources().getString(R.string.noflash);
            message = getResources().getString(R.string.flashnotsupported);
            log = getResources().getString(R.string.flashnotsupported);
        } else if (alertDialogID == 1) {
            title = getResources().getString(R.string.nocamera);
            message = getResources().getString(R.string.cameranotsupported);
            log = getResources().getString(R.string.cameranotsupported);
        } else {
            title = getResources().getString(R.string.noflash);
            message = getResources().getString(R.string.flashnotavailable);
            log = getResources().getString(R.string.flashnotavailable);
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
                                if (camera != null) {
                                    stopStrobe();

                                    camera.stopPreview();
                                    camera.release();
                                    camera = null;
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
                        Log.e("FlashlightActivity", log);
                        try {
                            if (camera != null) {
                                stopStrobe();

                                camera.stopPreview();
                                camera.release();
                                camera = null;
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
                if (camera == null)
                    camera = Camera.open();

                if (camera != null)
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

                    boolean turnedOn = button.isChecked();

                    if (turnedOn) {
                        final Parameters p = camera.getParameters();

                        p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(p);
                        camera.startPreview();

                        seekBar.setEnabled(true);

                        if (seekBar.getProgress() > 0)
                            startStrobe(seekBar.getProgress());

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
        if (camera == null)
            return;

        try {
            PackageManager pm = context.getPackageManager();

            if (isFlashSupported(pm)) {
                final Parameters p = camera.getParameters();

                boolean turnedOn = ((ToggleButton) view).isChecked();

                if (turnedOn) {
                    Log.i("FlashlightActivity", "Flashlight is turned on!");
                    p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(p);
                    camera.startPreview();

                    seekBar.setEnabled(true);

                    if (seekBar.getProgress() > 0)
                        startStrobe(seekBar.getProgress());

                    startNotification(true);

                    mToggleOn = true;

                } else {
                    Log.i("FlashlightActivity", "Flashlight is turned off!");
                    p.setFlashMode(Parameters.FLASH_MODE_OFF);
                    camera.setParameters(p);
                    camera.stopPreview();

                    stopStrobe();
                    seekBar.setEnabled(false);

                    startNotification(false);

                    mToggleOn = false;
                }
            } else {
                button.setChecked(false);
                seekBar.setEnabled(false);

                mToggleOn = false;

                mAlertDialogOpen = true;
                mAlertDialogID = FLASH_NOT_SUPPORTED;
                openAlertDialog(mAlertDialogID);
            }
        } catch (Exception e) {
            e.printStackTrace();
            button = findViewById(R.id.togglebutton);
            if (button != null) {
                button.setChecked(false);
            }
            seekBar.setEnabled(false);

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

            drawerLayout.closeDrawers();
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
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_settings:
                drawerLayout.closeDrawers();
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

            if (!theme.equals(initialTheme)) {
                initialTheme = theme;

                startNotification(false);

                if (camera != null) {
                    stopStrobe();

                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }

                Intent localIntent = getIntent();
                finish();
                localIntent.putExtra(TOGGLE_ON, mToggleOn);
                localIntent.putExtra(FREQUENCY, mStrobeFrequency);
                startActivity(localIntent);
            }

            if (colorScheme != initialColorScheme) {
                initialColorScheme = colorScheme;

                startNotification(false);

                if (camera != null) {
                    stopStrobe();

                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }

                Intent localIntent = getIntent();
                finish();
                localIntent.putExtra(TOGGLE_ON, mToggleOn);
                localIntent.putExtra(FREQUENCY, mStrobeFrequency);
                startActivity(localIntent);
            }

            if (!lang.equals(initialLanguage)) {
                initialLanguage = lang;

                startNotification(false);

                if (camera != null) {
                    stopStrobe();

                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }

                Intent localIntent = getIntent();
                finish();
                localIntent.putExtra(TOGGLE_ON, mToggleOn);
                localIntent.putExtra(FREQUENCY, mStrobeFrequency);
                startActivity(localIntent);
            }

            if (!orient.equals(initialOrientation)) {
                initialOrientation = orient;

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

            if (camera != null) {
                stopStrobe();

                camera.stopPreview();
                camera.release();
                camera = null;
            }

            Preferences.setStrobeFrequency(context, mStrobeFrequency);

            finish();
        } else {
            if (drawerLayout != null)
                drawerLayout.closeDrawers();
            mDrawerOpen = false;
        }
    }
}
