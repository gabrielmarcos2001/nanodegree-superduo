package it.jaschke.alexandria;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
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

import it.jaschke.alexandria.CameraPreview.CameraPreview;


/**
 * Created by gabrielmarcos on 7/7/15.
 */
public class ScannerActivity extends AppCompatActivity {

    private FrameLayout cameraHolder;
    private CameraPreview cameraPreview;

    // ZXing Barcode Reader
    private MultiFormatReader multiFormatReader;

    // The Barcode reader process
    private Timer scanTimer;
    private int barcodeScanInterval = 1;

    private SetupCameraTask setupCameraTask;
    private StopCameraTask stopCameraTask;

    private boolean mScannerActivated = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scanner);

        cameraHolder = (FrameLayout)findViewById(R.id.camera_holder);

        scanTimer = new Timer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //getMenuInflater().inflate(R.menu.scanner_menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){

            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
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

        multiFormatReader.setHints(hints);
    }

    private class SetupCameraTask extends AsyncTask {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if( stopCameraTask != null ) {
                try {
                    stopCameraTask.cancel(true);
                    stopCameraTask = null;
                } catch (Exception e) {}
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            // Setup the barcode reader
            if( multiFormatReader == null ) {
                multiFormatReader = new MultiFormatReader();
                setDecodeProductMode();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            cameraPreview.startCamera();

            // addView needs to run in the UI Thread (and onPostExecute runs in that thread)
            // Make sure the cameraPreview has not been attached
            if( cameraHolder.getChildCount() == 0 ) {
                cameraHolder.addView(cameraPreview);
            }

            startScanning();
        }
    }


    private class StopCameraTask extends AsyncTask {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if( setupCameraTask != null ) {
                try {
                    setupCameraTask.cancel(true);
                    setupCameraTask = null;
                } catch (Exception e) {}
            }
        }


        @Override
        protected Object doInBackground(Object[] objects) {

            // Stop the barcode reader
            multiFormatReader = null;

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            // Moved to the ui thread
            cameraPreview.stop();

            if( cameraPreview != null ) {
                cameraHolder.removeView(cameraPreview);
            }

            // Cancel the barcode scanner task
            if( scanTimer != null ){ scanTimer.cancel(); }
            scanTimer = null;
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

                if (scanTimer != null) {
                    try {
                        scanTimer.schedule(new ReadBarcodeTask(), barcodeScanInterval);
                    } catch (Exception e) {
                        // Timer canceled?
                    }
                }
            }
        }
    }



    public void readBarcode() {

        if( cameraPreview == null ) { return; }

        Result rawResult;
        PlanarYUVLuminanceSource source = cameraPreview.getLuminanceSource();

        if (source == null) {
            // No source (camera has not been initialized or there's an issue getting the luminance source)
            return;
        }

        // Convert the luminance source to a bitmap, which then gets fed into the barcode reader.
        BinaryBitmap scannerBitmap = new BinaryBitmap( new HybridBinarizer(source) );

        try {
            rawResult = multiFormatReader.decodeWithState(scannerBitmap);
            successfulScan( rawResult );
        } catch (NotFoundException e) {
            // Barcode Not Found
        } catch (NullPointerException e) {
            // the multiformatreader is off
        } catch (Exception e) {
            multiFormatReader.reset();
        }

    }


    public void successfulScan( final Result rawResult ) {

        try {
            multiFormatReader.reset();
        } catch(Exception e){}

        /* Once a barcode was detected we stop the barcode scanner */
        stopScanning();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                String code = rawResult.getText();

                if (code.length() > 1) {

                    String first = code.substring(0, 1);
                    if (first.equals("0")) {
                        code = code.substring(1, code.length());

                    }

                }

                //mPresenter.barcodeFound(code);

            }
        });

    }

    /*
    @Override
    public void productFetched(OWMProduct product) {

        // Caches the selected product
        new SetSelectedProductInteractor().set(product);

        // Starts the product detail activity
        Intent i = new Intent(ScannerActivity.this, ProductDetailActivity.class);
        startActivity(i);
    }*/

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
                scanTimer = new Timer();
                scanTimer.schedule(new ReadBarcodeTask(), barcodeScanInterval);

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
        if( cameraPreview == null ) {
            int cameraIndex = 0;
            cameraPreview = new CameraPreview(this, cameraIndex);
        }

        setupCameraTask = new SetupCameraTask();
        setupCameraTask.execute();
    }

    public void stopScanner() {

        if( setupCameraTask != null ) {
            try {
                setupCameraTask.cancel(true);
                setupCameraTask = null;
            } catch(Exception e) {}
        }

        if( stopCameraTask == null ) {
            stopCameraTask = new StopCameraTask();
            stopCameraTask.execute();
        }

        // Cancel the barcode scanner task
        if( scanTimer != null ){ scanTimer.cancel(); }
        scanTimer = null;

    }

}
