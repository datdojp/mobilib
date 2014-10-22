package com.datdo.mobilib.test.imageinput;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.datdo.mobilib.base.MblBaseAdapter;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblImageLoader;
import com.datdo.mobilib.util.MblUtils;

@SuppressLint("InflateParams")
public class ThumbnailAdapter extends MblBaseAdapter<String> {

    private MblImageLoader<String> mImageLoader = new MblImageLoader<String>() {

        @Override
        protected boolean shouldLoadImageForItem(String item) {
            return true;
        }

        @Override
        protected int getDefaultImageResource(String item) {
            return 0;
        }

        @Override
        protected int getErrorImageResource(String item) {
            return 0;
        }

        @Override
        protected int getLoadingIndicatorImageResource(String item) {
            return 0;
        }

        @Override
        protected String getItemBoundWithView(View view) {
            return (String) view.getTag();
        }

        @Override
        protected ImageView getImageViewFromView(View view) {
            return (ImageView) view.findViewById(R.id.thumbnail_image);
        }

        @Override
        protected String getItemId(String item) {
            return item;
        }

        @Override
        protected void retrieveImage(String item, MblRetrieveImageCallback cb) {
            cb.onRetrievedFile(item);
        }
    };

    @Override
    public View getView(int pos, View view, ViewGroup parent) {
        if (view == null) {
            view = MblUtils.getLayoutInflater().inflate(R.layout.cell_input_image, null);
        }

        String path = (String) getItem(pos);
        view.setTag(path);

        TextView pathText = (TextView) view.findViewById(R.id.path_text);
        pathText.setText(path);

        mImageLoader.loadImage(view);

        return view;
    }

}
