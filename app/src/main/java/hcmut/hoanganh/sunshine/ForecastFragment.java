package hcmut.hoanganh.sunshine;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by H.Anh on 18/01/2015.
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    public ForecastFragment() {

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String key = getString(R.string.pref_location_key);
                String defaultValue = getString(R.string.pref_location_default);
                String location = defaultSharedPreferences.getString(key, defaultValue);
                new FetchWeatherTask().execute(location);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        weatherAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(weatherAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Context context = getActivity();
                Intent intent = new Intent(context, DetailActivity.class);
                String data = weatherAdapter.getItem(position);
                intent.putExtra(DetailActivity.EXTRA, data);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private ArrayAdapter<String> weatherAdapter;

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

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
                    result = JsonUtils.getWeatherDataFromJson(data, NUM_OF_DAYS);
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
    }
}
