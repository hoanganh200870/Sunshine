package hcmut.hoanganh.sunshine;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.protocol.HTTP;

import hcmut.hoanganh.sunshine.data.WeatherContract;

/**
 * Created by H.Anh on 25/02/2015.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String DATE_EXTRA = "detail_fragment_date_extra";

    private ShareActionProvider shareProvider;
    private String mLocation;
    private boolean isMetric;
    private int DETAIL_LOADER = 0;

    @Override
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        Context context = getActivity();
        boolean isMetric = Utility.isMetric(context);
        String location = Utility.getLocationSetting(context);
        if (arguments != null && arguments.containsKey(DATE_EXTRA) &&
                (mLocation != null && !location.equals(mLocation) || this.isMetric != isMetric)) {
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        FragmentActivity activity = getActivity();
        Bundle bundle = getArguments();
        String date = bundle.getString(DATE_EXTRA);
        if (bundle != null && date != null) {
            mLocation = Utility.getLocationSetting(activity);
            isMetric = Utility.isMetric(activity);

            Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(mLocation, date);
            CursorLoader cursorLoader = new CursorLoader(activity, uri, FORECAST_COLUMNS, null, null, null);
            return cursorLoader;
        }

        return null;
    }

    private String shareMessage;

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {

            Context context = getActivity();

            String description = data.getString(COL_SHORT_DESC);
            txtDescription.setText(description);

            int weatherId = data.getInt(COL_WEATHER_ID);
            int icon = Utility.getArtResourceForWeatherCondition(weatherId);
            imgIcon.setImageResource(icon);
            imgIcon.setContentDescription(description);

            String date = data.getString(COL_DATETEXT);
            String day = Utility.getDayName(context, date);
            txtDay.setText(day);

            String dateText = Utility.getFormattedMonthDay(date);
            txtDate.setText(dateText);

            boolean isMetric = Utility.isMetric(context);
            double high = data.getDouble(COL_MAX_TEMP);
            String highString = Utility.formatTemperature(context, high, isMetric);
            txtHigh.setText(highString);

            double low = data.getDouble(COL_MIN_TEMP);
            String lowString = Utility.formatTemperature(context, low, isMetric);
            txtLow.setText(lowString);

            float humidity = data.getFloat(COL_HUMIDITY);
            String humidityString = context.getString(R.string.format_humidity, humidity);
            txtHumidity.setText(humidityString);

            float pressure = data.getFloat(COL_PRESSURE);
            String pressureString = context.getString(R.string.format_pressure, pressure);
            txtPressure.setText(pressureString);

            float wind = data.getFloat(COL_WIND);
            float windDir = data.getFloat(COL_DEGREES);
            String windString = Utility.getFormattedWind(context, wind, windDir);
            txtWind.setText(windString);

            shareMessage = String.format("%s - %s - %s/%s", dateText, description, high, low);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detail_fragment, menu);

        MenuItem item = menu.findItem(R.id.action_share);
        shareProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.action_share:
                if (shareProvider == null) {
                    return true;
                }

                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                sendIntent.setType(HTTP.PLAIN_TEXT_TYPE);

                if (shareMessage != null) {
                    String text = getString(R.string.format_share, shareMessage);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                    shareProvider.setShareIntent(sendIntent);
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private TextView txtDay, txtDate, txtDescription, txtHigh, txtLow, txtHumidity, txtPressure, txtWind;
    private ImageView imgIcon;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        txtDay = (TextView) rootView.findViewById(R.id.txtDay);
        txtDate = (TextView) rootView.findViewById(R.id.txtDate);
        txtDescription = (TextView) rootView.findViewById(R.id.txtDescription);
        txtHigh = (TextView) rootView.findViewById(R.id.txtHigh);
        txtLow = (TextView) rootView.findViewById(R.id.txtLow);
        txtHumidity = (TextView) rootView.findViewById(R.id.txtHumidity);
        txtPressure = (TextView) rootView.findViewById(R.id.txtPressure);
        txtWind = (TextView) rootView.findViewById(R.id.txtWind);
        imgIcon = (ImageView) rootView.findViewById(R.id.imgIcon);
        return rootView;
    }

    public static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
    };

    public static final int COL_ID = 0;
    public static final int COL_DATETEXT = 1;
    public static final int COL_SHORT_DESC = 2;
    public static final int COL_MAX_TEMP = 3;
    public static final int COL_MIN_TEMP = 4;
    public static final int COL_HUMIDITY = 5;
    public static final int COL_PRESSURE = 6;
    public static final int COL_WIND = 7;
    public static final int COL_DEGREES = 8;
    public static final int COL_WEATHER_ID = 9;
    public static final int COL_LOCATION_SETTING = 10;

}
