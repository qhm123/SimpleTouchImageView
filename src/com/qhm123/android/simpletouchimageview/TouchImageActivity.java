package com.qhm123.android.simpletouchimageview;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;
import android.widget.ZoomButtonsController;

public class TouchImageActivity extends Activity {

	public static final String TAG = TouchImageActivity.class.getSimpleName();
	private static final int PAGER_MARGIN_DP = 40;

	private RelativeLayout mRootLayout;
	private ViewPager mViewPager;

	private ImagePagerAdapter mPagerAdapter;

	private ZoomButtonsController mZoomButtonsController;

	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;

	private List<String> mImageList;
	private boolean mPaused;
	private boolean mOnScale = false;
	private boolean mOnPagerScoll = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewpager);

		mRootLayout = (RelativeLayout) findViewById(R.id.rootLayout);
		mViewPager = (ViewPager) findViewById(R.id.viewPager);

		final float scale = getResources().getDisplayMetrics().density;
		int pagerMarginPixels = (int) (PAGER_MARGIN_DP * scale + 0.5f);
		mViewPager.setPageMargin(pagerMarginPixels);

		mPagerAdapter = new ImagePagerAdapter();
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOnPageChangeListener(mPageChangeListener);

		mImageList = new ArrayList<String>();
		for (File file : new File("/mnt/sdcard/MIUI/photo/cars").listFiles()) {
			mImageList.add(file.getPath());
		}

		setupZoomButtonController(mRootLayout);
		setupOnTouchListeners(mViewPager);
	}

	ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
		@Override
		public void onPageSelected(int position, int prePosition) {
			Log.d(TAG, "onPageSelected" + position + ", prePosition: "
					+ prePosition);
			ImageViewTouch preImageView = mPagerAdapter.views.get(prePosition);
			preImageView.setImageBitmapResetBase(
					preImageView.mBitmapDisplayed.getBitmap(), true);
		}

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
			Log.d(TAG, "onPageScrolled");
			mOnPagerScoll = true;
		}

		@Override
		public void onPageScrollStateChanged(int state) {
			Log.d(TAG, "onPageScrollStateChanged: " + state);
			if (state == ViewPager.SCROLL_STATE_DRAGGING) {
				mOnPagerScoll = true;
			} else if (state == ViewPager.SCROLL_STATE_SETTLING) {
				mOnPagerScoll = false;
			} else {
				mOnPagerScoll = false;
			}
		}
	};

	// decodes image and scales it to reduce memory consumption
	private Bitmap decodeFile(File f) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			final int REQUIRED_SIZE = 400;

			// Find the correct scale value. It should be the power of 2.
			int width_tmp = o.outWidth, height_tmp = o.outHeight;
			int scale = 1;
			while (true) {
				if (width_tmp / 2 < REQUIRED_SIZE
						|| height_tmp / 2 < REQUIRED_SIZE)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	private void updateZoomButtonsEnabled() {
		ImageViewTouch imageView = getCurrentImageView();
		float scale = imageView.getScale();
		mZoomButtonsController.setZoomInEnabled(scale < imageView.mMaxZoom);
		mZoomButtonsController.setZoomOutEnabled(scale > imageView.mMinZoom);
	}

	@Override
	public void onStart() {
		super.onStart();
		mPaused = false;
	}

	@Override
	public void onStop() {
		super.onStop();
		mPaused = true;
	}

	private void setupOnTouchListeners(View rootView) {
		mGestureDetector = new GestureDetector(this, new MyGestureListener(),
				null, true);
		mScaleGestureDetector = new ScaleGestureDetector(this,
				new MyOnScaleGestureListener());

		OnTouchListener rootListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				// NOTE: gestureDetector may handle onScroll..
				if (!mOnScale && event.getPointerCount() == 1) {
					if (!mOnPagerScoll) {
						mGestureDetector.onTouchEvent(event);
					}

				}
				if (!mOnPagerScoll) {
					mScaleGestureDetector.onTouchEvent(event);
				}

				ImageViewTouch imageView = getCurrentImageView();
				if (!mOnScale && !(event.getPointerCount() > 1)) {
					Matrix m = imageView.getImageViewMatrix();
					RectF rect = new RectF(0, 0, imageView.mBitmapDisplayed
							.getBitmap().getWidth(), imageView.mBitmapDisplayed
							.getBitmap().getHeight());
					m.mapRect(rect);
					Log.d(TAG, "rect.right: " + rect.right + ", rect.left: "
							+ rect.left + ", imageView.getWidth(): "
							+ imageView.getWidth());
					if (rect.right > imageView.getWidth() + 0.1
							&& rect.left < -0.1) {
						// float diff = imageView.getWidth() - rect.right;
						// if (diff >= -0.1f) {
						// mViewPager.onTouchEvent(event);
						// }
					} else {
						try {
							mViewPager.onTouchEvent(event);
						} catch (ArrayIndexOutOfBoundsException e) {
							// why?
						}
					}
				}

				// We do not use the return value of
				// mGestureDetector.onTouchEvent because we will not receive
				// the "up" event if we return false for the "down" event.
				return true;
			}
		};

		rootView.setOnTouchListener(rootListener);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent m) {
		if (mPaused)
			return true;
		return super.dispatchTouchEvent(m);
	}

	@Override
	protected void onDestroy() {
		// This is necessary to make the ZoomButtonsController unregister
		// its configuration change receiver.
		if (mZoomButtonsController != null) {
			mZoomButtonsController.setVisible(false);
		}
		ImageViewTouch imageView = getCurrentImageView();
		imageView.mBitmapDisplayed.recycle();
		imageView.clear();
		super.onDestroy();
	}

	private void setupZoomButtonController(final View ownerView) {
		mZoomButtonsController = new ZoomButtonsController(ownerView);
		mZoomButtonsController.setZoomSpeed(100);
		mZoomButtonsController.getZoomControls();
		mZoomButtonsController
				.setOnZoomListener(new ZoomButtonsController.OnZoomListener() {
					public void onVisibilityChanged(boolean visible) {
						if (visible) {
							updateZoomButtonsEnabled();
						}
					}

					public void onZoom(boolean zoomIn) {
						if (zoomIn) {
							getCurrentImageView().zoomIn();
						} else {
							getCurrentImageView().zoomOut();
						}
						updateZoomButtonsEnabled();
					}
				});
	}

	private ImageViewTouch getCurrentImageView() {
		return (ImageViewTouch) mPagerAdapter.views.get((mViewPager
				.getCurrentItem()));
	}

	private class MyGestureListener extends
			GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			Log.d(TAG, "gesture onScroll");
			if (mOnScale) {
				return true;
			}
			if (mPaused) {
				return false;
			}
			ImageViewTouch imageView = getCurrentImageView();
			imageView.panBy(-distanceX, -distanceY);

			// 超出边界效果去掉这个
			imageView.center(true, true);

			return true;
		}

		@Override
		public boolean onUp(MotionEvent e) {
			// getCurrentImageView().center(true, true);
			return super.onUp(e);
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			mZoomButtonsController.setVisible(true);
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (mPaused) {
				return false;
			}
			ImageViewTouch imageView = getCurrentImageView();
			// Switch between the original scale and 3x scale.
			if (imageView.mBaseZoom < 1) {
				if (imageView.getScale() > 2F) {
					imageView.zoomTo(1f);
				} else {
					imageView.zoomToPoint(3f, e.getX(), e.getY());
				}
			} else {
				if (imageView.getScale() > (imageView.mMinZoom + imageView.mMaxZoom) / 2f) {
					imageView.zoomTo(imageView.mMinZoom);
				} else {
					imageView.zoomToPoint(imageView.mMaxZoom, e.getX(),
							e.getY());
				}
			}

			updateZoomButtonsEnabled();
			return true;
		}
	}

	private class MyOnScaleGestureListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {

		float currentScale;
		float currentMiddleX;
		float currentMiddleY;

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {

			updateZoomButtonsEnabled();

			ImageViewTouch imageView = getCurrentImageView();

			if (currentScale > imageView.mMaxZoom) {
				currentScale = imageView.mMaxZoom;
			} else if (currentScale < imageView.mMinZoom) {
				currentScale = imageView.mMinZoom;
			}
			imageView.zoomToNoCenter(currentScale, currentMiddleX,
					currentMiddleY);
			imageView.center(true, true);

			imageView.postDelayed(new Runnable() {
				@Override
				public void run() {
					mOnScale = false;
				}
			}, 300);
			Log.d(TAG, "gesture onScaleEnd");
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			Log.d(TAG, "gesture onScaleStart");
			mOnScale = true;
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector, float mx, float my) {
			Log.d(TAG, "gesture onScale");
			ImageViewTouch imageView = getCurrentImageView();
			float ns = imageView.getScale() * detector.getScaleFactor();

			currentScale = ns;
			currentMiddleX = mx;
			currentMiddleY = my;

			if (detector.isInProgress()) {
				imageView.zoomToNoCenter(ns, mx, my);
			}
			return true;
		}
	}

	private class ImagePagerAdapter extends PagerAdapter {
		public Map<Integer, ImageViewTouch> views = new HashMap<Integer, ImageViewTouch>();

		@Override
		public int getCount() {
			// Log.d(TAG, "getCount");
			return mImageList.size();
		}

		@Override
		public Object instantiateItem(View container, int position) {
			Log.d(TAG, "instantiateItem");
			ImageViewTouch imageView = new ImageViewTouch(
					TouchImageActivity.this);
			imageView.setLayoutParams(new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			imageView.setBackgroundColor(Color.BLACK);
			imageView.setFocusableInTouchMode(true);

			Bitmap b = decodeFile(new File(mImageList.get(position)));
			// Bitmap b = BitmapFactory.decodeFile(mImageList.get(position));
			imageView.setImageBitmapResetBase(b, true);

			((ViewPager) container).addView(imageView);
			views.put(position, imageView);

			return imageView;
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			Log.d(TAG, "destroyItem");
			ImageViewTouch imageView = (ImageViewTouch) object;
			imageView.mBitmapDisplayed.recycle();
			imageView.clear();
			((ViewPager) container).removeView(imageView);
			views.remove(position);
		}

		@Override
		public void startUpdate(View container) {
			Log.d(TAG, "startUpdate");
		}

		@Override
		public void finishUpdate(View container) {
			Log.d(TAG, "finishUpdate");
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			Log.d(TAG, "isViewFromObject");
			return view == ((ImageViewTouch) object);
		}

		@Override
		public Parcelable saveState() {
			Log.d(TAG, "saveState");
			return null;
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
			Log.d(TAG, "restoreState");
		}
	}
}