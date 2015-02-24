package hcmut.hoanganh.sunshine;

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
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Date;

import hcmut.hoanganh.sunshine.data.WeatherContract;

/**
 * Created by H.Anh on 18/01/2015.
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter weatherAdapter;

    private String mLocation;
    private boolean isImperial;
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
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    public static final int COL_WEATHER_ID = 0;
    public static final int COL_DATETEXT = 1;
    public static final int COL_SHORT_DESC = 2;
    public static final int COL_MAX_TEMP = 3;
    public static final int COL_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Date now = new Date();
        String startDate = WeatherContract.getDbDateString(now);

        Context context = getActivity();
        mLocation = Utilities.getLocationSetting(context);
        isImperial = Utilities.isImperial(context);

        Uri weatherLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(mLocation, startDate);
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC ";

        CursorLoader cursorLoader = new CursorLoader(context, weatherLocationUri, FORECAST_COLUMNS, null, null, sortOrder);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        weatherAdapter.swapCursor(cursor);
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

//    private void updateWeather() {
//        Context context = getActivity();
//        new FetchWeatherTask(context).execute();
//    }

    @Override
    public void onStart() {
        super.onStart();
//        this.updateWeather();
    }

    @Override
    public void onResume() {
        super.onResume();

        Context context = getActivity();
        String location = Utilities.getLocationSetting(context);
        boolean isImperial = Utilities.isImperial(context);
        if (mLocation != null && !location.equals(mLocation) || this.isImperial != isImperial) {
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
//                this.updateWeather();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        final Context context = getActivity();

        String[] from = {
                WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
        };

        int[] to = {
                R.id.list_item_date_textview,
                R.id.list_item_forecast_textview,
                R.id.list_item_high_textview,
                R.id.list_item_low_textview
        };

        weatherAdapter = new SimpleCursorAdapter(context, R.layout.list_item_forecast, null, from, to, 0);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(weatherAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = weatherAdapter.getCursor();

                if (cursor != null && cursor.moveToPosition(position)) {
                    boolean isImperial = Utilities.isImperial(context);

                    String date = cursor.getString(COL_DATETEXT);
                    date = Utilities.formatDate(date);

                    String city = cursor.getString(COL_SHORT_DESC);

                    double high = cursor.getDouble(COL_MAX_TEMP);
                    String highFormatted = Utilities.formatTemperature(high, isImperial);

                    double low = cursor.getDouble(COL_MIN_TEMP);
                    String lowFormatted = Utilities.formatTemperature(low, isImperial);

                    String forecast = String.format("%s - %s - %s / %s",
                            date, city, highFormatted, lowFormatted);

                    Intent intent = new Intent(context, DetailActivity.class);
                    intent.putExtra(DetailActivity.EXTRA, forecast);
                    startActivity(intent);
                }
            }
        });

        return rootView;
    }

    /*public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        public final int NUM_OF_DAYS = 7;
        public final String FORMAT = "json";
        public final String UNITS = "metric";

        @Override
        protected String[] doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            Uri.Builder builder = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/daily").buildUpon();
            builder.appendQueryParameter("cnt", Integer.toString(NUM_OF_DAYS));
            builder.appendQueryParameter("q", params[0]);
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
                    result = JsonUtils.getWeatherDataFromJson(data, NUM_OF_DAYS, getActivity(), null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.e("Data", Arrays.asList(result).toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {

                weatherAdapter.clear();

                for (String item : result) {
                    weatherAdapter.add(item);
                }
            }
        }
    }*/
}
