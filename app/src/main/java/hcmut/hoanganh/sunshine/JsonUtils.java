package hcmut.hoanganh.sunshine;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import hcmut.hoanganh.sunshine.adapter.SunshineSyncAdapter;
import hcmut.hoanganh.sunshine.data.WeatherContract;

/**
 * Created by H.Anh on 19/01/2015.
 */
public class JsonUtils {

    /* The date/time conversion code is going to be moved outside the asynctask later,
    * so for convenience we're breaking it out into its own method now.
    */
    public static String getReadableDateString(long time) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    public static String formatHighLows(double high, double low, Context context) {
        boolean isMetric = Utility.isMetric(context);
        String lowFormatted = Utility.formatTemperature(context, low, isMetric);
        String highFormatted = Utility.formatTemperature(context, high, isMetric);

        String highLowStr = highFormatted + "/" + lowFormatted;
        return highLowStr;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    public static String[] getWeatherDataFromJson(String forecastJsonStr, int numDays, Context context, String locationSetting)
            throws JSONException {

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        final String OWM_COORD_LAT = "lat";
        final String OWM_COORD_LONG = "lon";

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DATETIME = "dt";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        String cityName = cityJson.getString(OWM_CITY_NAME);
        JSONObject coordJSON = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = coordJSON.getLong(OWM_COORD_LAT);
        double cityLongitude = coordJSON.getLong(OWM_COORD_LONG);

        long locationId = addLocation(context, locationSetting, cityName, cityLatitude, cityLongitude);

        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        ContentValues[] values = new ContentValues[numDays];
        String[] resultStrs = new String[numDays];
        for (int i = 0; i < numDays; i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime = dayForecast.getLong(OWM_DATETIME);
            day = getReadableDateString(dateTime);

            double pressure = dayForecast.getDouble(OWM_PRESSURE);
            int humidity = dayForecast.getInt(OWM_HUMIDITY);
            double windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            double windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            int weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low, context);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;

            values[i] = new ContentValues();
            values[i].put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
            values[i].put(WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                    WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
            values[i].put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            values[i].put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            values[i].put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            values[i].put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            values[i].put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            values[i].put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            values[i].put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            values[i].put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
        }

        if (values.length > 0) {
            ContentResolver contentResolver = context.getContentResolver();
            int rowsInserted = contentResolver.bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, values);
            Log.v("Weather fetched", "Inserted " + rowsInserted + " rows of weather data");

            // delete old data
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);
            Date date = calendar.getTime();
            String yesterday = WeatherContract.getDbDateString(date);

            String where = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " <= ? ";
            String[] args = { yesterday };
            contentResolver.delete(WeatherContract.WeatherEntry.CONTENT_URI, where, args);

            SunshineSyncAdapter.notifyWeather(context);
        }

        return resultStrs;
    }

    public static long addLocation(Context context, String locationSetting, String cityName, double lat, double lon) {
        ContentResolver contentResolver = context.getContentResolver();

        String[] projection = {WeatherContract.LocationEntry._ID };
        String selection = WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";
        String[] selectionArgs = { locationSetting };
        Cursor cursor = contentResolver.query(WeatherContract.LocationEntry.CONTENT_URI, projection, selection, selectionArgs, null);

        if (cursor.moveToFirst()) {
            int index = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            long id = cursor.getLong(index);
            return id;
        }

        ContentValues values = new ContentValues();
        values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
        values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
        values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
        values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

        Uri locationUri = contentResolver.insert(WeatherContract.LocationEntry.CONTENT_URI, values);
        long id = ContentUris.parseId(locationUri);
        return id;
    }

}
