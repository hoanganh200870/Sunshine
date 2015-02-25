package hcmut.hoanganh.sunshine;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;

import hcmut.hoanganh.sunshine.data.WeatherContract;

/**
 * Created by H.Anh on 24/02/2015.
 */
public class FetchWeatherTask extends AsyncTask<Void, Void, Void> {

    private Context context;

    public FetchWeatherTask(Context context) {
        this.context = context;
    }

    public final int NUM_OF_DAYS = 7;
    public final String FORMAT = "json";
    public final String UNITS = "metric";

    @Override
    protected Void doInBackground(Void... params) {

        String locationSetting = Utility.getLocationSetting(context);

        Uri.Builder builder = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/daily").buildUpon();
        builder.appendQueryParameter("cnt", Integer.toString(NUM_OF_DAYS));
        builder.appendQueryParameter("q", locationSetting);
        builder.appendQueryParameter("mode", FORMAT);
        builder.appendQueryParameter("units", UNITS);

        Uri url = builder.build();
        HttpGet get = new HttpGet(url.toString());
        HttpClient client = new DefaultHttpClient();

        String[] result = null;
        try {
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            String data = EntityUtils.toString(entity);

            try {
                result = JsonUtils.getWeatherDataFromJson(data, NUM_OF_DAYS, context, locationSetting);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.e("Data", Arrays.asList(result).toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
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
