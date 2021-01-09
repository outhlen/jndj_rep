package com.example.administrator.myapplication;

import java.io.File;

public interface CallBackInterface {
    void takePicure(int requestCode);
    void uploadFile(File file);
}
