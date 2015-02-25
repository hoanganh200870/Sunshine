package hcmut.hoanganh.sunshine.adapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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
import java.util.Date;

import hcmut.hoanganh.sunshine.JsonUtils;
import hcmut.hoanganh.sunshine.MainActivity;
import hcmut.hoanganh.sunshine.R;
import hcmut.hoanganh.sunshine.Utility;
import hcmut.hoanganh.sunshine.data.WeatherContract;

/**
 * Created by H.Anh on 25/02/2015.
 */
public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public final int NUM_OF_DAYS = 7;
    public final String FORMAT = "json";
    public final String UNITS = "metric";

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Context context = getContext();

        String locationSetting = Utility.getPreferredLocation(context);

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

    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        Account syncAccount = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        ContentResolver.requestSync(syncAccount, authority, bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        String name = context.getString(R.string.app_name);
        String type = context.getString(R.string.sync_account_type);
        Account newAccount = new Account(name, type);

        // If the password doesn't exist, the account doesn't exist
        String password = accountManager.getPassword(newAccount);
        if (null == password) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    public static final int SYNC_INTERVAL = 60; // 180 * 60;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        String authority = context.getString(R.string.content_authority);
//        ContentResolver.setIsSyncable(newAccount, authority, 1);
        ContentResolver.setSyncAutomatically(newAccount, authority, true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    private static final long DAY_IN_MILLIS = 1000; //1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;


    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    public static void notifyWeather(Context context) {
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String key = context.getString(R.string.pref_notification_key);
        String defaultStr = context.getString(R.string.pref_notification_default);
        boolean defaultValue = Boolean.parseBoolean(defaultStr);
        boolean isEnable = prefs.getBoolean(key, defaultValue);

        if (!isEnable) {
            return;
        }

        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        long lastSync = prefs.getLong(lastNotificationKey, 0);

        long current = System.currentTimeMillis();
        if (current - lastSync >= DAY_IN_MILLIS) {
            // Last sync was more than 1 day ago, let's send a notification with the weather.
            String locationQuery = Utility.getPreferredLocation(context);

            Date date = new Date();
            String dateFormatted = WeatherContract.getDbDateString(date);
            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, dateFormatted);

            // we'll query our contentProvider, as always
            Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

            if (cursor.moveToFirst()) {
                int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                double high = cursor.getDouble(INDEX_MAX_TEMP);
                double low = cursor.getDouble(INDEX_MIN_TEMP);
                String desc = cursor.getString(INDEX_SHORT_DESC);

                int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                String title = context.getString(R.string.app_name);

                boolean isMetric = Utility.isMetric(context);

                // Define the text of the forecast.
                String highFormatted = Utility.formatTemperature(context, high, isMetric);
                String lowFormatted = Utility.formatTemperature(context, low, isMetric);
                String contentText = String.format(context.getString(R.string.format_notification),
                        desc, highFormatted, lowFormatted);

                //build your notification here.
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                        .setSmallIcon(iconId).setContentTitle(title).setContentText(contentText).setAutoCancel(true);

                Intent intent = new Intent(context, MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(intent);

                PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                builder.setContentIntent(pendingIntent);
                Notification notification = builder.build();

                notificationManager.notify(WEATHER_NOTIFICATION_ID, notification);

                //refreshing last sync
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(lastNotificationKey, current);
                editor.commit();
            }
        }

    }
}
