package com.asgj.android.appusage.Utility;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.widget.ImageView;

public class HttpImageLoader implements ComponentCallbacks2 {
	private TCLruCache cache;
	private Context mContext = null;
	private static HttpImageLoader mLoader = null;

	private HttpImageLoader(Context context) {
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		int maxKb = am.getMemoryClass() * 1024;
		int limitKb = maxKb / 8; // 1/8th of total ram
		cache = new TCLruCache(limitKb);
		mContext = context;
	}

	public static HttpImageLoader getInstance(Context context) {
		if (mLoader == null) {
			mLoader = new HttpImageLoader(context);
		}
		return mLoader;
	}

	public void display(String url, ImageView imageview, int defaultresource) {
		imageview.setImageResource(defaultresource);
		Bitmap image = cache.get(url);
		if (image != null) {
			imageview.setImageBitmap(image);
		} else {
			image = Utils.getApplicationIcon(mContext, url);
			cache.put(url, image);
			imageview.setImageBitmap(image);
		}
	}

	private class TCLruCache extends LruCache<String, Bitmap> {

		public TCLruCache(int maxSize) {
			super(maxSize);
		}

		@Override
		protected int sizeOf(String key, Bitmap value) {
			int kbOfBitmap = value.getByteCount() / 1024;
			return kbOfBitmap;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	}

	@Override
	public void onLowMemory() {
	}

	@Override
	public void onTrimMemory(int level) {
		if (level >= TRIM_MEMORY_MODERATE) {
			cache.evictAll();
		} else if (level >= TRIM_MEMORY_BACKGROUND) {
			cache.trimToSize(cache.size() / 2);
		}
	}
}