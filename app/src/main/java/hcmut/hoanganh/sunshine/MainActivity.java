package hcmut.hoanganh.sunshine;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import hcmut.hoanganh.sunshine.adapter.SunshineSyncAdapter;


public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback {

    private boolean isTwoPane;

    @Override
    public void onItemSelected(String date) {
        if (isTwoPane) {
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();

            DetailFragment detailFragment = new DetailFragment();
            Bundle args = new Bundle();
            args.putString(DetailFragment.DATE_EXTRA, date);
            detailFragment.setArguments(args);

            fragmentTransaction.replace(R.id.weather_detail_container, detailFragment);
            fragmentTransaction.commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA, date);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.container, new ForecastFragment())
//                    .commit();
//        }

        View detailFragment = findViewById(R.id.weather_detail_container);
        FragmentManager supportFragmentManager = getSupportFragmentManager();

        isTwoPane = detailFragment != null;
        Log.e("Main Activity", "Two pane: " + isTwoPane);
        if (isTwoPane) {
            Log.e("Main Activity", "Saved state: " + savedInstanceState);
            if (savedInstanceState == null) {

                FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.weather_detail_container, new DetailFragment());
                fragmentTransaction.commit();
            }
        }

        ForecastFragment forecastFragment = (ForecastFragment) supportFragmentManager.findFragmentById(R.id.fragment_forecast);
        forecastFragment.setIsTwoPane(isTwoPane);

        SunshineSyncAdapter.initializeSyncAdapter(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
