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
  - HTTP/HTTPS API to send GET/POST/PUT/DELETE request and receive response. It is thread-safe and provides almost all you need for a typical HTTP/HTTPS request: parameters (InputStream and File parameter are also supported for uploading big files), headers, caching, ignore SSL Certificate (for Dev environment).
  - Carrier/Interceptor model which is alternative of old Activity/Fragment model.
  - Cache Master which is a full solution for retrieving and caching objects.
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
ACTIVITY_CREATED
ACTIVITY_RESUMED
ACTIVITY_PAUSED
ACTIVITY_DESTROYED
ACTIVITY_RESULT
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
MblApi.run(new MblRequest()
    .setMethod(MblApi.Method.GET)
    .setUrl("http://your.website.com/img/logo.png")
    .setCacheDuration(1000l * 60l * 60l * 24l)
    .setCallback(new MblApi.MblApiCallback() {

        @Override
        public void onSuccess(int statusCode, byte[] data) {
            // ...
        };

        @Override
        public void onFailure(int error, String errorMessage) {
            // ...
        }
    }));
```

***Upload file***
```java
MblApi.run(new MblRequest()
    .setMethod(MblApi.Method.POST)
    .setUrl("https://your.website.com/v1/upload/avatar")
    .setParams(
            "user-name", username,
            "avatar", new File(uploadedFilePath)
    )
    .setHeaderParams("access-token", accessToken)
    .setVerifySSL(true)
    .setCallback(new MblApi.MblApiCallback() {

        @Override
        public void onSuccess(int statusCode, String data) {
            // ...
        }

        @Override
        public void onFailure(int error, String errorMessage) {
            // ...
        }
    }));
```

Carrier/Interceptor
-------------------
Carrier/Interceptor is a alternative of old Activity/Fragment model.

Due to the fact that Activity/Fragment model has too many drawbacks:

1. Quite complicated to start and manage lifecycle.

	How you start a Fragment with parameters
	```java
	Fragment newFragment = new ExampleFragment();
	Bundle args = new Bundle();
	args.putInt("param1", param1);
	args.putInt("param2", param2);
	newFragment.setArguments(args);
	FragmentTransaction transaction = getFragmentManager().beginTransaction(); // or getSupportFragmentManager()
	transaction.replace(R.id.fragment_container, newFragment);
	transaction.addToBackStack(null);
	transaction.commit();
	```
	 
	Fragment 's lifecycle (quite different from Activity 's lifecycle, why Google didn't make coding simpler?)
	```java
	onAttach -> onCreate -> onCreateView -> onActivityCreated -> onStart -> onResume -> onPause -> onStop -> onDestroyView -> onDestroy -> onDetach
	```

2. Cause potential bugs (especially `Fragment#getActivity()` method which causes `NullPointerException`).
3. Fragment can not contain another fragment (for example: you can not add Google MapFragment into your fragment)
4. Unable to start a fragment directly from another fragment while an Activity can be started directly from another Activity (you can do it by using getActivity() method, but it is still complicated, as mentioned in [1])
5. Activity must be subclass of FragmentActivity.

it is recommended to use Carrier/Interceptor alternative when you need to render multiple sub-screens in a parent screen.
 
**Benefits of Carrier/Interceptor:**

1. Easy to use
	
	How you start an Interceptor with parameters
	```java
	carrier.startInterceptor(ExampleInterceptor.class, null, "param1", param1, "param2", param2);
	```
	
	Interceptor 's lifecycle just looks like Activity 's lifecycle, even simpler
	```java
	onCreate -> onResume -> onPause -> onDestroy
	```
2. Interceptor can contains another interceptor due to the fact that interceptor is just a View
3. You can start an interceptor from another interceptor, just like starting Activity from another Activity, even simpler

	```java
	public class ExampleInterceptor extends MblInterceptor {
	    public void foo() {
	        startInterceptor(NextInterceptor.class, null, "param1", param1, "param2", param2);
	    }
	}
	```
4. MblCarrier is just an object wrapping a `FrameLayout` view which is the container view of its Interceptors, therefore Carrier can be plugged in any Activity or View.
 
Sample code:
```java
FrameLayout interceptorContainerView = (FrameLayout) findViewById(R.id.interceptor_container);
mCarrier = new MblSlidingCarrier(this, interceptorContainerView, new MblCarrier.MblCarrierCallback() {
    @Override
    public void onNoInterceptor() {
        // ... handle when Carrier does not contain any Interceptor
    }
});
mCarrier.startInterceptor(Interceptor1.class, new Options().newInterceptorStack(), "param1", param1);
```
 
**P/S: the name "Carrier/Interceptor" is inspired by legendary game Starcraft ;)**

Cache Master
------------
Cache Master (CM) is a full solution for retrieving and caching objects in Android app.

Objects can be retrieved from 3 data-sources:

1. Memory                           -> high speed, can get object instantly without blocking main thread, lost all objects when app is killed
2. Database/file                    -> medium speed, should be executed on asynchronous thread, objects is still intact even when app is killed
3. Server (via RESTful API, etc)    -> slow speed, use this way to retrieve new objects or expired objects, must be executed on asynchronous thread

The mechanism of CM is that objects are stored in Memory for instant retrieval, also stored in Database to keep them intact even when app is killed,
and fetched from server when they are expired or not existing in both Memory and Database.

The following example steps depicts how CM works to retrieve list of objects by their ids:

1. Given that we need to retrieve objects whose ids is [1,2,3,4]
2. Firstly, CM searches in Memory for [1,2,3,4]. It finds object1 and object2 but object2 is too old (expired). 3 & 4 is not found. Result = [object1]
3. Next, CM searches in Mobilib 's database for [2,3,4]. It finds out that 2 is existing but expired, 3 is existing and still fresh, 4 is not found.
    Then it searches in App 's database for 3 and retrieves object3. Result = [object1, object3]
4. Finally, CM calls RESTful API to fetch object2 and object4 from server. Result = [object1, object2, object3, object4]

Of course this is just a happy case. At step 4, we may not be able to fetch objects from server, and have to fallback to fetch them from App 's database even though they are expired.

***Note that Mobilib 's database and App 's database is different. Mobilib 's database is to determine whether object of certain id is existing or expired. App 's database is where App stores objects.***

For Memory Cache, CM utilizes `MblMemCache` which is just a simple id:object mapping with expiration.

For Database Cache, CM utilizes `MblDatabaseCache` which is just a simple id:timeInMs mapping to determine whether object of id is expired.

Fetching objects from App 's database and server is done by App by overriding 2 method `fetchFromDatabase(List)` and `fetchFromServer(List, MblCacheMaster.MblGetManyCallback)`.

get/put/delete/clear methods are executed serially by `MblSerializer` to make CM thread-safe, and also to ensure that we don't send 2 server requests for the same object.

All accesses to Databases are executed on asynchronous thread so that it doesn't burden main thread.

Sample code:
```java
MblCacheMaster cm = new MblCacheMaster<User>(User.class, 60 * 1000) {

    protected String getObjectId(User user) {
       return user.getId();
    }

    protected List<T> fetchFromDatabase(List<String> ids) {
       return UserDatabase.fetchUsers(ids);
    }

    protected void storeToDatabase(List<User> users) {
       UserDatabase.saveUsers(users);
    }

    protected void fetchFromServer(List<String> ids, final MblGetManyCallback<T> callback) {
       UserRestApi.fetchUsers(ids, new FetchUsersCallback() {
           
           public void onSuccess(List<User> users) {
               callback.onSuccess(users);
           }

           public void onError() {
               callback.onError();
           }
       });
    }

    protected boolean  fallbackToDatabaseWhenServerFail() {
       return true;
    }
}

cm.get(new String[] { "1", "2", "3", "4" }, new MblGetManyCallback<User>() {

    public void onSuccess(List<User> users) {
       // ... display users
    }

    public void onError() {
       // ... show error message
    }
});
```

Smart image loader
-----------------
Smart loader to display images for child views in a `ViewGroup`, like `ListView`, `LinearLayout`, `GridView`, etc.

Features:
1. Load images sequentially.
2. Automatically scale images to match sizes of `ImageView`.
3. Cache images using `LruCache`.
4. Only load images for currently displayed cells, which is very useful for `ListView`.
5. Fading animation when bitmap is loaded successfully.
6. Display `ProgressBar` to indicate bitmap loading

**MblSimpleImageLoader** is an implementation of Template Method pattern. You need to override some abstract methods to make it work.

```java
public class UserAdapter extends BaseAdapter {

    private MblSimpleImageLoader<User> mUserAvatarLoader = new MblSimpleImageLoader<User>() {

        @Override
        protected User getItemBoundWithView(View view) {
            return (User) view.getTag();
        }

        @Override
        protected ImageView getImageViewBoundWithView(View view) {
            return (ImageView) view.findViewById(R.id.avatar_image_view);
        }

        @Override
        protected String getItemId(User user) {
            return user.getId();
        }

        @Override
        protected void retrieveImage(User user, final MblRetrieveImageCallback cb) {
            MblApi.get(item, null, null, Long.MAX_VALUE, true, new MblApiCallback() {

                public void onSuccess(int statusCode, byte[] data) {
                    cb.onRetrievedByteArray(data);
                };

                public void onFailure(int error, String errorMessage) {
                    cb.onRetrievedError();
                }
            }, null);
        }

        @Override
        protected void onError(ImageView imageView, T item) {
            imageView.setImageResource(R.drawable.default_avatar);
        }
    };

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {

        // ...

        view.setTag(user);
        mUserImageLoader.loadImage(view);

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

**MblSerializer**

Tiny but very useful. Every instance of this class is a queue which you can push any task into it and wait for them to complete one after one.

Task is defined by implementing `MblSerializer#Task` interface.

Task is always invoked in main thread, then it can split processing to another thread itself if needed.
  
Sample code:
```java
MblSerializer s = new MblSerializer();

s.run(new MblSerializer.Task() {
    @Override
    public void run(Runnable finishCallback) {
        // run task 1 in async thread
        MblUtils.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                // ... do something here

                // finally, invoke callback
                finishCallback.run();
            }
        });
    }
});

s.run(new MblSerializer.Task() {
    @Override
    public void run(Runnable finishCallback) {
        // run task 2 in async thread
        MblUtils.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                // ... do something here

                // finally, invoke callback
                finishCallback.run();
            }
        });
    }
});

// ... next task/action comes here
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

**MblYieldLayout**

If you are familiar with Web development, you must know concept of "layout".

To use layout, you create an HTML file and one "yield" tag where your page 's content will be inserted in.

This widget brings Web 's "layout" concept to Android.

Firstly, you create a common layout named "common_layout.xml":
```xml
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <include layout="@layout/header"/>

    <LinearLayout
        android:id="@+id/yield"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center_horizontal">

    </LinearLayout>

    <include layout="@layout/footer"/>
</LinearLayout>
```

Secondly, use common layout in your layout for each screen. Pay attention to "layoutId" and "yieldId":
```xml
<com.datdo.mobilib.widget.MblYieldLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:layoutId="@layout/common_layout"
    app:yieldId="@+id/yield">

    <!-- Content of this screen-->

</com.datdo.mobilib.widget.MblYieldLayout>
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
