package it.jaschke.alexandria;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import it.jaschke.alexandria.views.PageDotsIndicator;


/**
 * Created by gabrielmarcos on 7/7/15.
 *
 * Sign Up Fragment - Used for displaying the Sign Up screen
 *
 */
public class IntroFragment extends Fragment implements ViewPager.OnPageChangeListener{

    private final int NUM_PAGES = 2;

    private int mCurrentIndex = 0;

    private ViewPager mViewPager;
    private Button mStartButton;
    private PageDotsIndicator mDotsIndicator;

    private PagerAdapter mAdapter;


    /**
     * Returns a new instance of the Fragment
     * @return fragment instance
     */
    public static IntroFragment newInstance() {
        return new IntroFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_intro, container, false);

        mAdapter = new PagerAdapter(getChildFragmentManager());

        mViewPager = (ViewPager)rootView.findViewById(R.id.viewPager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(this);

        mDotsIndicator = (PageDotsIndicator)rootView.findViewById(R.id.page_indicator);
        mDotsIndicator.setmNumberOfDots(2);
        mDotsIndicator.setActiveDot(0);

        mStartButton = (Button)rootView.findViewById(R.id.button_start);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences.Editor editor = getActivity().getSharedPreferences("data", Context.MODE_PRIVATE).edit();
                editor.putBoolean("tipsLearned",true);
                editor.apply();

                Intent i = new Intent(getActivity(),MainActivity.class);
                startActivity(i);
                getActivity().overridePendingTransition(R.anim.slide_in_horizontal, R.anim.slide_out_left);
                getActivity().finish();
            }
        });

        mStartButton.setVisibility(View.GONE);

        return rootView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    /**
     * PagerAdapter class for the ViewPager
     */
    public class PagerAdapter extends FragmentPagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {

                case 0:

                    IntroStepOne stepOne = IntroStepOne.newInstance();
                    return stepOne;

                case 1:
                    IntroStepTwo stepTwo = IntroStepTwo.newInstance();
                    return stepTwo;

            }

            return null;

        }

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mDotsIndicator.setActiveDot(position);

        if (position == 1) {
            showStart();
        }else {
            hideStart();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public void showStart() {

        if (mStartButton.getVisibility() == View.VISIBLE) return;

        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.show_from_right);
        mStartButton.setVisibility(View.VISIBLE);
        mStartButton.startAnimation(animation);

        Animation hideAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.hide_to_left);
        mDotsIndicator.startAnimation(hideAnim);
        hideAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mDotsIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    public void hideStart() {

        if (mStartButton.getVisibility() == View.GONE) return;

        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.show_from_left);
        mDotsIndicator.setVisibility(View.VISIBLE);
        mDotsIndicator.startAnimation(animation);

        Animation hideAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.hide_to_right);
        mStartButton.startAnimation(hideAnim);
        hideAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mStartButton.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

}
