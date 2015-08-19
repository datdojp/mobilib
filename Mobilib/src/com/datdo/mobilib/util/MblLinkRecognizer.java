package com.datdo.mobilib.util;

import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <pre>
 * Recognize links (email, web-url, phone) in a {@link String} and build new HTML {@link String} which wraps links in "{@literal <}a>...{@literal <}/a>".
 * This class is used together with {@link MblLinkMovementMethod}.
 * 
 * Here is sample usage:
 * <code>
 * String html = MblLinkRecognizer.getLinkRecognizedHtmlText(text);
 * textView.setText(Html.fromHtml(html));
 * textView.setMovementMethod(new MblLinkMovementMethod(new MblLinkMovementMethodCallback() {
 *     {@literal @}Override
 *     public void onLinkClicked(final String link) {
 *         // ...
 *     }
 * 
 *     {@literal @}Override
 *     public void onLongClicked() {
 *         // ...
 *     }
 * }));
 * </code>
 *
 * @see MblViewUtil#displayTextWithLinks(TextView, String)
 * </pre>
 */
public class MblLinkRecognizer {
    private static final String HTTP_PREFIX_PATTERN = "[Hh][Tt][Tt][Pp]://";
    private static final String HTTPS_PREFIX_PATTERN = "[Hh][Tt][Tt][Pp][Ss]://";
    private static final String LINK_BODY_PATTERN = "[^\\s]+";
    private static final String HTTP_PATTERN = HTTP_PREFIX_PATTERN + LINK_BODY_PATTERN;
    private static final String HTTPS_PATTERN = HTTPS_PREFIX_PATTERN + LINK_BODY_PATTERN;

    static final Pattern pattern =  Pattern.compile(Patterns.EMAIL_ADDRESS + "|" + Patterns.WEB_URL + "|" + Patterns.PHONE);
    StringBuilder result = new StringBuilder();
    StringBuilder source;

    /**
     * <pre>
     * Recognize links in a String and build new HTML String which wraps links in "<a>...</a>".
     * </pre>
     */
    public static String getLinkRecognizedHtmlText(String text) {
        return new MblLinkRecognizer(text).getResult();
    }

    private MblLinkRecognizer(String s) {
        if (s == null) s = "";
        source = new StringBuilder(s);
    }

    private String getResult() {
        while (source.length() > 0) {
            Matcher matcher = pattern.matcher(source.toString());
            if (!matcher.find()) matcher = null;
            if (replaceRegex(matcher)) continue;
            result.append(replaceChars(source.toString()));

            break;
        }
        return result.toString();
    }
    private boolean replaceRegex(Matcher m) {
        if (m == null) return false;
        String text = m.group();
        result.append(replaceChars(source.substring(0, m.start())));

        if (isEmail(text)) {
            result.append("<a " + "href=\"mailto:" + text + "\">" + text + "</a>");
        } if (isWebUrl(text)) {
            result.append("<a " + "href=\"" + lowerCaseHttpxPrefix(replaceChars(text)) + "\">" + replaceChars(text) + "</a>");
        } else if (isPhone(text)) {
            result.append("<a " + "href=\"tel:" + text + "\">" + text + "</a>");
        }
        source.delete(0, m.end());
        return true;
    }

    private String replaceChars(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(replaceChar(s.charAt(i)));
        }
        return sb.toString();
    }
    private String replaceChar(char c) {
        String r = "" + c;
        switch (c) {
        case '&':  r = "&amp;"; break;
        case '\"': r = "&quot;"; break;
        case '<':  r = "&lt;"; break;
        case '>':  r = "&gt;"; break;
        case ' ':  r = "&nbsp;"; break;
        case '\n': r = "<br>"; break;
        default: break;
        }
        return r;
    }

    static boolean isLink(String s) {
        return !TextUtils.isEmpty(s) && pattern.matcher(s).matches();
    }

    static boolean isWebUrl(String s) {
        return !TextUtils.isEmpty(s) && Patterns.WEB_URL.matcher(s).matches();
    }

    static boolean isEmail(String s) {
        return !TextUtils.isEmpty(s) && Patterns.EMAIL_ADDRESS.matcher(s).matches();
    }

    static boolean isPhone(String s) {
        return !TextUtils.isEmpty(s) && Patterns.PHONE.matcher(s).matches();
    }

    // android do not understand prefixes like "HTtP" or "hTtP"
    // so we need to make all http/https prefixes lower-case
    static String lowerCaseHttpxPrefix(String link) {
        if (TextUtils.isEmpty(link)) return link;
        String ret;
        if (link.matches(HTTP_PATTERN)) {
            ret = link.replaceFirst(HTTP_PREFIX_PATTERN, "http://");
        } else if (link.matches(HTTPS_PATTERN)) {
            ret = link.replaceFirst(HTTPS_PREFIX_PATTERN, "https://");
        } else {
            ret = link;
        }
        return ret;
    }
}
