package it.jaschke.alexandria;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * Created by gabrielmarcos on 7/7/15.
 */
public class IntroStepOne extends Fragment {

    /**
     * Returns a new instance of the Fragment
     * @return fragment instance
     */
    public static IntroStepOne newInstance() {
        return new IntroStepOne();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_intro_step_one, container, false);
        return rootView;
    }

}
