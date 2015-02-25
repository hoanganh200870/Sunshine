package hcmut.hoanganh.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import hcmut.hoanganh.sunshine.data.WeatherContract;

/**
 * Created by H.Anh on 19/01/2015.
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file
        // TODO: Add preferences from XML

        addPreferencesFromResource(R.xml.pref_general);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        // TODO: Add preferences
        String key = getString(R.string.pref_location_key);
        Preference preference = findPreference(key);
        bindPreferenceSummaryToValue(preference);

        key = getString(R.string.pref_units_key);
        preference = findPreference(key);
        bindPreferenceSummaryToValue(preference);
    }

    private boolean mBindingPreference;

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        mBindingPreference = true;

        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));

        mBindingPreference = false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        if (!mBindingPreference) {
            String key = preference.getKey();
            String locationKey = getString(R.string.pref_location_key);
            if (key.equals(locationKey)) {
                FetchWeatherTask weatherTask = new FetchWeatherTask(this);
                weatherTask.execute();
            } else {
                // notify code that weather may be impacted
                getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
            }
        }

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
        return true;
    }

    @Nullable
    @Override
    public Intent getParentActivityIntent() {
        Intent parentActivityIntent = super.getParentActivityIntent();
        Intent intent = parentActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }
}
