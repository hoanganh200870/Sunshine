package hcmut.hoanganh.sunshine;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Date;

import hcmut.hoanganh.sunshine.adapter.ForecastAdapter;
import hcmut.hoanganh.sunshine.data.WeatherContract;
import hcmut.hoanganh.sunshine.service.SunshineService;

/**
 * Created by H.Anh on 18/01/2015.
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private boolean isTwoPane;

    public void setIsTwoPane(boolean isTwoPane) {
        this.isTwoPane = isTwoPane;

        if (weatherAdapter != null) {
            weatherAdapter.setIsTwoPane(isTwoPane);
        }
    }

    public interface Callback {
        void onItemSelected(String date);
    }

    private ForecastAdapter weatherAdapter;

    private String mLocation;
    private boolean isMetric;
    private final int FORECAST_LOADER = 0;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
    }

    public static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    public static final int COL_ID = 0;
    public static final int COL_DATETEXT = 1;
    public static final int COL_SHORT_DESC = 2;
    public static final int COL_MAX_TEMP = 3;
    public static final int COL_MIN_TEMP = 4;
    public static final int COL_WEATHER_ID = 5;
    public static final int COL_LOCATION_SETTING = 6;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Date now = new Date();
        String startDate = WeatherContract.getDbDateString(now);

        Context context = getActivity();
        mLocation = Utility.getLocationSetting(context);
        isMetric = Utility.isMetric(context);

        Uri weatherLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(mLocation, startDate);
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC ";

        CursorLoader cursorLoader = new CursorLoader(context, weatherLocationUri, FORECAST_COLUMNS, null, null, sortOrder);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        weatherAdapter.swapCursor(cursor);

//        if (mPosition != ListView.INVALID_POSITION) {
//            mListForecast.setSelection(mPosition);
//        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        weatherAdapter.swapCursor(null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    private void updateWeather() {
        Context context = getActivity();

        Intent alarmIntent = new Intent(context, SunshineService.AlarmReceiver.class);
//        alarmIntent.putExtra(SunshineService.AlarmReceiver.EXTRA_LOCATION, mLocation);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long time = System.currentTimeMillis() + 5000;
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }

    @Override
    public void onResume() {
        super.onResume();

        Context context = getActivity();
        String location = Utility.getLocationSetting(context);
        boolean isMetric = Utility.isMetric(context);
        if (mLocation != null && !location.equals(mLocation) || this.isMetric != isMetric) {
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                updateWeather();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ListView mListForecast;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final Context context = getActivity();

        weatherAdapter = new ForecastAdapter(context, null, 0);
        weatherAdapter.setIsTwoPane(isTwoPane);

        mListForecast = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListForecast.setAdapter(weatherAdapter);

        mListForecast.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = weatherAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    Activity activity = getActivity();
                    if (activity instanceof Callback) {
                        Callback callback = (Callback) activity;
                        String date = cursor.getString(COL_DATETEXT);
                        callback.onItemSelected(date);
                    }
                }

//                mPosition = position;
            }
        });

//        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SELECTED)) {
//            mPosition = savedInstanceState.getInt(KEY_SELECTED);
//        }

        return rootView;
    }

//    private static String KEY_SELECTED = "selected_item";
//    private int mPosition;

}
