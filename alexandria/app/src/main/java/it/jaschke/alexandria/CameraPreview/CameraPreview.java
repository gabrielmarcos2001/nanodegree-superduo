package it.jaschke.alexandria.CameraPreview;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.zxing.PlanarYUVLuminanceSource;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by gabrielmarcos on 9/25/14.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static boolean DEBUGGING = false;

    private static final String LOG_TAG = "CameraPreview";
    private static final String CAMERA_PARAM_ORIENTATION = "orientation";
    private static final String CAMERA_PARAM_LANDSCAPE = "landscape";
    private static final String CAMERA_PARAM_PORTRAIT = "portrait";

    protected Activity mActivity;
    private SurfaceHolder mHolder;

    protected Camera mCamera;
    protected List<Camera.Size> mPreviewSizeList;
    protected List<Camera.Size> mPictureSizeList;
    protected Camera.Size mPreviewSize;
    protected Camera.Size mPictureSize;
    protected Camera.Parameters cameraParams;
    protected List<String> focusModes;

    private int mSurfaceChangedCallDepth = 0;
    private int mCameraId;

    private int mCenterPosX = -1;
    private int mCenterPosY;

    protected int frameByteSize;
    protected byte[] lastFrameYUV;
    protected byte[] lastFrameYUVRotated;

    protected Timer continuousAutofocusTimer;
    protected ContinuousAutofocusTask continuousAutofocusTask;

    PreviewReadyCallback mPreviewReadyCallback = null;

    public enum LayoutMode {
        FitToParent, // Scale to the size that no side is larger than the parent
        NoBlank // Scale to the size that no side is smaller than the parent
    }

    public interface PreviewReadyCallback {
        void onPreviewReady();
    }

    /**
     * State flag: true when surface's layout size is set and surfaceChanged()
     * process has not been completed.
     */
    protected boolean mSurfaceConfiguring = false;

    public CameraPreview(Activity activity, int cameraId) {

        super(activity);

        mActivity = activity;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {

            if (Camera.getNumberOfCameras() > cameraId) {
                mCameraId = cameraId;
            } else {
                mCameraId = 0;
            }

        } else {
            mCameraId = 0;
        }
    }

    /**
     * Starts the camera in the preview view
     */
    public void startCamera() {

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                mCamera = Camera.open(mCameraId);
            } else {
                mCamera = Camera.open();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mCamera == null) {
            return;
        }

        cameraParams = mCamera.getParameters();
        focusModes = cameraParams.getSupportedFocusModes();
        mPreviewSizeList = cameraParams.getSupportedPreviewSizes();
        mPictureSizeList = cameraParams.getSupportedPictureSizes();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {

            mCamera.setPreviewDisplay(mHolder);

        } catch (IOException e) {
            // This usually means the camera is being used by another process
            try {
                mCamera.release();
            } catch (Exception r) {
            }
            mCamera = null;

        } catch (Exception e) {

            // For methods called after release
            try {
                mCamera.release();
            } catch (Exception r) {
            }
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        mSurfaceChangedCallDepth++;
        doSurfaceChanged(width, height);
        mSurfaceChangedCallDepth--;
    }

    private void doSurfaceChanged(int width, int height) {

        if (mCamera == null) {
            return;
        }

        try {

            mCamera.stopPreview();

        } catch (RuntimeException e) {
            // stopPreview called after release
            // Todo: Handle this properly with a camera restart
            e.printStackTrace();
            return;
        }

        cameraParams = mCamera.getParameters();
        boolean portrait = isPortrait();

        // The code in this if-statement is prevented from executed again when surfaceChanged is
        // called again due to the change of the layout size in this if-statement.
        if (!mSurfaceConfiguring) {

            Camera.Size previewSize = determinePreviewSize(portrait, width, height);
            Camera.Size pictureSize = determinePictureSize(previewSize);

            if (DEBUGGING) {
                Log.v(LOG_TAG, "Desired Preview Size - w: " + width + ", h: " + height);
            }
            mPreviewSize = previewSize;
            mPictureSize = pictureSize;
            mSurfaceConfiguring = adjustSurfaceLayoutSize(previewSize, portrait, width, height);

            // Continue executing this method if this method is called recursively.
            // Recursive call of surfaceChanged is very special case, which is a path from
            // the catch clause at the end of this method.
            // The later part of this method should be executed as well in the recursive
            // invocation of this method, because the layout change made in this recursive
            // call will not trigger another invocation of this method.
            if (mSurfaceConfiguring && (mSurfaceChangedCallDepth <= 1)) {
                return;
            }
        }

        configureCameraParameters(cameraParams, portrait);
        mSurfaceConfiguring = false;

        try {

            mCamera.startPreview();

        } catch (Exception e) {

            // Remove failed size
            mPreviewSizeList.remove(mPreviewSize);
            mPreviewSize = null;

            // Reconfigure
            if (mPreviewSizeList.size() > 0) { // prevent infinite loop
                surfaceChanged(null, 0, width, height);
            } else {
                Toast.makeText(mActivity, "Can't start preview", Toast.LENGTH_LONG).show();

            }
        }

        if (null != mPreviewReadyCallback) {
            mPreviewReadyCallback.onPreviewReady();
        }
    }

    /**
     * @param portrait
     * @param reqWidth  must be the value of the parameter passed in surfaceChanged
     * @param reqHeight must be the value of the parameter passed in surfaceChanged
     * @return Camera.Size object that is an element of the list returned from Camera.Parameters.getSupportedPreviewSizes.
     */
    protected Camera.Size determinePreviewSize(boolean portrait, int reqWidth, int reqHeight) {
        // Meaning of width and height is switched for preview when portrait,
        // while it is the same as user's view for surface and metrics.
        // That is, width must always be larger than height for setPreviewSize.

        double previewScale = 1.0;

        int reqPreviewWidth; // requested width in terms of camera hardware
        int reqPreviewHeight; // requested height in terms of camera hardware

        // Todo: Set this based on the display size (the preview can't be bigger than the display)
        // Todo: Maybe set the previewScale value to a non-hdpi one (test performance using both)
        int maxPreviewWidth;
        int maxPreviewHeight;

        if (portrait) {
            // Since the app is portrait-only, this should be the only case
            reqPreviewWidth = (int) (reqHeight / previewScale);
            reqPreviewHeight = (int) (reqWidth / previewScale);
            maxPreviewWidth = reqPreviewHeight * 2;
            maxPreviewHeight = reqPreviewHeight;
        } else {
            reqPreviewWidth = (int) (reqWidth / previewScale);
            reqPreviewHeight = (int) (reqHeight / previewScale);
            maxPreviewWidth = reqPreviewWidth;
            maxPreviewHeight = reqPreviewWidth * 2;
        }

        if (DEBUGGING) {
            Log.d(LOG_TAG, "Required Preview Size: " + reqPreviewWidth + 'x' + reqPreviewHeight);
            Log.d(LOG_TAG, "Max Preview Size: " + maxPreviewWidth + 'x' + maxPreviewHeight);
            Log.v(LOG_TAG, "Listing all supported preview sizes");
            for (Camera.Size size : mPreviewSizeList) {
                Log.v(LOG_TAG, "  w: " + size.width + ", h: " + size.height);
            }
        }

        // Get the surface size that best fits the container
        Camera.Size bestSize = null;

        for (Camera.Size size : mPreviewSizeList) {
            if (size.width > maxPreviewWidth || size.height > maxPreviewHeight) {
                continue;
            }

            if (bestSize == null) {
                bestSize = size;
            } else if (size.width >= bestSize.width && size.height >= bestSize.height) {
                // Only use the next size if it's necessary
                if (bestSize.width < reqPreviewWidth || bestSize.height < reqPreviewHeight) {
                    bestSize = size;
                }
            }

        }

        return bestSize;
    }

    protected Camera.Size determinePictureSize(Camera.Size previewSize) {
        Camera.Size retSize = null;

        if (DEBUGGING) {
            Log.v(LOG_TAG, "Listing all supported picture sizes");
            for (Camera.Size size : mPictureSizeList) {
                Log.v(LOG_TAG, "  w: " + size.width + ", h: " + size.height);
            }
        }

        for (Camera.Size size : mPictureSizeList) {
            if (size.equals(previewSize)) {
                return size;
            }
        }

        if (DEBUGGING) {
            Log.v(LOG_TAG, "Same picture size not found.");
        }

        // if the preview size is not supported as a picture size
        float reqRatio = ((float) previewSize.width) / previewSize.height;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        for (Camera.Size size : mPictureSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }

        return retSize;
    }

    protected boolean adjustSurfaceLayoutSize(Camera.Size previewSize, boolean portrait,
                                              int availableWidth, int availableHeight) {
        double previewWidth, previewHeight;

        previewWidth = previewSize.width;
        previewHeight = previewSize.height;

        if (portrait) {
            previewWidth = previewSize.height;
            previewHeight = previewSize.width;
        }

        // Figure out by how much we need to scale the preview holder
        double scaleFactor = 1.0;
        if (previewWidth < availableWidth) {
            scaleFactor = (double) availableWidth / previewWidth;
        }

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.getLayoutParams();

        int holderWidth = (int) (previewWidth * scaleFactor);
        int holderHeight = (int) (previewHeight * scaleFactor);

        if (DEBUGGING) {
            Log.v(LOG_TAG, "Preview Size - w: " + previewWidth + ", h: " + previewHeight);
            Log.v(LOG_TAG, "Preview Holder Size - w: " + holderWidth + ", h: " + holderHeight + " (Factor: " + scaleFactor + ")");
        }

        boolean layoutChanged;
        if ((holderWidth != this.getWidth()) || (holderHeight != this.getHeight())) {

            layoutParams.width = holderWidth;
            layoutParams.height = holderHeight;

            this.setLayoutParams(layoutParams); // this will trigger another surfaceChanged invocation.
            layoutChanged = true;
        } else {
            layoutChanged = false;
        }

        return layoutChanged;
    }

    /**
     * @param x X coordinate of center position on the screen. Set to negative value to unset.
     * @param y Y coordinate of center position on the screen.
     */
    public void setCenterPosition(int x, int y) {
        mCenterPosX = x;
        mCenterPosY = y;
    }

    protected void configureCameraParameters(Camera.Parameters cameraParams, boolean portrait) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) { // for 2.1 and before
            if (portrait) {
                cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_PORTRAIT);
            } else {
                cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_LANDSCAPE);
            }
        } else { // for 2.2 and later
            int angle;
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            switch (display.getRotation()) {
                case Surface.ROTATION_0: // This is display orientation
                    angle = 90; // This is camera orientation
                    break;
                case Surface.ROTATION_90:
                    angle = 0;
                    break;
                case Surface.ROTATION_180:
                    angle = 270;
                    break;
                case Surface.ROTATION_270:
                    angle = 180;
                    break;
                default:
                    angle = 90;
                    break;
            }
            if (DEBUGGING) {
                Log.v(LOG_TAG, "angle: " + angle);
            }
            mCamera.setDisplayOrientation(angle);
        }

        // Pick the best FPS range
        int[] bestFPS = null;
        for (int[] fps : cameraParams.getSupportedPreviewFpsRange()) {
            if (DEBUGGING) {
                Log.v(LOG_TAG, "Preview FPS Range Available: " + fps[0] + "-" + fps[1]);
            }
            if (bestFPS == null || (fps[0] >= bestFPS[0] && fps[1] >= bestFPS[1])) {
                bestFPS = fps;
            }
        }

        if (bestFPS != null) {
            cameraParams.setPreviewFpsRange(bestFPS[0], bestFPS[1]);
        }

        if (cameraParams.isVideoStabilizationSupported()) {
            cameraParams.setVideoStabilization(false);
        }

        try {
            // RecordingHint GREATLY improves camera performance on Nexus 4, but breaks the scanner on other devices like the Samsung S3
            if (Build.MODEL.toLowerCase().equals("nexus 4") && Build.MANUFACTURER.toLowerCase().equals("lge")) {
                cameraParams.setRecordingHint(true);
            }
        } catch (Exception e) {
        }

        if (cameraParams.getSupportedWhiteBalance() != null && cameraParams.getSupportedWhiteBalance().contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
            cameraParams.setWhiteBalance(cameraParams.WHITE_BALANCE_AUTO);
        }
        if (cameraParams.isAutoWhiteBalanceLockSupported()) {
            cameraParams.setAutoWhiteBalanceLock(false);
        }

        if (cameraParams.getMinExposureCompensation() <= 0) {
            cameraParams.setExposureCompensation(0);
        }

        if (cameraParams.getSupportedAntibanding() != null && cameraParams.getSupportedAntibanding().contains(Camera.Parameters.ANTIBANDING_OFF)) {
            cameraParams.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
        }

        // Optimize the camera for barcode reading
        if (cameraParams.getSupportedSceneModes() != null) {

            if (cameraParams.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_BARCODE)) {
                cameraParams.setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
            } else if (cameraParams.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_AUTO)) {
                cameraParams.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            }
        }

        // Set the focus area to be a small rectangle in the center of the sensor for better accuracy.
        if (DEBUGGING) {
            Log.v(LOG_TAG, "Focus Areas: " + cameraParams.getMaxNumFocusAreas());
        }

        // Set a decent zoom so the typical barcode fits nicely into view
        if (cameraParams.isZoomSupported()) {

            // Zoom ratios are 100-based (100 is 1x, 200 is 2x, etc).
            int idealZoomRatio = 130;

            // Find the optimal zoom value for a specific zoom
            // http://stackoverflow.com/questions/12632392/android-camara-parameters-getzoomratio-values-not-as-expected
            // http://stackoverflow.com/questions/3261776/determine-angle-of-view-of-smartphone-camera
            List<Integer> zooms = cameraParams.getZoomRatios();

            if (DEBUGGING) {
                for (int zoom : zooms) {
                    Log.v(LOG_TAG, "Zoom Ratio: " + ((float) zoom / 100) + "x");
                }
            }

            // zoomRatioIndex keeps the index of the best zoom ratio available
            int zoomRatioIndex = 0;
            int zoomRatioValue = 100;
            int i = 0;
            for (int zoom : zooms) {
                // Find the largest zoom ratio that stays below the ideal ratio
                if (zoom <= idealZoomRatio) {
                    zoomRatioIndex = i;
                    zoomRatioValue = zoom;
                }
                i++;
            }

            // Todo: Use horizontal angle (and resolution) adjustment to improve the zoom calculation

            if (DEBUGGING) {
                Log.v(LOG_TAG, "Best Zoom: " + (float) zoomRatioValue / 100 + "x (Index " + zoomRatioIndex + ")");
            }

            // Set the zoom with some sanity checks
            cameraParams.setZoom((int) Math.min(cameraParams.getMaxZoom(), zoomRatioIndex));

        }


        // Todo: Fetch the view angle so the focal point and zoom is set in a way that shows the barcode perfectly

        // Todo: Add a filter that makes the scanner look better
        //cameraParams.setColorEffect( Camera.Parameters.EFFECT_MONO );

        // Todo: Maybe add some flashlight controls for low visibility conditions
        if (cameraParams.getSupportedFlashModes() != null && cameraParams.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_OFF)) {
            cameraParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }

        // Todo: If the device stays still for a second or so and no scan has been done, trigger the autofocus automatically.

        cameraParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        cameraParams.setPictureSize(mPictureSize.width, mPictureSize.height);
        if (DEBUGGING) {
            Log.v(LOG_TAG, "Preview Actual Size - w: " + mPreviewSize.width + ", h: " + mPreviewSize.height);
            Log.v(LOG_TAG, "Picture Actual Size - w: " + mPictureSize.width + ", h: " + mPictureSize.height);
        }

        mCamera.setParameters(cameraParams);

        setPreviewCallback(previewCallback);

        setupAutoFocus();
    }


    public boolean supportsFlash() {
        if (mCamera == null) {
            return false;
        }
        if (mCamera.getParameters().getSupportedFlashModes() != null) {

            if (mCamera.getParameters().getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_ON)) {
                return true;
            }
        }
        return false;
    }


    private static double zoomAngle(double degrees, int zoom) {
        double theta = Math.toRadians(degrees);
        return 2d * Math.atan(100d * Math.tan(theta / 2d) / zoom);
    }


    public void setupManualFocus() {

        if (mCamera == null) {
            return;
        }

        try {
            // Try to cancel the autofocus if it's running.
            // This can throw an exception if the camera was released
            mCamera.cancelAutoFocus();
        } catch (Exception e) {
        }

        // Todo: Set a fixed focal point that's very close to the camera (no autofocus)
        if (!focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
            // Shitty workaround #1: Use Macro mode (Fixed Focal Point)
            // This delivers decent, stable image quality without autofocus.
            if (DEBUGGING) {
                Log.v(LOG_TAG, "Focus Mode: Macro");
            }
            cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            mCamera.setParameters(cameraParams);
        } else {
            setupAutoFocus();
        }

        float focusDistances[] = new float[3];
        cameraParams.getFocusDistances(focusDistances);

        if (DEBUGGING) {
            for (float distance : focusDistances) {
                Log.v("DISTANCES", "" + distance);
            }
        }

    }


    public void setupAutoFocus() {

        if (mCamera == null) {
            return;
        }

        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            if (DEBUGGING) {
                Log.v(LOG_TAG, "Focus Mode: Continuous Picture");
            }
        }
        // FOCUS_MODE_MACRO and FOCUS_MODE_AUTO require a fake continuous autofocus routine
        else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
            cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            if (DEBUGGING) {
                Log.v(LOG_TAG, "Focus Mode: Macro");
            }
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            if (DEBUGGING) {
                Log.v(LOG_TAG, "Focus Mode: AutoFocus");
            }
        }

        // FOCUS_MODE_MACRO and FOCUS_MODE_AUTO require a fake continuous autofocus routine
        if (cameraParams.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_MACRO) || cameraParams.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {

            if (continuousAutofocusTask != null) {
                try {
                    continuousAutofocusTask.cancel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                continuousAutofocusTask = null;
            }
            continuousAutofocusTask = new ContinuousAutofocusTask();

            if (continuousAutofocusTimer != null) {
                try {
                    continuousAutofocusTimer.cancel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                continuousAutofocusTimer = null;
            }
            continuousAutofocusTimer = new Timer();

            try {
                continuousAutofocusTimer.schedule(continuousAutofocusTask, 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Continuous Picture
            try {
                mCamera.cancelAutoFocus();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                mCamera.setParameters(cameraParams);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isAutofocusing = false;
        }
    }


    boolean isAutofocusing = false;

    /**
     * This is the manual autofocus that can be triggered externally
     */
    public void doAutoFocus() {
        // We need to change the focus mode before doing autofocus
        if (DEBUGGING) {
            Log.v(LOG_TAG, "AutoFocus");
        }

        if (mCamera != null) {

            // If using continuous_picture we need to cancel the autofocus so the focus doesn't become fixed
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {

                try {
                    cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    mCamera.setParameters(cameraParams);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    isAutofocusing = true;
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            isAutofocusing = false;
                            // Do a cancelAutoFocus in a bit
                            (new Timer()).schedule(new RestoreContinuousAutofocusTask(), 200);
                        }
                    });

                } catch (Exception e) {
                    isAutofocusing = false;
                    e.printStackTrace();

                    try {
                        mCamera.cancelAutoFocus();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }

            } else {
                // Cancel any running fake continuous autofocus tasks
                if (continuousAutofocusTimer != null) {
                    try {
                        continuousAutofocusTimer.cancel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continuousAutofocusTimer = null;
                }

                if (continuousAutofocusTask != null) {
                    try {
                        continuousAutofocusTask.cancel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continuousAutofocusTask = null;
                }

                // After the autofocus is done, continue doing the continuous autofocus routine
                if (isAutofocusing) {
                    try {
                        mCamera.cancelAutoFocus();
                    } catch (Exception e) {
                    }
                }

                try {
                    isAutofocusing = true;
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            isAutofocusing = false;
                            setupAutoFocus();
                        }
                    });

                } catch (Exception e) {
                    isAutofocusing = false;
                    // Autofocus can fail if the camera isn't active yet
                    // Recreate the default autofocus anyway
                    setupAutoFocus();
                }
            }
        }
    }


    protected void doContinuousAutoFocus(Camera.AutoFocusCallback callback) {
        // We need to change the focus mode before doing autofocus

        if (mCamera != null) {
            try {
                isAutofocusing = true;
                mCamera.autoFocus(callback);
            } catch (Exception e) {
                // Autofocus can fail if the camera isn't active yet
                isAutofocusing = false;
            }
        }
    }


    /**
     * This task gets repeated constantly while the camera is on
     */
    protected class ContinuousAutofocusTask extends TimerTask {
        @Override
        public void run() {

            doContinuousAutoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {

                    isAutofocusing = false;

                    // If the autofocus was successful, keep the camera a bit longer in that state before autofocusing again
                    int nextAutofocusIn = 800;
                    if (success) {
                        nextAutofocusIn = 2500;
                    }

                    if (continuousAutofocusTimer == null) {
                        continuousAutofocusTimer = new Timer();
                    }

                    if (DEBUGGING) {
                        Log.v(LOG_TAG, "Next In " + nextAutofocusIn + " ms");
                    }
                    try {
                        continuousAutofocusTask = new ContinuousAutofocusTask();
                        continuousAutofocusTimer.schedule(continuousAutofocusTask, nextAutofocusIn);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continuousAutofocusTask = new ContinuousAutofocusTask();
                        continuousAutofocusTimer = new Timer();
                        continuousAutofocusTimer.schedule(continuousAutofocusTask, nextAutofocusIn);
                    }
                }
            });
        }
    }


    protected class RestoreContinuousAutofocusTask extends TimerTask {
        @Override
        public void run() {
            if (!isAutofocusing) {
                isAutofocusing = false;
                try {
                    mCamera.cancelAutoFocus();

                    try {
                        cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        mCamera.setParameters(cameraParams);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (DEBUGGING) {
                    Log.v(LOG_TAG, "Going back to Continuous Autofocus");
                }
            }
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    public void stop() {
        if (null == mCamera) {
            return;
        }
        try {
            mCamera.stopPreview();
            // We need to unset the preview callback before releasing it
            mCamera.setPreviewCallback(null);
        } catch (Exception e) {
            // This typically happens if the camera was released
        } finally {
            try {
                mCamera.release();
            } catch (Exception e) {
                // Just in case
            }
            mCamera = null;
        }
    }

    public boolean isPortrait() {
        return (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    public void setOneShotPreviewCallback(Camera.PreviewCallback callback) {
        if (null == mCamera) {
            return;
        }
        mCamera.setOneShotPreviewCallback(callback);
    }

    public void setPreviewCallback(Camera.PreviewCallback callback) {
        if (null == mCamera) {
            return;
        }
        mCamera.setPreviewCallback(callback);
    }

    public Camera.Size getPreviewSize() {
        return mPreviewSize;
    }

    public Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

        public void onPreviewFrame(byte[] data, Camera camera) {
            // Cache the last preview frame
            if (lastFrameYUV == null || lastFrameYUV.length != data.length) {
                lastFrameYUV = new byte[data.length];
            }
            System.arraycopy(data, 0, lastFrameYUV, 0, Math.min(data.length, lastFrameYUV.length));
        }

    };


    public PlanarYUVLuminanceSource getLuminanceSource() {

        if (lastFrameYUV == null || cameraParams == null) {
            return null;
        }

        // Get the luminance source (used to read the barcode)
        if (lastFrameYUVRotated == null || lastFrameYUV.length != lastFrameYUVRotated.length) {
            frameByteSize = (ImageFormat.getBitsPerPixel(cameraParams.getPreviewFormat()));
            lastFrameYUVRotated = new byte[lastFrameYUV.length];
        }

        // Do a straight memcopy, since it's more efficient
        System.arraycopy(lastFrameYUV, 0, lastFrameYUVRotated, 0, Math.min(lastFrameYUV.length, lastFrameYUVRotated.length));

        int width = mPreviewSize.width;
        int height = mPreviewSize.height;

        // Rotate the lastFrame
        if (isPortrait()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    lastFrameYUVRotated[x * height + height - y - 1] = lastFrameYUV[x + y * width];
                }
            }

            width = mPreviewSize.height;
            height = mPreviewSize.width;
        }

        // Only scan a short band in the middle of the image
        double horizontalPercent = 1.0;//1.0;
        double verticalPercent = 0.25;//0.02;

        int scannerWidth = (int) ((double) width * horizontalPercent);
        int scannerHeight = (int) ((double) height * verticalPercent);

        return new PlanarYUVLuminanceSource(lastFrameYUVRotated,
                width,
                height,
                (int) ((double) (width - scannerWidth) / 2.0f),
                (int) ((double) (height - scannerHeight) / 4.0f),
                scannerWidth,
                scannerHeight,
                false);

    }
}
