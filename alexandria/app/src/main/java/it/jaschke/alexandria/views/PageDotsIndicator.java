package it.jaschke.alexandria.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

import it.jaschke.alexandria.R;

/**
 * View Dots indicator
 *
 * This Custom view is useful for displaying
 * a set of dots indicating the current active
 * page
 *
 */
public class PageDotsIndicator extends LinearLayout {

	/**
	 * Array of dots reference
	 */
	private ArrayList<ImageView> mDots;

	private int mNumberOfDots = 0;
	private int mColor;

	/**
	 *
	 * Default view constructor
	 *
	 * @param context
	 */
	public PageDotsIndicator(Context context) {
		super(context);

		inflateLayout(context, null);

	}

	/**
	 *
	 * View constructor with attributes
	 *
	 * @param context
	 * @param attrs
	 */
	public PageDotsIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);

		inflateLayout(context,attrs);

	}

	/**
	 *
	 * Inflates the view layout
	 *
	 * @param context
	 */
	private void inflateLayout(Context context, AttributeSet attrs){

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.view_dots_indicator, this, true);

		mDots = new ArrayList<ImageView>();

		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.ViewDotsIndicator, 0, 0);

		mColor = a.getResourceId(R.styleable.ViewDotsIndicator_paintColor, R.color.peter_river);

		updateNumberOfDots();

		setActiveDot(0);

	}

	public void setmNumberOfDots(int mNumberOfDots) {
		this.mNumberOfDots = mNumberOfDots;

        updateNumberOfDots();
	}

	private void updateNumberOfDots() {

		mDots.clear();

		removeAllViews();

		for(int i=0; i < mNumberOfDots; i++){

			// Creates a new image view for each dot
			ImageView image = new ImageView(getContext());
			LayoutParams params = new LayoutParams((int)getResources().getDimension(R.dimen.dp_5),(int)getResources().getDimension(R.dimen.dp_5));
			params.setMargins((int)getResources().getDimension(R.dimen.dp_2),0,(int)getResources().getDimension(R.dimen.dp_2),0);
			image.setLayoutParams(params);
			image.setColorFilter(getResources().getColor(mColor));
			image.setMaxHeight((int)getResources().getDimension(R.dimen.dp_5));
			image.setMaxWidth((int)getResources().getDimension(R.dimen.dp_5));
			image.setImageResource(R.drawable.pagination_node);
			addView(image);

			// Adds the reference
			mDots.add(image);
		}

		setActiveDot(0);
	}

	/**
	 *
	 * Sets the active dot by index
	 *
	 * @param activeIndex
	 */
	public void setActiveDot(int activeIndex){

		for(int i = 0; i < mDots.size(); i++){
			if( i!= activeIndex){

				deactivateDot(mDots.get(i));

			}else{

				activateDot(mDots.get(i));
			}
		}
	}

	/**
	 *
	 * Activates the dot
	 *
	 * @param dot
	 */
	private void activateDot(ImageView dot){
		dot.setImageResource(R.drawable.pagination_filled_node);
	}

	/**
	 *
	 * Deactivates the dot
	 *
	 * @param dot
	 */
	private void deactivateDot(ImageView dot){
		dot.setImageResource(R.drawable.pagination_node);
	}

}
