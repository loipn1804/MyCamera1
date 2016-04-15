package loipn.mycamera1;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraSurfaceView extends SurfaceView implements
        SurfaceHolder.Callback, SensorEventListener {

    private boolean isPortrait = true;

    private Activity context;
    private Camera camera;
    private SurfaceHolder surfaceHolder;

    int i = 0;

    int degrees = 0;
    int refresh;

    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;

    private String FILE_NAME;
    private String FOLDER = "Virtnet";

    private byte[] dataImage;

    private Bitmap bmOut;

    long startTime = System.currentTimeMillis();

    boolean isDoing = false;

    public CameraSurfaceView(Activity context) {
        super(context);
        this.refresh = 0;
        this.context = context;
        this.FOLDER = context.getString(R.string.app_name);
        surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setFixedSize(768, 1024);

        setWillNotDraw(false); // them cai nay moi ve len onDraw() dc

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay()
                .getMetrics(metrics);
        Display display = ((WindowManager)
                context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    public void setFilename(String filename) {
        this.FILE_NAME = filename;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        sensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this,
                sensorAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);

        try {
            // open the camera
            camera = Camera.open();
//            camera = openFrontFacingCameraGingerbread();
        } catch (RuntimeException e) {
            // check for exceptions
            //System.err.println(e);
            return;
        }
        Camera.Parameters param;
        param = camera.getParameters();
        List<Size> supportedPictureSizes = camera.getParameters().getSupportedPictureSizes();
        List<Camera.Size> sizeList = param.getSupportedPreviewSizes();
//        List<int[]> listSupportedPreviewFpsRange = param.getSupportedPreviewFpsRange();
        String preview = "";
        for (int i = 0; i < sizeList.size(); i++) {
            preview += i + " w:" + sizeList.get(i).width + " h:" + sizeList.get(i).height + "\n";
        }
        String picture = "";
        for (int i = 0; i < supportedPictureSizes.size(); i++) {
            picture += i + " w:" + supportedPictureSizes.get(i).width + " h:" + supportedPictureSizes.get(i).height + "\n";
        }

//        Log.e("camera_info", preview);
//        Log.e("camera_info", picture);
        if (sizeList.size() > 0) {
//            Camera.Size size = getOptimalPreviewSize(sizeList, StaticFunction.getScreenWidth(context), StaticFunction.getScreenHeight(context));
            Camera.Size size = sizeList.get(9);
            Camera.Size sizePicture = supportedPictureSizes.get(15);
//            for (int i = 0; i < sizeList.size(); i++) {
//                if (sizeList.get(i).width == size.width && sizeList.get(i).height == size.height) {
//                    sizePicture = supportedPictureSizes.get(i);
//                    break;
//                }
//            }
            if (size != null) {
                param.setPreviewSize(size.width, size.height);
                param.setPictureSize(sizePicture.width, sizePicture.height);
//                if (size.width < 1000 || size.height < 1000) {
//                    param.setPictureSize(size.width * 2, size.height * 2);
//                } else {
//                    param.setPictureSize(size.width, size.height);
//                }
            }
        }
//        param.setPreviewSize(sizeList.get(0).width, sizeList.get(0).height);
//        param.setPictureSize(supportedPictureSizes.get(0).width, supportedPictureSizes.get(0).height);
//        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//        param.setPreviewFpsRange(15000, 15000);
//        int[] frameRate = new int[2];
//        param.getPreviewFpsRange(frameRate);
//        int pic = param.getPictureFormat();
//        List<Integer> pics = param.getSupportedPictureFormats();
        camera.setParameters(param);
        int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 90;
                break;
            case Surface.ROTATION_90:
                degrees = 0;
                break;
            case Surface.ROTATION_180:
                degrees = 270;
                break;
            case Surface.ROTATION_270:
                degrees = 180;
                break;
        }
        camera.setDisplayOrientation(degrees);

        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            // check for exceptions
            //System.err.println(e);
            return;
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        refreshCamera();
    }

    private void refreshCamera() {
        refresh++;
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.setPreviewCallback(null);
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 90;
                break;
            case Surface.ROTATION_90:
                degrees = 0;
                break;
            case Surface.ROTATION_180:
                degrees = 270;
                break;
            case Surface.ROTATION_270:
                degrees = 180;
                break;
        }
        camera.setDisplayOrientation(degrees);

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.setPreviewCallback(previewCallback);
//            camera.setOneShotPreviewCallback(previewCallback);
//            camera.setPreviewCallbackWithBuffer(previewCallback);
            camera.startPreview();
        } catch (Exception e) {

        }

        //txt1.setText(context.getResources().getConfiguration().orientation + "");
    }

    private PreviewCallback previewCallback = new PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            // get byte

            // TODO Auto-generated method stub
//            i++;
//            Log.e("previewCallback", System.currentTimeMillis() + "-" + i);

//            AsyncOnDraw asyncOnDraw = new AsyncOnDraw();
//            asyncOnDraw.execute(data);

//            long start = System.currentTimeMillis();

            if (isDoing) {
                camera.addCallbackBuffer(data);
            }

            isDoing = true;
            Camera.Parameters parameters = camera.getParameters();

            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;

            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);

            byte[] bytes = out.toByteArray();
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 1;
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
            // Rotate the Bitmap
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);

            bmOut = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            bmOut = Bitmap.createScaledBitmap(bmOut, (int) (bmOut.getWidth() * 1.6), (int) (bmOut.getHeight() * 1.6), false);
            Log.e("camera_info", "w " + bmOut.getWidth());
            Log.e("camera_info", "h " + bmOut.getHeight());

            invalidate();
            isDoing = false;

//            Log.e("previewCallback", "time: " + (System.currentTimeMillis() - start));

//            int width = bmOut.getWidth();
//            int height = bmOut.getHeight();
//            for (int x = 0; x < width; ++x) {
//                for (int y = 0; y < height; ++y) {
////                    int alpha = Color.alpha(bmOut.getPixel(x, y))/2;
////
////                    //int color = pngTestBM.getPixel(myX, myY);
////                    //boolean transparent = (color & 0xff000000) == 0x0;
////                    bmOut.setPixel(x, y, alpha);
//                }
//            }
        }
    };

    private class AsyncOnDraw extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... params) {
            byte[] data = params[0];
            Camera.Parameters parameters = camera.getParameters();
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;

            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);

            byte[] bytes = out.toByteArray();
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 1;
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);
            // Rotate the Bitmap
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);

            bmOut = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
//            bmOut = Bitmap.createScaledBitmap(bmOut, (int) (bmOut.getWidth() * 3.2), (int) (bmOut.getHeight() * 3.2), false);
            Log.e("camera_info", "w " + bmOut.getWidth());
            Log.e("camera_info", "h " + bmOut.getHeight());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            invalidate();
        }
    }

    private ShutterCallback shutterCallback = new ShutterCallback() {

        @Override
        public void onShutter() {
            // TODO Auto-generated method stub

        }
    };

    public void TakePicture() {
        if (camera != null) {
            camera.takePicture(shutterCallback, null, pictureCallback);
        }
    }

    private PictureCallback pictureCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            dataImage = rotateImage(data);
            SaveImageAsync saveImageAsync = new SaveImageAsync();
            saveImageAsync.execute();
//            FileOutputStream outStream = null;
//            try {
////                String absPath = Environment.getExternalStorageDirectory().getAbsolutePath();
//                String root = Environment.getExternalStorageDirectory().toString();
//                String mCurrentPhotoPath = root + "/" + FOLDER + "/" + FILE_NAME;
//                File myDir = new File(root + "/" + FOLDER);
//
//                if (!myDir.exists()) {
//                    myDir.mkdirs();
//                }
//
//                File file = new File(mCurrentPhotoPath);
//                if (file.exists()) file.delete();
//
//                outStream = new FileOutputStream(mCurrentPhotoPath);
//                outStream.write(data);
//                outStream.close();
//
//                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                File f = new File(mCurrentPhotoPath);
//                Uri contentUri = Uri.fromFile(f);
//                mediaScanIntent.setData(contentUri);
//                context.sendBroadcast(mediaScanIntent);
//
//                // Log.d("Log", "onPictureTaken - wrote bytes: " + data.length);
//                Toast.makeText(context, "Picture Saved", Toast.LENGTH_SHORT).show();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//            }
//            refreshCamera();
        }
    };

    private class SaveImageAsync extends AsyncTask<Void, Void, Uri> {

        private ProgressDialog progressDialog;
        private boolean isSuccess;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setCancelable(false);
            progressDialog.show();
            isSuccess = false;
        }

        @Override
        protected Uri doInBackground(Void... params) {
            FILE_NAME = System.currentTimeMillis() + ".png";
            FileOutputStream outStream = null;
            try {
//                String absPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                String root = Environment.getExternalStorageDirectory().toString();
                String mCurrentPhotoPath = root + "/" + FOLDER + "/" + FILE_NAME;
                File myDir = new File(root + "/" + FOLDER);

                if (!myDir.exists()) {
                    myDir.mkdirs();
                }

                File file = new File(mCurrentPhotoPath);
                if (file.exists()) file.delete();

                outStream = new FileOutputStream(mCurrentPhotoPath);
                outStream.write(dataImage);
                outStream.close();

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File f = new File(mCurrentPhotoPath);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                context.sendBroadcast(mediaScanIntent);

                isSuccess = true;
                return contentUri;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

            }
            return null;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            super.onPostExecute(uri);
            progressDialog.dismiss();
            if (isSuccess) {
                if (uri != null) {
                    Toast.makeText(context, "Picture Saved", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
            }
            refreshCamera();
        }
    }

    private byte[] rotateImage(byte[] data) {
        //Size previewSize = camera.getParameters().getPreviewSize();
        //ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] rawImage = null;

        // Decode image from the retrieved buffer to JPEG
        /*YuvImage yuv = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
        yuv.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);
	    rawImage = baos.toByteArray();*/

        // This is the same image as the preview but in JPEG and not rotated
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 1;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opt);
        //Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length);
        ByteArrayOutputStream rotatedStream = new ByteArrayOutputStream();

        // Rotate the Bitmap
        Matrix matrix = new Matrix();
//        matrix.postRotate(degrees);
//        if (isPortrait) {
//            matrix.postRotate(0);
//        } else {
//            matrix.postRotate(90);
//        }

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);

        // We dump the rotated Bitmap to the stream
        bitmap.compress(CompressFormat.PNG, 100, rotatedStream);

        rawImage = rotatedStream.toByteArray();

        bitmap.recycle();

        return rawImage;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        sensorManager.unregisterListener(this);
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
//            Toast.makeText(context, "released camera", Toast.LENGTH_SHORT).show();
            camera = null;
        }
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /*Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(StaticFunction.getScreenWidth(context), StaticFunction.getScreenHeight(context) / 2, conf);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int color = Color.argb(80, 102, 0, 51);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                bitmap.setPixel(x, y, color);
            }
        }*/

        if (bmOut != null) {
//            Bitmap bmp = toGrayscale(bmOut);
            canvas.drawBitmap(bmOut, 0, 0, new Paint());
        }

        Paint p = new Paint();
        p.setColor(Color.GREEN);
        p.setStrokeWidth(0);
        p.setTextSize(30);

        canvas.drawText("degrees: " + degrees, 100, 50, p);
        canvas.drawText("refresh: " + refresh, 100, 100, p);
        canvas.drawText("isPortrait: " + isPortrait, 100, 150, p);
        long now = System.currentTimeMillis();
        Log.e("previewCallback", "ondraw: " + (now - startTime));
        startTime = now;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
        float valueAzimuth = event.values[0];
        if (Math.abs(valueAzimuth) <= 6.5) {
            isPortrait = true;
        } else {
            isPortrait = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    private Camera openFrontFacingCameraGingerbread() {
        int Count = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Count = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < Count; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {

                }
            }
        }

        return cam;
    }

}
