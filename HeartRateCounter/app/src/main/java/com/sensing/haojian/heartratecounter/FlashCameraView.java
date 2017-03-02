package com.sensing.haojian.heartratecounter;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

/**
 * Created by haojian on 3/1/17.
 */

public class FlashCameraView  extends JavaCameraView {
    private boolean ifFlashOn = false;

    public FlashCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setFlashONOFF(boolean onoff) {
        Camera.Parameters params = mCamera.getParameters();
        if (onoff) {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
        ifFlashOn = onoff;
    }

    public boolean getFlashMode() {
        return ifFlashOn;
    }

}
