package com.aero.control.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.ViewConfiguration;

import com.aero.control.AeroActivity;
import com.aero.control.helpers.Android.CustomTextPreference;

/**
 * Created by Alexander Christ on 05.03.14.
 */
public class PreferenceHandler {

    public Context mContext;
    public PreferenceCategory mPrefCat;
    public PreferenceManager mPrefMan;
    private boolean mInvisibleAdded = false;

    private SharedPreferences mPreferences;

    private final static String NO_DATA_FOUND = "Unavailable";
    private final static String PREF_BLANKED = "BLANKED";

    /*
     * Default constructor to set our objects
     */
    public PreferenceHandler(Context context, PreferenceCategory PrefCat, PreferenceManager PrefMan) {
        this.mContext = context;
        this.mPrefCat = PrefCat;
        this.mPrefMan = PrefMan;
    }

    /**
     * Removes the added invisible preference
     */
    public final void removeInvisiblePreference() {
        if (mInvisibleAdded) {
            for (int i = 0; i < mPrefCat.getPreferenceCount(); i++) {
                if (mPrefCat.getPreference(i).getKey().equals(PREF_BLANKED)) {
                    mPrefCat.removePreference(mPrefCat.getPreference(i));
                    mInvisibleAdded = false;
                }
            }
        }
    }

    /**
     * Adds an invisible preference to the category for layout purposes.
     */
    public final void addInvisiblePreference() {

        // bail out if null or if already added
        if (mPrefCat == null || mContext == null || mInvisibleAdded)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                !(ViewConfiguration.get(mContext).hasPermanentMenuKey())) {
                /* For better KitKat+ looks; */
            Preference blankedPref = new Preference(mContext);
            blankedPref.setSelectable(false);
            blankedPref.setKey(PREF_BLANKED);
            mPrefCat.addPreference(blankedPref);
            mInvisibleAdded = true;
        }
    }

    /**
     * Generates the preferences for a path
     *
     * @param array     => Contains the parameters in the path/dictionary
     * @param path      => directory (where to look up the files)
     *
     * @return nothing
     */
    public final void genPrefFromDictionary(String[] array, String path) {

        int counter = array.length;
        int i = 0;

        for (String b : array) {
            generateSettings(b, path, false);
            i++;
            if (i == counter)
                addInvisiblePreference();
        }
    }

    /**
     * Gets a file from a given path and adds a Preference on top of it
     *
     * @param nameArray     => Array which contains the file names
     * @param paraArray     => Array which contains the file path (without name)
     * @param showEmpty     => Should we show a empty preference at the end?
     *
     * @return nothing
     */
    public final void genPrefFromFiles(String[] nameArray, String[] paraArray, Boolean showEmpty) {

        int counter = nameArray.length;
        int i = 0;

        for (int j = 0; j < nameArray.length; j++) {

            //TODO: Move this into the parent class
            if (nameArray[j].equals("vtg_level") || nameArray[j].equals("amp"))
                generateSettings(nameArray[j], paraArray[j], true);
            else
                generateSettings(nameArray[j], paraArray[j], false);
            
            i++;
            if (i == counter && showEmpty)
                addInvisiblePreference();
        }
    }

    /**
     * Gets a file from a given path and adds a Preference on top of it
     *
     * @param path     => Generates a single preference from a complete path
     *
     * @return nothing
     */
    public final void genPrefFromSingleFile(String path) {

        removeInvisiblePreference();

        String[] array = path.split("/");
        String paraName = "";
        int i = 0;

        for (String a : array) {
            if (array.length - 1 == i)
                paraName = a;
            i++;
        }
        path = path.replace("/" + paraName, "");

        generateSettings(paraName, path, false);
    }

    /**
     * Gets all files in a given dictionary and adds Preferences on top of them
     *
     * @param parameter     => actual file to read/write
     * @param path          => directory (where to look up the file)
     * @param flag          => force vibration after change
     *
     * @return nothing
     */
    private void generateSettings(final String parameter, final String path, final boolean flag) {

        final CustomTextPreference prefload = new CustomTextPreference(mContext);
        // Strings saves the complete path for a given governor;
        final String parameterPath = path + "/" + parameter;
        final String summary = AeroActivity.shell.getInfo(parameterPath);

        if (summary.equals(NO_DATA_FOUND))
            return;

        // Dont try to read those files;
        if (parameter.equals("uevent") || parameter.equals("dev"))
            return;

        // If the file doesn't exist, no need to waste time;
        if (!(AeroActivity.genHelper.doesExist(parameterPath)))
            return;

        Integer tmp = null;
        try {
            tmp = Integer.parseInt(summary);
        } catch (NumberFormatException e) {
            // Do nothing
        }

        // Only show numbers in input field if its a number;
        if (tmp != null)
            prefload.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

        mPreferences = mPrefMan.getSharedPreferences();

        // If no entry exists, we can be sure that we won't have to check the checkbox;
        if (mPreferences.getString(parameterPath, null) != null) {
            prefload.setChecked(true);
        }

        // Setup all things we would normally do in XML;
        prefload.setPrefSummary(summary);
        prefload.setTitle(parameter);
        prefload.setText(summary);
        prefload.setPrefText(parameter);
        prefload.setDialogTitle(parameter);
        prefload.setName(parameterPath);

        if (prefload.getPrefSummary().equals(NO_DATA_FOUND)) {
            prefload.setEnabled(false);
            prefload.setPrefSummary("This value can't be changed.");
        }

        mPrefCat.addPreference(prefload);

        // Custom OnChangeListener for each element in our list;
        prefload.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {

                String a = (String) o;

                // Return, if empty string;
                if (a.equals(""))
                    return false;

                AeroActivity.shell.setRootInfo(a, parameterPath);

                prefload.setPrefSummary(a);

                if (prefload.isChecked() == true) {
                    // Store our custom preferences if available;
                    mPreferences.edit().putString(parameterPath, o.toString()).commit();
                }

                if (flag)
                    forceVibration();

                return true;
            };
        });
    }

    public void forceVibration() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Log.e("Aero", "Something interrupted the main Thread, try again.", e);
        }

        Vibrator vibrate = (Vibrator)mContext.getSystemService(mContext.VIBRATOR_SERVICE);

        vibrate.vibrate(500);
    }

}


