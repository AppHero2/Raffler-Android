package com.raffler.app.classes;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.CircleBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.raffler.app.R;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Ghost on 14/8/2017.
 */

public class AppConsts {

    public static DisplayImageOptions displayImageOptions_original = new DisplayImageOptions.Builder()
            .showImageOnLoading(android.R.drawable.sym_def_app_icon)
            .showImageForEmptyUri(android.R.drawable.sym_def_app_icon)
            .showImageOnFail(android.R.drawable.sym_def_app_icon)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .build();

    public static DisplayImageOptions displayImageOptions_circluar = new DisplayImageOptions.Builder()
            .showImageOnLoading(android.R.drawable.sym_def_app_icon)
            .showImageForEmptyUri(android.R.drawable.sym_def_app_icon)
            .showImageOnFail(android.R.drawable.sym_def_app_icon)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .displayer(new CircleBitmapDisplayer(0xccff8000, 1))
            .build();

    public static class AnimateFirstDisplayListener extends SimpleImageLoadingListener {

        static final List<String> displayedImages = Collections.synchronizedList(new LinkedList<String>());

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (loadedImage != null) {
                ImageView imageView = (ImageView) view;
                boolean firstDisplay = !displayedImages.contains(imageUri);
                if (firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 500);
                    displayedImages.add(imageUri);
                }
            }
        }
    }

    public static void setProfileImage(String url, ImageView imgView){
        ImageLoader.getInstance().displayImage(url, imgView, AppConsts.displayImageOptions_circluar, new AppConsts.AnimateFirstDisplayListener());
    }
}
