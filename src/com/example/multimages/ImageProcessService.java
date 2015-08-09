package com.example.multimages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import magick.ImageInfo;
import magick.MagickException;
import magick.MagickImage;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ImageProcessService extends Service {

	public ArrayList<String> pathnames;
	public String str;
	File dir_comb;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Let it continue running until it is stopped.
		Toast.makeText(this, "Converting..", Toast.LENGTH_LONG).show();
		pathnames = intent.getStringArrayListExtra("pathnames");
		new Convert_Async().execute();
		return 0;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Toast.makeText(this, "Converted successfully",
		// Toast.LENGTH_SHORT).show();
	}

	class Convert_Async extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub

			ArrayList<Bitmap> bitmap_array = new ArrayList<Bitmap>();

			// add images to bitmap_array having resolution 320*240
			for (String path : pathnames) {
				Log.d("arraylist", path);
				bitmap_array.add(decodeSampledBitmapFromPath(path, 640, 480));
			}

			try {
				File root = android.os.Environment
						.getExternalStorageDirectory();
				File dir = new File(root.getAbsolutePath() + "/TIF_images");
				if (!dir.exists()) {
					dir.mkdirs();
				}
				String time = "" + System.currentTimeMillis();
				str = dir.getAbsolutePath() + "/converted" + time + ".tif";

				// combine bitmaps and store in comb_path location
				String comb_path = combineImages(bitmap_array);

				Log.d("jpg image path:", comb_path);

				MagickImage Image = new MagickImage(new ImageInfo(comb_path));
				// Image = Image.scaleImage(800, 800);
				Image.setImageFormat("tif");
				Image.setFileName(str);

				ImageInfo localImageInfo = new ImageInfo(str);
				localImageInfo.setMagick("tif");
				Image.writeImage(localImageInfo);

				// store as tif
				byte[] blob = Image.imageToBlob(localImageInfo);
				FileOutputStream localFileOutputStream = new FileOutputStream(
						str);
				localFileOutputStream.write(blob);
				localFileOutputStream.close();

				blob = null;

				delete_jpg(dir_comb);
				
				if(!MainActivity.list.isEmpty())
				{
					MainActivity.list.clear();
				}
				
				Log.d("tif image path:", str);

				stopSelf();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				stopSelf();
				e.printStackTrace();
			} catch (MagickException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				stopSelf();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				stopSelf();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			Toast.makeText(getApplicationContext(),
					"Saved at location :" + str, Toast.LENGTH_LONG).show();
			super.onPostExecute(result);
		}

	}

	private static Bitmap decodeSampledBitmapFromPath(String path,
			int reqWidth, int reqHeight) {

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;

		options.inDither = false;
		options.inScaled = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;

		Bitmap bmp = BitmapFactory.decodeFile(path, options);

		int width = bmp.getWidth();
		int height = bmp.getHeight();
		float scaleWidth = ((float) reqWidth) / width;
		float scaleHeight = ((float) reqHeight) / height;

		// Resize using matrix-more efficient than createScaledBitmap()
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap scaled_bitmap = Bitmap.createBitmap(bmp, 0, 0, width, height,
				matrix, true);
		bmp = null;

		// bmp=Bitmap.createScaledBitmap(bmp,reqWidth,reqHeight,true);//scale

		return scaled_bitmap;
	}

	private static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {

		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}
		return inSampleSize;
	}

	private String combineImages(ArrayList<Bitmap> bitmaparray) {

		Bitmap temp = null;
		// eliminate w,h calculation when bitmaparray contains only one element
		if (bitmaparray.size() > 1) {
			int w = 0, h = 0;
			for (int i = 0; i < bitmaparray.size(); i++) {
				if (i < bitmaparray.size() - 1) {
					w = bitmaparray.get(i).getWidth() > bitmaparray.get(i + 1)
							.getWidth() ? bitmaparray.get(i).getWidth()
							: bitmaparray.get(i + 1).getWidth();
				}
				h += bitmaparray.get(i).getHeight();
			}

			temp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(temp);
			int top = 0;
			for (int i = 0; i < bitmaparray.size(); i++) {
				top = (i == 0 ? 0 : top + bitmaparray.get(i).getHeight());
				canvas.drawBitmap(bitmaparray.get(i), 0f, top, null);
			}
		} else
			temp = bitmaparray.get(0);

		// deallocate bitmaparray
		bitmaparray.clear();

		File root = android.os.Environment.getExternalStorageDirectory();
		dir_comb = new File(root.getAbsolutePath() + "/testcombined");
		if (!dir_comb.exists()) {
			dir_comb.mkdirs();
		}
		String time = "" + System.currentTimeMillis();
		String str = dir_comb.getAbsolutePath() + "/combined" + time + ".jpg";
		FileOutputStream stream = null;

		try {
			stream = new FileOutputStream(str);
			temp.compress(CompressFormat.JPEG, 100, stream);
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

		return str;
	}
	public void delete_jpg(File fileList)
	{
		
		//check if dir is not null
		if (fileList != null)
		{
			// so we can list all files
			File[] filenames = fileList.listFiles();
			// loop through each file and delete
			for (File tmpf : filenames){
				tmpf.delete();
			}
			fileList.delete();
		}	
	}

}
