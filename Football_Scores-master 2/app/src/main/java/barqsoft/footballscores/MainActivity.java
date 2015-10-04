package barqsoft.footballscores;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import barqsoft.footballscores.service.DataFetchService;

/**
 * Main Activity
 * <p/>
 * Used to host the Pager Fragment for
 * displaying the scores
 */
public class MainActivity extends AppCompatActivity {

    private static final String FRAGMENT_TAG = "pager_fragment";

    public static int mSelectedMatchId;
    public static int mCurrentFragment = 2;

    private PagerFragment mPagerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Used for testing layout mirroring
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            //getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {

            mPagerFragment = new PagerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mPagerFragment, FRAGMENT_TAG)
                    .commit();
        }

        // Updates the scores when the activity is created
        updateScores();
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

        if (id == R.id.action_about) {
            Intent aboutActivity = new Intent(this, AboutActivity.class);
            startActivity(aboutActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt("currentPage", mPagerFragment.mPagerHandler.getCurrentItem());
        outState.putInt("selectedMatch", mSelectedMatchId);

        getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG, mPagerFragment);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        mCurrentFragment = savedInstanceState.getInt("currentPage");
        mSelectedMatchId = savedInstanceState.getInt("selectedMatch");
        mPagerFragment = (PagerFragment) getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_TAG);

        super.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * Starts the service for updating scores
     */
    public void updateScores() {
        Intent scoresService = new Intent(this, DataFetchService.class);
        startService(scoresService);
    }
}
