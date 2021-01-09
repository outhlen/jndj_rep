package com.example.administrator.myapplication;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import java.io.File;

public class JsInterface {
    private Context mContext;
    private  CallBackInterface callBackInterface;

    public JsInterface(Context context,CallBackInterface callBackInterface) {
        this.mContext = context;
        this.callBackInterface  = callBackInterface;
    }

    //在js中调用jsonObject.getImage()，便会触发此方法。
    @JavascriptInterface
    public void takePhonto() {
        Log.e("Log", "h5进行调用");
        callBackInterface.takePicure(200);
    }

    @JavascriptInterface
    public void uploadFile(File file) {
        Log.e("Log", "图片上传");
        callBackInterface.uploadFile(file);
    }

}
