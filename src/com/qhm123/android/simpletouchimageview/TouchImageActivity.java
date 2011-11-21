package com.qhm123.android.simpletouchimageview;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ZoomControls;

public class TouchImageActivity extends Activity implements OnClickListener {

	private static final String TAG = TouchImageActivity.class.getSimpleName();

	private static final int REQUIRED_BITMAP_SIZE = 400;
	private static final int SHOW_HIDE_CONTROL_ANIMATION_TIME = 500;

	private static final int PAGER_MARGIN_DP = 40;
	private static final int HIDE_CONTROLS_DELAY = 5000;

	private static final int MSG_HIDE_CONTROLS = 1;

	private RelativeLayout mRootLayout;
	private ViewPager mViewPager;
	private ViewGroup mHeader;
	private ViewGroup mBottom;
	private TextView mPageShwo;
	private TextView mPicName;
	private Button mNext;
	private Button mPrevious;
	// private Button mZoomIn;
	// private Button mZoomOut;
	private Button mOpen;
	private Button mMore;
	private ZoomControls mZoomButtons;
	private AlertDialog mMoreDialog;

	private ImagePagerAdapter mPagerAdapter;

	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;

	private boolean mPaused;
	private boolean mOnScale = false;
	private boolean mOnPagerScoll = false;
	private boolean mControlsShow = false;

	// 传入参数
	private List<String> mImageList;
	private int mPosition;

	// // 控制控制栏延迟隐藏
	// private final Handler mHandler = new Handler() {
	// @Override
	// public void handleMessage(Message msg) {
	// Log.d(TAG, "msg.what: " + msg.what);
	// switch (msg.what) {
	// case MSG_HIDE_CONTROLS:
	// hideControls();
	// break;
	// }
	// }
	// };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewpager);

		mRootLayout = (RelativeLayout) findViewById(R.id.rootLayout);
		mViewPager = (ViewPager) findViewById(R.id.viewPager);
		mHeader = (ViewGroup) findViewById(R.id.ll_header);
		mBottom = (ViewGroup) findViewById(R.id.ll_bottom);
		mPageShwo = (TextView) findViewById(R.id.tv_page);
		mPicName = (TextView) findViewById(R.id.tv_pic_name);
		mNext = (Button) findViewById(R.id.btn_next);
		mNext.setOnClickListener(this);
		mPrevious = (Button) findViewById(R.id.btn_pre);
		mPrevious.setOnClickListener(this);
		mOpen = (Button) findViewById(R.id.btn_open);
		mOpen.setOnClickListener(this);
		mMore = (Button) findViewById(R.id.btn_dialog);
		mMore.setOnClickListener(this);
		// mZoomIn = (Button) findViewById(R.id.btn_zoom_in);
		// mZoomIn.setOnClickListener(this);
		// mZoomOut = (Button) findViewById(R.id.btn_zoom_out);
		// mZoomOut.setOnClickListener(this);
		mZoomButtons = (ZoomControls) findViewById(R.id.zoomButtons);
		mZoomButtons.setZoomSpeed(100);
		mZoomButtons.setOnZoomInClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getCurrentImageView().zoomIn();
				updateZoomButtonsEnabled();
			}
		});
		mZoomButtons.setOnZoomOutClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getCurrentImageView().zoomOut();
				updateZoomButtonsEnabled();
			}
		});

		final float scale = getResources().getDisplayMetrics().density;
		int pagerMarginPixels = (int) (PAGER_MARGIN_DP * scale + 0.5f);
		mViewPager.setPageMargin(pagerMarginPixels);
		mViewPager.setPageMarginDrawable(new ColorDrawable(Color.BLACK));

		mPagerAdapter = new ImagePagerAdapter();
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOnPageChangeListener(mPageChangeListener);
		setupOnTouchListeners(mViewPager);

		// 参数传入
		mImageList = new ArrayList<String>();
		// "/mnt/sdcard/MIUI/photo/cars"
		// "/sdcard/download"
		for (File file : new File("/sdcard/download").listFiles()) {
			mImageList.add(file.getPath());
		}
		mPosition = 0;

		mViewPager.setCurrentItem(mPosition, false);
		updateShowInfo();
		updatePreNextButtonEnable();
		hideControls();
	}

	private void updateShowInfo() {
		if (mImageList.size() > 0) {
			mPageShwo.setText(String.format("%d/%d", mPosition + 1,
					mImageList.size()));
			mPicName.setText(getPositionFileName(mPosition));
		}
	}

	private void updatePreNextButtonEnable() {
		mPrevious.setEnabled(mPosition > 0);
		mNext.setEnabled(mPosition < mImageList.size() - 1);
	}

	private void updateZoomButtonsEnabled() {
		ImageViewTouch imageView = getCurrentImageView();
		if (imageView != null) {
			float scale = imageView.getScale();
			// mZoomIn.setEnabled(scale < imageView.mMaxZoom);
			// mZoomOut.setEnabled(scale > imageView.mMinZoom);
			mZoomButtons.setIsZoomInEnabled(scale < imageView.mMaxZoom);
			mZoomButtons.setIsZoomOutEnabled(scale > imageView.mMinZoom);
		}
	}

	private void showControls() {
		AlphaAnimation animation = new AlphaAnimation(0f, 1f);
		animation.setFillAfter(true);
		animation.setDuration(SHOW_HIDE_CONTROL_ANIMATION_TIME);
		mZoomButtons.startAnimation(animation);
		mHeader.startAnimation(animation);
		mBottom.startAnimation(animation);

		mControlsShow = true;
		mZoomButtons.setVisibility(View.VISIBLE);
		mHeader.setVisibility(View.VISIBLE);
		mBottom.setVisibility(View.VISIBLE);
	}

	private void hideControls() {
		AlphaAnimation animation = new AlphaAnimation(1f, 0f);
		animation.setFillAfter(true);
		animation.setDuration(SHOW_HIDE_CONTROL_ANIMATION_TIME);
		mZoomButtons.startAnimation(animation);
		mHeader.startAnimation(animation);
		mBottom.startAnimation(animation);

		mControlsShow = false;
		mZoomButtons.setVisibility(View.GONE);
		mHeader.setVisibility(View.GONE);
		mBottom.setVisibility(View.GONE);
	}

	// private void delayHideControls() {
	// mHandler.removeMessages(MSG_HIDE_CONTROLS);
	// mHandler.sendEmptyMessageDelayed(MSG_HIDE_CONTROLS, HIDE_CONTROLS_DELAY);
	// }

	private String getPositionFileName(int position) {
		String path = mImageList.get(position);
		String[] splits = path.split("/");
		String name = "";
		if (splits.length > 0) {
			name = splits[splits.length - 1];
		}

		return name;
	}

	// decodes image and scales it to reduce memory consumption
	private Bitmap decodeFile(File f) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			final int REQUIRED_SIZE = REQUIRED_BITMAP_SIZE;

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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_MR1) {
			mScaleGestureDetector = new ScaleGestureDetector(this,
					new MyOnScaleGestureListener());
		}
		mGestureDetector = new GestureDetector(this, new MyGestureListener());

		OnTouchListener rootListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				// NOTE: gestureDetector may handle onScroll..
				if (!mOnScale) {
					if (!mOnPagerScoll) {
						mGestureDetector.onTouchEvent(event);
					}
				}

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_MR1) {
					if (!mOnPagerScoll) {
						mScaleGestureDetector.onTouchEvent(event);
					}
				}

				ImageViewTouch imageView = getCurrentImageView();
				if (!mOnScale) {
					Matrix m = imageView.getImageViewMatrix();
					RectF rect = new RectF(0, 0, imageView.mBitmapDisplayed
							.getBitmap().getWidth(), imageView.mBitmapDisplayed
							.getBitmap().getHeight());
					m.mapRect(rect);
					// Log.d(TAG, "rect.right: " + rect.right + ", rect.left: "
					// + rect.left + ", imageView.getWidth(): "
					// + imageView.getWidth());
					// 图片超出屏幕范围后移动
					if (!(rect.right > imageView.getWidth() + 0.1 && rect.left < -0.1)) {
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
		// delayHideControls();
		return super.dispatchTouchEvent(m);
	}

	@Override
	protected void onDestroy() {
		ImageViewTouch imageView = getCurrentImageView();
		imageView.mBitmapDisplayed.recycle();
		imageView.clear();
		super.onDestroy();
	}

	private ImageViewTouch getCurrentImageView() {
		return (ImageViewTouch) mPagerAdapter.views.get((mViewPager
				.getCurrentItem()));
	}

	ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
		@Override
		public void onPageSelected(int position, int prePosition) {
			// Log.d(TAG, "onPageSelected" + position + ", prePosition: "
			// + prePosition);
			ImageViewTouch preImageView = mPagerAdapter.views.get(prePosition);
			if (preImageView != null) {
				preImageView.setImageBitmapResetBase(
						preImageView.mBitmapDisplayed.getBitmap(), true);
			}
			mPosition = position;

			updateZoomButtonsEnabled();
			updateShowInfo();
			updatePreNextButtonEnable();
		}

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
			// Log.d(TAG, "onPageScrolled");
			mOnPagerScoll = true;
		}

		@Override
		public void onPageScrollStateChanged(int state) {
			// Log.d(TAG, "onPageScrollStateChanged: " + state);
			if (state == ViewPager.SCROLL_STATE_DRAGGING) {
				mOnPagerScoll = true;
			} else if (state == ViewPager.SCROLL_STATE_SETTLING) {
				mOnPagerScoll = false;
			} else {
				mOnPagerScoll = false;
			}
		}
	};

	private class MyGestureListener extends
			GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			// Log.d(TAG, "gesture onScroll");
			if (mOnScale) {
				return true;
			}
			if (mPaused) {
				return false;
			}
			ImageViewTouch imageView = getCurrentImageView();
			imageView.panBy(-distanceX, -distanceY);
			imageView.center(true, true);

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
			if (mControlsShow) {
				// delayHideControls();
				hideControls();
			} else {
				updateZoomButtonsEnabled();
				showControls();
				// mHandler.sendEmptyMessageDelayed(MSG_HIDE_CONTROLS,
				// HIDE_CONTROLS_DELAY);
			}

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

			final ImageViewTouch imageView = getCurrentImageView();

			Log.d(TAG, "currentScale: " + currentScale + ", maxZoom: "
					+ imageView.mMaxZoom);
			if (currentScale > imageView.mMaxZoom) {
				imageView
						.zoomToNoCenterWithAni(currentScale
								/ imageView.mMaxZoom, 1, currentMiddleX,
								currentMiddleY);
				currentScale = imageView.mMaxZoom;
				imageView.zoomToNoCenterValue(currentScale, currentMiddleX,
						currentMiddleY);
			} else if (currentScale < imageView.mMinZoom) {
				imageView.zoomToNoCenterWithAni(currentScale,
						imageView.mMinZoom, currentMiddleX, currentMiddleY);
				currentScale = imageView.mMinZoom;
				imageView.zoomToNoCenterValue(currentScale, currentMiddleX,
						currentMiddleY);
			} else {
				imageView.zoomToNoCenter(currentScale, currentMiddleX,
						currentMiddleY);
			}

			imageView.center(true, true);

			// NOTE: 延迟修正缩放后可能移动问题
			imageView.postDelayed(new Runnable() {
				@Override
				public void run() {
					mOnScale = false;
				}
			}, 300);
			// Log.d(TAG, "gesture onScaleEnd");
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			// Log.d(TAG, "gesture onScaleStart");
			mOnScale = true;
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector, float mx, float my) {
			// Log.d(TAG, "gesture onScale");
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
			// Log.d(TAG, "instantiateItem");
			ImageViewTouch imageView = new ImageViewTouch(
					TouchImageActivity.this);
			imageView.setLayoutParams(new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			imageView.setBackgroundColor(Color.BLACK);
			imageView.setFocusableInTouchMode(true);

			Bitmap b = decodeFile(new File(mImageList.get(position)));
			imageView.setImageBitmapResetBase(b, true);

			((ViewPager) container).addView(imageView);
			views.put(position, imageView);

			return imageView;
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			// Log.d(TAG, "destroyItem");
			ImageViewTouch imageView = (ImageViewTouch) object;
			imageView.mBitmapDisplayed.recycle();
			imageView.clear();
			((ViewPager) container).removeView(imageView);
			views.remove(position);
		}

		@Override
		public void startUpdate(View container) {
			// Log.d(TAG, "startUpdate");
		}

		@Override
		public void finishUpdate(View container) {
			// Log.d(TAG, "finishUpdate");
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			// Log.d(TAG, "isViewFromObject");
			return view == ((ImageViewTouch) object);
		}

		@Override
		public Parcelable saveState() {
			// Log.d(TAG, "saveState");
			return null;
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
			// Log.d(TAG, "restoreState");
		}
	}

	private Uri getCurrentImageUri() {
		File file = new File(mImageList.get(mPosition));
		return Uri.fromFile(file);
	}

	private AlertDialog getMoreDialog() {
		if (mMoreDialog == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			LayoutInflater inflater = (LayoutInflater) this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(R.layout.moredialog, null);
			builder.setView(view);
			Button mail = (Button) view.findViewById(R.id.mail);
			mail.setOnClickListener(this);
			Button share = (Button) view.findViewById(R.id.share);
			share.setOnClickListener(this);
			Button close = (Button) view.findViewById(R.id.close);
			close.setOnClickListener(this);
			mMoreDialog = builder.create();
		}
		return mMoreDialog;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_next:
			if (mPosition < mImageList.size() - 1) {
				mViewPager.setCurrentItem(++mPosition);
			}
			updatePreNextButtonEnable();
			break;
		case R.id.btn_pre:
			if (mPosition > 0) {
				mViewPager.setCurrentItem(--mPosition);
			}
			updatePreNextButtonEnable();
			break;
		case R.id.btn_open: {
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(getCurrentImageUri(), "image/*");
			startActivity(intent);
		}
			break;
		case R.id.btn_dialog:
			getMoreDialog().show();
			break;
		case R.id.mail: {
			getMoreDialog().dismiss();

			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_STREAM, getCurrentImageUri());
			intent.setType("message/rfc882");
			Intent.createChooser(intent, "Choose Email Client");
			startActivity(intent);
		}
			break;
		case R.id.share: {
			getMoreDialog().dismiss();

			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_STREAM, getCurrentImageUri());
			intent.addCategory(Intent.CATEGORY_DEFAULT);
			intent.setType("image/*");
			startActivity(intent);
		}
			break;
		case R.id.close:
			getMoreDialog().dismiss();
			break;
		// case R.id.btn_zoom_in:
		// getCurrentImageView().zoomIn();
		// updateZoomButtonsEnabled();
		// break;
		// case R.id.btn_zoom_out:
		// getCurrentImageView().zoomOut();
		// updateZoomButtonsEnabled();
		// break;
		}
	}
}
