package barqsoft.footballscores;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

/**
 *
 * Login Activity
 *
 */
public class IntroActivity extends AppCompatActivity {

    private static final String FRAGMENT_TAG = "intro_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_container);

        Fragment introFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);

        if (introFragment == null) {
            introFragment = IntroFragment.newInstance();
        }

        // Updates the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, introFragment, FRAGMENT_TAG).commit();

    }

}
