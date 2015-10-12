package it.jaschke.alexandria;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
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
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;

/**
 * Book Detail Fragment
 */
public class BookDetail extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

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
    private View mRootView;

    private String mEan;
    private String mBookTitle;

    private ShareActionProvider mShareActionProvider;

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

        mRootView = inflater.inflate(R.layout.fragment_full_book, container, false);

        ButterKnife.bind(this, mRootView);

        if (arguments != null) {
            mEan = arguments.getString(BookDetail.EAN_KEY);
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
        setHasOptionsMenu(true);

        return mRootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_detail, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        updateShareIntent();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(mEan)),
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

        mBookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        mFullBookTitle.setText(mBookTitle);

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

        if (mShareActionProvider != null && mBookTitle != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + mBookTitle);
            mShareActionProvider.setShareIntent(shareIntent);
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
        bookIntent.putExtra(BookService.EAN, mEan);
        bookIntent.setAction(BookService.DELETE_BOOK);
        getActivity().startService(bookIntent);

        if (MainActivity.IS_TABLET && mRootView.findViewById(R.id.right_container) == null) {

            // Removes the Fragment
            getActivity().getSupportFragmentManager().popBackStack();

        } else {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

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