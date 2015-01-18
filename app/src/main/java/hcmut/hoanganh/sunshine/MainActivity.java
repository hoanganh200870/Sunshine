package hcmut.hoanganh.sunshine;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment())
                    .commit();
        }

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
            case R.id.aciton_show_location:

                SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String key = getString(R.string.pref_location_key);
                String defaultValue = getString(R.string.pref_location_default);
                String location = defaultSharedPreferences.getString(key, defaultValue);

                Intent mapIntent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse("geo:0,0").buildUpon().appendQueryParameter("q", location).build();
                mapIntent.setData(uri);

                ComponentName resolveActivity = mapIntent.resolveActivity(getPackageManager());
                if (resolveActivity != null) {
                    startActivity(mapIntent);
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
