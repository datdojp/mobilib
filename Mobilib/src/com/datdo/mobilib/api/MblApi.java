package com.datdo.mobilib.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.datdo.mobilib.util.MblUtils;

/**
 * <pre>
 * Util class for communicating with server via HTTP/HTTPS.
 * Support 2 most basic methods GET and PUT. PUT method allows file uploading.
 * Subclasses must specify cache duration for a URL by overriding {@link #getCacheDuration(String, boolean)}
 * </pre>
 */
@SuppressWarnings("deprecation")
public class MblApi {

    private static final String TAG = MblApi.class.getSimpleName();

    private static final String UTF8 = "UTF-8";
    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    /**
     * <pre>
     * Common callback for all methods.
     * </pre> 
     */
    public static interface MblApiCallback {
        /**
         * <pre>
         * Invoked on request success.
         * </pre>
         * @param statusCode HTTP status code or -1 (in case of fetching data from cache)
         * @param data result data in byte array
         */
        public void onSuccess(int statusCode, byte[] data);
        /**
         * <pre>
         * Invoked on request failure.
         * </pre>
         * @param error HTTP status code (in case status code != 2xx) or -1 (in case of unknown error)
         * @param errorMessage cause of error (returned from server or message of local exception)
         */
        public abstract void onFailure(int error, String errorMessage);
    }
    
    /**
     * <pre>
     * Send GET request asynchronously.
     * If calling thread is main thread, this method automatically creates {@link AsyncTask} and runs on that {@link AsyncTask},
     * otherwise it runs on calling thread.
     * </pre>
     * @param url starts with "http://" or "https://"
     * @param params {key,value} containing request parameters (combined with "url" to generate full URL). Value accepts String, Long, Integer, Double, Float
     * @param headerParams {key,value} containing request headers
     * @param isCacheEnabled should use cache for this request?
     * @param cacheDuration how long cache data will valid (ignore this param if isCacheEnabled is FALSE
     * @param isIgnoreSSLCertificate should ignore SSL Certificate? (in case "url" starts with "https://" only)
     * @param callback callback to receive result data
     * @param callbackHandler {@link Handler} links to thread on which callback 's method will be invoked
     */
    @SuppressWarnings("unchecked")
    public static void get(
            final String url,
            Map<String, ? extends Object> params,
            final Map<String, String> headerParams,
            final boolean isCacheEnabled,
            final long cacheDuration,
            final boolean isIgnoreSSLCertificate,
            final MblApiCallback callback,
            Handler callbackHandler) {

        Map<String, ? extends Object> paramsNoEmptyVal = getParamsIgnoreEmptyValues(params);

        final Handler fCallbackHandler;
        if (callbackHandler != null) {
            fCallbackHandler = callbackHandler;
        } else {
            fCallbackHandler = MblUtils.getMainThreadHandler();
        }

        if (!MblUtils.isEmpty(paramsNoEmptyVal)) {
            for (String key : paramsNoEmptyVal.keySet()) {
                Object val = paramsNoEmptyVal.get(key);
                if (val instanceof String) {
                    continue;
                }
                if (val instanceof Long) {
                    continue;
                }
                if (val instanceof Integer) {
                    continue;
                }
                if (val instanceof Double) {
                    continue;
                }
                if (val instanceof Float) {
                    continue;
                }

                final String message = "params " + key + " must be String, Long, Integer, Double, Float, current value is " + val.getClass().getSimpleName();
                Log.e(TAG, "GET '" + url + "': " + message);
                if (callback != null) {
                    fCallbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(-1, message);
                        }
                    });
                }
                return;
            }
        }

        final String fullUrl = generateGetMethodFullUrl(url, paramsNoEmptyVal);

        MblUtils.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {

                MblCache existingCache = null;
                if (isCacheEnabled) {
                    existingCache = MblCache.get(fullUrl);
                    boolean shouldReadFromCache =
                            existingCache != null &&
                            (   !MblUtils.isNetworkConnected() ||
                                    System.currentTimeMillis() - existingCache.getDate() <= cacheDuration    );
                    if (shouldReadFromCache) {
                        try {
                            final byte[] data = MblUtils.readCacheFile(existingCache.getFileName());
                            if (data != null) {
                                if (callback != null) {
                                    fCallbackHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            callback.onSuccess(-1, data);
                                        }
                                    });
                                }

                                return;
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Cache not exist", e);
                        }
                    }
                }

                try {

                    HttpClient httpClient = getHttpClient(fullUrl, isIgnoreSSLCertificate);
                    HttpContext httpContext = new BasicHttpContext();
                    HttpGet httpGet = new HttpGet(fullUrl);

                    httpGet.setHeaders(getHeaderArray(headerParams));

                    final HttpResponse response = httpClient.execute(httpGet, httpContext);

                    final int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode < 200 || statusCode > 299) {
                        if (callback != null) {
                            fCallbackHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFailure(statusCode, response.getStatusLine().getReasonPhrase());
                                }
                            });
                        }

                        return;
                    }

                    final byte[] data = EntityUtils.toByteArray(response.getEntity());

                    if (isCacheEnabled) {
                        saveCache(existingCache, fullUrl, data);
                    }

                    if (callback != null) {
                        fCallbackHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(statusCode, data);
                            }
                        });
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "GET request failed due to unexpected exception", e);
                    if (callback != null) {
                        fCallbackHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(-1, "Unexpected exception: " + e.getMessage());
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * <pre>
     * Get absolute path to cache file of a URL.
     * </pre>
     * @param url starts with "http://" or "https://"
     * @param params {key,value} containing request parameters (combined with "url" to generate full URL)
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String getCacheFilePath(String url, Map<String, String> params) {
        String fullUrl = generateGetMethodFullUrl(url, getParamsIgnoreEmptyValues(params));
        MblCache existingCache = MblCache.get(fullUrl);
        if (existingCache == null || existingCache.getFileName() == null) {
            return null;
        }
        String path = MblUtils.getCacheAsbPath(existingCache.getFileName());
        File file = new File(path);
        if (!file.exists() || file.length() == 0) {
            return null;
        }
        return path;
    }

    /**
     * <pre>
     * Send POST request asynchronously.
     * If calling thread is main thread, this method automatically creates {@link AsyncTask} and runs on that {@link AsyncTask},
     * otherwise it runs on calling thread.
     * </pre>
     * @param url starts with "http://" or "https://"
     * @param params {key,value} containing request parameters. Value accepts String, Long, Integer, Double, Float, InputStream or File
     * @param headerParams {key,value} containing request headers
     * @param isIgnoreSSLCertificate should ignore SSL Certificate? (in case "url" starts with "https://" only)
     * @param callback callback to receive result data
     * @param callbackHandler {@link Handler} links to thread on which callback 's method will be invoked
     */
    public static void post(
            String url,
            Map<String, ? extends Object> params,
            Map<String, String> headerParams,
            boolean isIgnoreSSLCertificate,
            MblApiCallback callback,
            Handler callbackHandler) {

        sendRequestWithBody(
                Method.POST,
                url,
                params,
                headerParams,
                isIgnoreSSLCertificate,
                callback,
                callbackHandler);
    }

    private static enum Method {
        POST,
        PUT,
        DELETE;

        public HttpEntityEnclosingRequestBase getHttpRequest(String url) {
            if (this == POST) {
                return new HttpPost(url);
            }
            if (this == PUT) {
                return new HttpPut(url);
            }
            if (this == DELETE) {
                return new HttpDeleteWithBody(url);
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void sendRequestWithBody(
            final Method method,
            final String url,
            Map<String, ? extends Object> params,
            final Map<String, String> headerParams,
            final boolean isIgnoreSSLCertificate,
            final MblApiCallback callback,
            Handler callbackHandler) {

        Assert.assertNotNull(method);

        final Map<String, ? extends Object> paramsNoEmptyVal = getParamsIgnoreEmptyValues(params);

        final Handler fCallbackHandler;
        if (callbackHandler != null) {
            fCallbackHandler = callbackHandler;
        } else {
            fCallbackHandler = MblUtils.getMainThreadHandler();
        }

        boolean isMultipart = false;
        if (!MblUtils.isEmpty(paramsNoEmptyVal)) {
            for (String key : paramsNoEmptyVal.keySet()) {
                Object val = paramsNoEmptyVal.get(key);
                if (val instanceof InputStream) {
                    isMultipart = true;
                    continue;
                }
                if (val instanceof File) {
                    isMultipart = true;
                    continue;
                }
                if (val instanceof String) {
                    continue;
                }
                if (val instanceof Long) {
                    continue;
                }
                if (val instanceof Integer) {
                    continue;
                }
                if (val instanceof Double) {
                    continue;
                }
                if (val instanceof Float) {
                    continue;
                }

                final String message = "params " + key + " must be String, Long, Integer, Double, Float, InputStream or File, current value is " + val.getClass().getSimpleName();
                Log.e(TAG, method.name() + " '" + url + "': " + message);
                if (callback != null) {
                    fCallbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(-1, message);
                        }
                    });
                }
                return;
            }
        }
        final boolean fIsMultipart = isMultipart;

        MblUtils.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {

                try {
                    HttpClient httpClient = getHttpClient(url, isIgnoreSSLCertificate);
                    HttpContext httpContext = new BasicHttpContext();
                    HttpEntityEnclosingRequestBase httpRequest = method.getHttpRequest(url);

                    if (!MblUtils.isEmpty(paramsNoEmptyVal)) {
                        if (fIsMultipart) {
                            MultipartEntity multipartContent = new MultipartEntity();
                            for (String key : paramsNoEmptyVal.keySet()) {
                                Object val = paramsNoEmptyVal.get(key);
                                if (val instanceof InputStream) {
                                    multipartContent.addPart(key, new InputStreamBody((InputStream)val, key));
                                } else if (val instanceof File) {
                                    File file = (File)val;
                                    FileInputStream fis = new FileInputStream(file);
                                    multipartContent.addPart(key, new InputStreamBody(fis, file.getName()));
                                } else if (val instanceof String){
                                    multipartContent.addPart(key, new StringBody((String)val, CHARSET_UTF8));
                                } else {
                                    multipartContent.addPart(key, new StringBody(String.valueOf(val), CHARSET_UTF8));
                                }
                            }
                            httpRequest.setEntity(multipartContent);
                        } else {
                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                            for (String key : paramsNoEmptyVal.keySet()) {
                                nameValuePairs.add(new BasicNameValuePair(key, paramsNoEmptyVal.get(key).toString()));
                            }
                            httpRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs, UTF8));
                        }
                    }

                    httpRequest.setHeaders(getHeaderArray(headerParams));

                    final HttpResponse response = httpClient.execute(httpRequest, httpContext);

                    final int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode < 200 || statusCode > 299) {
                        if (callback != null) {
                            fCallbackHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFailure(
                                            statusCode,
                                            response.getStatusLine().getReasonPhrase());
                                }
                            });
                        }
                        return;
                    }

                    final byte[] data = EntityUtils.toByteArray(response.getEntity());

                    if (callback != null) {
                        fCallbackHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(statusCode, data);
                            }
                        });
                    }

                } catch (final Exception e) {
                    Log.e(TAG, method.name() + " request failed due to unexpected exception", e);
                    if (callback != null) {
                        fCallbackHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(-1, "Unexpected exception: " + e.getMessage());
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * <pre>
     * Send DELETE request asynchronously.
     * If calling thread is main thread, this method automatically creates {@link AsyncTask} and runs on that {@link AsyncTask},
     * otherwise it runs on calling thread.
     * </pre>
     * @param url starts with "http://" or "https://"
     * @param params {key,value} containing request parameters. Value accepts String, Long, Integer, Double, Float, InputStream or File
     * @param headerParams {key,value} containing request headers
     * @param isIgnoreSSLCertificate should ignore SSL Certificate? (in case "url" starts with "https://" only)
     * @param callback callback to receive result data
     * @param callbackHandler {@link Handler} links to thread on which callback 's method will be invoked
     */
    public static void delete(
            String url,
            Map<String, String> params,
            Map<String, String> headerParams,
            boolean isIgnoreSSLCertificate,
            MblApiCallback callback,
            Handler callbackHandler) {

        sendRequestWithBody(
                Method.DELETE,
                url,
                params,
                headerParams,
                isIgnoreSSLCertificate,
                callback,
                callbackHandler);
    }

    /**
     * <pre>
     * Send PUT request asynchronously.
     * If calling thread is main thread, this method automatically creates {@link AsyncTask} and runs on that {@link AsyncTask},
     * otherwise it runs on calling thread.
     * </pre>
     * @param url starts with "http://" or "https://"
     * @param params {key,value} containing request parameters. Value accepts String, Long, Integer, Double, Float, InputStream or File
     * @param headerParams {key,value} containing request headers
     * @param isIgnoreSSLCertificate should ignore SSL Certificate? (in case "url" starts with "https://" only)
     * @param callback callback to receive result data
     * @param callbackHandler {@link Handler} links to thread on which callback 's method will be invoked
     */
    public static void put(
            String url,
            Map<String, ? extends Object> params,
            Map<String, String> headerParams,
            boolean isIgnoreSSLCertificate,
            MblApiCallback callback,
            Handler callbackHandler) {

        sendRequestWithBody(
                Method.PUT,
                url,
                params,
                headerParams,
                isIgnoreSSLCertificate,
                callback,
                callbackHandler);
    }

    @SuppressWarnings("unused")
    private static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        @Override
        public String getMethod() {
            return "DELETE";
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }

    private static HttpClient getHttpClient(String url, boolean ignoreSSLCertificate) {
        if (MblSSLCertificateUtils.isHttpsUrl(url) && ignoreSSLCertificate) {
            return MblSSLCertificateUtils.getHttpClientIgnoreSSLCertificate();
        } else {
            return new DefaultHttpClient();
        }
    }

    private static void saveCache(MblCache existingCache, String fullUrl, byte[] data) {
        try {
            MblCache cacheToSave;
            if (existingCache == null) {
                cacheToSave = new MblCache();
                cacheToSave.setKey(fullUrl);
                cacheToSave.setDate(System.currentTimeMillis());
                MblCache.insert(cacheToSave);
            } else {
                cacheToSave = existingCache;
                cacheToSave.setDate(System.currentTimeMillis());
                MblCache.update(cacheToSave);
            }
            MblUtils.saveCacheFile(data, cacheToSave.getFileName());
        } catch (Exception e) {
            Log.e(TAG, "Failed to cache url: " + fullUrl, e);
        }
    }

    private static String generateGetMethodFullUrl(String url, Map<String, ? extends Object> params) {
        if (!MblUtils.isEmpty(params)) {
            Uri.Builder builder = Uri.parse(url).buildUpon();
            for (String key : params.keySet()) {
                builder.appendQueryParameter(key, params.get(key).toString());
            }
            return builder.build().toString();
        } else {
            return url;
        }
    }

    private static Header[] getHeaderArray(Map<String, String> headerParams) {

        Header[] headers = null;

        if (!MblUtils.isEmpty(headerParams)) {
            headers = new Header[headerParams.keySet().size()];
            int i = 0;
            for (final String key : headerParams.keySet()) {
                final String val = headerParams.get(key);
                headers[i++] = new Header() {

                    @Override
                    public HeaderElement[] getElements() throws ParseException { return null; }

                    @Override
                    public String getName() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return val;
                    }
                };
            }
        }

        return headers;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map getParamsIgnoreEmptyValues(Map params) {
        if (MblUtils.isEmpty(params)) {
            return params;
        }
        Map ret = new HashMap();
        for (Object key : params.keySet()) {
            Object val = params.get(key);
            if (!shouldIgnoreParamValue(val)) {
                ret.put(key, val);
            }
        }
        return ret;
    }

    private static boolean shouldIgnoreParamValue(Object val) {
        if (val == null) {
            return true;
        }
        if (val instanceof String && MblUtils.isEmpty((String)val)) {
            return true;
        }
        return false;
    }
}