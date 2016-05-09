package com.datdo.mobilib.api;

import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.datdo.mobilib.api.MblRequest.MblStatusCodeValidator;
import com.datdo.mobilib.cache.MblDatabaseCache;
import com.datdo.mobilib.util.MblUtils;

import junit.framework.Assert;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * Util class for communicating with server via HTTP/HTTPS
 *
 * Sample code:
 * {@code
 * MblApi.run(new MblRequest()
 *      .setMethod(MblApi.Method.GET)
 *      .setUrl("http://your.website.com/img/logo.png")
 *      .setParams(
 *          "user-name", username,
 *          "avatar", new File(uploadedFilePath)
 *      )
 *      .setHeaderParams("access-token", accessToken)
 *      .setVerifySSL(true)
 *      .setCacheDuration(1000l * 60l * 60l * 24l)
 *      .setCallback(new MblApi.MblApiCallback() {
 *
 *          @Override
 *          public void onSuccess(int statusCode, byte[] data) {
 *              // ...
 *          };
 *
 *          @Override
 *          public void onFailure(int error, String errorMessage) {
 *              // ...
 *          }
 *      }));
 * }
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
         */
        public void onSuccess(MblResponse response);
        /**
         * <pre>
         * Invoked on request failure.
         * </pre>
         */
        public abstract void onFailure(MblResponse response);
    }

    /**
     * <pre>
     * General method to run an arbitrary request.
     * </pre>
     */
    public static void run(final MblRequest request) {
        if (request == null) {
            throw new RuntimeException("request must not be NULL");
        }
        if (request.getUrl() == null || request.getMethod() == null) {
            throw new RuntimeException("request.url and request.method must not be NULL");
        }

        final MblApiCallback callback;
        if (request.getCallback() != null && request.getTimeout() > 0) {
            final long requestedAt = System.currentTimeMillis();
            final Runnable timeout = new Runnable() {
                @Override
                public void run() {
                    MblResponse response = new MblResponse();
                    response.setRequest(request);
                    response.setStatusCode(-1);
                    response.setStatusCodeReason("Time out");
                    request.getCallback().onFailure(response);
                }
            };
            MblUtils.getMainThreadHandler().postDelayed(timeout, request.getTimeout());
            callback = new MblApiCallback() {

                boolean isExpired() {
                    return System.currentTimeMillis() - requestedAt > request.getTimeout();
                }

                @Override
                public void onSuccess(MblResponse response) {
                    MblUtils.getMainThreadHandler().removeCallbacks(timeout);
                    if (isExpired()) {
                        return;
                    }
                    request.getCallback().onSuccess(response);
                }

                @Override
                public void onFailure(MblResponse response) {
                    MblUtils.getMainThreadHandler().removeCallbacks(timeout);
                    if (isExpired()) {
                        return;
                    }
                    request.getCallback().onFailure(response);
                }
            };
        } else {
            callback = request.getCallback();
        }

        if (request.getMethod() == Method.GET) {
            get(    request.getUrl(),
                    request.getParams(),
                    request.getHeaderParams(),
                    request.getCacheDuration(),
                    !request.isVerifySSL(),
                    callback,
                    request.getCallbackHandler(),
                    request.getStatusCodeValidator(),
                    request.isRedirectEnabled(),
                    request.isNotReturnByteArrayData(),
                    request);
        } else {
            sendRequestWithBody(
                    request.getMethod(),
                    request.getUrl(),
                    request.getParams(),
                    request.getHeaderParams(),
                    !request.isVerifySSL(),
                    callback,
                    request.getCallbackHandler(),
                    request.getStatusCodeValidator(),
                    request.getData(),
                    request.isRedirectEnabled(),
                    request);
        }
    }

    @SuppressWarnings("unchecked")
    private static void get(
            final String url,
            Map<String, ? extends Object> params,
            final Map<String, String> headerParams,
            final long cacheDuration,
            final boolean isIgnoreSSLCertificate,
            final MblApiCallback callback,
            Handler callbackHandler,
            final MblStatusCodeValidator statusCodeValidator,
            final boolean redirectEnabled,
            final boolean notReturnByteArrayData,
            final MblRequest request) {

        final boolean isCacheEnabled = cacheDuration > 0;

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
                    MblUtils.executeOnHandlerThread(fCallbackHandler, new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(new MblResponse()
                                    .setRequest(request)
                                    .setStatusCode(-1)
                                    .setStatusCodeReason(message));
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

                MblDatabaseCache existingCache = null;
                if (isCacheEnabled) {
                    existingCache = MblDatabaseCache.get(fullUrl);
                    boolean shouldReadFromCache =
                            existingCache != null &&
                            MblUtils.isValidFile(MblUtils.getCacheAsbPath(getCacheFileName(existingCache))) &&
                            (   !MblUtils.isNetworkConnected() ||
                                    System.currentTimeMillis() - existingCache.getDate() <= cacheDuration    );
                    if (shouldReadFromCache) {
                        try {
                            final byte[] data;
                            if (!notReturnByteArrayData) {
                                data = MblUtils.readCacheFile(getCacheFileName(existingCache));
                            } else {
                                data = null;
                            }
                            if (callback != null) {
                                MblUtils.executeOnHandlerThread(fCallbackHandler, new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onSuccess(new MblResponse()
                                                .setRequest(request)
                                                .setStatusCode(-1)
                                                .setData(data));
                                    }
                                });
                            }

                            return;
                        } catch (IOException e) {
                            Log.e(TAG, "Cache not exist", e);
                        }
                    }
                }

                try {

                    HttpClient httpClient = getHttpClient(fullUrl, isIgnoreSSLCertificate);
                    HttpContext httpContext = new BasicHttpContext();
                    HttpGet httpGet = new HttpGet(fullUrl);
                    if (!redirectEnabled) {
                        disableRedirect(httpGet);
                    }

                    httpGet.setHeaders(getHeaderArray(headerParams));

                    final HttpResponse response = httpClient.execute(httpGet, httpContext);

                    final int statusCode = response.getStatusLine().getStatusCode();
                    final String statusCodeReason = response.getStatusLine().getReasonPhrase();
                    final Map<String, String> headers = new HashMap<String, String>();
                    for (Header h : response.getAllHeaders()) {
                        headers.put(h.getName(), h.getValue());
                    }
                    final byte[] data;
                    if (!notReturnByteArrayData) {
                        data = EntityUtils.toByteArray(response.getEntity());
                    } else {
                        data = null;
                    }

                    if (!statusCodeValidator.isSuccess(statusCode)) {
                        if (callback != null) {
                            MblUtils.executeOnHandlerThread(fCallbackHandler, new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFailure(new MblResponse()
                                            .setRequest(request)
                                            .setStatusCode(statusCode)
                                            .setStatusCodeReason(statusCodeReason)
                                            .setHeaders(headers)
                                            .setData(data));
                                }
                            });
                        }

                        return;
                    }

                    if (isCacheEnabled) {
                        if (!notReturnByteArrayData) {
                            saveCache(fullUrl, data);
                        } else {
                            saveCache(fullUrl, response.getEntity());
                        }
                    }

                    if (callback != null) {
                        MblUtils.executeOnHandlerThread(fCallbackHandler, new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(new MblResponse()
                                        .setRequest(request)
                                        .setStatusCode(statusCode)
                                        .setStatusCodeReason(statusCodeReason)
                                        .setHeaders(headers)
                                        .setData(data));
                            }
                        });
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "GET request failed due to unexpected exception", e);
                    if (callback != null) {
                        MblUtils.executeOnHandlerThread(fCallbackHandler, new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(new MblResponse()
                                        .setRequest(request)
                                        .setStatusCode(-1)
                                        .setStatusCodeReason("Unexpected exception: " + e.getMessage()));
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
    public static String getCacheFilePath(String url, Map<String, ? extends Object> params) {
        String fullUrl = generateGetMethodFullUrl(url, getParamsIgnoreEmptyValues(params));
        MblDatabaseCache existingCache = MblDatabaseCache.get(fullUrl);
        if (existingCache != null) {
            String cacheFileName = getCacheFileName(existingCache);
            if (!MblUtils.isEmpty(cacheFileName)) {
                String path = MblUtils.getCacheAsbPath(cacheFileName);
                File file = new File(path);
                if (file.exists() && file.length() > 0) {
                    return path;
                }
            }
        }
        return null;
    }

    public static enum Method {
        GET,
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
            Handler callbackHandler,
            final MblStatusCodeValidator statusCodeValidator,
            final String data,
            final boolean redirectEnabled,
            final MblRequest request) {

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
                    MblUtils.executeOnHandlerThread(fCallbackHandler, new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(new MblResponse()
                                    .setRequest(request)
                                    .setStatusCode(-1)
                                    .setStatusCodeReason(message));
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
                    if (!redirectEnabled) {
                        disableRedirect(httpRequest);
                    }

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
                    } else if (!MblUtils.isEmpty(data)) {
                        httpRequest.setEntity(new StringEntity(data, UTF8));
                    }

                    httpRequest.setHeaders(getHeaderArray(headerParams));

                    final HttpResponse response = httpClient.execute(httpRequest, httpContext);

                    final int statusCode = response.getStatusLine().getStatusCode();
                    final String statusCodeReason = response.getStatusLine().getReasonPhrase();
                    final Map<String, String> headers = new HashMap<String, String>();
                    for (Header h : response.getAllHeaders()) {
                        headers.put(h.getName(), h.getValue());
                    }
                    final byte[] data = EntityUtils.toByteArray(response.getEntity());

                    if (!statusCodeValidator.isSuccess(statusCode)) {
                        if (callback != null) {
                            MblUtils.executeOnHandlerThread(fCallbackHandler, new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFailure(new MblResponse()
                                            .setRequest(request)
                                            .setStatusCode(statusCode)
                                            .setStatusCodeReason(statusCodeReason)
                                            .setHeaders(headers)
                                            .setData(data));
                                }
                            });
                        }
                        return;
                    }


                    if (callback != null) {
                        MblUtils.executeOnHandlerThread(fCallbackHandler, new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(new MblResponse()
                                        .setRequest(request)
                                        .setStatusCode(statusCode)
                                        .setStatusCodeReason(statusCodeReason)
                                        .setHeaders(headers)
                                        .setData(data));
                            }
                        });
                    }

                } catch (final Exception e) {
                    Log.e(TAG, method.name() + " request failed due to unexpected exception", e);
                    if (callback != null) {
                        MblUtils.executeOnHandlerThread(fCallbackHandler, new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailure(new MblResponse()
                                        .setRequest(request)
                                        .setStatusCode(-1)
                                        .setStatusCodeReason("Unexpected exception: " + e.getMessage()));
                            }
                        });
                    }
                }
            }
        });
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

    private static void saveCache(String fullUrl, byte[] data) {
        try {
            MblDatabaseCache c = new MblDatabaseCache(fullUrl, System.currentTimeMillis());
            MblDatabaseCache.upsert(c);
            MblUtils.saveCacheFile(data, getCacheFileName(c));
        } catch (Exception e) {
            Log.e(TAG, "Failed to cache url: " + fullUrl, e);
        }
    }

    private static void saveCache(String fullUrl, HttpEntity entity) {
        try {
            MblDatabaseCache c = new MblDatabaseCache(fullUrl, System.currentTimeMillis());
            MblDatabaseCache.upsert(c);
            String cachePath = MblUtils.getCacheAsbPath(getCacheFileName(c));
            entity.writeTo(new FileOutputStream(cachePath));
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

    /**
     * <pre>
     * Clear cache of all GET requests.
     * </pre>
     */
    public static void clearCache() {

        // delete cache file
        List<MblDatabaseCache> caches = MblDatabaseCache.getAll();
        for (MblDatabaseCache c : caches) {
            String path = MblUtils.getCacheAsbPath(getCacheFileName(c));
            if (!MblUtils.isEmpty(path)) {
                new File(path).delete();
            }
        }

        // delete cache records
        MblDatabaseCache.deleteAll();
    }

    private static String getCacheFileName(MblDatabaseCache c) {
        if (c != null && !MblUtils.isEmpty(c.getKey())) {
            return MblUtils.md5(c.getKey());
        } else {
            return null;
        }
    }

    private static void disableRedirect(HttpRequest httpRequest) {
        HttpParams params = new BasicHttpParams();
        params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        httpRequest.setParams(params);
    }
}