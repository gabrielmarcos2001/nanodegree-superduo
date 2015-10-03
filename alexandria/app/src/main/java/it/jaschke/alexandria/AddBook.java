package it.jaschke.alexandria;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;


import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.data.Book;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.views.BookDataPanel;
import it.jaschke.alexandria.views.SearchableEditText;

public class AddBook extends SearchableFragment implements LoaderManager.LoaderCallbacks<Cursor>, SearchableEditText.SearchableEditTextActions, AlexandriaFragment, ScannerFragment.ScannerFragmentInterface {

    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";

    private final int LOADER_ID = 1;
    private View rootView;


    private final String EAN_CONTENT="eanContent";

    private String eanValue = "";

    @Bind(R.id.progress_wheel)
    View mProgressWheel;

    @Bind(R.id.ean)
    SearchableEditText mEanTextField;

    @Bind(R.id.book_data_panel)
    BookDataPanel mBookDataPanel;

    private ScannerFragment mScannerFragment;

    /**
     * Fragment Constructor
     */
    public AddBook(){

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        // Saves the ean value
        if(mEanTextField!=null) {
            outState.putString(EAN_CONTENT, mEanTextField.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);

        ButterKnife.bind(this,rootView);

        hideLoader();

        mEanTextField.setmInterface(this);

        mBookDataPanel.setmInterface(new BookDataPanel.BookPanelActions() {

            @Override
            public void onCancelClicked() {

                // Just closes the panel
                if (mScannerFragment != null) {
                    mScannerFragment.startScanning();
                }

                mEanTextField.setEnabled(true);
                mEanTextField.setText("");
                mBookDataPanel.hidePanel();
            }

            @Override
            public void onSaveClicked(Book book) {

                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, eanValue);

                Bundle bundle = new Bundle();
                bundle.putParcelable(BookService.BOOK, book);

                bookIntent.putExtras(bundle);
                bookIntent.setAction(BookService.ADD_BOOK);
                getActivity().startService(bookIntent);

                if (mScannerFragment != null) {
                    mScannerFragment.startScanning();
                }

                mEanTextField.setEnabled(true);
                mEanTextField.setText("");
                mBookDataPanel.hidePanel();

            }

            @Override
            public void onDeleteClicked(Book book) {

                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, eanValue);
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);

                if (mScannerFragment != null) {
                    mScannerFragment.startScanning();
                }

                mEanTextField.setEnabled(true);
                mEanTextField.setText("");
                mBookDataPanel.hidePanel();
            }
        });

        if(savedInstanceState!=null){

            mEanTextField.setText(savedInstanceState.getString(EAN_CONTENT));
            mEanTextField.setHint("");

            mScannerFragment = (ScannerFragment)getChildFragmentManager().findFragmentByTag("scanner");

            if (mScannerFragment != null) {
                mScannerFragment.setmInterface(this);
            }
        }

        return rootView;
    }

    @OnClick(R.id.scan_button)
    void onScanClicked() {

        mScannerFragment = (ScannerFragment)getChildFragmentManager().findFragmentByTag("scanner");

        if (mScannerFragment == null) {
            mScannerFragment = ScannerFragment.newInstance();
        }

        mScannerFragment.setmInterface(this);

        // Updates the main content by replacing fragments
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.push_up_anim, R.anim.push_down_anim, R.anim.push_up_anim, R.anim.push_down_anim)
                .addToBackStack(null)
                .replace(R.id.scanner_fragment_container, mScannerFragment, "scanner").commit();

    }

    @Override
    public void onCloseClicked() {
        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.popBackStack();

        mScannerFragment = null;
    }

    @Override
    public void onBarCodeFound(String number) {
        valueChanged(number);
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if(eanValue.length()==0){
            return null;
        }

        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanValue)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {

        // This Method will be called only when the loader gets a book from the database
        // not when a new book is fetched from the API

        if (!data.moveToFirst()) {
            return;
        }

        // Hides the loader
        hideLoader();

        // Gets the Book data from the cursor
        Book bookData = new Book();

        bookData.ean = mEanTextField.getText().toString();

        String authors[] = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR)).split(",");
        String categories[] = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY)).split(",");

        bookData.title = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        bookData.subTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        bookData.imageUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));

        for (int i=0; i < authors.length;i++) {
            bookData.authors.add(authors[i]);
        }

        for (int i=0; i < categories.length;i++) {
            bookData.categories.add(categories[i]);
        }

        // Sets the book data to the panel
        mEanTextField.setEnabled(false);
        mBookDataPanel.setBookData(bookData);
        mBookDataPanel.setMode(BookDataPanel.MODE_SAVED_BOOK);
        mBookDataPanel.showPanel();

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void showLoader() {
        mProgressWheel.setVisibility(View.VISIBLE);
    }

    private void hideLoader() {
        mProgressWheel.setVisibility(View.GONE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    @Override
    public void valueChanged(String ean) {

        if (!isAdded()) return;

        //catch isbn10 numbers
        if(ean.length()==10 && !ean.startsWith("978")){
            ean="978"+ean;
        }

        if(ean.length()<13){
            return;
        }

        eanValue = ean;

        // Hides the keyboard if open
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(mEanTextField.getWindowToken(), 0);
        }

        // Shows the loader
        showLoader();

        // Once we have an ISBN, start a book intent for fetching the book
        // information
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, ean);
        bookIntent.setAction(BookService.FETCH_BOOK);
        getActivity().startService(bookIntent);

        AddBook.this.restartLoader();

    }

    @Override
    public void showMessage(String message) {

        // Hides the loader
        hideLoader();

        // Displays the message with a Snackbar
        Snackbar.make(rootView,message,Snackbar.LENGTH_LONG).show();

        if (mScannerFragment != null) {
            mScannerFragment.startScanning();
        }
    }

    @Override
    public void newBookDataFetched(Book book) {

        // Hides the loader
        hideLoader();

        // Disables the Ean Text Field
        mEanTextField.setEnabled(false);

        // Sets the book data to the panel
        mBookDataPanel.setBookData(book);

        // Opens the panel in the New Book Mode
        mBookDataPanel.setMode(BookDataPanel.MODE_NEW_BOOK);

        // Shows the panel
        mBookDataPanel.showPanel();
    }
}
