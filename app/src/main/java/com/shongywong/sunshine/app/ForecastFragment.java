package com.shongywong.sunshine.app;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by sarah on 10/25/2016.
 */
public class ForecastFragment extends Fragment
{
    ArrayAdapter<String> mArrayAdapter;

    public ForecastFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance)
    {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mArrayAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.list_item_forecast, R.id.list_item_forecast_textview, new ArrayList<String>());

        ListView listView = (ListView)rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mArrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                String text = mArrayAdapter.getItem(i);
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                detailIntent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(detailIntent);
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if(id == R.id.action_refresh)
        {
            updateWeather();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        updateWeather();
    }


    private void updateWeather()
    {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        String tempUnits = prefs.getString(getString(R.string.pref_temp_key),
                getString(R.string.pref_temp_default));
        weatherTask.execute(location, tempUnits);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>
    {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params)
        {
            if(params == null)
                return null;

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;
            String mode = "json";
            String units = "metric";
            int daysToShow = 7;

            try
            {
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri uri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, mode)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, daysToShow+"")
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

                URL url = new URL(uri.toString());

                Log.d(LOG_TAG, "Built URI " + uri.toString());

                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if(inputStream == null)
                    return null;

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while((line = reader.readLine()) != null)
                {
                    buffer.append(line + "\n");
                }

                if(buffer.length() == 0)
                    return null;

                forecastJsonStr = buffer.toString();
                Log.d(LOG_TAG, "forecast JSON " +forecastJsonStr);
                String[] parsedWeatherArr = parseWeatherJSON(forecastJsonStr, params[1]);

                return parsedWeatherArr;
            }
            catch (IOException e)
            {
                Log.e(LOG_TAG, "Error ", e);

                return null;
            }
            finally
            {
                if(urlConnection != null)
                {
                    urlConnection.disconnect();
                }
                if(reader!= null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (final IOException e)
                    {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
        }

        @Override
        public void onPostExecute(String[] params)
        {
            if(params != null)
            {
                mArrayAdapter.clear();
                for(String str : params)
                {
                    mArrayAdapter.add(str);
                }
            }
        }

        private String[] parseWeatherJSON(String jsonStr, String tempUnits)
        {
            try
            {
                StringBuilder stringBuilder = new StringBuilder();


                JSONObject jsonObject = new JSONObject(jsonStr);

                int count = jsonObject.getInt("cnt");
                String[] parsedArr = new String[count];

                JSONArray listJSONArr = jsonObject.getJSONArray("list");
                JSONArray dayDescripJSONArr = null;
                JSONObject dayWeatherInfoJSONObj = null;
                JSONObject tempJSONObj = null;
                JSONObject dayDescriptionJSONObj = null;
                Calendar calendar = GregorianCalendar.getInstance();
                int startDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                long[] tempConversion;

                for(int i = 0; i < listJSONArr.length(); i++)
                {
                    //get the readable weekday and Month/day
                    stringBuilder.append(getDateFormat(calendar.getTimeInMillis()) + " - ");
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    //get the current forecast info
                    dayWeatherInfoJSONObj = listJSONArr.getJSONObject(i);
                    //get the temps object
                    tempJSONObj = dayWeatherInfoJSONObj.getJSONObject("temp");
                    //get the weather array
                    dayDescripJSONArr = dayWeatherInfoJSONObj.getJSONArray("weather");
                    dayDescriptionJSONObj = dayDescripJSONArr.getJSONObject(0);
                    //get the weather description
                    stringBuilder.append(dayDescriptionJSONObj.getString("main") + " - ");
                    //get the day and night temps
                    tempConversion = formatTemps(tempJSONObj, tempUnits);
                    if(tempConversion == null)
                        return null;
                    stringBuilder.append(tempConversion[0] + "/" + tempConversion[1]);
                    //add it to the list of forecast info
                    parsedArr[i] = stringBuilder.toString();
                    //clear the sb
                    stringBuilder.setLength(0);
                }

                for(int i = 0; i < parsedArr.length; i++)
                    Log.d(LOG_TAG, "Forecast entry " + parsedArr[i]);

                return parsedArr;
            }
            catch (JSONException e)
            {
                Log.e(LOG_TAG, "Error " + e);
                return null;
            }
        }

        private long[] formatTemps(JSONObject tempJSONObj, String tempUnits)
        {
            try
            {
                double day = tempJSONObj.getDouble("day");
                double night = tempJSONObj.getDouble("night");
                long[] result = new long[2];

                if (tempUnits.equals("Fahrenheit"))
                {
                    day *= 1.8;
                    day += 32;
                    night *= 1.8;
                    night += 32;
                }
                result[0] = Math.round(day);
                result[1] = Math.round(night);
                return result;
            }
            catch (JSONException e)
            {
                Log.e(LOG_TAG, e.toString());
            }
            return null;
        }


        private String getDateFormat(long date)
        {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd");
           return simpleDateFormat.format(date);
        }
    }


}
