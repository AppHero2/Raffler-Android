package com.raffler.app.interfaces;

/**
 * Created by Ghost on 9/8/2017.
 */

public interface FileUploadListener {
    void onSuccess(boolean result, String path);
    void onProgress(int percent);
    void onError(Exception e);
}
