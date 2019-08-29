/**
 * Copyright CMW Mobile.com, 2010.
 */
package com.zoromatic.flashlight;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.ListPreference;
import androidx.appcompat.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListAdapter;

/**
 * The ImageListPreference class responsible for displaying an image for each
 * item within the list.
 * @author Casper Wakkers
 */
public class ImageListPreference extends ListPreference {
    private int[] imageIds = null;
    private int[] colorIds = null;
    private String[] fontPaths = null;
    private int[] batteryIds = null;
    private int[] colorSchemeIds = null;

    private Dialog mDialog;
    private int mWhichButtonClicked;

    private Context context;

    /**
     * Constructor of the ImageListPreference. Initializes the custom images.
     * @param context application context.
     * @param attrs custom xml attributes.
     */
    public ImageListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.ImageListPreference);

        if (typedArray.getIndexCount() > 0) {

            int id = typedArray.getResourceId(R.styleable.ImageListPreference_entryImages, -1);

            if (id >= 0) {
                String[] imageNames = context.getResources().getStringArray(id);

                if (imageNames[0] == null) {
                    imageIds = null;
                } else {

                    imageIds = new int[imageNames.length];

                    for (int i = 0; i < imageNames.length; i++) {
                        String imageName = imageNames[i];

                        imageIds[i] = context.getResources().getIdentifier(imageName,
                                null, context.getPackageName());
                    }
                }
            } else
                imageIds = null;

            id = typedArray.getResourceId(R.styleable.ImageListPreference_entryBatteries, -1);

            if (id >= 0) {
                String[] batteryNames = context.getResources().getStringArray(id);

                if (batteryNames[0] == null) {
                    batteryIds = null;
                } else {

                    batteryIds = new int[batteryNames.length];

                    for (int i = 0; i < batteryNames.length; i++) {
                        String batteryName = batteryNames[i];

                        batteryIds[i] = context.getResources().getIdentifier(batteryName,
                                null, context.getPackageName());
                    }
                }
            } else
                batteryIds = null;

            id = typedArray.getResourceId(R.styleable.ImageListPreference_entryColorSchemes, -1);

            if (id >= 0) {
                String[] colorSchemeNames = context.getResources().getStringArray(id);

                if (colorSchemeNames[0] == null) {
                    colorSchemeIds = null;
                } else {

                    colorSchemeIds = new int[colorSchemeNames.length];

                    for (int i = 0; i < colorSchemeNames.length; i++) {
                        String colorSchemeName = colorSchemeNames[i];

                        colorSchemeIds[i] = context.getResources().getIdentifier(colorSchemeName,
                                null, context.getPackageName());
                    }
                }
            } else
                colorSchemeIds = null;

            id = typedArray.getResourceId(R.styleable.ImageListPreference_entryColors, -1);

            if (id >= 0) {
                String[] colorNames = context.getResources().getStringArray(id);

                if (colorNames[0] == null) {
                    colorIds = null;
                } else {
                    colorIds = new int[colorNames.length];

                    for (int i = 0; i < colorNames.length; i++) {
                        String colorName = colorNames[i];

                        colorIds[i] = context.getResources().getIdentifier(colorName,
                                null, context.getPackageName());
                    }
                }
            } else
                colorIds = null;

            id = typedArray.getResourceId(R.styleable.ImageListPreference_entryFonts, -1);

            if (id >= 0) {
                String[] fontNames = context.getResources().getStringArray(id);

                if (fontNames[0] == null) {
                    fontPaths = null;
                } else {
                    fontPaths = fontNames;
                }
            } else
                fontPaths = null;
        }

        typedArray.recycle();
    }

    @Override
    protected void showDialog(Bundle state) {

        int index = findIndexOfValue(getSharedPreferences().getString(
                getKey(), "1"));

        ListAdapter listAdapter = new ImageArrayAdapter(getContext(),
                R.layout.listitem, getEntries(), imageIds, colorIds, fontPaths, batteryIds, colorSchemeIds, index);

        String theme = Preferences.getTheme(getContext());

        int themeResId;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            themeResId = theme.compareToIgnoreCase("light") == 0 ? android.R.style.Theme_Material_Light_Dialog_Alert : android.R.style.Theme_Material_Dialog_Alert;
        } else {
            themeResId = theme.compareToIgnoreCase("light") == 0 ? android.R.style.Theme_Holo_Light : android.R.style.Theme_Holo;
        }

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(context, themeResId);
        mBuilder.setTitle(getTitle());
        mBuilder.setIcon(getDialogIcon());
        mBuilder.setNegativeButton(getNegativeButtonText(), this);
        mBuilder.setAdapter(listAdapter, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                if (which >= 0 && getEntryValues() != null) {
                    String value = getEntryValues()[which].toString();
                    if (callChangeListener(value))
                        setValue(value);
                }
            }
        });

        final View contentView = onCreateDialogView();

        if (contentView != null) {
            onBindDialogView(contentView);
            mBuilder.setView(contentView);
        } else
            mBuilder.setMessage(getDialogMessage());

        //mBuilder.show();

        final Dialog dialog = mDialog = mBuilder.create();

        if (state != null) {
            dialog.onRestoreInstanceState(state);
        }

        dialog.setOnDismissListener(this);
        dialog.show();
    }

    public void onClick(DialogInterface dialog, int which) {
        mWhichButtonClicked = which;
    }

    public void onDismiss(DialogInterface dialog) {

        mDialog = null;
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE);
    }

    /**
     * Called when the dialog is dismissed and should be used to save data to
     * the {@link SharedPreferences}.
     *
     * @param positiveResult Whether the positive button was clicked (true), or
     *            the negative button was clicked or the dialog was canceled (false).
     */
    protected void onDialogClosed(boolean positiveResult) {
    }

    /**
     * Gets the dialog that is shown by this preference.
     *
     * @return The dialog, or null if a dialog is not being shown.
     */
    public Dialog getDialog() {
        return mDialog;
    }

    /**
     * {@inheritDoc}
     */
    public void onActivityDestroy() {

        if (mDialog == null || !mDialog.isShowing()) {
            return;
        }

        mDialog.dismiss();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.isDialogShowing = true;
        myState.dialogBundle = mDialog.onSaveInstanceState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (myState.isDialogShowing) {
            showDialog(myState.dialogBundle);
        }
    }

    private static class SavedState extends BaseSavedState {
        boolean isDialogShowing;
        Bundle dialogBundle;

        SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            dialogBundle = source.readBundle(getClass().getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeBundle(dialogBundle);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
