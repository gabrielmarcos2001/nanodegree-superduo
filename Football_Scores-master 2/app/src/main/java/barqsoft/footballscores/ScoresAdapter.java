package barqsoft.footballscores;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by yehya khaled on 2/26/2015.
 */
public class ScoresAdapter extends CursorAdapter {

    public double detail_match_id = 0;
    public static final int COL_DATE = 1;
    public static final int COL_MATCHTIME = 2;
    public static final int COL_HOME = 3;
    public static final int COL_AWAY = 4;
    public static final int COL_HOME_ID = 5;
    public static final int COL_AWAY_ID = 6;
    public static final int COL_LEAGUE = 7;
    public static final int COL_HOME_GOALS = 8;
    public static final int COL_AWAY_GOALS = 9;
    public static final int COL_ID = 10;
    public static final int COL_MATCHDAY = 11;

    private final String FOOTBALL_SCORES_HASHTAG = "#FootballScores";

    public ScoresAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View mItem = LayoutInflater.from(context).inflate(R.layout.scores_list_item, parent, false);
        ViewHolder holder = new ViewHolder(mItem);
        mItem.setTag(holder);

        return mItem;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.homeName.setText(cursor.getString(COL_HOME));
        holder.awayName.setText(cursor.getString(COL_AWAY));
        holder.date.setText(cursor.getString(COL_MATCHTIME));
        holder.score.setText(Utilies.getScores(cursor.getInt(COL_HOME_GOALS), cursor.getInt(COL_AWAY_GOALS)));
        holder.match_id = cursor.getDouble(COL_ID);

        holder.homeCrest.setImageResource(Utilies.getTeamCrestByTeamId(
                cursor.getInt(COL_HOME_ID)));

        holder.awayCrest.setImageResource(Utilies.getTeamCrestByTeamId(
                cursor.getInt(COL_AWAY_ID)
        ));

        LayoutInflater vi = (LayoutInflater) context.getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = vi.inflate(R.layout.detail_fragment, null);
        ViewGroup container = (ViewGroup) view.findViewById(R.id.details_fragment_container);

        if (holder.match_id == detail_match_id) {

            container.addView(v, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.MATCH_PARENT));

            TextView matchDay = (TextView) v.findViewById(R.id.matchday_textview);
            matchDay.setText(Utilies.getMatchDay(context,cursor.getInt(COL_MATCHDAY)));

            TextView league = (TextView) v.findViewById(R.id.league_textview);
            league.setText(Utilies.getLeague(context,cursor.getInt(COL_LEAGUE)));

            Button shareButton = (Button) v.findViewById(R.id.share_button);
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //add Share Action
                    context.startActivity(createShareForecastIntent(holder.homeName.getText() + " "
                            + holder.score.getText() + " " + holder.awayName.getText() + " "));
                }
            });

        } else {
            container.removeAllViews();
        }

    }

    public Intent createShareForecastIntent(String ShareText) {

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, ShareText + FOOTBALL_SCORES_HASHTAG);
        return shareIntent;
    }

}
