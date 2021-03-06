package it.jaschke.alexandria;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import it.jaschke.alexandria.api.Callback;

public class MainActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, Callback {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment navigationDrawerFragment;

    private Toolbar mToolbar;

    /**
     * Used to store the last screen mTitle. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    /**
     * Tracks if the current device is a tablet
     */
    public static boolean IS_TABLET = false;

    /**
     * Keeps track of the current selected position
     */
    private int mCurrentPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IS_TABLET = isTablet();

        if (IS_TABLET) {
            setContentView(R.layout.activity_main_tablet);
        } else {
            setContentView(R.layout.activity_main);
        }

        mToolbar = (Toolbar) findViewById(R.id.lolipop_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle("");
            setSupportActionBar(mToolbar);
        }

        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mTitle = getTitle();

        // Set up the drawer.
        navigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);

        mCurrentPosition = -1;

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        if (position != mCurrentPosition) {

            mCurrentPosition = position;
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment nextFragment;

            switch (position) {
                default:
                case 0:
                    nextFragment = new ListOfBooks();
                    break;
                case 1:
                    nextFragment = new AddBook();
                    break;
                case 2:
                    nextFragment = new About();
                    break;

            }

            fragmentManager.beginTransaction()
                    .replace(R.id.container, nextFragment)
                    .commit();

        }
    }

    public void setTitle(int titleId) {
        mTitle = getString(titleId);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (!navigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_horizontal, R.anim.slide_out_left);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    /**
     * A Book was selected from the list of books
     */
    public void onItemSelected(String ean) {

        Bundle args = new Bundle();
        args.putString(BookDetail.EAN_KEY, ean);

        if (findViewById(R.id.right_container) != null) {

            args.putBoolean(BookDetail.TABLET, true);

            BookDetail fragment = new BookDetail();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.right_container, fragment)
                    .addToBackStack("BookDetail")
                    .commit();

        } else {

            Intent i = new Intent(this, BookDetailActivity.class);
            i.putExtras(args);

            startActivity(i);
            overridePendingTransition(R.anim.slide_in_horizontal, R.anim.slide_out_left);

        }

    }

    /**
     * Checks if the device is a Tablet
     * @return
     */
    private boolean isTablet() {
        return (getApplicationContext().getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

}