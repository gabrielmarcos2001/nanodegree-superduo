package barqsoft.footballscores.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.util.Log;

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
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import barqsoft.footballscores.AppConfig;
import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.model.Match;
import barqsoft.footballscores.widget.WidgetProvider;

/**
 * Created by yehya khaled on 3/2/2015.
 */
public class DataFetchService extends IntentService {

    public static final String LOG_TAG = "DataFetchService";

    public DataFetchService() {
        super("DataFetchService");
    }

    private Messenger mMessenger;

    @Override
    protected void onHandleIntent(Intent intent) {

        // Gets a reference to the messenger for sending error
        // messages
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mMessenger = (Messenger) extras.get("messenger");
        }

        // Gets the data for the next 2 days
        getData("n2");

        // Gets the data for 2 days in the past
        getData("p2");

        return;
    }

    /**
     * Gets the Data from the football api
     * @param timeFrame
     */
    private void getData(String timeFrame) {

        //Creating fetch URL
        final String BASE_URL = AppConfig.BASE_URL; //Base URL
        final String QUERY_TIME_FRAME = "timeFrame"; //Time Frame parameter to determine days

        Uri fetchBuild = Uri.parse(BASE_URL).buildUpon().
                appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build();

        HttpURLConnection httpConnection = null;
        BufferedReader reader = null;
        String jsonData = null;

        //Opening Connection
        try {

            URL fetch = new URL(fetchBuild.toString());
            httpConnection = (HttpURLConnection) fetch.openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.addRequestProperty("X-Auth-Token", getString(R.string.api_key));
            httpConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = httpConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
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
                return;
            }

            jsonData = buffer.toString();

        } catch (Exception e) {
            sendErrorMessage(getString(R.string.default_error));

        } finally {

            if (httpConnection != null) {
                httpConnection.disconnect();
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error Closing Stream");
                }
            }
        }try {

            if (jsonData != null) {

                //This bit is to check if the data contains any matches. If not, we call processJson on the dummy data
                JSONArray matches = new JSONObject(jsonData).getJSONArray("fixtures");
                if (matches.length() == 0) {
                    //if there is no data, call the function on dummy data
                    //this is expected behavior during the off season.
                    processJsonData(getString(R.string.dummy_data), getApplicationContext(), false);
                    return;
                }

                processJsonData(jsonData, getApplicationContext(), true);

            } else {
                sendErrorMessage(getString(R.string.default_error));
            }

        } catch (Exception e) {
            sendErrorMessage(getString(R.string.default_error));
        }
    }

    /**
     * Process the JSON Data returned by the API
     * @param jsonData
     * @param mContext
     * @param isReal
     */
    private void processJsonData(String jsonData, Context mContext, boolean isReal) {

        final String SEASON_LINK = "http://api.football-data.org/alpha/soccerseasons/";
        final String MATCH_LINK = "http://api.football-data.org/alpha/fixtures/";
        final String TEAM_LINK = "http://api.football-data.org/alpha/teams/";
        final String FIXTURES = "fixtures";
        final String LINKS = "_links";
        final String SOCCER_SEASON = "soccerseason";
        final String SELF = "self";
        final String MATCH_DATE = "date";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";
        final String AWAY_TEAM_REF = "awayTeam";
        final String HOME_TEAM_REF = "homeTeam";


        //Match data
        String leagueId;
        String homeId;
        String awayId;
        String date;
        String time;
        String homeName;
        String awayName;
        String homeGoals;
        String awayGoals;
        String matchId;
        String matchDay;

        // Flag used for checking if the score is for the current date
        boolean todayScore;

        try {

            JSONObject jsonObject = new JSONObject(jsonData);

            // Gets the current date formatted
            Date today = Calendar.getInstance().getTime();
            SimpleDateFormat todayDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String todayFormatted = todayDateFormat.format(today);

            // List of matches for the current date - We sent these to the Widget
            ArrayList<Parcelable> currentDateMatchList = new ArrayList<>();

            JSONArray matches = jsonObject.getJSONArray(FIXTURES);

            //ContentValues to be inserted
            Vector<ContentValues> values = new Vector<>(matches.length());

            // Process every match returned by the api
            for (int i = 0; i < matches.length(); i++) {

                todayScore = false;

                JSONObject matchData = matches.getJSONObject(i);
                leagueId = matchData.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).
                        getString("href");

                leagueId = leagueId.replace(SEASON_LINK, "");

                homeId = matchData.getJSONObject(LINKS).getJSONObject(HOME_TEAM_REF).
                        getString("href");

                homeId = homeId.replace(TEAM_LINK, "");

                awayId = matchData.getJSONObject(LINKS).getJSONObject(AWAY_TEAM_REF).
                        getString("href");

                awayId = awayId.replace(TEAM_LINK, "");

                //This if statement controls which leagues we're interested in the data from.
                //add leagues here in order to have them be added to the DB.
                // If you are finding no data in the app, check that this contains all the leagues.
                // If it doesn't, that can cause an empty DB, bypassing the dummy data routine.
                if (Utilies.checkProcessLeague(leagueId)) {

                    matchId = matchData.getJSONObject(LINKS).getJSONObject(SELF).
                            getString("href");

                    matchId = matchId.replace(MATCH_LINK, "");

                    if (!isReal) {
                        //This if statement changes the match ID of the dummy data so that it all goes into the database
                        matchId = matchId + Integer.toString(i);
                    }

                    date = matchData.getString(MATCH_DATE);
                    time = date.substring(date.indexOf("T") + 1, date.indexOf("Z"));
                    date = date.substring(0, date.indexOf("T"));
                    SimpleDateFormat matchDate = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
                    matchDate.setTimeZone(TimeZone.getTimeZone("UTC"));

                    // Checks if the Score if for today
                    if (date.equals(todayFormatted)) {
                        todayScore = true;
                    }

                    try {

                        Date parsedDate = matchDate.parse(date + time);
                        SimpleDateFormat newDate = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
                        newDate.setTimeZone(TimeZone.getDefault());
                        date = newDate.format(parsedDate);
                        time = date.substring(date.indexOf(":") + 1);
                        date = date.substring(0, date.indexOf(":"));

                        if (!isReal) {

                            //This if statement changes the dummy data's date to match our current date range.
                            Date fragmentDate = new Date(System.currentTimeMillis() + ((i - 2) * 86400000));
                            SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
                            date = mformat.format(fragmentDate);
                        }

                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }

                    homeName = matchData.getString(HOME_TEAM);
                    awayName = matchData.getString(AWAY_TEAM);
                    homeGoals = matchData.getJSONObject(RESULT).getString(HOME_GOALS);
                    awayGoals = matchData.getJSONObject(RESULT).getString(AWAY_GOALS);
                    matchDay = matchData.getString(MATCH_DAY);

                    ContentValues match_values = new ContentValues();
                    match_values.put(DatabaseContract.ScoresTable.MATCH_ID, matchId);
                    match_values.put(DatabaseContract.ScoresTable.DATE_COL, date);
                    match_values.put(DatabaseContract.ScoresTable.TIME_COL, time);
                    match_values.put(DatabaseContract.ScoresTable.HOME_COL, homeName);
                    match_values.put(DatabaseContract.ScoresTable.AWAY_COL, awayName);
                    match_values.put(DatabaseContract.ScoresTable.HOME_ID, homeId);
                    match_values.put(DatabaseContract.ScoresTable.AWAY_ID, awayId);
                    match_values.put(DatabaseContract.ScoresTable.LEAGUE_COL, leagueId);
                    match_values.put(DatabaseContract.ScoresTable.HOME_GOALS_COL, homeGoals);
                    match_values.put(DatabaseContract.ScoresTable.AWAY_GOALS_COL, awayGoals);
                    match_values.put(DatabaseContract.ScoresTable.MATCH_DAY, matchDay);

                    // We keep track of today scores for
                    // sending it to the widget
                    if (todayScore) {
                        Match match = new Match();
                        match.homeName = homeName;
                        match.awayName = awayName;
                        match.date = date;
                        match.homeGoals = Integer.valueOf(homeGoals);
                        match.awayGoals = Integer.valueOf(awayGoals);
                        match.homeId = Integer.valueOf(homeId);
                        match.awayId = Integer.valueOf(awayId);
                        currentDateMatchList.add(match);
                    }

                    values.add(match_values);
                }
            }

            // Every time the service updates and we are processing the scores
            // for today we send a broadcast message so we can update the widget
            // with the scores information
            Intent dataUpdatedIntent = new Intent(WidgetProvider.ACTION_DATA_UPDATED);
            dataUpdatedIntent.putParcelableArrayListExtra("matches",currentDateMatchList);
            sendBroadcast(dataUpdatedIntent);

            ContentValues[] insert_data = new ContentValues[values.size()];
            values.toArray(insert_data);

            // Inserts the data in the database
            mContext.getContentResolver().bulkInsert(
                    DatabaseContract.BASE_CONTENT_URI, insert_data);

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

    }

    /**
     * Sends an error message by using a messenger
     * @param error
     */
    private void sendErrorMessage(String error) {

        if (mMessenger != null) {

            Message msg = Message.obtain();
            msg.arg1 = Activity.RESULT_CANCELED;
            Bundle bundle = new Bundle();
            bundle.putString("errorMessage",error);
            msg.setData(bundle);
            try {
                mMessenger.send(msg);
            } catch (android.os.RemoteException e1) {

            }
        }
    }
}

