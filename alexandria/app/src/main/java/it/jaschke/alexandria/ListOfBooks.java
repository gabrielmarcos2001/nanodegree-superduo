package it.jaschke.alexandria;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.jaschke.alexandria.api.BookListAdapter;
import it.jaschke.alexandria.api.Callback;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.data.Book;
import it.jaschke.alexandria.views.SearchableEditText;


public class ListOfBooks extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, AlexandriaFragment {

    class DataProviderObserver extends ContentObserver {

        DataProviderObserver(Handler h) {
            super(h);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            if (isAdded()) {
                restartLoader();
            }
        }
    }

    private BookListAdapter mBookListAdapter;

    private int mPosition = ListView.INVALID_POSITION;

    @Bind(R.id.listOfBooks)
    ListView mBookList;

    @Bind(R.id.searchText)
    EditText mSearchText;

    private static DataProviderObserver sDataObserver;

    private final int LOADER_ID = 10;

    public ListOfBooks() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Cursor cursor = getActivity().getContentResolver().query(
                AlexandriaContract.BookEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );


        mBookListAdapter = new BookListAdapter(getActivity(), cursor, 0);

        View rootView = inflater.inflate(R.layout.fragment_list_of_books, container, false);

        ButterKnife.bind(this, rootView);

        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    ListOfBooks.this.restartLoader();
                }
            }
        });

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    ListOfBooks.this.restartLoader();
                    return true;
                }
                return false;
            }
        });

        mBookList = (ListView) rootView.findViewById(R.id.listOfBooks);
        mBookList.setAdapter(mBookListAdapter);

        mBookList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                sDataObserver = new DataProviderObserver(new Handler());

                Cursor cursor = mBookListAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {

                    // We want to know if a book has been deleted
                    Uri uri = AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry._ID)))).buildUpon().appendPath("/delete").build();

                    getActivity().getContentResolver().registerContentObserver(uri, true, sDataObserver);

                    ((Callback) getActivity())
                            .onItemSelected(cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry._ID)));
                }
            }
        });

        return rootView;
    }

    @OnClick(R.id.searchButton)
    void onSearchClicked() {
        ListOfBooks.this.restartLoader();
    }

    private void restartLoader() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        final String selection = AlexandriaContract.BookEntry.TITLE + " LIKE ? OR " + AlexandriaContract.BookEntry.SUBTITLE + " LIKE ? ";
        String searchString = mSearchText.getText().toString();

        if (searchString.length() > 0) {
            searchString = "%" + searchString + "%";
            return new CursorLoader(
                    getActivity(),
                    AlexandriaContract.BookEntry.CONTENT_URI,
                    null,
                    selection,
                    new String[]{searchString, searchString},
                    null
            );
        }

        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        mBookListAdapter.swapCursor(data);

        if (mPosition != ListView.INVALID_POSITION) {
            mBookList.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mBookListAdapter.swapCursor(null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.books);
    }

    @Override
    public void showMessage(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void newBookDataFetched(Book book) {

    }
}
