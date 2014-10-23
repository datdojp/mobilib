Mobilib
=======
Mobilib is an Android library which resolves multiple inconveniences caused by missing features in Android platform. In addition, Mobilib provides many common utility classes and methods which are used frequently in almost every project.

Author
------
Dat Do

datdvt@gmail.com

Version
-------
1.0

Features
--------
  - Event API to register/unregister an event listener and to post an event to all listeners (observer pattern). It is thread-safe and immune from memory leak threat.
  - Detect events which are not supported by pure Android via Event API: app goes to background, app goes to foreground, keyboard shown, keyboard hidden, network on, network off.
  - HTTP/HTTPS API to send GET/POST/PUT/DELETE request and receive response. It is thread-safe and provides almost all you need for a typical HTTP/HTTPS request: parameters (InputStream and File parameter is also supported for uploading), headers, caching, ignore SSL Certificate (for Dev environment).
  - Smart image loader to load and display images for child views of an AdapterView.
  - Extra library to get image input from user: pick images from gallery, take image, crop image (MobilibImageInput).
  - And lots of utility classes and methods.

Source code
-----------
  - `Mobilib` project is the main source code.
  - `MobilibImageInput` project is extra library to get image input from user.
  - `MobilibTest` project is sample code to test and also to understand how to use Mobilib.

How to plug it into your app?
-----------------------------
Due to the fact that Mobilib provides some events which are not supported by pure Android, it requires you to work a little bit to use.

**Your Application class must extend MblApplication**
```java
public class MyApplication extends MblBaseApplication {
    ...
}
```

**All your activities must extend one of base activities: MblBaseActivity, MblBaseFragmentActivity, MblBaseActionBarActivity**
```java
public class MyActivity extends MblActivity {
    ...
}

public class MyFragmentActivity extends MblBaseFragmentActivity {
    ...
}

public class MyActionBarActivity extends MblBaseActionBarActivity {
    ...
}

```
Almost all activities in your app extend one of **Activity, FragmentActivity, ActionBarActivity**. All you need to do is replacing them with **MblBaseActivity, MblBaseFragmentActivity, MblBaseActionBarActivity**.

Let me know if you have an activity whose super class is not in above list. I will add a new one into Mobilib as soon as possible :)

Event API
---------
***Listen for events***

Event is simply a String. Check **MblCommonEvents** class for events those are not supported in Android platform.
```java
ORIENTATION_CHANGED
NETWORK_ON
NETWORK_OFF
KEYBOARD_SHOWN
KEYBOARD_HIDDEN
GO_TO_BACKGROUND
GO_TO_FOREGROUND
ACTIVITY_RESUMED
```
Note that **onEvent()** method is always executed in Main Thread regardless of which thread the event was posted from.

Method **MblEventCenter#getArgAt()** is recommended when retrieving attached data to prevent **NullPointerException** and **IndexOutOfBoundsException**

```java
public class MyActivity extends MblBaseActivity implements MblEventListener {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {

    	...

    	MblEventCenter.addListener(this, new String[] {
    			"my_event_name",
                MblCommonEvents.KEYBOARD_HIDDEN,
                MblCommonEvents.KEYBOARD_SHOWN
        });
    }

    @Override
    protected void onDestroy() {
        // you don't need to call MblEventCenter#removeListenerFromAllEvents() to unregister this activity.
        super.onDestroy();
    }

    @Override
    public void onEvent(Object sender, String name, Object... args) {
    	if ("my_event_name".equals(name)) {
    		Object attachedData = MblEventCenter.getArgAt(0, args);
    		...
        } else if (MblCommonEvents.KEYBOARD_HIDDEN.equals(name)) {
            ...
        } else if (MblCommonEvents.KEYBOARD_SHOWN.equals(name)) {
            ...
        }
    }
}
```

Observer pattern is one of the most potential causes of memory-leak, especially in case you register a listener and forget to unregister it. To prevent such problem, Event API only keeps a WeakReference to listener. Therefore you don't need to unregister a listener when it is no longer used.

If you want to create an anonymous listener, use **MblStrongEventListener** instead of **MblEventListener**, otherwise the listener will be destroyed by Garbage Collector. Read Javadoc of **MblStrongEventListener** for more details.

For **MblStrongEventListener**, you have to unregister it by calling ***terminate()*** method when it is no longer used.

```java
MblEventCenter.addListener(new MblStrongEventListener() {
    @Override
    public void onEvent(Object sender, String name, Object... args) {
        if (MblCommonEvents.GO_TO_BACKGROUND == name) {
            ...
        } else if (MblCommonEvents.GO_TO_FOREGROUND == name) {
            ...
        }

        if (shouldStopListening) {
            terminate(); // unregister this anonymous listener
        }
    }
}, new String[] {
        MblCommonEvents.GO_TO_BACKGROUND,
        MblCommonEvents.GO_TO_FOREGROUND
});
```

***Post an event***

You can post event with/without sender and attached data but name is mandatory. Number of attached data objects is unlimited.

```java
MblEventCenter.postEvent(
	this,			// sender
	"my_event_name" // name
);
MblEventCenter.postEvent(
	null,					// sender
	"event_name_with_args",	// name
	data1, data2			// attached data
);
```

HTTP/HTTPS API
--------------
HTTP/HTTPS API is light-weight and straightforward.

***Download an image***
```java
MblApi.get(
	"http://your.website.com/img/logo.png", // url
	null, 					// no parameter required
	null, 					// no header required
	true, 					// enable cache
	1000l * 60l * 60l * 24l,// cache expires within 1 day
	true,					// ignore invalid SSL Certificate
    new MblApiCallback() {

        @Override
        public void onSuccess(int statusCode, byte[] data) {
            ...
        };

        @Override
        public void onFailure(int error, String errorMessage) {
            ...
        }
    },
    MblUtils.getMainThreadHandler()
);
```

***Upload file***
```java
Map<String, Object> params = new HashMap<String, Object>();
params.put("user-name", username);
params.put("avatar", new File(uploadedFilePath));

Map<String, String> headers = new HashMap<String, String>();
headers.put("access-token", accessToken);

MblApi.post(
	"https://your.website.com/v1/upload/avatar",
	params,
	headers,
	false,  // must check SSL Certificate
    new MblApiCallback() {

		@Override
		public void onSuccess(int statusCode, String data) {
			...
		}

		@Override
		public void onFailure(int error, String errorMessage) {
			...
		}
	},
	MblUtils.getMainThreadHandler()
)
```

Smart image loader
-----------------
Subclasses of AdapterView (like ListView, GridView, ...) are used very frequently in Android apps. Most of them contains at least one image for each item and we need to handle some thing as following:

  - Scale bitmap to match sizes of ImageView.
  - Cache loaded bitmaps in LruCache so that we don't need to reload previous images when scrolling back to top.
  - An important problem: in case that AdapterView has many items and user scrolls to bottom, lots of bitmap will be loaded, which takes much time. Therefore, user has to wait very long for currently displayed image being fully loaded.

All problems above are solved by using **MblImageLoader**.

**MblImageLoader** is an implementation of Template Method pattern. You need to override some abstract methods to make it work.

```java
public class UserAdapter extends BaseAdapter<User> {

	private MblImageLoader<User> mImageLoader = new MblImageLoader<User>() {

		@Override
	    protected boolean shouldLoadImageForItem(User user) {
	        return user.getAvatar() != null;
	    }

	    @Override
	    protected int getDefaultImageResource(User user) {
	        return R.drawable.default_img;
	    }

	    @Override
	    protected int getErrorImageResource(User user) {
	        return R.drawable.error_img;
	    }

	    @Override
	    protected int getLoadingIndicatorImageResource(User user) {
	        return R.drawable.loading_img;
	    }

		@Override
	    protected User getItemBoundWithView(View view) {
	        return (User) view.getTag();
	    }

	    @Override
	    protected ImageView getImageViewFromView(View view) {
	        return (ImageView) view.findViewById(R.id.avatar_img);
	    }

	    @Override
	    protected String getItemId(User user) {
	        return user.getId();
	    }

	    @Override
	    protected void retrieveImage(User user, final MblRetrieveImageCallback cb) {
	        MblApi.get(user.getAvatar(), null, null, true, Long.MAX_VALUE, true, new MblApiGetCallback() {

	            @Override
	            public void onSuccess(int statusCode, byte[] data) {
	                cb.onRetrievedByteArray(data);
	            };

	            @Override
	            public void onFailure(int error, String errorMessage) {
	                cb.onRetrievedByteArray(null);
	            }
	        }, MblUtils.getMainThreadHandler());
	    }
	};

	@Override
    public View getView(int pos, View view, ViewGroup parent) {
    	...

    	User user = (User) getItem(pos);
    	view.setTag(user);
    	mImageLoader.loadImage(view);
    	return view;
    }
}
```


MobilibImageInput
-----------------
**Features**

  - Pick images from gallery (single or multiple). It will scan for images on both device memory and removable memory (like an removable SDCard). Unlike Android Gallery app which sometimes shows deleted images, MobilibImageInput always shows the latest list of images in memory.
  - Take image by camera and preview with zoom-in and zoom-out.
  - Crop image to specific sizes. Support both "pick then crop" or "take then crop".

**Sample code**

Configurations (optional, can be ignored if you want to use default values)
```java
String folderToSaveTakenImages = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/MyGreateApp";
String[] extensionsOfPickedImages = new String[] { "jpg", "jpeg", "png" };
String foldersToPickImages = new String[] { Environment.DIRECTORY_DCIM };
float cropMinZoom = 0.5f;
float cropMaxZoom = 3.0f;

MblImageInput.configure(
			folderToSaveTakenImages,
            extensionsOfPickedImages,
            foldersToPickImages,
            cropMinZoom,
            cropMaxZoom);
```

AndroidManifest.xml (mandatory)
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<!-- ... -->
<activity
    android:name="com.datdo.mobilib.imageinput.MblTakeImageActivity"
    android:configChanges="orientation|keyboardHidden|screenSize"
    android:screenOrientation="portrait" />
<activity
    android:name="com.datdo.mobilib.imageinput.MblPickImageActivity"
    android:screenOrientation="portrait" />
```

Take image
```java
MblTakeImageActivity.start(null, -1, -1, new MblTakeImageCallback() {

    @Override
    public void onFinish(String path) {
    	// ...
    }

    @Override
    public void onCancel() {
        // ...
    }
});
```

Pick images (multiple)
```java
MblPickImageActivity.start(5, -1, -1, new MblPickImageCallback() {

    @Override
    public void onFinish(String[] paths) {
        // ...
    }

    @Override
    public void onCancel() {
        // ...
    }
});
```

Take then crop
```java
int cropToWidth = 200;
int cropToHeight = 200;
MblTakeImageActivity.start(null, cropToWidth, cropToHeight, new MblTakeImageCallback() {

    @Override
    public void onFinish(String path) {
        // ...
    }

    @Override
    public void onCancel() {
        // ...
    }
});
```

Pick then crop
```java
int cropToWidth = 200;
int cropToHeight = 200;
MblPickImageActivity.start(1, cropToWidth, cropToHeight, new MblPickImageCallback() {

    @Override
    public void onFinish(String[] paths) {
        // ...
    }

    @Override
    public void onCancel() {
        // ...
    }
});
```


Utility classes and methods
---------------------------
Mobilib provide many classes and methods those are essential for all Android app.

**Get current context globally**
```java
MblUtils.getCurrentContext()
```

**Get SharedPreferences globally**
```java
MblUtils.getPrefs()
```

**Get Handler of Main Thread globally**
```java
MblUtils.getMainThreadHandler()
```

**Get LayoutInflater instance which is essential for adapters**
```java
MblUtils.getLayoutInflater()
```

**Execute an action on Main Thread**
```java
MblUtils.executeOnMainThread(new Runnable() {
	@Override
	public void run() {
		...
	}
});
```

**Execute an action on an async thread. You no longer have to think about AsyncTask**
```java
MblUtils.executeOnAsyncThread(new Runnable() {
	@Override
	public void run() {
		...
	}
});
```

**Common bundle to save object temporarily.**
```java
MblUtils.putToCommonBundle(key, value);
...
Object value = MblUtils.removeFromCommonBundle(key);
```

It is useful when you want to transfer special data between activities, due to the inconvenience that Intent 's extra doesn't allow you to put ANYTHING into it.
```java
SpecialObject specialObject = new SpecialObject();
Intent intent = new Intent(context, MyActivity.class);
intent.putExtra("special_object", MblUtils.putToCommonBundle(specialObject));
startActivity(intent);

...

public class MyActivity extends MblActivity {
	@Override
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        // ...

        String key = getIntent().getExtras().getString("special_object");
        SpecialObject specialObject = (SpecialObject) MblUtils.removeFromCommonBundle(key);
    }
}
```

**Keyboard**
```java
MblUtils.showKeyboard(editText);
MblUtils.hideKeyboard();
```

**Convert DP/SP to Pixel**
```java
int px = MblUtils.pxFromDp(150);
int px = MblUtils.pxFromSp(150);
```

**Current status check**
```java
MblUtils.isMainThread();
MblUtils.isPortraitDisplay();
MblUtils.isNetworkConnected();
MblUtils.isAppInForeGround();
MblUtils.isKeyboardOn();
MblUtils.getAppFlagDebug();
```

**Bitmap**
```java
MblUtils.loadBitmapMatchSpecifiedSize(150, 150, bitmapFilePath);
MblUtils.getBitmapSizes(bitmapFilePath);
MblUtils.recycleBitmap(bitmapObject);
MblUtils.recycleImageView(imageView);
MblUtils.cleanupView(view);
MblUtils.extractBitmap(imageView);
MblUtils.getImageRotateAngle(imagePath);
MblUtils.correctBitmapOrientation(imagePath, bitmapObject);
MblUtils.loadBitmapForImageView(imagePath, imageView, width, height, callback);
```

**Logging**
```java
MblUtils.logLongString(TAG, veryLongString);
MblUtils.logStackTrace(TAG);
```

**File**
```java
MblUtils.saveCacheFile(byteArray, relativePath)
MblUtils.readCacheFile(relativePath);
MblUtils.saveFile(byteArray, absolutePath);
MblUtils.readFile(absolutePath);
MblUtils.saveInternalFile(byteArray, absolutePath);
MblUtils.readInternalFile(absolutePath);
MblUtils.deleteInternalFile(absolutePath);
MblUtils.copyFile(inputStream, outputStream);
```

**Dialogs**
```java
MblUtils.showAlert(title, message, postTask);
MblUtils.showProgressDialog(message, cancelable);
MblUtils.hideProgressDialog();
MblUtils.showToast(String text, int duration);
```


**Miscellaneous**
```java
MblUtils.focusNothing(activity); // remove all focus
MblUtils.getDisplaySizes(); // get width and height of display screen
MblUtils.md5(data); // create MD5-hashed string for data
MblUtils.sendEmail(subject, emails, cc, bcc, text, title, attachmentFilenames);
MblUtils.motionEventOnView(motionEvent, view); // check if a MotionEvent happens on a view
MblUtils.copyTextToClipboard(text);
MblUtils.generateDeviceId(); // generate a UDID which is unique for each device
MblUtils.closeApp(MainActivity.class); // close this app
MblUtils.isAppInstalled(packageName); // check if app is installed
MblUtils.getAppPackageInfo(); // get app 's PackageInfo
MblUtils.getKeyHash(); // get app 's key hash
```