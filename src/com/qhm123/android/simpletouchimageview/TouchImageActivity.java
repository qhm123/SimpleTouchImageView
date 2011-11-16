package com.qhm123.android.simpletouchimageview;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.camera.gallery.IImage;
import com.android.camera.gallery.IImageList;

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
import android.widget.ZoomButtonsController;

public class TouchImageActivity extends Activity {

	public static final String TAG = TouchImageActivity.class.getSimpleName();

	private ImageViewTouch mImage;
	private RelativeLayout mRootLayout;
	private ViewPager mViewPager;
	// Gallery

	private ImagePagerAdapter mPagerAdapter;

	private ZoomButtonsController mZoomButtonsController;

	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;

	private List<String> mImageList;
	private boolean mPaused;
	private boolean mOnScale = false;
	private boolean mOnPagerScoll = false;
	private int mPosition = -1;
	private IImageList mAllImages;

	private ImageGetter mGetter;
	final GetterHandler mHandler = new GetterHandler();

	private class ImagePagerAdapter extends PagerAdapter {
		public Map<Integer, Object> views = new HashMap<Integer, Object>();

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

			imageView.setRecycler(mCache);

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

	private BitmapCache mCache;

	void setImage(int pos, boolean showControls) {
		mPosition = pos;

		Bitmap b = mCache.getBitmap(pos);
		if (b != null) {
			IImage image = mAllImages.getImageAt(pos);
			mImage.setImageRotateBitmapResetBase(
					new RotateBitmap(b, image.getDegreesRotated()), true);
			updateZoomButtonsEnabled();
		}

		ImageGetterCallback cb = new ImageGetterCallback() {
			public void completed() {
			}

			public boolean wantsThumbnail(int pos, int offset) {
				return !mCache.hasBitmap(pos + offset);
			}

			public boolean wantsFullImage(int pos, int offset) {
				return offset == 0;
			}

			public int fullImageSizeToUse(int pos, int offset) {
				// this number should be bigger so that we can zoom. we may
				// need to get fancier and read in the fuller size image as the
				// user starts to zoom.
				// Originally the value is set to 480 in order to avoid OOM.
				// Now we set it to 2048 because of using
				// native memory allocation for Bitmaps.
				final int imageViewSize = 2048;
				return imageViewSize;
			}

			public void imageLoaded(int pos, int offset, RotateBitmap bitmap,
					boolean isThumb) {
				// shouldn't get here after onPause()

				// We may get a result from a previous request. Ignore it.
				if (pos != mPosition) {
					bitmap.recycle();
					return;
				}

				if (isThumb) {
					mCache.put(pos + offset, bitmap.getBitmap());
				}
				if (offset == 0) {
					// isThumb: We always load thumb bitmap first, so we will
					// reset the supp matrix for then thumb bitmap, and keep
					// the supp matrix when the full bitmap is loaded.
					getCurrentImageView().setImageRotateBitmapResetBase(bitmap,
							isThumb);
					updateZoomButtonsEnabled();
				}
			}

			@Override
			public int[] loadOrder() {
				return new int[] { 0, 1, -1 };
			}
		};

		// Could be null if we're stopping a slide show in the course of pausing
		if (mGetter != null) {
			mGetter.setPosition(pos, cb, mAllImages, mHandler);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewpager);
		mImageList = new ArrayList<String>();

		mRootLayout = (RelativeLayout) findViewById(R.id.rootLayout);
		// mImage = (ImageViewTouch) findViewById(R.id.image);
		// mImage1 = (ImageViewTouch) findViewById(R.id.image1);
		// mViewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);
		mViewPager = (ViewPager) findViewById(R.id.viewPager);
		mViewPager.setPageMargin(40);

		mPagerAdapter = new ImagePagerAdapter();
		mViewPager.setAdapter(mPagerAdapter);

		String url = getIntent().getStringExtra("url");
		// url = "/mnt/sdcard/DCIM/Camera/IMG_20111109_133446.jpg";
		// Bitmap b = BitmapFactory.decodeFile(url);
		for (File file : new File("/mnt/sdcard/MIUI/photo/cars").listFiles()) {
			mImageList.add(file.getPath());
		}

		// Bitmap b0 = decodeFile(new File(mImageList.get(0)));
		// Bitmap b1 = decodeFile(new File(mImageList.get(1)));
		// if (b0 != null) {
		// mImage.setImageBitmapResetBase(b0, true);
		// mImage1.setImageBitmapResetBase(b1, true);
		// }

		mViewPager
				.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

					@Override
					public void onPageSelected(int position) {
						Log.d(TAG, "onPageSelected" + position);
						// mImage = (ImageViewTouch) mViewPager
						// .getChildAt(position);
						mImage = (ImageViewTouch) mViewPager
								.getChildAt(position);
						mPosition = position;
					}

					@Override
					public void onPageScrolled(int position,
							float positionOffset, int positionOffsetPixels) {
						Log.d(TAG, "onPageScrolled");
						mOnPagerScoll = true;
					}

					@Override
					public void onPageScrollStateChanged(int state) {
						Log.d(TAG, "onPageScrollStateChanged");
						if (state == ViewPager.SCROLL_STATE_DRAGGING) {
							mOnPagerScoll = true;
						} else if (state == ViewPager.SCROLL_STATE_SETTLING) {
							mOnPagerScoll = false;
						} else {
							mOnPagerScoll = false;
						}
					}
				});

		mGetter = new ImageGetter(getContentResolver());
		mCache = new BitmapCache(3);

		setupZoomButtonController(mRootLayout);
		setupOnTouchListeners(mViewPager);
	}

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
				// Log.d(TAG, "current item: " + mViewPager.getCurrentItem());
				// NOTE: gestureDetector may handle onScroll..
				if (!mOnScale && event.getPointerCount() == 1) {
					// mViewPager.onInterceptTouchEvent(event);
					// mViewPager.onTouchEvent(event);
					if (!mOnPagerScoll) {
						mGestureDetector.onTouchEvent(event);
					}

				}
				if (!mOnPagerScoll) {
					mScaleGestureDetector.onTouchEvent(event);
				}

				// mViewPager.onTouchEvent(event);

				ImageViewTouch imageView = getCurrentImageView();
				if (!mOnScale && !(event.getPointerCount() > 1)) {
					Log.d(TAG, "imageView.getScale(): " + imageView.getScale()
							+ ", " + imageView.mBaseZoom);
					if (imageView.getScale() < imageView.mBaseZoom) {
						// 计算边界
						Log.d(TAG, "<");
						Matrix m = imageView.getImageViewMatrix();
						RectF rect = new RectF(0, 0, imageView.mBitmapDisplayed
								.getBitmap().getWidth(),
								imageView.mBitmapDisplayed.getBitmap()
										.getHeight());
						m.mapRect(rect);
						float diff = imageView.getWidth() - rect.right;
						Log.d(TAG, "diff: " + diff);
						if (diff >= -0.1f) {
							mViewPager.onTouchEvent(event);
						}

						// float transX = imageView.getValue(
						// imageView.mDisplayMatrix, Matrix.MTRANS_X);
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
		// NOTE: ZoomButtonsController 会 捕获 ownerView的onTouch事件!
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
		// if (mPosition == -1) {
		// mPosition = 0;
		// }
		// return (ImageViewTouch) mViewPager.getChildAt(mPosition);
		// mViewPager.get
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
			// mImage.center(true, true);

			mOnScale = false;
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
}

// This is a cache for Bitmap displayed in ViewImage (normal mode, thumb only).
class BitmapCache implements ImageViewTouch.Recycler {
	public static class Entry {
		int mPos;
		Bitmap mBitmap;

		public Entry() {
			clear();
		}

		public void clear() {
			mPos = -1;
			mBitmap = null;
		}
	}

	private final Entry[] mCache;

	public BitmapCache(int size) {
		mCache = new Entry[size];
		for (int i = 0; i < mCache.length; i++) {
			mCache[i] = new Entry();
		}
	}

	// Given the position, find the associated entry. Returns null if there is
	// no such entry.
	private Entry findEntry(int pos) {
		for (Entry e : mCache) {
			if (pos == e.mPos) {
				return e;
			}
		}
		return null;
	}

	// Returns the thumb bitmap if we have it, otherwise return null.
	public synchronized Bitmap getBitmap(int pos) {
		Entry e = findEntry(pos);
		if (e != null) {
			return e.mBitmap;
		}
		return null;
	}

	public synchronized void put(int pos, Bitmap bitmap) {
		// First see if we already have this entry.
		if (findEntry(pos) != null) {
			return;
		}

		// Find the best entry we should replace.
		// See if there is any empty entry.
		// Otherwise assuming sequential access, kick out the entry with the
		// greatest distance.
		Entry best = null;
		int maxDist = -1;
		for (Entry e : mCache) {
			if (e.mPos == -1) {
				best = e;
				break;
			} else {
				int dist = Math.abs(pos - e.mPos);
				if (dist > maxDist) {
					maxDist = dist;
					best = e;
				}
			}
		}

		// Recycle the image being kicked out.
		// This only works because our current usage is sequential, so we
		// do not happen to recycle the image being displayed.
		if (best.mBitmap != null) {
			best.mBitmap.recycle();
		}

		best.mPos = pos;
		best.mBitmap = bitmap;
	}

	// Recycle all bitmaps in the cache and clear the cache.
	public synchronized void clear() {
		for (Entry e : mCache) {
			if (e.mBitmap != null) {
				e.mBitmap.recycle();
			}
			e.clear();
		}
	}

	// Returns whether the bitmap is in the cache.
	public synchronized boolean hasBitmap(int pos) {
		Entry e = findEntry(pos);
		return (e != null);
	}

	// Recycle the bitmap if it's not in the cache.
	// The input must be non-null.
	public synchronized void recycle(Bitmap b) {
		for (Entry e : mCache) {
			if (e.mPos != -1) {
				if (e.mBitmap == b) {
					return;
				}
			}
		}
		b.recycle();
	}
}