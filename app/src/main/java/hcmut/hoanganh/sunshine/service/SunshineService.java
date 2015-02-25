package hcmut.hoanganh.sunshine.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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

import hcmut.hoanganh.sunshine.JsonUtils;
import hcmut.hoanganh.sunshine.Utility;
import hcmut.hoanganh.sunshine.data.WeatherContract;

/**
 * Created by H.Anh on 25/02/2015.
 */
public class SunshineService extends IntentService {

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public SunshineService() {
        super("Sunshine Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String locationSetting = Utility.getLocationSetting(this);

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
                result = JsonUtils.getWeatherDataFromJson(data, NUM_OF_DAYS, this, locationSetting);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.e("Data", Arrays.asList(result).toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final int NUM_OF_DAYS = 7;
    public final String FORMAT = "json";
    public final String UNITS = "metric";
}
