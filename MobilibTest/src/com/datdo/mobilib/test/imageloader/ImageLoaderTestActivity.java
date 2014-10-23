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

import com.datdo.mobilib.api.MblApi;
import com.datdo.mobilib.api.MblApi.MblApiCallback;
import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.base.MblBaseAdapter;
import com.datdo.mobilib.test.R;
import com.datdo.mobilib.util.MblImageLoader;
import com.datdo.mobilib.util.MblUtils;

public class ImageLoaderTestActivity extends MblBaseActivity {

    private static final String[] LINKS = new String[] {
        "http://cupcakepedia.com/wp-content/uploads/2013/12/cute-cat-love2.jpg",
        "http://images.wisegeek.com/young-calico-cat.jpg",
        "http://2.bp.blogspot.com/-AM6OorTilnA/T7bEVEefD3I/AAAAAAAAGy4/m5Y-u7ohe6U/s640/Beautiful+White+Cute+Cat+Pictures+%253A+Photos+%253A+Wallpapers+11.jpg",
        "http://www.vetprofessionals.com/catprofessional/images/home-cat.jpg",
        "https://www.petfinder.com/wp-content/uploads/2012/11/99059361-choose-cat-litter-632x475.jpg",
        "http://scienceblogs.com/gregladen/files/2012/12/Beautifull-cat-cats-14749885-1600-1200-590x442.jpg",
        "http://www.globalpost.com/sites/default/files/imagecache/gp3_slideshow_large/photos/2013-October/germany_cat.jpg",
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
        "http://news.distractify.com/wp-content/uploads/2014/01/new-userguide-image.jpg",
        "http://hemet.animalsfirst4u.org/puppies.jpg",
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

        private MblImageLoader<String> mImageLoader = new MblImageLoader<String>() {

            @Override
            protected String getItemId(String item) {
                return item;
            }

            @Override
            protected int getDefaultImageResource(String item) {
                return R.drawable._default;
            }

            @Override
            protected int getErrorImageResource(String item) {
                return R.drawable.error;
            }

            @Override
            protected int getLoadingIndicatorImageResource(String item) {
                return R.drawable.loading;
            }

            @Override
            protected boolean shouldLoadImageForItem(String item) {
                return item.startsWith("http");
            }

            @Override
            protected ImageView getImageViewFromView(View view) {
                return (ImageView) view;
            }

            @Override
            protected String getItemBoundWithView(View view) {
                return ((Holder) view.getTag()).link;
            }

            @Override
            protected void retrieveImage(String item, final MblRetrieveImageCallback cb) {
                MblApi.get(item, null, null, true, Long.MAX_VALUE, true, new MblApiCallback() {

                    @Override
                    public void onSuccess(int statusCode, byte[] data) {
                        cb.onRetrievedByteArray(data);
                    };

                    @Override
                    public void onFailure(int error, String errorMessage) {
                        cb.onRetrievedByteArray(null);
                    }
                }, null);
            }
        };

        @Override
        public View getView(int pos, View view, ViewGroup parent) {
            Holder holder;
            String link = (String) getItem(pos);

            if (view == null) {
                ImageView iv = new ImageView(ImageLoaderTestActivity.this);
                iv.setLayoutParams(new AbsListView.LayoutParams(
                        AbsListView.LayoutParams.MATCH_PARENT,
                        MblUtils.pxFromDp(150)));
                iv.setScaleType(ScaleType.FIT_CENTER);
                view = iv;

                holder = new Holder();
                view.setTag(holder);

                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ImageLoaderTestActivity.this, ViewImageActivity.class);
                        intent.putExtra("link", ((Holder)v.getTag()).link);
                        startActivity(intent);
                    }
                });
            } else {
                holder = (Holder) view.getTag();
            }

            holder.link = link; 
            mImageLoader.loadImage(view);

            return view;
        }

        class Holder {
            String link;
        }

        public void flush() {
            MblUtils.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    mImageLoader.stop();

                    for (int i = 0; i < mListView.getChildCount(); i++) {
                        ImageView iv = (ImageView) mListView.getChildAt(i);
                        iv.setImageBitmap(null);
                    }
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
