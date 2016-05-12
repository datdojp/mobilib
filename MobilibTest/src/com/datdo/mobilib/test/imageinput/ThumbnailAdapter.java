package com.datdo.mobilib.test.imageinput;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.datdo.mobilib.base.MblBaseAdapter;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblUtils;
import com.datdo.mobilib.v2.image.ImageLoader;

import java.io.File;

@SuppressLint("InflateParams")
public class ThumbnailAdapter extends MblBaseAdapter<String> {

    private Context context;
    private ImageLoader imageLoader;

    public ThumbnailAdapter(Context context, ImageLoader imageLoader) {
        this.context = context;
        this.imageLoader = imageLoader;
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.cell_input_image, null);
        }

        String path = (String) getItem(pos);

        TextView pathText = (TextView) view.findViewById(R.id.path_text);
        pathText.setText(path);

        imageLoader.with(context)
                .load(new File(path))
                .into((ImageView) view.findViewById(R.id.thumbnail_image));

        return view;
    }

}
