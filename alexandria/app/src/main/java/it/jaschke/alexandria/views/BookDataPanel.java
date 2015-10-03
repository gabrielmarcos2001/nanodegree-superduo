package it.jaschke.alexandria.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.Book;

/**
 * Created by gabrielmarcos on 8/31/15.
 */
public class BookDataPanel extends RelativeLayout {

    public static final int MODE_NEW_BOOK = 1;
    public static final int MODE_SAVED_BOOK = 2;

    private Book mBook;

    public interface BookPanelActions {
        void onCancelClicked();
        void onSaveClicked(Book book);
        void onDeleteClicked(Book book);
    }

    @Bind(R.id.added_indicator)
    View mAddedIndicator;

    @Bind(R.id.save_button)
    Button mSaveButton;

    @Bind(R.id.background)
    View mBackground;

    @Bind(R.id.data_container)
    View mPanel;

    @Bind(R.id.actions_area)
    View mActionsPanel;

    @Bind(R.id.categories)
    TextView mCategories;

    @Bind(R.id.authors)
    TextView mAuthors;

    @Bind(R.id.fullBookTitle)
    TextView mFullBookTitle;

    @Bind(R.id.fullBookSubTitle)
    TextView mFullBookSubTitle;

    @Bind(R.id.fullBookCover)
    ImageView mFullBookCover;

    private int mode = MODE_NEW_BOOK;

    private BookPanelActions mInterface;

    public BookDataPanel(Context context) {
        super(context);

        inflateViews(context);
    }

    public BookDataPanel(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflateViews(context);
    }

    private void inflateViews(Context context) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.book_data_panel, this, true);

        ButterKnife.bind(this, this);

        mBackground.setVisibility(View.INVISIBLE);
        mPanel.setVisibility(View.GONE);
        mActionsPanel.setVisibility(View.GONE);
    }

    @OnClick(R.id.save_button)
    void onSaveClicked() {
        if (mInterface != null) {

            if (mode == MODE_NEW_BOOK)
                mInterface.onSaveClicked(mBook);
            else
                mInterface.onDeleteClicked(mBook);
        }
    }

    @OnClick(R.id.delete_button)
    void onDeleteClicked() {
        if (mInterface != null) {
            mInterface.onCancelClicked();
        }
    }

    /**
     * Shows the panel data with awesome animations
     */
    public void showPanel() {

        final Animation showPanelAnim = AnimationUtils.loadAnimation(getContext(),
                R.anim.push_up_anim);

        Animation fadeInAnim = AnimationUtils.loadAnimation(getContext(),
                R.anim.fade_in_anim);

        mPanel.setVisibility(View.VISIBLE);
        mActionsPanel.setVisibility(View.VISIBLE);
        mPanel.startAnimation(showPanelAnim);
        mActionsPanel.startAnimation(showPanelAnim);

        mBackground.setVisibility(View.VISIBLE);
        mBackground.startAnimation(fadeInAnim);

    }

    /**
     * Hides the panel data with super cool animations
     */
    public void hidePanel() {

        final Animation hidePanelAnim = AnimationUtils.loadAnimation(getContext(),
                R.anim.push_down_anim);

        Animation fadeOutAnim = AnimationUtils.loadAnimation(getContext(),
                R.anim.fade_out_anim);

        mBackground.startAnimation(fadeOutAnim);
        fadeOutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mBackground.setVisibility(View.INVISIBLE);
                mPanel.startAnimation(hidePanelAnim);
                mActionsPanel.startAnimation(hidePanelAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        hidePanelAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                mPanel.setVisibility(View.GONE);
                mActionsPanel.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    /**
     * Sets the book data to the view
     * @param data
     */
    public void setBookData(Book data) {

        this.mBook = data;

        mFullBookTitle.setText(data.title);

        if (data.subTitle.isEmpty()) {
            mFullBookSubTitle.setText(getContext().getString(R.string.not_available));
        }else {
            mFullBookSubTitle.setText(data.subTitle);
        }

        StringBuilder authorsBuilder = new StringBuilder();
        for (String author : data.authors) {
            authorsBuilder.append(author);
            authorsBuilder.append("\n");
        }

        if (authorsBuilder.toString().isEmpty()) {
            mAuthors.setText(getContext().getString(R.string.not_available));
        }else {
            mAuthors.setText(authorsBuilder.toString());
        }

        if(Patterns.WEB_URL.matcher(data.imageUrl).matches()){

            // Loads the product image
            Picasso.with(getContext())
                    .load(data.imageUrl)
                    .into(mFullBookCover);

        }else {

            // Show default image
            mFullBookCover.setImageResource(R.drawable.book_default);

        }

        StringBuilder categoriesBuilder = new StringBuilder();
        for (String category : data.categories) {
            authorsBuilder.append(category);
            authorsBuilder.append("\n");
        }

        if (categoriesBuilder.toString().isEmpty()) {
            mCategories.setText(getContext().getString(R.string.not_available));
        }else {
            mCategories.setText(categoriesBuilder.toString());
        }

    }

    public void setmInterface(BookPanelActions mInterface) {
        this.mInterface = mInterface;
    }

    public void setMode(int mode) {
        this.mode = mode;

        if (mode == MODE_NEW_BOOK) {
            mSaveButton.setText(getContext().getString(R.string.add_to_list_uppercase));
            mAddedIndicator.setVisibility(View.GONE);
        }else {
            mSaveButton.setText(getContext().getString(R.string.delete_book_uppercase));
            mAddedIndicator.setVisibility(View.VISIBLE);
        }
    }
}
