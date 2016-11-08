package com.shongywong.sunshine.app;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_temp_key)));

    }

    private void bindPreferenceSummaryToValue(Preference preference)
    {
        //whenever a preference is changed, then trigger method to update the setting change
        preference.setOnPreferenceChangeListener(this);
        onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                                                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o)
    {
        String value = o.toString();

        if(preference instanceof ListPreference)
        {
            ListPreference listPreference = (ListPreference)preference;
            int index = listPreference.findIndexOfValue(value);
            if(index >= 0)
            {
                preference.setSummary(listPreference.getEntries()[index]);
            }
        }
        else
        {
            //case for updating value for edit text preference
            preference.setSummary(value);
        }

        return true;
    }
}
