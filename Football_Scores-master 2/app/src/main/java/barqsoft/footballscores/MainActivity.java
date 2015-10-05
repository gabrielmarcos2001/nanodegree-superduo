package barqsoft.footballscores;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.lang.ref.WeakReference;

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

    /**
     * Handler for getting messages from the update service
     */
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            if (msg.arg1 == RESULT_CANCELED ) {
                // Show Error
                String error = msg.getData().getString("errorMessage",null);
                if (error != null) {
                    Snackbar.make(findViewById(android.R.id.content), error, Snackbar.LENGTH_LONG).show();
                }
            }
            return true;
        }
    });

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
        Messenger messenger = new Messenger(mHandler);
        scoresService.putExtra("messenger", messenger);

        startService(scoresService);
    }


}
