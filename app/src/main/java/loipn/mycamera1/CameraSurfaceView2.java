package loipn.mycamera1;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by loipn on 4/19/2016.
 */
public class CameraSurfaceView2 extends SurfaceView {

    HandlerThread mCameraThread = null;
    Handler mCameraHandler = null;

    private Camera camera;

    public CameraSurfaceView2(Context context) {
        super(context);
    }

    private void startCamera() {
        if (mCameraThread == null) {
            mCameraThread = new HandlerThread("CAMERA_THREAD_NAME");
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());
        }
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    camera = Camera.open();
                } catch (Exception e) {

                }

            }
        });
    }

    SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    class LooperThread extends Thread {
        public Handler mHandler;

        public void run() {
            Looper.prepare();

            mHandler = new Handler();
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                }
            });

            Looper.loop();
        }
    }
}
