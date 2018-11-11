package com.zoromatic.flashlight;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

@SuppressLint("NewApi")
public class FlashlightPreferenceActivity extends ThemeAppCompatActivity {
    SettingsFragment mSettingsFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_prefs);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary,
                outValue,
                true);
        int primaryColor = outValue.resourceId;

        setStatusBarColor(findViewById(R.id.statusBarBackground),
                getResources().getColor(primaryColor));

        PreferenceFragment existingFragment = (PreferenceFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);

        if (existingFragment == null || !existingFragment.getClass().equals(SettingsFragment.class)) {
            mSettingsFragment = new SettingsFragment();

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
                actionBar.setTitle(R.string.action_settings);
            }

            // Display the fragment as the main content.
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, mSettingsFragment)
                    .commit();
        }
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @SuppressLint("InlinedApi")
    public void setStatusBarColor(View statusBar, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            statusBar.getLayoutParams().height = getStatusBarHeight();
            statusBar.setBackgroundColor(color);
        } else {
            statusBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //onBackPressed();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mSettingsFragment != null)
            setContentView(mSettingsFragment.getView());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
            actionBar.onConfigurationChanged(newConfig);
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        public ListPreference mMainTheme;
        public ListPreference mMainColorScheme;
        public ListPreference mOrientation;
        public ListPreference mLanguage;
        public CheckBoxPreference mTurnOnOnOpen;
        public CheckBoxPreference mKeepStrobeFreq;
        public CheckBoxPreference mKeepActive;

        @Override
        public void onCreate(Bundle paramBundle) {
            super.onCreate(paramBundle);
            setRetainInstance(true);

            Context context = getActivity();

            if (context != null) {
                String lang = Preferences.getLanguageOptions(context);

                if (lang.equals("")) {
                    String langDef = Locale.getDefault().getLanguage();

                    if (!langDef.equals(""))
                        lang = langDef;
                    else
                        lang = "en";

                    Preferences.setLanguageOptions(context, lang);
                }

                // Change locale settings in the application
                Resources res = context.getResources();
                DisplayMetrics dm = res.getDisplayMetrics();
                android.content.res.Configuration conf = res.getConfiguration();
                conf.locale = new Locale(lang.toLowerCase());
                res.updateConfiguration(conf, dm);

                setPreferences(context);
            }
        }

        private void setPreferences(Context context) {
            PreferenceManager localPrefs = getPreferenceManager();
            localPrefs.setSharedPreferencesName(Preferences.PREF_NAME);

            addPreferencesFromResource(R.xml.flashlight_prefs);

            mMainTheme = (ListPreference) findPreference(Preferences.PREF_THEME);

            if (mMainTheme != null) {

                String theme = Preferences.getTheme(context);

                if (theme.equals("") || mMainTheme.findIndexOfValue(theme) < 0) {
                    theme = "dark";
                }

                mMainTheme.setValueIndex(mMainTheme.findIndexOfValue(theme));
                mMainTheme.setSummary(mMainTheme.getEntries()[mMainTheme.findIndexOfValue(theme)]);
            }

            mMainColorScheme = (ListPreference) findPreference(Preferences.PREF_COLOR_SCHEME);

            if (mMainColorScheme != null) {
                mMainColorScheme.setValueIndex(Preferences.getColorScheme(context));
                mMainColorScheme.setSummary(mMainColorScheme.getEntries()[Preferences.getColorScheme(context)]);
            }

            mOrientation = (ListPreference) findPreference(Preferences.PREF_ORIENTATION);

            if (mOrientation != null) {

                String orient = Preferences.getOrientation(context);

                if (orient.equals("") || mOrientation.findIndexOfValue(orient) < 0) {
                    orient = "sens";
                }

                mOrientation.setValueIndex(mOrientation.findIndexOfValue(orient));
                mOrientation.setSummary(mOrientation.getEntries()[mOrientation.findIndexOfValue(orient)]);
            }

            mLanguage = (ListPreference) findPreference(Preferences.PREF_LANGUAGE_OPTIONS);

            if (mLanguage != null) {
                String lang = Preferences.getLanguageOptions(context);

                if (lang.equals("") || mLanguage.findIndexOfValue(lang) < 0) {
                    lang = "en";
                }

                mLanguage.setValueIndex(mLanguage.findIndexOfValue(lang));
                mLanguage.setSummary(mLanguage.getEntries()[mLanguage.findIndexOfValue(lang)]);
            }

            mTurnOnOnOpen = (CheckBoxPreference) findPreference(Preferences.PREF_TURNONONOPEN);

            if (mTurnOnOnOpen != null) {
                boolean bTurnOnOnOpen = Preferences.getTurnOnOnOpen(context);
                mTurnOnOnOpen.setChecked(bTurnOnOnOpen);

                String turnon;

                if (bTurnOnOnOpen)
                    turnon = getResources().getString(R.string.turnon);
                else
                    turnon = getResources().getString(R.string.donotturnon);

                mTurnOnOnOpen.setSummary(turnon);
            }

            mKeepStrobeFreq = (CheckBoxPreference) findPreference(Preferences.PREF_KEEPSTROBEFREQ);

            if (mKeepStrobeFreq != null) {
                boolean bKeepStrobeFreq = Preferences.getKeepStrobeFrequency(context);
                mKeepStrobeFreq.setChecked(bKeepStrobeFreq);

                String keepstrobe;

                if (bKeepStrobeFreq)
                    keepstrobe = getResources().getString(R.string.keepstrobefreq);
                else
                    keepstrobe = getResources().getString(R.string.donotkeepstrobefreq);

                mKeepStrobeFreq.setSummary(keepstrobe);
            }

            mKeepActive = (CheckBoxPreference) findPreference(Preferences.PREF_KEEPACTIVE);

            if (mKeepActive != null) {
                boolean bKeepActive = Preferences.getKeepActive(context);
                mKeepActive.setChecked(bKeepActive);

                String keep;

                if (bKeepActive)
                    keep = getResources().getString(R.string.keepactive);
                else
                    keep = getResources().getString(R.string.turnoff);

                mKeepActive.setSummary(keep);
            }
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            Context context = getActivity();

            if (context != null) {
                if (key.equals(Preferences.PREF_THEME)) {
                    mMainTheme = (ListPreference) findPreference(Preferences.PREF_THEME);

                    if (mMainTheme != null) {
                        String value = mMainTheme.getValue();

                        if (value.equals("") || mMainTheme.findIndexOfValue(value) < 0)
                            value = "dark";

                        Preferences.setTheme(context, value);

                        mMainTheme.setValueIndex(mMainTheme.findIndexOfValue(value));
                        mMainTheme.setSummary(mMainTheme.getEntries()[mMainTheme.findIndexOfValue(value)]);

                        Intent intent = getActivity().getIntent();
                        getActivity().finish();
                        getActivity().startActivity(intent);
                    }
                }

                if (key.equals(Preferences.PREF_COLOR_SCHEME)) {
                    mMainColorScheme = (ListPreference) findPreference(Preferences.PREF_COLOR_SCHEME);

                    if (mMainColorScheme != null) {
                        Preferences.setColorScheme(context, mMainColorScheme.findIndexOfValue(mMainColorScheme.getValue()));
                        mMainColorScheme.setSummary(mMainColorScheme.getEntries()[Preferences.getColorScheme(context)]);

                        Intent intent = getActivity().getIntent();
                        getActivity().finish();
                        getActivity().startActivity(intent);
                    }
                }

                if (key.equals(Preferences.PREF_ORIENTATION)) {
                    mOrientation = (ListPreference) findPreference(Preferences.PREF_ORIENTATION);

                    if (mOrientation != null) {
                        String value = mOrientation.getValue();

                        if (value.equals("") || mOrientation.findIndexOfValue(value) < 0)
                            value = "sens";

                        Preferences.setOrientation(context, value);

                        mOrientation.setValueIndex(mOrientation.findIndexOfValue(value));
                        mOrientation.setSummary(mOrientation.getEntries()[mOrientation.findIndexOfValue(value)]);
                    }
                }

                if (key.equals(Preferences.PREF_LANGUAGE_OPTIONS)) {
                    mLanguage = (ListPreference) findPreference(Preferences.PREF_LANGUAGE_OPTIONS);

                    if (mLanguage != null) {
                        if (!mLanguage.getValue().equals("") && mLanguage.findIndexOfValue(mLanguage.getValue()) >= 0)
                            Preferences.setLanguageOptions(context, mLanguage.getValue());
                        else
                            Preferences.setLanguageOptions(context, "en");

                        // Change locale settings in the application
                        Resources res = context.getResources();
                        DisplayMetrics dm = res.getDisplayMetrics();
                        android.content.res.Configuration conf = res.getConfiguration();
                        conf.locale = new Locale(mLanguage.getValue().toLowerCase());
                        res.updateConfiguration(conf, dm);

                        Intent intent = getActivity().getIntent();
                        getActivity().finish();
                        getActivity().startActivity(intent);
                    }
                }

                if (key.equals(Preferences.PREF_TURNONONOPEN)) {
                    mTurnOnOnOpen = (CheckBoxPreference) findPreference(Preferences.PREF_TURNONONOPEN);

                    if (mTurnOnOnOpen != null) {
                        boolean bTurnOnOnOpen = mTurnOnOnOpen.isChecked();

                        Preferences.setTurnOnOnOpen(context, bTurnOnOnOpen);

                        String turnon;

                        if (bTurnOnOnOpen)
                            turnon = getResources().getString(R.string.turnon);
                        else
                            turnon = getResources().getString(R.string.donotturnon);

                        mTurnOnOnOpen.setSummary(turnon);
                    }
                }

                if (key.equals(Preferences.PREF_KEEPSTROBEFREQ)) {
                    mKeepStrobeFreq = (CheckBoxPreference) findPreference(Preferences.PREF_KEEPSTROBEFREQ);

                    if (mKeepStrobeFreq != null) {
                        boolean bKeepStrobe = mKeepStrobeFreq.isChecked();

                        Preferences.setKeepStrobeFrequency(context, bKeepStrobe);

                        String keep;

                        if (bKeepStrobe)
                            keep = getResources().getString(R.string.keepstrobefreq);
                        else
                            keep = getResources().getString(R.string.donotkeepstrobefreq);

                        mKeepStrobeFreq.setSummary(keep);
                    }
                }

                if (key.equals(Preferences.PREF_KEEPACTIVE)) {
                    mKeepActive = (CheckBoxPreference) findPreference(Preferences.PREF_KEEPACTIVE);

                    if (mKeepActive != null) {
                        boolean bKeepActive = mKeepActive.isChecked();

                        Preferences.setKeepActive(context, bKeepActive);

                        String keep;

                        if (bKeepActive)
                            keep = getResources().getString(R.string.keepactive);
                        else
                            keep = getResources().getString(R.string.turnoff);

                        mKeepActive.setSummary(keep);
                    }
                }
            }
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();

            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();

            // Unregister the listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }

}
