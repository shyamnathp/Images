package com.example.multimages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class MainActivity extends ActionBarActivity {

	double toast_counter = 0;
	int min_counter = 2000;
	GridView gridGallery;
	Handler handler;
	GalleryAdapter adapter;

	ImageView imgSinglePick;
	Button btnGalleryPick;
	Button btnGalleryPickMul;
	Button convert;

	String action;
	ViewSwitcher viewSwitcher;
	ImageLoader imageLoader;
	ArrayList<String> pathnames = new ArrayList<String>();

	private static final String IMAGE_DIRECTORY_NAME = "JPGtoTIFF Camera";
	private ArrayList<String> listOfImagesPath;

	public static ArrayList<String> list = new ArrayList<String>();

	private Uri fileUri; // file url to store image/video
	Button captureBtn = null;
	private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
	public static final int MEDIA_TYPE_IMAGE = 1;
	private static final int REQUEST_CODE = 1;
	final int CAMERA_CAPTURE = 1;
	private Uri picUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		getSupportActionBar().setBackgroundDrawable(
				new ColorDrawable(Color.parseColor("#3f1cd3")));
		initImageLoader();
		init();

	}

	private void initImageLoader() {
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
				.cacheOnDisc().imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
				.bitmapConfig(Bitmap.Config.RGB_565).considerExifParams(true)
				.build();
		ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(
				this).defaultDisplayImageOptions(defaultOptions).memoryCache(
				new WeakMemoryCache());

		ImageLoaderConfiguration config = builder.build();
		imageLoader = ImageLoader.getInstance();
		imageLoader.init(config);
	}

	private void init() {

		handler = new Handler();
		gridGallery = (GridView) findViewById(R.id.gridGallery);
		gridGallery.setFastScrollEnabled(true);
		adapter = new GalleryAdapter(getApplicationContext(), imageLoader);
		adapter.setMultiplePick(false);
		gridGallery.setAdapter(adapter);

		viewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);
		viewSwitcher.setDisplayedChild(1);

		imgSinglePick = (ImageView) findViewById(R.id.imgSinglePick);

		convert = (Button) findViewById(R.id.convert_fab);
		convert.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!pathnames.isEmpty() || !list.isEmpty()) {
					Intent i = new Intent(getApplicationContext(),
							ImageProcessService.class);

					if (list.isEmpty())
						i.putExtra("pathnames", pathnames);// gallery
					else
						i.putExtra("pathnames", list);// camera

					startService(i);
				} else {
					Toast t = new Toast(getApplicationContext());
					if ((System.currentTimeMillis() - toast_counter) > min_counter) {
						t = Toast.makeText(getApplicationContext(),
								"No items selected", Toast.LENGTH_SHORT);
						t.show();
						toast_counter = System.currentTimeMillis();
					} else
						t.cancel();
				}

			}
		});
		registerForContextMenu(gridGallery);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// successfully captured the image
				// display it in image view
				previewCapturedImage();
			} else if (resultCode == RESULT_CANCELED) {
				// user cancelled Image capture
				Toast.makeText(getApplicationContext(),
						"User cancelled image capture", Toast.LENGTH_SHORT)
						.show();
			} else {
				// failed to capture image
				Toast.makeText(getApplicationContext(),
						"Sorry! Failed to capture image", Toast.LENGTH_SHORT)
						.show();
			}
		} else if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
			String[] all_path = data.getStringArrayExtra("all_path");

			ArrayList<CustomGallery> dataT = new ArrayList<CustomGallery>();

			pathnames.clear();// when adding to list make sure previous elements
								// are cleared

			for (String string : all_path) {
				CustomGallery item = new CustomGallery();
				item.sdcardPath = string;
				pathnames.add(string);
				dataT.add(item);
				Log.d("images", string);

			}

			viewSwitcher.setDisplayedChild(0);
			adapter.addAll(dataT);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		/*
		 * if (id == R.id.pick) {
		 * 
		 * Intent i = new Intent(Action.ACTION_MULTIPLE_PICK);
		 * startActivityForResult(i, 200);
		 * 
		 * return true; }
		 */
		if (id == R.id.camera) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

			fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

			intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

			// start the image capture Intent
			startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
			return true;
		} else if (id == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		// AdapterView.AdapterContextMenuInfo cmi =
		// (AdapterView.AdapterContextMenuInfo) item.getMenuInfo ();

		if (item.getTitle() == "Delete") {

			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();
			// Bitmap m=(Bitmap) gridGallery.getItemAtPosition(info.position);
			list.remove(info.position);
			adapter.delete(info.position);
			adapter.notifyDataSetChanged();

			return super.onContextItemSelected(item);

		} else {
			return false;
		}
		// return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.setHeaderTitle("Select The Action");
		menu.add(0, v.getId(), 0, "Delete");// groupId, itemId, order, title
		// menu.add(0, v.getId(), 0, "Move Social");
	}

	// Clear cache on exit
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		try {
			trimCache(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void previewCapturedImage() {
		try {

			Log.d("fileURI", fileUri.getPath());

			list.add(fileUri.getPath());
			listOfImagesPath = null;
			listOfImagesPath = list;
			if (listOfImagesPath != null) {
				Log.d("its", "not null");
			}

			// Rotate images when rotate happens already on storing images
			ExifInterface ei = new ExifInterface(fileUri.getPath());
			int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				Bitmap bitmap_90 = decodeSampledBitmapFromPath(fileUri
						.getPath());
				rotateImage(bitmap_90, 90);
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				Bitmap bitmap_180 = decodeSampledBitmapFromPath(fileUri
						.getPath());
				rotateImage(bitmap_180, 180);
				break;
			// etc.
			}

			ArrayList<CustomGallery> dataT = new ArrayList<CustomGallery>();

			for (int i = 0; i < listOfImagesPath.size(); ++i) {
				CustomGallery item = new CustomGallery();
				item.sdcardPath = listOfImagesPath.get(i);

				dataT.add(item);

			}

			viewSwitcher.setDisplayedChild(0);
			adapter.addAll(dataT);

			// imgreview.setImageBitmap(bitmap);
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void rotateImage(Bitmap bmap, int angle) {
		// Create object of new Matrix.
		Matrix matrix = new Matrix();

		// set image rotation value to 90 degrees in matrix.
		matrix.postRotate(angle);

		int newWidth = bmap.getWidth();
		int newHeight = bmap.getHeight();

		// Create bitmap with new values.
		Bitmap bitmap = Bitmap.createBitmap(bmap, 0, 0, newWidth, newHeight,
				matrix, true);

		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(fileUri.getPath());
			bitmap.compress(CompressFormat.JPEG, 100, stream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		bitmap=null;

	}

	private static Bitmap decodeSampledBitmapFromPath(String path) {

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		options.inSampleSize = 1;

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;

		Bitmap bmp = BitmapFactory.decodeFile(path, options);

		return bmp;
	}

	public Uri getOutputMediaFileUri(int type) {
		return Uri.fromFile(getOutputMediaFile(type));
	}

	/*
	 * returning image / video
	 */
	private File getOutputMediaFile(int type) {

		// External sdcard location
		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				IMAGE_DIRECTORY_NAME);

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
						+ IMAGE_DIRECTORY_NAME + " directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.getDefault()).format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".jpg");
			// } else if (type == MEDIA_TYPE_VIDEO) {
			// mediaFile = new File(mediaStorageDir.getPath() + File.separator
			// + "VID_" + timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}
	
	
	public static void trimCache(Context context) {
		try {
			File dir = context.getExternalCacheDir();
			if (dir != null && dir.isDirectory()) {
				deleteDir(dir);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//recursive deletion
	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

}
