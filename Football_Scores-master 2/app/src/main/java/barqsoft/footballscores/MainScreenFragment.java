package barqsoft.footballscores;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import barqsoft.footballscores.model.Match;
import barqsoft.footballscores.widget.WidgetProvider;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainScreenFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    public static final int SCORES_LOADER = 0;

    public ScoresAdapter mAdapter;
    private String[] mFragmentDate = new String[1];
    private View mLoader;
    private View mEmptyView;
    private SwipeRefreshLayout mSwipeLayout;

    /**
     * Constructor
     */
    public MainScreenFragment() {

    }

    public void setmFragmentDate(String date) {
        mFragmentDate[0] = date;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mLoader = rootView.findViewById(R.id.loader);
        mEmptyView = rootView.findViewById(R.id.empty_list);
        mEmptyView.setVisibility(View.GONE);

        mSwipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorSchemeResources(R.color.fb_green_1,
                R.color.fb_yellow_1,
                R.color.fb_green_1,
                R.color.fb_yellow_1);

        final ListView scoreList = (ListView) rootView.findViewById(R.id.scores_list);

        mAdapter = new ScoresAdapter(getActivity(), null, 0);

        scoreList.setAdapter(mAdapter);
        getLoaderManager().initLoader(SCORES_LOADER, null, this);

        mAdapter.detail_match_id = MainActivity.mSelectedMatchId;

        scoreList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ViewHolder selected = (ViewHolder) view.getTag();
                mAdapter.detail_match_id = selected.match_id;
                MainActivity.mSelectedMatchId = (int) selected.match_id;
                mAdapter.notifyDataSetChanged();

            }
        });

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        // Creates a loader for getting the scores with the current date
        return new CursorLoader(getActivity(), DatabaseContract.ScoresTable.buildScoreWithDate(),
                null, null, mFragmentDate, null);
    }


    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        mAdapter.swapCursor(cursor);
        mLoader.setVisibility(View.GONE);
        mSwipeLayout.setRefreshing(false);

        if (mAdapter.getCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        }else {
            mEmptyView.setVisibility(View.GONE);
        }

        Calendar calendar = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        if (formatter.format(calendar.getTime()).equals(mFragmentDate[0])) {

            // We send the data to the Widget if it is from today
            ArrayList<Parcelable> matchList = new ArrayList<>();
            while (cursor.moveToNext()) {

                Match match = new Match();
                match.homeName = cursor.getString(ScoresAdapter.COL_HOME);
                match.awayName = cursor.getString(ScoresAdapter.COL_AWAY);
                match.date = cursor.getString(ScoresAdapter.COL_DATE);
                match.homeGoals = Integer.valueOf(cursor.getString(ScoresAdapter.COL_HOME_GOALS));
                match.awayGoals = Integer.valueOf(cursor.getString(ScoresAdapter.COL_AWAY_GOALS));
                match.homeId = Integer.valueOf(cursor.getString(ScoresAdapter.COL_HOME_ID));
                match.awayId = Integer.valueOf(cursor.getString(ScoresAdapter.COL_AWAY_ID));
                matchList.add(match);

            }

            Intent dataUpdatedIntent = new Intent(WidgetProvider.ACTION_DATA_UPDATED);
            dataUpdatedIntent.putParcelableArrayListExtra("matches", matchList);
            getActivity().sendBroadcast(dataUpdatedIntent);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onRefresh() {
        if (getActivity() instanceof  MainActivity) {
            ((MainActivity) getActivity()).updateScores();
        }
    }

    public void onError() {
        if (isAdded()) {
            mSwipeLayout.setRefreshing(false);
        }
    }
}
