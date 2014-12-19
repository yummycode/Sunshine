package com.example.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by youngjin on 14. 12. 17..
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> arrayAdapter;
    private String postCode = "94043";

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
//            TextView todayText = (TextView)rootView.findViewById(R.id.date_text);
//            DateFormat dateFormat = new SimpleDateFormat("MMM dd");
//            Date date = new Date();
//            todayText.setText("Today, " + dateFormat.format(date));

        List<String> weatherList = new ArrayList<String>(Arrays.asList(
                "Today - Sunny - 88/63",
                "Tomorrow - Foggy - 70/45",
                "Weds - Cloudy - 72/63",
                "Thurs - Rainy - 64/51",
                "Fri - Foggy - 70/46",
                "Sat - Sunny - 76/68",
                "Sun - Sunny - 80/68"
        ));

        arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weatherList
        );

        ListView mListView = (ListView)rootView.findViewById(R.id.listView_forecast);
        mListView.setAdapter(arrayAdapter);

//        new FetchWeatherTask().execute(postCode);

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            new FetchWeatherTask().execute(postCode);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String makeUrl(String postCode) {
        final String openWeatherUri = "http://api.openweathermap.org/data/2.5/forecast/daily";
        Uri weatherUri = Uri.parse(openWeatherUri);
        Uri.Builder uriBuilder = weatherUri.buildUpon();
        uriBuilder.appendQueryParameter("q",postCode);
        uriBuilder.appendQueryParameter("mode","json");
        uriBuilder.appendQueryParameter("units","metric");
        uriBuilder.appendQueryParameter("cnt","7");
        Uri requestUri = uriBuilder.build();
        return requestUri.toString();
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        protected String[] doInBackground(String... params) {
            String[] weatherInfo = null;

            if(params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
//                URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");
                URL requestUrl = new URL(makeUrl(params[0]));
//                String requestUrl = makeUrl("94043");

//                Log.d("url test",requestUrl);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) requestUrl.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                    return null;
                }
                forecastJsonStr = buffer.toString();
                try {
                    weatherInfo = new WeatherDataParser().getWeatherDataFromJson(forecastJsonStr, 7);
                    for(String weather : weatherInfo) {
                        Log.d(LOG_TAG, weather);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return weatherInfo;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);
            if(arrayAdapter != null && strings != null) {
                arrayAdapter.clear();
                for(String weatherInfo : strings) {
                    arrayAdapter.add(weatherInfo);
                }
            }
        }
    }
}
