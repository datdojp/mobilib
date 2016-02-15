package com.datdo.mobilib.test.urlrecognizer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Html;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.datdo.mobilib.base.MblBaseActivity;
import com.datdo.mobilib.util.MblLinkMovementMethod;
import com.datdo.mobilib.util.MblLinkMovementMethod.MblLinkMovementMethodCallback;
import com.datdo.mobilib.util.MblLinkRecognizer;
import com.datdo.mobilib.util.MblUtils;

public class UrlRecognizerTestActivity extends MblBaseActivity {

    private static final Object[][] TEXTS = new Object[][] {
        new Object[] { "HTtP://google.com", true },
        new Object[] { "http://google.com", true },
        new Object[] { "http://google.com<script>alert(\"hello\");</script>", true },
        new Object[] { "http://", false },
        new Object[] { "http://a", true },
        new Object[] { "http://google.com&something", true },
        new Object[] { "http://google.com</a><a>something", true },
        new Object[] { "Https://google.com.Http?id=Http", true },
        new Object[] { "href=\"http://google.com\"", "http://google.com\""},
        new Object[] { "   http://google.com", "http://google.com"},
        new Object[] { "a http://google.com", "http://google.com"},
        new Object[] { "ahttp://google.com", "http://google.com"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout ll = new LinearLayout(this);
        ll.setLayoutParams(new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        ll.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        scrollView.addView(ll);
        setContentView(scrollView);

        for (Object[] item : TEXTS) {
            // title
            TextView titleTextView = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = MblUtils.pxFromDp(20);
            titleTextView.setLayoutParams(lp);
            if (item[1] instanceof Boolean) {
                boolean isLink = (Boolean) item[1];
                if (isLink) {
                    titleTextView.setText("This is A LINK");
                } else {
                    titleTextView.setText("This is NOT A LINK");
                }
            } else if (item[1] instanceof String) {
                titleTextView.setText("/" + item[1] + "/ part is A LINK");
            }

            // link
            String link = (String) item[0];
            final TextView linkTextView = new TextView(this);
            linkTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            String html = MblLinkRecognizer.getLinkRecognizedHtmlText(link, new MblLinkRecognizer.MblOptions());
            linkTextView.setText(Html.fromHtml(html));
            linkTextView.setTag(link);
            linkTextView.setMovementMethod(new MblLinkMovementMethod(new MblLinkMovementMethodCallback() {
                @Override
                public void onLinkClicked(final String link) {
                    new AlertDialog.Builder(UrlRecognizerTestActivity.this)
                    .setMessage("Open link in browser? \n" + link)
                    .setPositiveButton("YES", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            MblUtils.openWebUrl(link);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                }

                @Override
                public void onLongClicked() {
                    String link = (String) linkTextView.getTag();
                    MblUtils.copyTextToClipboard(link);
                    MblUtils.showToast("Link is copied to Clipboard", Toast.LENGTH_SHORT);
                }
            }));

            // add to layout
            ll.addView(titleTextView);
            ll.addView(linkTextView);
        }
    }
}
