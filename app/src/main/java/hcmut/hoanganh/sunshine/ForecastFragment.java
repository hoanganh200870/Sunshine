package hcmut.hoanganh.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
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
                new FetchWeatherTask().execute("Ho Chi Minh");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ArrayList<String> forecasts = new ArrayList<>();
        forecasts.add("Today - Sunny - 88/63");
        forecasts.add("Tomorrow - Foggy - 70/46");
        forecasts.add("Weds - Cloudy - 72/63");
        forecasts.add("Thurs - Rainy - 64/54");
        forecasts.add("Fri - Foggy - 70/46");
        forecasts.add("Sat - Sunny - 76/68");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, forecasts);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(adapter);

        return rootView;
    }

    public static class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        public static final int NUM_OF_DAYS = 7;
        public static final String FORMAT = "json";
        public static final String UNITS = "metric";

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

    }
}
