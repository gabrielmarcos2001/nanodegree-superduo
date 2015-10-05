package it.jaschke.alexandria;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;

import it.jaschke.alexandria.data.Book;

/**
 * Created by gabrielmarcos on 9/3/15.
 */
public abstract class SearchableFragment extends Fragment {

    public static final String MESSAGE_EVENT = "MESSAGE_EVENT";
    public static final String MESSAGE_KEY = "MESSAGE_EXTRA";
    public static final String BOOK_KEY = "BOOK_EXTRA";

    private BroadcastReceiver mMessageReceiver;

    public abstract void showMessage(String message);

    public abstract void newBookDataFetched(Book book);

    private class MessageReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getStringExtra(MESSAGE_KEY) != null) {
                showMessage(intent.getStringExtra(MESSAGE_KEY));
            } else if (intent.getExtras().getParcelable(BOOK_KEY) != null) {
                newBookDataFetched((Book) intent.getExtras().getParcelable(BOOK_KEY));
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMessageReceiver = new MessageReciever();
        IntentFilter filter = new IntentFilter(MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filter);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }
}
