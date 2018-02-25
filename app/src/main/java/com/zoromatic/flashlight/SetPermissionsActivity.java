package com.zoromatic.flashlight;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

public class SetPermissionsActivity extends AppCompatActivity {
    public static final String PERMISSIONS_TYPE = "PERMISSIONS_TYPE";
    public static final int PERMISSIONS_REQUEST_CAMERA = 1;
    private int mPermissionType = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_permissions);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            mPermissionType = extras.getInt(PERMISSIONS_TYPE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check permissions and open request if not granted
            if (mPermissionType == PERMISSIONS_REQUEST_CAMERA) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        new Thread() {
                            @Override
                            public void run() {
                                SetPermissionsActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ExplanationDialogFragment question = new ExplanationDialogFragment();
                                        question.setPermissionType(mPermissionType);
                                        question.show(getSupportFragmentManager(), "ExplanationDialogFragment");
                                    }
                                });
                            }
                        }.start();
                    } else {
                        // No explanation needed, we can request the permission.
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.CAMERA},
                                PERMISSIONS_REQUEST_CAMERA);

                        // PERMISSIONS_REQUEST_CAMERA is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                } else {
                    initializeActivity();
                }
            }
        } else {
            initializeActivity();
        }
    }

    private void initializeActivity() {
        setResult(RESULT_OK);
        finish();
    }

    private void closeActivity() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    initializeActivity();
                } else {
                    // permission denied
                    closeActivity();
                }

                break;
            }
            default: {
                closeActivity();
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public int getPermissionType() {
        return mPermissionType;
    }

    public void setPermissionType(int mPermissionGroup) {
        this.mPermissionType = mPermissionGroup;
    }


    public static class ExplanationDialogFragment extends DialogFragment {
        Context mContext;
        private int mPermissionType = -1;

        public ExplanationDialogFragment() {
            mContext = getActivity();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mContext = getActivity();
            String theme = Preferences.getTheme(getContext());

            int themeResId;

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                themeResId = theme.compareToIgnoreCase("light") == 0 ? android.R.style.Theme_Material_Light_Dialog_Alert : android.R.style.Theme_Material_Dialog_Alert;
            } else {
                themeResId = theme.compareToIgnoreCase("light") == 0 ? android.R.style.Theme_Holo_Light : android.R.style.Theme_Holo;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, themeResId);

            if (mPermissionType == PERMISSIONS_REQUEST_CAMERA) {
                builder
                        .setTitle(mContext.getResources().getString(R.string.permission_needed_camera))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();

                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{Manifest.permission.CAMERA},
                                        PERMISSIONS_REQUEST_CAMERA);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                ((SetPermissionsActivity) mContext).finish();
                            }
                        });
            }

            return builder.create();
        }

        public int getPermissionType() {
            return mPermissionType;
        }

        public void setPermissionType(int mPermissionType) {
            this.mPermissionType = mPermissionType;
        }
    }
}
