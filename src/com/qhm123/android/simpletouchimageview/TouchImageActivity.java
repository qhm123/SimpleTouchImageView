package com.qhm123.android.simpletouchimageview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;
import android.widget.ZoomButtonsController;

public class TouchImageActivity extends Activity {

	public static final String TAG = TouchImageActivity.class.getSimpleName();

	private ImageViewTouch mImage;
	private RelativeLayout mRootLayout;

	private ZoomButtonsController mZoomButtonsController;

	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;

	private boolean mPaused;
	private boolean mOnScale = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewimage);

		mRootLayout = (RelativeLayout) findViewById(R.id.rootLayout);
		mImage = (ImageViewTouch) findViewById(R.id.image);

		String url = getIntent().getStringExtra("url");
		// url = "/mnt/sdcard/DCIM/Camera/IMG_20111109_133446.jpg";
		Bitmap b = BitmapFactory.decodeFile(url);
		if (b != null) {
			mImage.setImageBitmapResetBase(b, true);
		}

		setupZoomButtonController(mImage);
		setupOnTouchListeners(mRootLayout);
	}

	private void updateZoomButtonsEnabled() {
		ImageViewTouch imageView = mImage;
		float scale = imageView.getScale();
		mZoomButtonsController.setZoomInEnabled(scale < imageView.mMaxZoom);
		mZoomButtonsController.setZoomOutEnabled(scale > 1);
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
					mGestureDetector.onTouchEvent(event);
				}
				mScaleGestureDetector.onTouchEvent(event);

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
		mImage.mBitmapDisplayed.recycle();
		mImage.clear();
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
							mImage.zoomIn();
						} else {
							mImage.zoomOut();
						}
						updateZoomButtonsEnabled();
					}
				});
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
			ImageViewTouch imageView = mImage;
			// if (imageView.getScale() > 1F) {
			// imageView.postTranslate(-distanceX, -distanceY);
			imageView.panBy(-distanceX, -distanceY);

			// imageView.center(true, true);
			// }
			return true;
		}

		@Override
		public boolean onUp(MotionEvent e) {
			mImage.center(true, true);
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
			ImageViewTouch imageView = mImage;
			// Switch between the original scale and 3x scale.
			if (imageView.getScale() > 2F) {
				mImage.zoomTo(1f);
			} else {
				mImage.zoomToPoint(3f, e.getX(), e.getY());
			}
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

			if (currentScale > mImage.mMaxZoom) {
				currentScale = mImage.mMaxZoom;
			} else if (currentScale < mImage.mMinZoom) {
				currentScale = mImage.mMinZoom;
			}
			mImage.zoomToNoCenter(currentScale, currentMiddleX, currentMiddleY);
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
			float ns = mImage.getScale() * detector.getScaleFactor();
			// if (ns > mImage.mMaxZoom) {
			// ns = mImage.mMaxZoom;
			// } else if (ns < 1f) {
			// ns = 1f;
			// }
			currentScale = ns;
			currentMiddleX = mx;
			currentMiddleY = my;

			if (detector.isInProgress()) {
				mImage.zoomToNoCenter(ns, mx, my);
			}
			return true;
		}
	}
}