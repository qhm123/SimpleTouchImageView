package com.qhm123.android.simpletouchimageview;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SelectActivity extends Activity implements OnClickListener {

	public static final String TAG = SelectActivity.class.getSimpleName();
	public static final int REQ_CODE_PICTURES = 1;

	private Button mButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mButton = (Button) findViewById(R.id.button);
		mButton.setOnClickListener(this);

	}

	private void openPictures() {
		Intent intent = new Intent();
		/* Open the page of select pictures and set the type to image */
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(intent, REQ_CODE_PICTURES);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQ_CODE_PICTURES:
				Uri uri = data.getData();
				ContentResolver cr = this.getContentResolver();
				// get the physical path of the image
				Cursor c = cr.query(uri, null, null, null, null);
				c.moveToFirst();
				String photoTemp = c.getString(c.getColumnIndex("_data"));
				// uploadImage(photoTemp);
				Log.d(TAG, "photo url: " + photoTemp);
				// Intent i = new Intent(SelectActivity.this, ViewImage.class);
				Intent i = new Intent(SelectActivity.this, TouchImageActivity.class);
				i.putExtra("url", photoTemp);
				startActivity(i);
				break;
			default:
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button:
			openPictures();
			break;

		default:
			break;
		}
	}
}
