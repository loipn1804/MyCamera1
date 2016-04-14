package loipn.mycamera1;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private FrameLayout frameCamera;
    private View btnCap;

    private CameraSurfaceView cameraSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameCamera = (FrameLayout) findViewById(R.id.frameCamera);
        btnCap = findViewById(R.id.btnCap);

        cameraSurfaceView = new CameraSurfaceView(this);
        frameCamera.addView(cameraSurfaceView);

        btnCap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraSurfaceView.TakePicture();
            }
        });
    }
}
