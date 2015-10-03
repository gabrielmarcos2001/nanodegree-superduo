package barqsoft.footballscores;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Main Activity
 * <p/>
 * Used to host the Pager Fragment for
 * displaying the scores
 */
public class MainActivity extends AppCompatActivity {

    public static int selectedMatchId;
    public static int currentFragment = 2;
    private static final String FRAGMENT_TAG = "pager_fragment";

    private PagerFragment pagerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {

            pagerFragment = new PagerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, pagerFragment, FRAGMENT_TAG)
                    .commit();
        } else {

            //pagerFragment = (PagerFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);

        }
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
        outState.putInt("currentPage", pagerFragment.mPagerHandler.getCurrentItem());
        outState.putInt("selectedMatch", selectedMatchId);

        getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG, pagerFragment);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        currentFragment = savedInstanceState.getInt("currentPage");
        selectedMatchId = savedInstanceState.getInt("selectedMatch");
        pagerFragment = (PagerFragment) getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_TAG);

        super.onRestoreInstanceState(savedInstanceState);
    }
}
