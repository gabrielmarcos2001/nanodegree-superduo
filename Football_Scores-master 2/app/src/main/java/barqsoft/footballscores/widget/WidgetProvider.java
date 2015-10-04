package barqsoft.footballscores.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.ArrayList;

import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.model.Match;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.service.DataFetchService;

/**
 * Created by gabrielmarcos on 10/3/15.
 */
public class WidgetProvider extends AppWidgetProvider {

    private static ArrayList<Match> matches = new ArrayList<>();
    private static int position = 0;

    public static final String ACTION_DATA_UPDATED = "com.bargsfot.footballscores.ACTION_DATA_UPDATED";
    public static final String ACTION_PREV_CLICKED = "com.bargsfot.footballscores.ACTION_PREV";
    public static final String ACTION_NEXT_CLICKED = "com.bargsfot.footballscores.ACTION_NEXT";


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int i=0; i < appWidgetIds.length; i++) {

            // Set up the remote view object
            RemoteViews widgetView = new RemoteViews(context.getPackageName(),
                    R.layout.widget);

            // Initialize the widget data as empty
            updateViewWithMatch(widgetView, null,context);

            // We create an intent for starting the app when the user
            // selects the widget
            Intent intent = new Intent(context, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent
                    .getActivity(context, 0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

            widgetView.setOnClickPendingIntent(R.id.data_container, pendingIntent);
            widgetView.setOnClickPendingIntent(R.id.prev, getPendingSelfIntent(context,ACTION_PREV_CLICKED));
            widgetView.setOnClickPendingIntent(R.id.next, getPendingSelfIntent(context,ACTION_NEXT_CLICKED));

            appWidgetManager.updateAppWidget(appWidgetIds[i], widgetView);
        }

        position ++;
        if (position >= matches.size()) {
            position = 0;
        }

        // We query for the data
        Intent scoresService = new Intent(context, DataFetchService.class);
        context.startService(scoresService);

        super.onUpdate(context, appWidgetManager, appWidgetIds);

    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // The Data was updated
        if (ACTION_DATA_UPDATED.equals(intent.getAction())) {

            position = 0;
            matches = intent.getParcelableArrayListExtra("matches");

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int appWidgetIds[] = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, WidgetProvider.class));

            RemoteViews widgetView = new RemoteViews(context.getPackageName(),
                    R.layout.widget);

            if (matches.size() > 0) {

                Match matchData = matches.get(position);
                updateViewWithMatch(widgetView, matchData,context);
            }else {
                updateViewWithMatch(widgetView, null,context);
            }

            appWidgetManager.updateAppWidget(appWidgetIds,widgetView);

        }else if (ACTION_NEXT_CLICKED.equals(intent.getAction())) {

            position ++;
            if (position >= matches.size()) {
                position = 0;
            }

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int appWidgetIds[] = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, WidgetProvider.class));

            RemoteViews widgetView = new RemoteViews(context.getPackageName(),
                    R.layout.widget);

            if (matches.size() > 0) {

                Match matchData = matches.get(position);
                updateViewWithMatch(widgetView, matchData,context);

            }

            appWidgetManager.updateAppWidget(appWidgetIds,widgetView);

        }else if (ACTION_PREV_CLICKED.equals(intent.getAction())) {

            position --;
            if (position < 0) {
                position = matches.size()-1;
            }

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int appWidgetIds[] = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, WidgetProvider.class));

            RemoteViews widgetView = new RemoteViews(context.getPackageName(),
                    R.layout.widget);

            if (matches.size() > 0) {

                Match matchData = matches.get(position);
                updateViewWithMatch(widgetView,matchData,context);

            }

            appWidgetManager.updateAppWidget(appWidgetIds,widgetView);
        }

    }

    /**
     * Updates the widget view with the match data
     * @param widgetView
     * @param matchData
     * @param context
     */
    private void updateViewWithMatch(RemoteViews widgetView, Match matchData, Context context) {

        if (matchData == null) {
            widgetView.setTextViewText(R.id.home_name, "");
            widgetView.setTextViewText(R.id.away_name, "");
            widgetView.setTextViewText(R.id.data_textview, "");
            widgetView.setTextViewText(R.id.score_textview, Utilies.getScores(0, 0));
            widgetView.setImageViewResource(R.id.home_crest, Utilies.getTeamCrestByTeamName(""));
            widgetView.setImageViewResource(R.id.away_crest, Utilies.getTeamCrestByTeamName(""));
        }else {
            widgetView.setTextViewText(R.id.home_name,matchData.homeName);
            widgetView.setTextViewText(R.id.away_name,matchData.awayName);
            widgetView.setTextViewText(R.id.data_textview,context.getString(R.string.today));
            widgetView.setTextViewText(R.id.score_textview, Utilies.getScores(matchData.homeGoals,matchData.awayGoals));
            widgetView.setImageViewResource(R.id.home_crest, Utilies.getTeamCrestByTeamName(matchData.homeName));
            widgetView.setImageViewResource(R.id.away_crest, Utilies.getTeamCrestByTeamName(matchData.awayName));
        }
    }


}
