package it.jaschke.alexandria.views;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.rengwuxian.materialedittext.MaterialEditText;

/**
 * Created by gabrielmarcos on 8/13/15.
 */
public class SearchableEditText extends MaterialEditText implements TextWatcher {

    public interface SearchableEditTextActions {
        void valueChanged(String sku);
    }

    private SearchableEditTextActions mInterface;

    /**
     * A handler is used to wait some time before sending the
     * value on text changed
     */
    private Handler mHandler = new Handler();

    Runnable mFilterTask = new Runnable() {

        @Override
        public void run() {

            if (mInterface != null) {
                mInterface.valueChanged(getText().toString());
            }
        }
    };

    public SearchableEditText(Context context) {
        super(context);

        initViews();
    }

    public SearchableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        initViews();
    }

    public SearchableEditText(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);

        initViews();
    }

    public void setmInterface(SearchableEditTextActions mInterface) {
        this.mInterface = mInterface;
    }

    private void initViews() {
        addTextChangedListener(this);
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void afterTextChanged(Editable s) {

        String value = s.toString();

        if (value.length() == 10 && !value.startsWith("978")) {
            value = "978" + value;
        }

        if (value.length() < 13) {
            return;
        }

        mHandler.removeCallbacks(mFilterTask);
        mHandler.postDelayed(mFilterTask, 1000);
    }
}
