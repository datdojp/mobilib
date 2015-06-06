package com.datdo.mobilib.test.imageloader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;

import com.datdo.mobilib.api.MblApi;
import com.datdo.mobilib.api.MblApi.MblApiCallback;
import com.datdo.mobilib.api.MblRequest;
import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.base.MblBaseAdapter;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblImageLoader;
import com.datdo.mobilib.util.MblSimpleImageLoader;
import com.datdo.mobilib.util.MblUtils;

import java.util.Map;

public class ImageLoaderTestActivity extends MblBaseActivity {

    private static final String[] LINKS = new String[] {
            "http://cupcakepedia.com/wp-content/uploads/2013/12/cute-cat-love2.jpg",
            "http://images.wisegeek.com/young-calico-cat.jpg",
            "http://2.bp.blogspot.com/-AM6OorTilnA/T7bEVEefD3I/AAAAAAAAGy4/m5Y-u7ohe6U/s640/Beautiful+White+Cute+Cat+Pictures+%253A+Photos+%253A+Wallpapers+11.jpg",
            "http://www.vetprofessionals.com/catprofessional/images/home-cat.jpg",
            "https://www.petfinder.com/wp-content/uploads/2012/11/99059361-choose-cat-litter-632x475.jpg",
            "http://i.dailymail.co.uk/i/pix/2013/12/17/article-2524993-1A158ACB00000578-186_634x400.jpg",
            "http://exmoorpet.com/wp-content/uploads/2012/08/cat.png",
            "http://static.guim.co.uk/sys-images/Guardian/Pix/pictures/2013/10/30/1383128094134/248b4f05-05f0-449b-9a23-59b82dbc40a3-460x276.jpeg",
            "http://i.telegraph.co.uk/multimedia/archive/02763/cats_2763799b.jpg",
            "http://www.thetimes.co.uk/tto/multimedia/archive/00342/114240651_cat_342943c.jpg",
            "http://www.thetimes.co.uk/tto/multimedia/archive/00342/114240651_cat_342943c.jpg",
            "http://www.thetimes.co.uk/tto/multimedia/archive/00342/114240651_cat_342943c.jpg",
            "http://cdn.sheknows.com/articles/2012/10/isolated-cat.jpg",
            "https://www.friskies.com/Content/images/headers/cat_wet.png",
            "http://www.projectpawsitive.com/wp-content/uploads/2013/08/CAt_No-Background1.png",
            "http://upload.wikimedia.org/wikipedia/commons/4/41/Siberischer_tiger_de_edit02.jpg",
            "http://www.cats.org.uk/uploads/images/pages/photo_latest14.jpg",
            "https://www.petfinder.com/wp-content/uploads/2012/11/100691619-what-is-cat-fostering-632x475.jpg",
            "http://news.bbcimg.co.uk/media/images/66791000/jpg/_66791419_cat-norwegian.jpg",
            "don't-load",
            "http://should.fail.link",
            "http://fc06.deviantart.net/fs70/i/2011/194/a/f/bored_cat_by_blueann94-d3nrovc.jpg",
            "http://i.huffpost.com/gen/1691894/thumbs/o-MONKEY-MASSAGES-CAT-900.jpg?1",
            "http://www.funchap.com/wp-content/uploads/2014/05/cute-dog-baby-wallpaper.jpg",
            "http://www.w8themes.com/wp-content/uploads/2013/09/Dog-Computer-Wallpaper.jpg",
            "http://www.w8themes.com/wp-content/uploads/2013/09/Dog-Computer-Wallpaper.jpg",
            "http://www.w8themes.com/wp-content/uploads/2013/09/Dog-Computer-Wallpaper.jpg"
    };

    private ListView mListView;

    private class Adapter extends MblBaseAdapter<String> {

        public Adapter(String[] links) {
            for (String l : links) {
                getData().add(l);
            }
        }

        private MblSimpleImageLoader<String> mImageLoader = new MblSimpleImageLoader<String>() {

            @Override
            protected String getItemId(String item) {
                return item;
            }

            @Override
            protected ImageView getImageViewBoundWithView(View view) {
                return (ImageView) view.findViewById(R.id.image);
            }

            @Override
            protected String getItemBoundWithView(View view) {
                return (String) view.getTag();
            }

            @Override
            protected void retrieveImage(String item, final MblRetrieveImageCallback cb) {
                MblApi.run(new MblRequest()
                        .setMethod(MblApi.Method.GET)
                        .setUrl(item)
                        .setCacheDuration(Long.MAX_VALUE)
                        .setCallback(new MblApiCallback() {

                            @Override
                            public void onSuccess(int statusCode, byte[] data, Map<String, String> headers) {
                                cb.onRetrievedByteArray(data);
                            };

                            @Override
                            public void onFailure(int error, String errorMessage) {
                                cb.onRetrievedError();
                            }
                        }));
            }

            @Override
            protected void onError(ImageView imageView, String item) {
                imageView.setImageResource(R.drawable._default);
            }
        };

        @Override
        public View getView(int pos, View view, ViewGroup parent) {
            String link = (String) getItem(pos);

            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.cell_image_loader, null);
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ImageLoaderTestActivity.this, ViewImageActivity.class);
                        intent.putExtra("link", (String)v.getTag());
                        startActivity(intent);
                    }
                });
            }

            view.setTag(link);
            mImageLoader.loadImage(view);
            ((TextView) view.findViewById(R.id.url)).setText(link);

            return view;
        }

        public void flush() {
            MblUtils.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    mImageLoader.stop();
                }
            });
        }
    };

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListView = new ListView(this);
        mListView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mListView.setBackgroundColor(0xff000000);
        mListView.setAdapter(new Adapter(LINKS));
        setContentView(mListView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((Adapter) mListView.getAdapter()).notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ((Adapter) mListView.getAdapter()).flush();
    }
}
