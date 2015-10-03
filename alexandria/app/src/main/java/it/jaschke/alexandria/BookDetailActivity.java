package it.jaschke.alexandria;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by gabrielmarcos on 7/7/15.
 */
public class BookDetailActivity extends AppCompatActivity {

    private static final String FRAGMENT_TAG = "book_detail_fragment";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_single_container);

        Fragment bookDetailFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);

        if (bookDetailFragment == null) {
            bookDetailFragment = new BookDetail();
        }

        if (savedInstanceState == null) {
            bookDetailFragment.setArguments(getIntent().getExtras());
        }

        // Updates the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, bookDetailFragment, FRAGMENT_TAG).commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_horizontal);
    }
}
