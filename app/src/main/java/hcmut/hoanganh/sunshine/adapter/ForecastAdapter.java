package hcmut.hoanganh.sunshine.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import hcmut.hoanganh.sunshine.ForecastFragment;
import hcmut.hoanganh.sunshine.R;
import hcmut.hoanganh.sunshine.Utility;

/**
 * Created by H.Anh on 24/02/2015.
 */
public class ForecastAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_COUNT = 2;
    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private boolean isTwoPane;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        int type = position == 0 && !isTwoPane ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
        return type;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int position = cursor.getPosition();

        int layoutId = -1;

        int type = getItemViewType(position);
        switch (type) {
            case VIEW_TYPE_TODAY:
                layoutId = R.layout.list_item_forecast_today;
                break;
            case VIEW_TYPE_FUTURE_DAY:
                layoutId = R.layout.list_item_forecast;
                break;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Read weather icon ID from cursor
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_ID);

        // Use placeholder image for now
        int icon;

        int position = cursor.getPosition();
        int type = getItemViewType(position);
        switch (type) {
            case VIEW_TYPE_TODAY:
                icon = Utility.getArtResourceForWeatherCondition(weatherId);
                break;
            default:
                icon = Utility.getIconResourceForWeatherCondition(weatherId);
        }

        viewHolder.imgIcon.setImageResource(icon);

        // Read date from cursor
        String dateString = cursor.getString(ForecastFragment.COL_DATETEXT);
        // Find TextView and set formatted date on it
        String day = Utility.getFriendlyDayString(context, dateString);
        viewHolder.txtDate.setText(day);

        // Read weather forecast from cursor
        String description = cursor.getString(ForecastFragment.COL_SHORT_DESC);
        // Find TextView and set weather forecast on it
        viewHolder.txtDescription.setText(description);

        // Read user preference for metric or imperial temperature units
        boolean isMetric = Utility.isMetric(context);

        // Read high temperature from cursor
        double high = cursor.getDouble(ForecastFragment.COL_MAX_TEMP);
        String highFormatted = Utility.formatTemperature(context, high, isMetric);
        viewHolder.txtHigh.setText(highFormatted);

        // Read low temperature from cursor
        double low = cursor.getDouble(ForecastFragment.COL_MIN_TEMP);
        viewHolder.txtLow.setText(Utility.formatTemperature(context, low, isMetric));
    }

    public void setIsTwoPane(boolean isTwoPane) {
        this.isTwoPane = isTwoPane;
    }

    public static class ViewHolder {
        public final ImageView imgIcon;
        public final TextView txtDate, txtDescription, txtHigh, txtLow;

        public ViewHolder(View view) {
            imgIcon = (ImageView) view.findViewById(R.id.list_item_icon);
            txtDate = (TextView) view.findViewById(R.id.list_item_date_textview);
            txtDescription = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            txtLow = (TextView) view.findViewById(R.id.list_item_low_textview);
            txtHigh = (TextView) view.findViewById(R.id.list_item_high_textview);
        }
    }
}
