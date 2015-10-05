package it.jaschke.alexandria;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.jaschke.alexandria.CameraPreview.CameraPreview;

/**
 * Created by gabrielmarcos on 8/18/15.
 */
public class ScannerFragment extends Fragment {

    public interface ScannerFragmentInterface {
        void onCloseClicked();
        void onBarCodeFound(String number);
    }

    private ScannerFragmentInterface mInterface;

    @Bind(R.id.camera_holder)
    FrameLayout mCameraHolder;

    private CameraPreview mCameraPreview;

    // ZXing Barcode Reader
    private MultiFormatReader mMultiFormatReader;

    // The Barcode reader process
    private Timer mScanTimer;
    private int mBarcodeScanInterval = 1;

    private SetupCameraTask mSetupCameraTask;
    private StopCameraTask mStopCameraTask;

    private boolean mScannerActivated = false;

    /**
     * Returns a new instance of the Fragment
     * @return fragment instance
     */
    public static ScannerFragment newInstance() {
        return new ScannerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scanner, container, false);
        ButterKnife.bind(this, rootView);

        mScanTimer = new Timer();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                // We add a small delay to improve animations performance
                if (isAdded()) {
                    startScanner();
                }

            }
        }, 500);

    }

    @Override
    public void onPause() {
        super.onPause();

        stopScanner();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setDecodeProductMode() {

        // Limits the reader so it only reads the standard barcode formats
        Hashtable<DecodeHintType, Object> hints = new Hashtable<>(1);
        Vector<BarcodeFormat> vector = new Vector<>(1);

        vector.addElement(BarcodeFormat.EAN_13);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);

        mMultiFormatReader.setHints(hints);
    }

    private class SetupCameraTask extends AsyncTask {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if( mStopCameraTask != null ) {
                try {
                    mStopCameraTask.cancel(true);
                    mStopCameraTask = null;
                } catch (Exception e) {}
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            // Setup the barcode reader
            if( mMultiFormatReader == null ) {
                mMultiFormatReader = new MultiFormatReader();
                setDecodeProductMode();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            mCameraPreview.startCamera();

            // addView needs to run in the UI Thread (and onPostExecute runs in that thread)
            // Make sure the mCameraPreview has not been attached
            if( mCameraHolder.getChildCount() == 0 ) {
                mCameraHolder.addView(mCameraPreview);
            }

            startScanning();
        }
    }


    private class StopCameraTask extends AsyncTask {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if( mSetupCameraTask != null ) {
                try {
                    mSetupCameraTask.cancel(true);
                    mSetupCameraTask = null;
                } catch (Exception e) {}
            }
        }


        @Override
        protected Object doInBackground(Object[] objects) {

            // Stop the barcode reader
            mMultiFormatReader = null;

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            // Moved to the ui thread
            mCameraPreview.stop();

            if( mCameraPreview != null ) {
                mCameraHolder.removeView(mCameraPreview);
            }

            // Cancel the barcode scanner task
            if( mScanTimer != null ){ mScanTimer.cancel(); }
            mScanTimer = null;
        }
    }

    /**
     * This task gets repeated constantly while the camera is on
     */
    private class ReadBarcodeTask extends TimerTask {
        @Override
        public void run() {

            readBarcode();

            /* If the scanner is activated it will re-scan  depending
            * on the scanner interval */
            if( mScannerActivated ) {

                if (mScanTimer != null) {
                    try {
                        mScanTimer.schedule(new ReadBarcodeTask(), mBarcodeScanInterval);
                    } catch (Exception e) {
                        // Timer canceled?
                    }
                }
            }
        }
    }



    public void readBarcode() {

        if( mCameraPreview == null ) { return; }

        Result rawResult;
        PlanarYUVLuminanceSource source = mCameraPreview.getLuminanceSource();

        if (source == null) {
            // No source (camera has not been initialized or there's an issue getting the luminance source)
            return;
        }

        // Convert the luminance source to a bitmap, which then gets fed into the barcode reader.
        BinaryBitmap scannerBitmap = new BinaryBitmap( new HybridBinarizer(source) );

        try {
            rawResult = mMultiFormatReader.decodeWithState(scannerBitmap);
            successfulScan( rawResult );
        } catch (NotFoundException e) {
            // Barcode Not Found
        } catch (NullPointerException e) {
            // the multiformatreader is off
        } catch (Exception e) {
            mMultiFormatReader.reset();
        }

    }


    public void successfulScan( final Result rawResult ) {

        try {
            mMultiFormatReader.reset();
        } catch(Exception e){}

        /* Once a barcode was detected we stop the barcode scanner */
        stopScanning();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                String code = rawResult.getText();

                if (mInterface != null) {
                    mInterface.onBarCodeFound(code);
                }
            }
        });

    }

    /**
     * Starts the barcode scanner
     */
    public void startScanning(){

        // We add a small delay to the start scanning process
        // to prevent accidental queue scanning

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                mScannerActivated = true;

                // Start the barcode reader task
                mScanTimer = new Timer();
                mScanTimer.schedule(new ReadBarcodeTask(), mBarcodeScanInterval);

            }
        }, 1500);


    }

    /**
     * Stops the barcode scanner
     */
    public void stopScanning(){
        mScannerActivated = false;
    }


    public void startScanner() {

        // Usually, it's 0 for back-facing camera, 1 for front-facing camera.
        if( mCameraPreview == null ) {
            int cameraIndex = 0;
            mCameraPreview = new CameraPreview(getActivity(), cameraIndex);
        }

        mSetupCameraTask = new SetupCameraTask();
        mSetupCameraTask.execute();
    }


    public void stopScanner() {

        if( mSetupCameraTask != null ) {
            try {
                mSetupCameraTask.cancel(true);
                mSetupCameraTask = null;
            } catch(Exception e) {}
        }

        if( mStopCameraTask == null ) {
            mStopCameraTask = new StopCameraTask();
            mStopCameraTask.execute();
        }

        // Cancel the barcode scanner task
        if( mScanTimer != null ){ mScanTimer.cancel(); }
        mScanTimer = null;

    }

    public void setmInterface(ScannerFragmentInterface mInterface) {
        this.mInterface = mInterface;
    }

    @OnClick(R.id.close_scanner)
    void onCloseClicked() {

        stopScanning();
        stopScanner();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mInterface != null) mInterface.onCloseClicked();

            }
        }, 500);

    }

}
