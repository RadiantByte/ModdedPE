package com.mojang.minecraftpe;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NativeActivity;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Process;
import android.os.*;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.mojang.android.StringValue;
import com.mojang.minecraftpe.input.InputDeviceManager;
import com.mojang.minecraftpe.platforms.Platform;
import com.mojang.minecraftpe.python.PythonPackageLoader;
import org.fmod.FMOD;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 13.08.2022
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
@SuppressWarnings("JavaJniMissingFunction")
public class MainActivity extends NativeActivity implements View.OnKeyListener, FilePickerManagerHandler {
    static final int MSG_CORRELATION_CHECK = 672;
    static final int MSG_CORRELATION_RESPONSE = 837;
    static final int OPEN_FILE_RESULT_CODE = 5;
    private static final int POST_NOTIFICATIONS_PERMISSION_ID = 2;
    public static int RESULT_PICK_IMAGE = 1;
    static final int SAVE_FILE_RESULT_CODE = 4;
    private static final int STORAGE_PERMISSION_ID = 1;
    private static boolean _isPowerVr;
    private static boolean mHasStoragePermission;
    public static MainActivity mInstance;
    Class SystemProperties;
    private ClipboardManager clipboardManager;
    private InputDeviceManager deviceManager;
    Method getPropMethod;
    HeadsetConnectionReceiver headsetConnectionReceiver;
    private Locale initialUserLocale;
    private BatteryMonitor mBatteryMonitor;
    private AlertDialog mDialog;
    private HardwareInformation mHardwareInformation;
    public int mLastPermissionRequestReason;
    private NetworkMonitor mNetworkMonitor;
    public ParcelFileDescriptor mPickedFileDescriptor;
    private ThermalMonitor mThermalMonitor;
    Platform platform;
    TextInputProxyEditTextbox textInputWidget;
    private TextToSpeech textToSpeechManager;
    private Thread mMainThread = null;
    public int virtualKeyboardHeight = 0;
    private boolean _fromOnCreate = false;
    private boolean mCursorLocked = false;
    private boolean mIsSoftKeyboardVisible = false;
    private long mFileDialogCallback = 0;
    private float mVolume = 1.0f;
    private boolean mIsRunningInAppCenter = false;
    private boolean mPauseTextboxUIUpdates = false;
    AtomicInteger mCaretPositionMirror = new AtomicInteger(0);
    AtomicReference<String> mCurrentTextMirror = new AtomicReference<>("");
    public List<ActivityListener> mActivityListeners = new ArrayList();
    private FilePickerManager mFilePickerManager = null;
    private WorldRecovery mWorldRecovery = null;
    private WifiManager mWifiManager = null;
    private WifiManager.MulticastLock mMulticastLock = null;
    Messenger mService = null;
    MessageConnectionStatus mBound = MessageConnectionStatus.NOTSET;
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = new Messenger(iBinder);
            mBound = MessageConnectionStatus.CONNECTED;
            Message obtain = Message.obtain(null, MainActivity.MSG_CORRELATION_CHECK, 0, 0);
            obtain.replyTo = mMessenger;
            try {
                mService.send(obtain);
            } catch (RemoteException unused) {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mBound = MessageConnectionStatus.DISCONNECTED;
        }
    };

    private int _userInputStatus = -1;
    private String[] _userInputText = null;
    private ArrayList<StringValue> _userInputValues = new ArrayList<>();
    ActivityManager.MemoryInfo mCachedMemoryInfo = new ActivityManager.MemoryInfo();
    long mCachedMemoryInfoUpdateTime = 0;
    long mCachedUsedMemory = 0;
    long mCachedUsedMemoryUpdateTime = 0;
    Debug.MemoryInfo mCachedDebugMemoryInfo = new Debug.MemoryInfo();
    long mCachedDebugMemoryUpdateTime = 0;
    private long mCallback = 0;

    enum MessageConnectionStatus {
        NOTSET,
        CONNECTED,
        DISCONNECTED
    }


    private void assertIsMainThread() {
    }

    private static native void nativeWaitCrashManagementSetupComplete();


    public static boolean isPowerVR() {
        return _isPowerVr;
    }

    public static void saveScreenshot(String filename, int w, int h, int[] pixels) {
        final Bitmap bitmap = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888);

        try (FileOutputStream fileOutputStream = new FileOutputStream(filename)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOutputStream);
            fileOutputStream.flush();
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't create file: " + filename);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    native void nativeFireNetworkChangedEvent(String networkConnectionType);

    native boolean isAndroidChromebook();

    public void buyGame() {
    }

    public int checkLicense() {
        return 0;
    }

    public void displayDialog(int dialogId) {
    }

    public boolean hasBuyButtonWhenInvalidLicense() {
        return true;
    }

    native boolean isAndroidAmazon();

    native void nativeSetIntegrityToken(String integrityToken);

    native void nativeSetIntegrityTokenErrorMessage(String errorMessage);

    native boolean isAndroidTrial();

    native boolean isBrazeEnabled();

    public boolean isBrazeSDKDisabled() {
        return true;
    }

    protected boolean isDemo() {
        return false;
    }

    public native boolean isEduMode();

    public native boolean isPublishBuild();

    native boolean isTestInfrastructureDisabled();

    native void nativeBackPressed();

    native void nativeRunNativeCallbackOnUiThread(long fn);

    native void nativeBackSpacePressed();

    native String nativeCheckIfTestsAreFinished();

    native void nativeClearAButtonState();

    native void nativeDeviceCorrelation(long myTime, String theirDeviceId, long theirTime, String theirLastSessionId);

    native String nativeGetActiveScreen();

    native String nativeGetDevConsoleLogName();

    native String nativeGetDeviceId();

    native String nativeGetLogText(String fileInfo);

    native long nativeInitializeLibHttpClient(long hcInitArgs);

    native long nativeInitializeXboxLive(long xalInitArgs, long xblInitArgs);

    public native boolean nativeKeyHandler(final int keycode, final int action);

    native void nativeOnDestroy();

    native void nativeOnPickFileCanceled();

    native void nativeOnPickFileSuccess(String tempPickedFilePath);

    native void nativeOnPickImageCanceled(long callback);

    native void nativeOnPickImageSuccess(long callback, String picturePath);

    native void nativeProcessIntentUriQuery(String host, String query);

    native void nativeResize(int width, int height);

    native void nativeReturnKeyPressed();

    native void nativeSetHeadphonesConnected(boolean connected);

    native String nativeSetOptions(String optionsString);

    native void nativeCaretPosition(final int caretPosition);

    native void nativeSetTextboxText(String text);

    native void nativeShutdown();

    native void nativeStopThis();

    native void nativeStoragePermissionRequestResult(boolean result, int reason);

    native void nativeSuspend();

    @Override
    public void onBackPressed() {
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }

    native void onSoftKeyboardClosed();

    public void postScreenshotToFacebook(String filename, int w, int h, int[] pixels) {
    }

    public void statsTrackEvent(String eventName, String eventParameters) {
    }

    public void statsUpdateUserData(String graphicsVendor, String graphicsRenderer) {
    }

    public void tick() {
    }

    public void launchUri(String uri) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
    }

    public void share(String title, String description, String uri) {
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TITLE, description);
        intent.putExtra(Intent.EXTRA_TEXT, uri);
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, title));
    }

    public void openAndroidAppSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            Log.e("ModdedPE", "openAndroidAppSettings: Failed to open android app settings: " + e);
        }
    }

    public void setClipboard(String value) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("ModdedPE-Clipdata", value));
    }

    public float getKeyboardHeight() {
        return virtualKeyboardHeight;
    }

    public void trackPurchaseEvent(String contentId, String contentType, String revenue, String clientId, String userId, String playerSessionId, String currencyCode, String eventName) {
    }

    public void setBrazeID(String ID) {
    }

    public void enableBrazeSDK() {
    }

    public void disableBrazeSDK() {
    }

    public void setCachedDeviceId(String deviceId) {
        final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.putString("deviceId", deviceId);
        edit.apply();
    }

    public void throwRuntimeExceptionFromNative(final String message) {
        new Handler(getMainLooper()).post(() -> {
            throw new RuntimeException(message);
        });
    }

    public void deviceIdCorrelationStart() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final int i = defaultSharedPreferences.getInt("correlationAttempts", 10);
        if (i == 0) {
            return;
        }
        final Intent intent = new Intent();
        intent.setComponent(new ComponentName(getPackageName().contains("trial") ? getPackageName() : "com.mojang.minecrafttrialpe", "com.mojang.minecraftpe.ImportService"));
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        SharedPreferences.Editor edit = defaultSharedPreferences.edit();
        edit.putInt("correlationAttempts", i - 1);
        edit.apply();
    }

    public BatteryMonitor getBatteryMonitor() {
        if (mBatteryMonitor == null) {
            mBatteryMonitor = new BatteryMonitor(this);
        }
        return mBatteryMonitor;
    }

    public HardwareInformation getHardwareInfo() {
        if (mHardwareInformation == null) {
            mHardwareInformation = new HardwareInformation(this);
        }
        return mHardwareInformation;
    }

    public ThermalMonitor getThermalMonitor() {
        if (mThermalMonitor == null) {
            mThermalMonitor = new ThermalMonitor(this);
        }
        return mThermalMonitor;
    }

    public CrashManager initializeCrashManager(String crashDumpFolder, String sessionId) {
        return new CrashManager();
    }

    public void initializeCrashManager() {
    }

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("ModdedPEPlatform", "MainActivity::onCreate");
        mMainThread = Thread.currentThread();
        super.onCreate(savedInstanceState);
        if (getResources() == null) {
            Log.w("ModdedPE - replacing", "App is installing/replacing. Killing...");
            Process.killProcess(Process.myPid());
        }
        nativeWaitCrashManagementSetupComplete();
        if (isEduMode()) {
            try {
                getClassLoader().loadClass("com.microsoft.applications.events.HttpClient").getConstructor(Context.class).newInstance(getApplicationContext());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            configureBrazeAtRuntime();
            if (Build.VERSION.SDK_INT >= 33) {
                requestPushPermission();
            }
        }
        platform = Platform.createPlatform(true);
        setVolumeControlStream(3);
        FMOD.init(this);
        deviceManager = InputDeviceManager.create(this);
        headsetConnectionReceiver = new HeadsetConnectionReceiver();
        mNetworkMonitor = new NetworkMonitor(getApplicationContext());
        platform.onAppStart(getWindow().getDecorView());
        mHasStoragePermission = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) == 0;
        nativeSetHeadphonesConnected(((AudioManager) getSystemService(Context.AUDIO_SERVICE)).isWiredHeadsetOn());
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        initialUserLocale = Locale.getDefault();
        final FilePickerManager filePickerManager = new FilePickerManager(this);
        mFilePickerManager = filePickerManager;
        addListener(filePickerManager);
        mInstance = this;
        _fromOnCreate = true;
        textInputWidget = createTextWidget();
        findViewById(android.R.id.content).getRootView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                nativeResize(right - left, bottom - top);
            }
        });
        if (isEduMode()) {
            new PythonPackageLoader(getApplicationContext().getAssets(), getFilesDir()).unpack();
        }
        if (!isTestInfrastructureDisabled() && InstrumentationRegistryHelper.getIsRunningInAppCenter()) {
            mIsRunningInAppCenter = true;
            Log.w("ModdedPE", "Automation: in MainActivity::onCreate, we are running in AppCenter");
        }
        mWorldRecovery = new WorldRecovery(getApplicationContext(), getApplicationContext().getContentResolver());
    }

    public void initializeMulticast() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiManager = wifiManager;
        if (wifiManager != null) {
            mMulticastLock = wifiManager.createMulticastLock("ModdedPEMCLock");
            setMulticastReferenceCounting(false);
        }
    }

    public void releaseMulticast() {
        if (mMulticastLock == null || !isMulticastHeld()) {
            return;
        }
        mMulticastLock.release();
    }

    public void acquireMulticast() {
        if (mMulticastLock == null || isMulticastHeld()) {
            return;
        }
        mMulticastLock.acquire();
    }

    public void setMulticastReferenceCounting(boolean useReferenceCounting) {
        WifiManager.MulticastLock multicastLock = mMulticastLock;
        if (multicastLock != null) {
            multicastLock.setReferenceCounted(useReferenceCounting);
        }
    }

    public void lockCursor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View rootView = findViewById(android.R.id.content).getRootView();
            mCursorLocked = true;
            rootView.requestPointerCapture();
        }
    }

    public void unlockCursor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View rootView = findViewById(android.R.id.content).getRootView();
            mCursorLocked = false;
            rootView.setPointerIcon(PointerIcon.getSystemIcon(getApplicationContext(), 1000));
            rootView.releasePointerCapture();
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (mCursorLocked && (event.getSource() & 8194) == 8194) {
            lockCursor();
        }
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (nativeKeyHandler(event.getKeyCode(), event.getAction())) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 25 || keyCode == 24) {
            platform.onVolumePressed();
        }
        return super.onKeyUp(keyCode, event);
    }

    public void setTextToSpeechEnabled(boolean enabled) {
        if (enabled) {
            if (textToSpeechManager == null) {
                try {
                    textToSpeechManager = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                        }
                    });
                    return;
                } catch (Exception unused) {
                    return;
                }
            }
            return;
        }
        textToSpeechManager = null;
    }

    public void requestStoragePermission(int permissionReason) {
        mLastPermissionRequestReason = permissionReason;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_ID);
        }
    }

    @TargetApi(33)
    public void requestPushPermission() {
        Log.i("ModdedPE", "MainActivity::requestPushPermission");
        if (checkPermission(Manifest.permission.POST_NOTIFICATIONS, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread(() -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, POST_NOTIFICATIONS_PERMISSION_ID));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i("ModdedPE", "MainActivity::onRequestPermissionsResult");
        if (requestCode == STORAGE_PERMISSION_ID) {
            mHasStoragePermission = grantResults.length > 0 && grantResults[0] == 0;
            nativeStoragePermissionRequestResult(mHasStoragePermission, mLastPermissionRequestReason);
        }
    }

    public boolean hasWriteExternalStoragePermission() {
        final boolean hasStoragePermission = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
        mHasStoragePermission = hasStoragePermission;
        return hasStoragePermission;
    }

    public boolean hasHardwareKeyboard() {
        return getResources().getConfiguration().keyboard == 2;
    }

    public void setupKeyboardViews(String text, int maxLength, boolean limitInput, boolean numbersOnly, boolean isMultiline) {
        int caretPosition = getCaretPosition();
        mPauseTextboxUIUpdates = true;
        textInputWidget.updateFilters(maxLength, !isMultiline);
        textInputWidget.setInputType((isMultiline ? 131072 : 524288) | (numbersOnly ? 2 : 1));
        mPauseTextboxUIUpdates = false;
        textInputWidget.setText(text);
        setCaretPosition(caretPosition);
        mCurrentTextMirror.set(text);
        textInputWidget.setVisibility(View.VISIBLE);
        textInputWidget.requestFocus();
        getInputMethodManager().showSoftInput(textInputWidget, 0);
    }


    public TextInputProxyEditTextbox createTextWidget() {
        final TextInputProxyEditTextbox textInputProxyEditTextbox = new TextInputProxyEditTextbox(this);
        textInputProxyEditTextbox.setVisibility(View.GONE);
        textInputProxyEditTextbox.setFocusable(true);
        textInputProxyEditTextbox.setFocusableInTouchMode(true);
        textInputProxyEditTextbox.setImeOptions(268435461);
        textInputProxyEditTextbox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                Log.w("ModdedPE - keyboard", "onEditorAction: " + actionId);
                boolean z = actionId == 5;
                boolean z2 = actionId == 0 && keyEvent != null && keyEvent.getAction() == 0;
                if (!z && !z2) {
                    if (actionId != 7) {
                        return false;
                    }
                    nativeBackPressed();
                    return true;
                }
                if (z) {
                    nativeReturnKeyPressed();
                }
                String obj = textInputProxyEditTextbox.getText().toString();
                int selectionEnd = textInputProxyEditTextbox.getSelectionEnd();
                if (selectionEnd < 0 || selectionEnd > obj.length()) {
                    selectionEnd = obj.length();
                }
                if ((textInputProxyEditTextbox.getInputType() & 131072) == 0) {
                    return true;
                }
                textInputProxyEditTextbox.setText(obj.substring(0, selectionEnd) + "\n" + obj.substring(selectionEnd, obj.length()));
                textInputProxyEditTextbox.setSelection(Math.min(selectionEnd + 1, textInputProxyEditTextbox.getText().length()));
                return true;
            }
        });
        textInputProxyEditTextbox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mPauseTextboxUIUpdates) {
                    return;
                }
                String obj = editable.toString();
                mCurrentTextMirror.set(obj);
                nativeSetTextboxText(obj);
            }
        });
        textInputProxyEditTextbox.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void sendAccessibilityEvent(View view, int eventType) {
                super.sendAccessibilityEvent(view, eventType);
                if (eventType == 8192) {
                    int selectionStart = textInputProxyEditTextbox.getSelectionStart();
                    mCaretPositionMirror.set(selectionStart);
                    nativeCaretPosition(selectionStart);
                }
            }
        });
        textInputProxyEditTextbox.setOnMCPEKeyWatcher(new TextInputProxyEditTextbox.MCPEKeyWatcher() {
            @Override
            public void onDeleteKeyPressed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        nativeBackSpacePressed();
                    }
                });
            }

            @Override
            public boolean onBackKeyPressed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.w("ModdedPE - keyboard", "textInputWidget.onBackPressed");
                        nativeBackPressed();
                    }
                });
                return true;
            }
        });
        ((ViewGroup) findViewById(android.R.id.content)).addView(textInputProxyEditTextbox, new ViewGroup.LayoutParams(320, 50));
        final View rootView = findViewById(android.R.id.content).getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect rect = new Rect();
                rootView.getWindowVisibleDisplayFrame(rect);
                virtualKeyboardHeight = rootView.getRootView().getHeight() - rect.height();
                if (rootView.getRootView().getHeight() - rect.bottom > 0) {
                    if (mIsSoftKeyboardVisible) {
                        return;
                    }
                    mIsSoftKeyboardVisible = true;
                } else if (mIsSoftKeyboardVisible) {
                    mIsSoftKeyboardVisible = false;
                }
            }
        });
        return textInputProxyEditTextbox;
    }


    public void updateLocalization(final String lang, final String region) {
        runOnUiThread(() -> {
            final Locale locale = new Locale(lang, region);
            Locale.setDefault(locale);
            final Configuration configuration = new Configuration();
            configuration.locale = locale;
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        });
    }

    public void showKeyboard(final String text, final int maxLength, final boolean limitInput, final boolean numbersOnly, final boolean isMultiline) {
        nativeClearAButtonState();
        runOnUiThread(() -> setupKeyboardViews(text, maxLength, limitInput, numbersOnly, isMultiline));
    }

    public void hideKeyboard() {
        runOnUiThread(this::dismissTextWidget);
    }

    public boolean isSoftKeyboardVisible() {
        return mIsSoftKeyboardVisible;
    }

    public boolean isTextWidgetActive() {
        final TextInputProxyEditTextbox textInputProxyEditTextbox = textInputWidget;
        return textInputProxyEditTextbox != null && textInputProxyEditTextbox.getVisibility() == View.VISIBLE;
    }

    public void dismissTextWidget() {
        if (isTextWidgetActive()) {
            getInputMethodManager().hideSoftInputFromWindow(textInputWidget.getWindowToken(), 0);
            mPauseTextboxUIUpdates = true;
            textInputWidget.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            mPauseTextboxUIUpdates = false;
            textInputWidget.setVisibility(View.GONE);
        }
    }

    public int getCursorPosition() {
        if (!isTextWidgetActive()) {
            return -1;
        }
        return textInputWidget.getSelectionStart();
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private void createAlertDialog(boolean hasOkButton, boolean hasCancelButton, boolean preventBackKey) {
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("");
        if (preventBackKey) {
            builder.setCancelable(false);
        }
        builder.setOnCancelListener(dialog -> onDialogCanceled());
        if (hasOkButton) {
            builder.setPositiveButton("Ok", (dialog, which) -> onDialogCompleted());
        }
        if (hasCancelButton) {
            builder.setNegativeButton("Cancel", (dialog, which) -> onDialogCanceled());
        }
        final AlertDialog create = builder.create();
        mDialog = create;
        create.setOwnerActivity(this);
    }

    public void setIsPowerVR(boolean status) {
        _isPowerVr = status;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && mCursorLocked) {
            lockCursor();
        }
        super.onWindowFocusChanged(hasFocus);
        platform.onViewFocusChanged(hasFocus);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    public int getKeyFromKeyCode(int keyCode, int metaState, int deviceId) {
        if (deviceId < 0) {
            final int[] deviceIds = InputDevice.getDeviceIds();
            if (deviceIds.length == 0) {
                return 0;
            }
            deviceId = deviceIds[0];
        }
        final InputDevice device = InputDevice.getDevice(deviceId);
        if (device == null) {
            return 0;
        }
        return device.getKeyCharacterMap().get(keyCode, metaState);
    }

    public void copyToPickedFile(String inPath) {
        try {
            final FileInputStream fileInputStream = new FileInputStream(inPath);
            final ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(mPickedFileDescriptor);
            copyFile(fileInputStream, autoCloseOutputStream);
            autoCloseOutputStream.close();
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPickedFileDescriptor = null;
    }

    public void copyFromPickedFile(String outPath) {
        try {
            final ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(mPickedFileDescriptor);
            final FileOutputStream fileOutputStream = new FileOutputStream(outPath);
            copyFile(autoCloseInputStream, fileOutputStream);
            fileOutputStream.close();
            autoCloseInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPickedFileDescriptor = null;
    }

    private void copyFile(@NonNull InputStream inStream, OutputStream outStream) throws IOException {
        final byte[] bArr = new byte[1024];
        int read = inStream.read(bArr);
        while (read != -1) {
            outStream.write(bArr, 0, read);
            read = inStream.read(bArr);
        }
        inStream.close();
        outStream.close();
    }

    public byte[] getFileDataBytes(@NonNull String filename) {
        if (filename.isEmpty()) {
            return null;
        }

        try {
            final AssetManager assets = getApplicationContext().getAssets();
            if (assets == null) {
                System.err.println("getAssets returned null: Could not getFileDataBytes " + filename);
                return null;
            }

            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(assets.open(filename))) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096]; // Лучше использовать меньший размер буфера

                int bytesRead;
                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                return byteArrayOutputStream.toByteArray();
            } catch (IOException e) {
                System.err.println("Error reading file " + filename + ": " + e.getMessage());
            }
        } catch (NullPointerException e) {
            System.err.println("getAssets threw NPE: Could not getFileDataBytes " + filename);
        }

        return null;
    }

    public int[] getImageData(String filename) {
        Bitmap decodeFile = BitmapFactory.decodeFile(filename);
        if (decodeFile == null) {
            try {
                final AssetManager assets = getApplicationContext().getAssets();
                if (assets != null) {
                    try {
                        decodeFile = BitmapFactory.decodeStream(assets.open(filename));
                    } catch (IOException unused) {
                        System.err.println("getImageData: Could not open image " + filename);
                        return null;
                    }
                } else {
                    System.err.println("getAssets returned null: Could not open image " + filename);
                    return null;
                }
            } catch (NullPointerException unused2) {
                System.err.println("getAssets threw NPE: Could not open image " + filename);
                return null;
            }
        }
        final Bitmap bitmap = decodeFile;
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final int[] iArr = new int[(width * height) + 2];
        iArr[0] = width;
        iArr[1] = height;
        bitmap.getPixels(iArr, 2, width, 0, 0, width, height);
        return iArr;
    }

    public int getScreenWidth() {
        final Display defaultDisplay = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int max = Math.max(defaultDisplay.getWidth(), defaultDisplay.getHeight());
        System.out.println("getwidth: " + max);
        return max;
    }

    public int getScreenHeight() {
        final Display defaultDisplay = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int min = Math.min(defaultDisplay.getWidth(), defaultDisplay.getHeight());
        System.out.println("getheight: " + min);
        return min;
    }

    public int getDisplayWidth() {
        if (getAndroidVersion() >= 17) {
            final Point point = new Point();
            getWindowManager().getDefaultDisplay().getRealSize(point);
            return point.x;
        }
        return 0;
    }

    public int getDisplayHeight() {
        if (getAndroidVersion() >= 17) {
            final Point point = new Point();
            getWindowManager().getDefaultDisplay().getRealSize(point);
            return point.y;
        }
        return 0;
    }

    public int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    public String getDeviceModel() {
        return HardwareInformation.getDeviceModelName();
    }

    public String getLocale() {
        return HardwareInformation.getLocale();
    }

    public String getExternalStoragePath() {
        return getExternalFilesDir(null).getAbsolutePath();
    }

    public String getLegacyExternalStoragePath(String gameFolder) {
        boolean z;
        final File externalStorageDirectory = Environment.getExternalStorageDirectory();
        try {
            new FileOutputStream(new File(new File(externalStorageDirectory, gameFolder), "test")).close();
            z = true;
        } catch (Exception unused) {
            z = false;
        }
        return z ? externalStorageDirectory.getAbsolutePath() : "";
    }

    public String getLegacyExternalStorage_debug() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public boolean getLegacyExternalStorageReadable_debug() {
        return Environment.getExternalStorageDirectory().canRead();
    }

    public boolean getLegacyExternalStorageDirWritable_debug() {
        return Environment.getExternalStorageDirectory().canWrite();
    }

    public boolean getLegacyExternalStorageGameFolderWritable_debug(String gameFolder) {
        return new File(Environment.getExternalStorageDirectory(), gameFolder).canWrite();
    }

    public boolean getLegacyExternalStorageGameFolderReadable_debug(String gameFolder) {
        try {
            final FileInputStream fileInputStream = new FileInputStream(new File(new File(Environment.getExternalStorageDirectory(), gameFolder), "minecraftpe/clientId.txt"));
            final int read = fileInputStream.read(new byte[1024]);
            fileInputStream.close();
            return read > 0;
        } catch (Exception unused) {
            return false;
        }
    }

    public int isExternalStorageLegacy_debug() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Environment.isExternalStorageLegacy() ? 1 : 0;
        }
        return -1;
    }

    public String getInternalStoragePath() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return getDataDir().getAbsolutePath();
        }
        return getFilesDir().getParent();
    }

    public String getObbDirPath() {
        return getApplicationContext().getObbDir().getAbsolutePath();
    }

    public float getPixelsPerMillimeter() {
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return ((displayMetrics.xdpi + displayMetrics.ydpi) * 0.5f) / 25.4f;
    }

    public void quit() {
        runOnUiThread(this::finish);
    }

    public String getSecureStorageKey(String key) {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(key, "");
    }

    public void setSecureStorageKey(String key, String value) {
        final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.putString(key, value);
        edit.apply();
    }

    public String getPlatformStringVar(int id) {
        if (id == 0) {
            return Build.MODEL;
        }
        return null;
    }

    public boolean isNetworkEnabled(boolean onlyWifiAllowed) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(9);
        if (networkInfo == null || !networkInfo.isConnected()) {
            networkInfo = connectivityManager.getNetworkInfo(1);
            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
            networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected() && !onlyWifiAllowed;
        }
        return true;
    }

    public boolean isOnWifi() {
        return ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(1).isConnectedOrConnecting();
    }

    public void setSession(String sessionId) {
        final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.putString("sessionID", sessionId);
        edit.apply();
    }

    public void setRefreshToken(String refreshToken) {
        final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.putString("refreshToken", refreshToken);
        edit.apply();
    }

    public void setLoginInformation(String accessToken, String clientId, String profileId, String profileName) {
        final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.putString("accessToken", accessToken);
        edit.putString("clientId", clientId);
        edit.putString("profileId", profileId);
        edit.putString("profileName", profileName);
        edit.apply();
    }

    public void clearLoginInformation() {
        final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.remove("accessToken");
        edit.remove("clientId");
        edit.remove("profileId");
        edit.remove("profileName");
        edit.apply();
    }

    public String getAccessToken() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("accessToken", "");
    }

    public String getClientId() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("clientId", "");
    }

    public String getProfileId() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("profileId", "");
    }

    public String getProfileName() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("profileName", "");
    }

    public void runNativeCallbackOnUiThread(final long fn) {
        FutureTask futureTask = new FutureTask(() -> {
            nativeRunNativeCallbackOnUiThread(fn);
            return null;
        });
        runOnUiThread(futureTask);
        try {
            futureTask.get();
        } catch (Exception unused) {
        }
    }

    public String[] getBroadcastAddresses() {
        final ArrayList<String> arrayList = new ArrayList<>();
        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                final NetworkInterface nextElement = networkInterfaces.nextElement();
                if (!nextElement.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : nextElement.getInterfaceAddresses()) {
                        if (interfaceAddress.getBroadcast() != null) {
                            arrayList.add(interfaceAddress.getBroadcast().toString().substring(1));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList.toArray(new String[arrayList.size()]);
    }

    boolean isHardwareKeyboardHidden() {
        return getWindow().getContext().getResources().getConfiguration().hardKeyboardHidden == 2;
    }

    boolean isTablet() {
        if (isChromebook()) {
            return isHardwareKeyboardHidden();
        }
        return getProp("ro.build.characteristics").contains("tablet");
    }

    boolean isChromebook() {
        return getWindow().getContext().getPackageManager().hasSystemFeature("android.hardware.type.pc");
    }

    String getProp(String propertyKey) {
        try {
            if (getPropMethod == null || SystemProperties == null) {
                @SuppressLint("PrivateApi") Class<?> cls = Class.forName("android.os.SystemProperties");
                SystemProperties = cls;
                getPropMethod = cls.getMethod("get", String.class);
            }
            return (String) getPropMethod.invoke(SystemProperties, propertyKey);
        } catch (Exception e) {
            Log.e("ModdedPE", "Exception occured while getting a property [" + propertyKey + "]\n" + e.getMessage());
            return "";
        }
    }

    String chromebookCompatibilityIP() {
        return isChromebook() ? getProp("arc.net.ipv4.host_address") : "";
    }

    public String[] getIPAddresses() {
        final ArrayList<String> arrayList = new ArrayList<>();
        final String chromebookCompatibilityIP = chromebookCompatibilityIP();
        if (!chromebookCompatibilityIP.isEmpty()) {
            arrayList.add(chromebookCompatibilityIP);
        }
        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                final NetworkInterface nextElement = networkInterfaces.nextElement();
                if (!nextElement.isLoopback() && nextElement.isUp()) {
                    for (InterfaceAddress interfaceAddress : nextElement.getInterfaceAddresses()) {
                        final InetAddress address = interfaceAddress.getAddress();
                        if (address != null && !address.isAnyLocalAddress() && !address.isMulticastAddress() && !address.isLinkLocalAddress()) {
                            arrayList.add(interfaceAddress.getAddress().toString().substring(1));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList.toArray(new String[arrayList.size()]);
    }

    public void initiateUserInput(int id) {
        _userInputText = null;
        _userInputStatus = -1;
    }

    public int getUserInputStatus() {
        return _userInputStatus;
    }

    public String[] getUserInputString() {
        return _userInputText;
    }

    public void vibrate(int milliSeconds) {
        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(milliSeconds);
    }

    public void onDialogCanceled() {
        _userInputStatus = 0;
    }

    ActivityManager.MemoryInfo getMemoryInfo() {
        final long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis >= mCachedMemoryInfoUpdateTime) {
            ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mCachedMemoryInfo);
            mCachedMemoryInfoUpdateTime = uptimeMillis + 2000;
        }
        return mCachedMemoryInfo;
    }

    public long getTotalMemory() {
        ActivityManager.MemoryInfo memoryInfo = getMemoryInfo();
        long j = memoryInfo.totalMem;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Process.is64Bit() || 4294967296L >= j) {
            return j;
        }
        return 4294967296L;
    }

    public long getFreeMemory() {
        final ActivityManager.MemoryInfo memoryInfo = getMemoryInfo();
        return memoryInfo.availMem - memoryInfo.threshold;
    }

    public long getMemoryLimit() {
        return getTotalMemory() - getMemoryInfo().threshold;
    }

    public long getUsedMemory() {
        final long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis >= mCachedUsedMemoryUpdateTime) {
            mCachedUsedMemory = Debug.getNativeHeapAllocatedSize();
            mCachedUsedMemoryUpdateTime = uptimeMillis + 10000;
        }
        return mCachedUsedMemory;
    }

    public long getDebugMemoryInfo(String statName) {
        if (statName == null) {
            return 0L;
        }
        final long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis >= mCachedDebugMemoryUpdateTime) {
            Debug.getMemoryInfo(mCachedDebugMemoryInfo);
            mCachedDebugMemoryUpdateTime = uptimeMillis + 1000;
        }
        char c = 65535;
        switch (statName.hashCode()) {
            case -785265396:
                if (statName.equals("TotalPss")) {
                    c = 0;
                    break;
                }
                break;
            case 1974219091:
                if (statName.equals("TotalPrivateDirty")) {
                    c = 1;
                    break;
                }
                break;
            case 2139129981:
                if (statName.equals("TotalSwappablePss")) {
                    c = 2;
                    break;
                }
                break;
        }
        int totalPss;
        switch (c) {
            case 0:
                totalPss = mCachedDebugMemoryInfo.getTotalPss();
                break;
            case 1:
                totalPss = mCachedDebugMemoryInfo.getTotalPrivateDirty();
                break;
            case 2:
                totalPss = mCachedDebugMemoryInfo.getTotalSwappablePss();
                break;
            default:
                if (Build.VERSION.SDK_INT >= 23) {
                    final String memoryStat = mCachedDebugMemoryInfo.getMemoryStat("summary." + statName);
                    if (memoryStat != null) {
                        return Long.parseLong(memoryStat);
                    }
                    return 0L;
                }
                return 0L;
        }
        return totalPss;
    }

    public long calculateAvailableDiskFreeSpace(String rootPath) {
        try {
            return new StatFs(rootPath).getAvailableBytes();
        } catch (Exception unused) {
            return 0L;
        }
    }

    public void onDialogCompleted() {
        final int size = _userInputValues.size();
        _userInputText = new String[size];
        for (int i = 0; i < size; i++) {
            _userInputText[i] = _userInputValues.get(i).getStringValue();
        }
        for (String str : _userInputText) {
            System.out.println("js: " + str);
        }
        _userInputStatus = 1;
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(getCurrentFocus(), InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    protected void onStart() {
        Log.d("ModdedPE", "onStart");
        super.onStart();
        deviceManager.register();
        if (_fromOnCreate) {
            _fromOnCreate = false;
            processIntent(getIntent());
        }
    }

    @Override
    public void onResume() {
        Log.d("ModdedPE", "onResume");
        super.onResume();
        registerReceiver(headsetConnectionReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        if (isTextWidgetActive()) {
            final String text = textInputWidget.getText().toString();
            final int allowedLength = textInputWidget.allowedLength;
            final int inputType = textInputWidget.getInputType();

            final boolean isNumberInput = (inputType & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER;
            final boolean isMultiline = (inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            dismissTextWidget();
            showKeyboard(text, allowedLength, false, isNumberInput, isMultiline);
        }
        for (ActivityListener activityListener : mActivityListeners) {
            activityListener.onResume();
        }
    }

    public String getTextBoxBackend() {
        return mCurrentTextMirror.get();
    }

    public void setTextBoxBackend(final String newText) {
        runOnUiThread(() -> {
            textInputWidget.setText(newText);
            mCurrentTextMirror.set(newText);
            setCaretPosition(newText.length());
        });
    }

    public int getCaretPosition() {
        return mCaretPositionMirror.get();
    }

    public void setCaretPosition(final int caretPosition) {
        runOnUiThread(() -> {
            int i = caretPosition;
            int length = textInputWidget.getText().toString().length();
            if (i < 0 || i > length) {
                i = length;
            }
            textInputWidget.setSelection(i);
            mCaretPositionMirror.set(i);
        });
    }

    @Override
    public void onPause() {
        Log.d("ModdedPE", "onPause");
        nativeSuspend();
        super.onPause();
        if (isFinishing()) {
            nativeShutdown();
        }
    }

    @Override
    protected void onStop() {
        Log.d("ModdedPE", "onStop");
        nativeStopThis();
        super.onStop();
        deviceManager.unregister();
        for (ActivityListener activityListener : this.mActivityListeners) {
            activityListener.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("ModdedPE", "onDestroy");
        if (isChangingConfigurations()) {
            Log.d("ModdedPE", "Unhandled changing configurations, changingConfigurations=" + getChangingConfigurations());
        }
        System.out.println("onDestroy");
        FMOD.close();
        for (ActivityListener listener : new ArrayList<>(mActivityListeners)) {
            listener.onDestroy();
        }
        removeListener(mFilePickerManager);
        mInstance = null;
        nativeOnDestroy();
        super.onDestroy();
        System.exit(0);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String stringExtra = intent.getStringExtra("intent_cmd");
        if (stringExtra != null && !stringExtra.isEmpty()) {
            try {
                JSONObject jSONObject = new JSONObject(stringExtra);
                String string = jSONObject.getString("Command");
                if (string.equals("keyboardResult")) {
                    nativeSetTextboxText(jSONObject.getString("Text"));
                    return;
                } else if (!string.equals("fileDialogResult") || mFileDialogCallback == 0) {
                    return;
                } else {
                    if (jSONObject.getString("Result").equals("Ok")) {
                        nativeOnPickImageSuccess(mFileDialogCallback, jSONObject.getString("Path"));
                    } else {
                        nativeOnPickImageCanceled(mFileDialogCallback);
                    }
                    mFileDialogCallback = 0L;
                    return;
                }
            } catch (JSONException e) {
                Log.d("ModdedPE", "JSONObject exception:" + e);
                return;
            }
        }
        String action = intent.getAction();
        if ("xbox_live_game_invite".equals(action)) {
            String stringExtra2 = intent.getStringExtra("xbl");
            Log.d("ModdedPE", "[XboxLive] Received Invite " + stringExtra2);
            nativeProcessIntentUriQuery(action, stringExtra2);
        } else if (!"android.intent.action.VIEW".equals(action) && !"org.chromium.arc.intent.action.VIEW".equals(action)) {
        } else {
            String scheme = intent.getScheme();
            Uri data = intent.getData();
            if (data == null) {
                return;
            }
            if ("minecraft".equalsIgnoreCase(scheme) || "minecraftedu".equalsIgnoreCase(scheme)) {
                String host = data.getHost();
                String query = data.getQuery();
                if (host == null && query == null) {
                    return;
                }
                nativeProcessIntentUriQuery(host, query);
            } else if ("file".equalsIgnoreCase(scheme)) {
                nativeProcessIntentUriQuery("fileIntent", data.getPath() + "&" + data.getPath());
            } else if (!"content".equalsIgnoreCase(scheme)) {
            } else {
                String name = new File(data.getPath()).getName();
                File file = new File(getApplicationContext().getCacheDir() + "/" + name);
                try {
                    InputStream openInputStream = getContentResolver().openInputStream(data);
                    try {
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(file);
                            byte[] bArr = new byte[1048576];
                            while (true) {
                                int read = openInputStream.read(bArr);
                                if (read != -1) {
                                    fileOutputStream.write(bArr, 0, read);
                                } else {
                                    fileOutputStream.close();
                                    nativeProcessIntentUriQuery("contentIntent", data.getPath() + "&" + file.getAbsolutePath());
                                    try {
                                        openInputStream.close();
                                        return;
                                    } catch (IOException e) {
                                        Log.e("ModdedPE", "IOException while closing input stream\n" + e.toString());
                                    }
                                }
                            }
                        } catch (Throwable th) {
                            try {
                                openInputStream.close();
                            } catch (IOException e3) {
                                Log.e("ModdedPE", "IOException while closing input stream\n" + e3);
                            }
                            throw th;
                        }
                    } catch (IOException e4) {
                        Log.e("ModdedPE", "IOException while copying file from content intent\n" + e4);
                        try {
                            file.delete();
                        } catch (Exception unused) {
                        }
                        try {
                            openInputStream.close();
                        } catch (IOException e) {
                            Log.e("ModdedPE", "IOException while closing input stream\n" + e.toString());
                        }
                    }
                } catch (IOException e) {
                    Log.e("ModdedPE", "IOException while opening file from content intent\n" + e);
                }
            }
        }
    }

    public boolean isFirstSnooperStart() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("snooperId", "").isEmpty();
    }

    public String getLegacyDeviceID() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("snooperId", "");
    }

    public String createUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public Intent createAndroidLaunchIntent() {
        final Context applicationContext = getApplicationContext();
        return applicationContext.getPackageManager().getLaunchIntentForPackage(applicationContext.getPackageName());
    }

    boolean hasHardwareChanged() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String string = defaultSharedPreferences.getString("lastAndroidVersion", "");
        final boolean z = string.isEmpty() || !string.equals(Build.VERSION.RELEASE);
        if (z) {
            final SharedPreferences.Editor edit = defaultSharedPreferences.edit();
            edit.putString("lastAndroidVersion", Build.VERSION.RELEASE);
            edit.apply();
        }
        return z;
    }

    public boolean isMulticastHeld() {
        WifiManager.MulticastLock multicastLock = this.mMulticastLock;
        if (multicastLock != null) {
            return multicastLock.isHeld();
        }
        return false;
    }

    void pickImage(long callback) {
        mCallback = callback;
        try {
            startActivityForResult(new Intent("android.intent.action.PICK", MediaStore.Images.Media.EXTERNAL_CONTENT_URI), RESULT_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    void setFileDialogCallback(long callback) {
        mFileDialogCallback = callback;
    }

    void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, OPEN_FILE_RESULT_CODE);
    }

    public void shareFile(String subject, String title, String filePath) {
        Uri uriForFile = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", new File(filePath));
        Intent intent = new Intent();
        intent.setAction("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.SUBJECT", subject);
        intent.putExtra("android.intent.extra.TITLE", title);
        intent.putExtra("android.intent.extra.STREAM", uriForFile);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uriForFile, getContentResolver().getType(uriForFile));
        startActivity(Intent.createChooser(intent, subject));
    }

    void saveFile(String defaultFileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, defaultFileName);
        startActivityForResult(intent, SAVE_FILE_RESULT_CODE);
    }

    void onPickFileSuccess(boolean shouldCopy) {
        final String str = getApplicationContext().getCacheDir() + "/tempPickedFile";
        if (shouldCopy) {
            copyFromPickedFile(str);
        }
        nativeOnPickFileSuccess(str);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intentData) {
        super.onActivityResult(requestCode, resultCode, intentData);
        if (resultCode == Activity.RESULT_CANCELED) {
            nativeOnPickFileCanceled();
            return;
        }
        if (resultCode == Activity.RESULT_OK && intentData != null && intentData.getData() != null) {
            Uri data = intentData.getData();
            try {
                if (requestCode == OPEN_FILE_RESULT_CODE || requestCode == SAVE_FILE_RESULT_CODE) {
                    mPickedFileDescriptor = getContentResolver().openFileDescriptor(data, requestCode == OPEN_FILE_RESULT_CODE ? "r" : "w");
                    onPickFileSuccess(requestCode == OPEN_FILE_RESULT_CODE);
                } else if (requestCode == RESULT_PICK_IMAGE) {
                    String[] projection = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(data, projection, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        String filePath = cursor.getString(cursor.getColumnIndexOrThrow(projection[0]));
                        nativeOnPickImageSuccess(mCallback, filePath);
                        cursor.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == RESULT_PICK_IMAGE) {
            nativeOnPickImageCanceled(mCallback);
        }
    }


    @Override
    public void startPickerActivity(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
    }

    public void addListener(ActivityListener listener) {
        mActivityListeners.add(listener);
    }

    public void removeListener(ActivityListener listener) {
        mActivityListeners.remove(listener);
    }

    public void setVolume(float volume) {
        mVolume = volume;
    }

    public void startTextToSpeech(String s) {
        if (textToSpeechManager != null) {
            Bundle bundle = new Bundle();
            bundle.putFloat("volume", mVolume);
            textToSpeechManager.speak(s, 0, bundle, null);
        }
    }

    public void stopTextToSpeech() {
        final TextToSpeech textToSpeech = this.textToSpeechManager;
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public boolean isTextToSpeechInProgress() {
        final TextToSpeech textToSpeech = this.textToSpeechManager;
        if (textToSpeech != null) {
            return textToSpeech.isSpeaking();
        }
        return false;
    }

    public int getAPIVersion(String apiName) {
        for (Field field : Build.VERSION_CODES.class.getFields()) {
            if (field.getName().equals(apiName)) {
                try {
                    return field.getInt(new Object());
                } catch (IllegalAccessException unused) {
                    Log.e("ModdedPE", "IllegalAccessException in getApiVersion(" + apiName + ")");
                } catch (IllegalArgumentException unused2) {
                    Log.e("ModdedPE", "IllegalArgumentException in getApiVersion(" + apiName + ")");
                } catch (NullPointerException unused3) {
                    Log.e("ModdedPE", "NullPointerException in getApiVersion(" + apiName + ")");
                }
            }
        }
        Log.e("ModdedPE", "Failed to find API version for: " + apiName);
        return -1;
    }

    private void configureBrazeAtRuntime() {
    }

    public String MC_GetActiveScreen() {
        return isTestInfrastructureDisabled() ? "" : nativeGetActiveScreen();
    }

    public String MC_SetOptions(String optionsString) {
        return isTestInfrastructureDisabled() ? "" : nativeSetOptions(optionsString);
    }

    public String MC_CheckIfTestsAreFinished() {
        return isTestInfrastructureDisabled() ? "" : nativeCheckIfTestsAreFinished();
    }

    public String MC_GetDevConsoleLogName() {
        return isTestInfrastructureDisabled() ? "" : nativeGetDevConsoleLogName();
    }

    public String MC_GetLogText(String fileInfo) {
        return isTestInfrastructureDisabled() ? "" : nativeGetLogText(fileInfo);
    }

    public boolean getIsRunningInAppCenter() {
        return mIsRunningInAppCenter;
    }

    public boolean getIsRunningInBrowserStack() {
        return getIntent().getBooleanExtra("IS_BROWSERSTACK", false);
    }

    public boolean isTTSInstalled() {
        for (AccessibilityServiceInfo accessibilityServiceInfo : ((AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE)).getInstalledAccessibilityServiceList()) {
            if ((accessibilityServiceInfo.feedbackType & 1) != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isTTSEnabled() {
        final Context applicationContext = getApplicationContext();
        if (isTestInfrastructureDisabled() || !mIsRunningInAppCenter) {
            final AccessibilityManager accessibilityManager;
            return applicationContext != null && (accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE)) != null && accessibilityManager.isEnabled() && !accessibilityManager.getEnabledAccessibilityServiceList(1).isEmpty();
        }
        Log.w("ModdedPE", "Automation: We are running in AppCenter, forcing isTTSEnabled to false to avoid screen reader popup");
        return false;
    }

    public long initializeLibHttpClient(final long hcInitArgs) {
        final FutureTask<Long> futureTask = new FutureTask<>(() -> nativeInitializeLibHttpClient(hcInitArgs));
        runOnUiThread(futureTask);
        try {
            return futureTask.get();
        } catch (Exception e) {
            Log.e("ModdedPE", "Failed to initialize libHttpClient: '" + e.getMessage() + "'");
            return -2147467259L;
        }
    }

    public long initializeXboxLive(final long xalInitArgs, final long xblInitArgs) {
        if (!isEduMode()) {
            final FutureTask<Long> futureTask = new FutureTask<>(() -> {
                FirebaseApp.initializeApp(getApplicationContext());
                return nativeInitializeXboxLive(xalInitArgs, xblInitArgs);
            });
            runOnUiThread(futureTask);
            try {
                return futureTask.get();
            } catch (Exception e) {
                Log.e("ModdedPE", "Failed to initialize xbox live: '" + e.getMessage() + "'");
                return -2147467259L;
            }
        }
        return 0L;
    }

    public void startPlayIntegrityCheck() {
    }

    public boolean supportsSizeQuery(String path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                getApplicationContext().getSystemService(StorageManager.class).getUuidForPath(new File(path));
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public long getAllocatableBytes(String path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                final StorageManager storageManager = getApplicationContext().getSystemService(StorageManager.class);
                return storageManager.getAllocatableBytes(storageManager.getUuidForPath(new File(path)));
            } catch (IOException e) {
                Log.e("ModdedPE", "IOException while attempting to get allocatable bytes\n" + e);
                return 0L;
            }
        }
        return 0L;
    }

    public int getPlatformDpi() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return (int) ((displayMetrics.xdpi + displayMetrics.ydpi) * 0.5f);
    }

    public void setKeepScreenOnFlag(final boolean keepScreenOn) {
        runOnUiThread(() -> {
            if (keepScreenOn) {
                getWindow().addFlags(128);
            } else {
                getWindow().clearFlags(128);
            }
        });
    }

    public long getTimeFromProcessStart() {
        if (Build.VERSION.SDK_INT >= 24) {
            return SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime();
        }
        return 0L;
    }

    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MainActivity.MSG_CORRELATION_RESPONSE) {
                final String packageName = getApplicationContext().getPackageName();
                final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                defaultSharedPreferences.getString("deviceId", "");
                try {
                    final long firstInstallTime = getPackageManager().getPackageInfo(packageName, 0).firstInstallTime;
                    final String deviceId = msg.getData().getString("deviceId");
                    final String sessionId = msg.getData().getString("sessionId");
                    long time = msg.getData().getLong("time");
                    if (firstInstallTime > time) {
                        defaultSharedPreferences.edit().apply();
                        nativeDeviceCorrelation(firstInstallTime, deviceId, time, sessionId);
                    }
                    final SharedPreferences.Editor edit = defaultSharedPreferences.edit();
                    edit.putInt("correlationAttempts", 0);
                    edit.apply();
                    if (mBound == MessageConnectionStatus.CONNECTED) {
                        unbindService(mConnection);
                        return;
                    }
                    return;
                } catch (PackageManager.NameNotFoundException unused) {
                    return;
                }
            }
            super.handleMessage(msg);
        }
    }

    private class HeadsetConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                final int intExtra = intent.getIntExtra("state", -1);
                if (intExtra == 0) {
                    Log.d("ModdedPE", "Headset unplugged");
                    nativeSetHeadphonesConnected(false);
                } else if (intExtra != 1) {
                } else {
                    Log.d("ModdedPE", "Headset plugged in");
                    nativeSetHeadphonesConnected(true);
                }
            }
        }
    }
}
