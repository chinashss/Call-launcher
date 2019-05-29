package cn.szx.zbarscanner.base;

import android.hardware.Camera;

public class CameraWrapper {
    public Camera camera;
    public int cameraId;

    private CameraWrapper(Camera camera, int cameraId) {
        this.camera = camera;
        this.cameraId = cameraId;
    }

    public static CameraWrapper getWrapper(Camera camera, int cameraId) {
        if (camera == null) {
            return null;
        } else {
            return new CameraWrapper(camera, cameraId);
        }
    }
}