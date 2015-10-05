package it.jaschke.alexandria;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.jaschke.alexandria.api.Callback;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;

/**
 * Book Detail Fragment
 */
public class BookDetail extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * We want to observe for content updates in case the book gets deleted
     */
    class DataProviderObserver extends ContentObserver {

        DataProviderObserver(Handler h) {
            super(h);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            if (isAdded()) {
                if (MainActivity.IS_TABLET && rootView.findViewById(R.id.right_container) == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                } else {
                    getActivity().onBackPressed();
                }
            }

        }
    }

    public static final String EAN_KEY = "EAN";
    public static final String TABLET = "TABLET";

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.categories)
    TextView mCategories;

    @Bind(R.id.authors)
    TextView mAuthors;

    @Bind(R.id.fullBookTitle)
    TextView mFullBookTitle;

    @Bind(R.id.fullBookDesc)
    TextView mFullBookDesc;

    @Bind(R.id.fullBookSubTitle)
    TextView mFullBookSubTitle;

    @Bind(R.id.fullBookCover)
    ImageView mFullBookCover;

    @Bind(R.id.confirm_area)
    View mDeleteConfirm;

    @Bind(R.id.delete_button)
    View mDeleteButton;

    private final int LOADER_ID = 10;
    private View rootView;

    private String ean;
    private String bookTitle;
    private static DataProviderObserver sDataObserver;

    private ShareActionProvider shareActionProvider;

    /**
     * Empty Fragment Constructor
     */
    public BookDetail() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle arguments = getArguments();

        rootView = inflater.inflate(R.layout.fragment_full_book, container, false);

        ButterKnife.bind(this, rootView);

        if (arguments != null) {
            ean = arguments.getString(BookDetail.EAN_KEY);
            getLoaderManager().restartLoader(LOADER_ID, null, this);

            if (arguments.getBoolean(TABLET)) {
                mToolbar.setVisibility(View.INVISIBLE);
            } else {
                ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.title_book_detail));

            }
        }

        mDeleteConfirm.setVisibility(View.GONE);
        sDataObserver = new DataProviderObserver(new Handler());

        // We want to know if a book has been deleted
        Uri uri = AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)).buildUpon().appendPath("/delete").build();
        getActivity().getContentResolver().registerContentObserver(uri, true, sDataObserver);

        setHasOptionsMenu(true);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (sDataObserver != null) {
            getActivity().getContentResolver().unregisterContentObserver(sDataObserver);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_detail, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        updateShareIntent();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                if (MainActivity.IS_TABLET && rootView.findViewById(R.id.right_container) == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                } else {
                    getActivity().onBackPressed();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(ean)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {

        if (!data.moveToFirst()) {
            return;
        }

        bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        mFullBookTitle.setText(bookTitle);

        // Updates the Share intent with the book title
        updateShareIntent();

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        mFullBookSubTitle.setText(bookSubTitle);

        String desc = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.DESC));
        mFullBookDesc.setText(desc);

        if (bookSubTitle.isEmpty() && desc.isEmpty()) {
            mFullBookSubTitle.setText(getString(R.string.not_available));
        }

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));

        if (authors == null) {
            mAuthors.setText(getString(R.string.not_available));
        } else {
            String[] authorsArr = authors.split(",");

            if (authorsArr.length == 0) {
                mAuthors.setText(getString(R.string.not_available));
            } else {
                mAuthors.setLines(authorsArr.length);
                mAuthors.setText(authors.replace(",", "\n"));
            }
        }

        // Loads the Book Image URL
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));

        if (Patterns.WEB_URL.matcher(imgUrl).matches()) {

            // Loads the product image
            Picasso.with(getActivity())
                    .load(imgUrl)
                    .into(mFullBookCover);

        } else {

            // Show default image
            mFullBookCover.setImageResource(R.drawable.book_default);

        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));

        if (categories == null) {
            mCategories.setText(getString(R.string.not_available));
        } else {
            if (categories.isEmpty()) {
                mCategories.setText(getString(R.string.not_available));
            } else {
                mCategories.setText(categories);
            }
        }

    }

    /**
     * Updates the share intent with the book data
     */
    private void updateShareIntent() {

        if (shareActionProvider != null && bookTitle != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + bookTitle);
            shareActionProvider.setShareIntent(shareIntent);
        }
    }

    @OnClick(R.id.delete_button)
    void onDeleteBookClicked() {

        // Show a confirmation modal first
        showConfirmArea();
    }

    @OnClick(R.id.cancel)
    void onDeleteCancelClicked() {
        hideConfirmArea();
    }

    @OnClick(R.id.yes)
    void onConfirmDeleteClicked() {
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, ean);
        bookIntent.setAction(BookService.DELETE_BOOK);
        getActivity().startService(bookIntent);

        if (MainActivity.IS_TABLET && rootView.findViewById(R.id.right_container) == null) {
            getActivity().getSupportFragmentManager().popBackStack();
        } else {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    @Override
    public void onPause() {
        super.onDestroyView();
        if (MainActivity.IS_TABLET && rootView.findViewById(R.id.right_container) == null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    /**
     * Shows the Delete Confirm Area
     */
    public void showConfirmArea() {

        if (mDeleteConfirm.getVisibility() == View.VISIBLE) return;

        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.show_from_right);
        mDeleteConfirm.setVisibility(View.VISIBLE);
        mDeleteConfirm.startAnimation(animation);

        Animation hideAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.hide_to_left);
        mDeleteButton.startAnimation(hideAnim);
        hideAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mDeleteButton.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    /**
     * Hides the Delete Confirm Area
     */
    public void hideConfirmArea() {

        if (mDeleteConfirm.getVisibility() == View.GONE) return;

        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.show_from_left);
        mDeleteButton.setVisibility(View.VISIBLE);
        mDeleteButton.startAnimation(animation);

        Animation hideAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.hide_to_right);
        mDeleteConfirm.startAnimation(hideAnim);
        hideAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mDeleteConfirm.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}