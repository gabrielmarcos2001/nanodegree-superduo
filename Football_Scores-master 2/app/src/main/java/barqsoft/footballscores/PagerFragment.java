package barqsoft.footballscores;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by yehya khaled on 2/27/2015.
 */
public class PagerFragment extends Fragment {

    // We display 2 days behind + 2 days ahead + the current day
    public static final int NUM_PAGES = 5;

    public ViewPager mPagerHandler;
    private ScoresPageAdapter mPagerAdapter;

    // Reference to the different fragment views
    private MainScreenFragment[] mViewFragments = new MainScreenFragment[5];

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.pager_fragment, container, false);

        Toolbar toolbar = (Toolbar)rootView.findViewById(R.id.toolbar);

        // Sets the toolbar to the activity
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        PagerTabStrip pagerTabStrip = (PagerTabStrip) rootView.findViewById(R.id.pager_header);
        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTextColor(getResources().getColor(R.color.fb_yellow_1));
        pagerTabStrip.setTabIndicatorColor(getResources().getColor(R.color.fb_yellow_1));

        mPagerHandler = (ViewPager) rootView.findViewById(R.id.pager);
        mPagerAdapter = new ScoresPageAdapter(getChildFragmentManager());

        // Adds the fragments for each date
        for (int i = 0; i < NUM_PAGES; i++) {

            Date date = new Date(System.currentTimeMillis() + ((i - 2) * 86400000));
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            mViewFragments[i] = new MainScreenFragment();
            mViewFragments[i].setmFragmentDate(dateFormat.format(date));

        }

        mPagerHandler.setAdapter(mPagerAdapter);
        mPagerHandler.setCurrentItem(MainActivity.mCurrentFragment);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            Intent aboutActivity = new Intent(getActivity(), AboutActivity.class);
            startActivity(aboutActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Custom Pager Adapter
     */
    private class ScoresPageAdapter extends FragmentStatePagerAdapter {

        public ScoresPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return mViewFragments[i];
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return getDayName(getActivity(), System.currentTimeMillis() + ((position - 2) * 86400000));
        }

        public String getDayName(Context context, long dateInMillis) {

            // If the date is today, return the localized version of "Today" instead of the actual
            // day name.

            Time t = new Time();
            t.setToNow();

            int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
            int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);

            if (julianDay == currentJulianDay) {

                return context.getString(R.string.today).toUpperCase(Locale.US);

            } else if (julianDay == currentJulianDay + 1) {

                return context.getString(R.string.tomorrow).toUpperCase(Locale.US);

            } else if (julianDay == currentJulianDay - 1) {

                return context.getString(R.string.yesterday).toUpperCase(Locale.US);

            } else {

                Time time = new Time();
                time.setToNow();
                // Otherwise, the format is just the day of the week (e.g "Wednesday".
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
                return dayFormat.format(dateInMillis).toUpperCase(Locale.US);

            }
        }
    }

    /**
     * Notifies the Fragments about the error
     */
    public void onError() {
        for (int i = 0; i < NUM_PAGES; i++) {
            if (mViewFragments[i] != null) {
                mViewFragments[i].onError();
            }
        }
    }
}
