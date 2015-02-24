package hcmut.hoanganh.sunshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import hcmut.hoanganh.sunshine.data.WeatherContract;

/**
 * Created by H.Anh on 24/02/2015.
 */
public class Utilities {

    public static String getLocationSetting(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.pref_location_key);
        String defaultValue = context.getString(R.string.pref_location_default);
        String location = defaultSharedPreferences.getString(key, defaultValue);
        return location;
    }

    public static boolean isImperial(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getString(R.string.pref_units_key);
        String defaultValue = context.getString(R.string.pref_units_default);
        String unitType = defaultSharedPreferences.getString(key, defaultValue);

        String imperialType = context.getString(R.string.pref_units_imperial);
        boolean isImperial = unitType.equals(imperialType);
        return isImperial;
    }

    public static String formatTemperature(double temp, boolean isImperial) {
        if (isImperial) {
            temp = temp * 1.8 + 32;
        }

        String format = String.format("%.0f", temp);
        return format;
    }

    public static String formatDate(String dateText) {
        Date date = WeatherContract.getDateFromDb(dateText);
        DateFormat dateFormat = DateFormat.getInstance();
        String dateFormatted = dateFormat.format(date);
        return dateFormatted;
    }
}
