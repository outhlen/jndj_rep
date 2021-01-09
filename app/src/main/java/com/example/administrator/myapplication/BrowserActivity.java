package com.example.administrator.myapplication;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.administrator.myapplication.utils.FileUtils;
import com.example.administrator.myapplication.utils.X5WebView;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient.CustomViewCallback;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebSettings.LayoutAlgorithm;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;
import com.tencent.smtt.utils.TbsLog;

import org.json.JSONException;
import org.json.JSONObject;

public class BrowserActivity extends Activity   {

    public static final int  CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE  = 1001;
    /**
     * 作为一个浏览器的示例展示出来，采用android+web的模式
     */
    private X5WebView mWebView;
    private ViewGroup mViewParent;

    private static final String url = "https://jndjapp.rcdqej.gov.cn";
    private static final String TAG = "SdkDemo";
    private static final int MAX_LENGTH = 14;
    private boolean mNeedTestPage = false;
    private final int disable = 120;
    private final int enable = 255;

    private ProgressBar mPageLoadingProgressBar = null;

    private ValueCallback<Uri> uploadFile;

    private URL mIntentUrl;

    private String iamgeUrl  = null;

    Button button  = null;

    Uri imageUri  = null;
    File tmepFile =  null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
		Intent intent = getIntent();
		if (intent != null) {
			try {
				mIntentUrl = new URL(url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {

			}
		}
        try {
            if (Integer.parseInt(android.os.Build.VERSION.SDK) >= 11) {
                getWindow()
                        .setFlags(
                                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            }
        } catch (Exception e) {
        }

        /*
         * getWindow().addFlags(
         * android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
         */
        setContentView(R.layout.activity_main);
        mViewParent = (ViewGroup) findViewById(R.id.webView1);
        button  = findViewById(R.id.button);
        mTestHandler.sendEmptyMessageDelayed(MSG_INIT_UI, 10);

        if (Build.VERSION.SDK_INT > 22) {
            ActivityCompat.requestPermissions(BrowserActivity.this,
                    new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

//        button.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                takePicure();
//            }
//        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //这里已经获取到了摄像头的权限，想干嘛干嘛了可以
                } else {
                    //这里是拒绝给APP摄像头权限，给个提示什么的说明一下都可以。
                    Toast.makeText(BrowserActivity.this, "请手动打开相机权限", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }


    private void init() {
        mWebView = new X5WebView(this, null);
        mViewParent.addView(mWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT,
                FrameLayout.LayoutParams.FILL_PARENT));
        mWebView.requestFocus();
        //mWebView.addJavascriptInterface(new JsInterface(this,this), "takePhoto");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String pre = "protocol://android";
                if (!url.contains(pre)) {
                    //该url是http请求，用webview加载url
                    view.loadUrl(url);
                    return false;
                }
                //该url是调用android方法的请求，通过解析url中的参数来执行相应方法
                Map<String, String> map = getParamsMap(url, pre);
                String code = map.get("code");
                String data = map.get("data");
                parseCode(code, data);
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // super.onReceivedSslError(view, handler, error);
                if (handler != null) {
                    handler.proceed();//忽略证书的错误继续加载页面内容，不会变成空白页面
                }
            }


            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // mTestHandler.sendEmptyMessage(MSG_OPEN_TEST_URL);
                mTestHandler.sendEmptyMessageDelayed(MSG_OPEN_TEST_URL, 5000);// 5s?
//				if (Integer.parseInt(android.os.Build.VERSION.SDK) >= 16)
//					changGoForwardButton(view);
                /* mWebView.showLog("test Log"); */
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView arg0, String arg1, String arg2,
                                       JsResult arg3) {
                return super.onJsConfirm(arg0, arg1, arg2, arg3);
            }
            View myVideoView;
            View myNormalView;
            CustomViewCallback callback;


            /**
             * 全屏播放配置
             */
            @Override
            public void onShowCustomView(View view,
                                         CustomViewCallback customViewCallback) {
//				FrameLayout normalView = (FrameLayout) findViewById(R.id.web_filechooser);
//				ViewGroup viewGroup = (ViewGroup) normalView.getParent();
//				viewGroup.removeView(normalView);
//				viewGroup.addView(view);
//				myVideoView = view;
//				myNormalView = normalView;
//				callback = customViewCallback;
            }

            @Override
            public void onHideCustomView() {
                if (callback != null) {
                    callback.onCustomViewHidden();
                    callback = null;
                }
                if (myVideoView != null) {
                    ViewGroup viewGroup = (ViewGroup) myVideoView.getParent();
                    viewGroup.removeView(myVideoView);
                    viewGroup.addView(myNormalView);
                }
            }

            @Override
            public boolean onJsAlert(WebView arg0, String arg1, String arg2,
                                     JsResult arg3) {
                /**
                 * 这里写入你自定义的window alert
                 */
                return super.onJsAlert(null, arg1, arg2, arg3);
            }
        });

        mWebView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String arg0, String arg1, String arg2, String arg3, long arg4) {
                TbsLog.d(TAG, "url: " + arg0);
                new AlertDialog.Builder(BrowserActivity.this)
                        .setTitle("allow to download？")
                        .setPositiveButton("yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        Toast.makeText(
                                                BrowserActivity.this,
                                                "fake message: i'll download...",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                        .setNegativeButton("no",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        // TODO Auto-generated method stub
                                        Toast.makeText(
                                                BrowserActivity.this,
                                                "fake message: refuse download...",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                        .setOnCancelListener(
                                new DialogInterface.OnCancelListener() {

                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        // TODO Auto-generated method stub
                                        Toast.makeText(
                                                BrowserActivity.this,
                                                "fake message: refuse download...",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }).show();
            }
        });

        WebSettings webSetting = mWebView.getSettings();
        webSetting.setAllowFileAccess(true);
        webSetting.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);
        webSetting.setSupportZoom(true);
        webSetting.setBuiltInZoomControls(true);
        webSetting.setUseWideViewPort(true);
        webSetting.setSupportMultipleWindows(false);
        // webSetting.setLoadWithOverviewMode(true);
        webSetting.setAppCacheEnabled(true);
        // webSetting.setDatabaseEnabled(true);
        webSetting.setDomStorageEnabled(true);
        webSetting.setJavaScriptEnabled(true);
        webSetting.setGeolocationEnabled(true);
        webSetting.setAppCacheMaxSize(Long.MAX_VALUE);
        webSetting.setAppCachePath(this.getDir("appcache", 0).getPath());
        webSetting.setDatabasePath(this.getDir("databases", 0).getPath());
        webSetting.setGeolocationDatabasePath(this.getDir("geolocation", 0)
                .getPath());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSetting.setAllowFileAccessFromFileURLs(true);
            webSetting.setAllowUniversalAccessFromFileURLs(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSetting.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        // webSetting.setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);
        webSetting.setPluginState(WebSettings.PluginState.ON_DEMAND);
        // webSetting.setRenderPriority(WebSettings.RenderPriority.HIGH);
        // webSetting.setPreFectch(true);
        long time = System.currentTimeMillis();
        if (mIntentUrl == null) {
            mWebView.loadUrl(url);
        } else {
            mWebView.loadUrl(mIntentUrl.toString());
        }
        TbsLog.d("time-cost", "cost time: "
                + (System.currentTimeMillis() - time));
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().sync();


        mWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void acceptUrl(String url,String base64) {
                String path  = tmepFile.getPath();
                String sourceFile =    FileUtils.imageToBase64(path);
                mWebView.loadUrl("javascript:acceptUrl(\"" + path + "\",\"" + sourceFile + "\")");
//                Toast.makeText(getApplicationContext(),
//                        "新图片地址："+url, Toast.LENGTH_SHORT).show();
            }
            @JavascriptInterface
            public void takePhoto() {
                takePicure();
            }
        }, "AndroidJS");
    }

    private String acceptUrl(String path) {
        if(path==null){
           return null;
        }
       return path;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
//				if (Integer.parseInt(android.os.Build.VERSION.SDK) >= 16)
//					changGoForwardButton(mWebView);
                return true;
            } else
                return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        TbsLog.d(TAG, "onActivityResult, requestCode:" + requestCode
                + ",resultCode:" + resultCode);
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
              if(resultCode == RESULT_OK){
                  if(data!=null){
                      Bitmap thumbnail = data.getParcelableExtra("data");
                      //得到bitmap后的操作
                  }else{
                      if(tmepFile!=null){
                          uploadImageFile(null,new File(tmepFile.getPath()));
                      }
                  }
              }
        }else{
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case 0:
                        if (null != uploadFile) {
                            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
                            uploadFile.onReceiveValue(result);
                            uploadFile = null;
                        }
                        break;
                    default:
                        break;
                }
            } else if (resultCode == RESULT_CANCELED) {
                if (null != uploadFile) {
                    uploadFile.onReceiveValue(null);
                    uploadFile = null;
                }
            }
        }

    }

    /**
     *  图片上报
     * @param file  调用接口上报
     */
    private void uploadImageFile(Bitmap bitmap,File file) {
        String sourceFile  = null;
        Logger.getLogger("压缩前地址："+file.getTotalSpace());
        String path  = file.getPath();
        if(compressBitmapToFile(bitmap,file)){ //压缩成功
            Logger.getLogger("压缩后地址："+file.getTotalSpace());
            sourceFile =    FileUtils.imageToBase64(path);
            mWebView.loadUrl("javascript:acceptUrl(\"" + path + "\",\"" + sourceFile + "\")");
        }else{
            sourceFile =    FileUtils.imageToBase64(path);
            mWebView.loadUrl("javascript:acceptUrl(\"" + path + "\",\"" + sourceFile + "\")");
        }
    }

    //采样率压缩文件
    public static boolean compressBitmapToFile(Bitmap bitmap,File file){
       boolean isCompress  = true;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap result = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 把压缩后的数据存放到baos中
        result.compress(Bitmap.CompressFormat.JPEG, 90 ,baos);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            isCompress  = false;
        }
        return  isCompress;
    }


    @Override
    protected void onDestroy() {
        if (mTestHandler != null)
            mTestHandler.removeCallbacksAndMessages(null);
        if (mWebView != null)
            mWebView.destroy();
        super.onDestroy();
    }

    public static final int MSG_OPEN_TEST_URL = 0;
    public static final int MSG_INIT_UI = 1;
    private final int mUrlStartNum = 0;
    private int mCurrentUrl = mUrlStartNum;
    private Handler mTestHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_OPEN_TEST_URL:
                    if (!mNeedTestPage) {
                        return;
                    }
                    String testUrl = "file:///sdcard/outputHtml/html/"
                            + Integer.toString(mCurrentUrl) + ".html";
                    if (mWebView != null) {
                        mWebView.loadUrl(testUrl);
                    }
                    mCurrentUrl++;
                    break;
                case MSG_INIT_UI:
                    init();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void takePicure() {
        //在这里做具体安卓原生拍照，相册代码
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
         String filePath  = Environment.getExternalStorageDirectory().getPath()+"/temp";
         tmepFile   =  createOrExistsFile(filePath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this,"com.example.administrator.myapplication.fileProvider", tmepFile));
        } else {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmepFile));
        }
        startActivityForResult(takePictureIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }


    public File createOrExistsFile(String path)  {
       String fileName  = SystemClock.currentThreadTimeMillis()+"_photo.jpg";
       File dirFile   = new File(path);
        File file = new File(path,fileName);
       if(!dirFile.exists()){
           try
           {dirFile.mkdirs();}
           catch (Exception e)
           { e.printStackTrace();}

       }
        if(!file.exists()) {
            try
            {file.createNewFile();}catch (Exception e)
            { e.printStackTrace();}
            Log.e("createOrExistsFile","ok"+path);
        }
        return file;
    }

    private Map<String, String> getParamsMap(String url, String pre) {
        ArrayMap<String, String> queryStringMap = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            queryStringMap = new ArrayMap<>();
        }
        if (url.contains(pre)) {
            int index = url.indexOf(pre);
            int end = index + pre.length();
            String queryString = url.substring(end + 1);
            String[] queryStringSplit = queryString.split("&");

            String[] queryStringParam;
            for (String qs : queryStringSplit) {
                if (qs.toLowerCase().startsWith("data=")) {
                    //单独处理data项，避免data内部的&被拆分
                    int dataIndex = queryString.indexOf("data=");
                    String dataValue = queryString.substring(dataIndex + 5);
                    queryStringMap.put("data", dataValue);
                } else {
                    queryStringParam = qs.split("=");
                    String value = "";
                    if (queryStringParam.length > 1) {
                        //避免后台有时候不传值,如“key=”这种
                        value = queryStringParam[1];
                    }
                    queryStringMap.put(queryStringParam[0].toLowerCase(), value);
                }
            }
        }
        return queryStringMap;
    }

    private void parseCode(String code, String data) { //acceptUrl
        if(code.contains("takePhoto")) {
            takePicure();
        }
//        if(code.contains("acceptUrl")) {
//           // acceptUrl(tmepFile.getPath());
//            String path  = tmepFile.getPath();
//            mWebView.loadUrl("javascript:acceptUrl("+path+")");
//        }

//        if(code.equals("toast")) {
//            try {
//                JSONObject json = new JSONObject(data);
//                String toast = json.optString("data");
//                Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            return;
//        }
    }

}
