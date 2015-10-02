package erdincozdemir.com.cameraexample;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

public class MainActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private RelativeLayout llMain;
    private int[] myPixels;
    private int frameWidth, frameHeight;
    private FrameLayout preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        llMain = (RelativeLayout) findViewById(R.id.llMain);

        mCamera = getCameraInstance();
        Camera.Parameters cameraParam = mCamera.getParameters();
        cameraParam.setPreviewFormat(ImageFormat.NV21);
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(cameraParam);
        cameraParam = mCamera.getParameters();
        mCamera.setParameters(cameraParam);
        mCamera.startPreview();

        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                frameHeight = camera.getParameters().getPreviewSize().height;
                frameWidth = camera.getParameters().getPreviewSize().width;
                int rgb[] = new int[frameWidth * frameHeight];
                myPixels = decodeYUV420SP(rgb, data, frameWidth, frameHeight);
            }
        });

        mCameraPreview = new CameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);
        mCameraPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(MainActivity.this, String.valueOf(event.getX()) + "," + String.valueOf(event.getY()), Toast.LENGTH_SHORT).show();
                int pixel = (Math.round(event.getY())*frameWidth) + Math.round(event.getX());
                final SelectedColor selectedColor = new SelectedColor(MainActivity.this);
                selectedColor.setColor(Integer.parseInt(String.format("%06X", (0xFFFFFF & myPixels[pixel])), 16) + 0xFF000000);
                selectedColor.setColorValue(String.format("#%06X", (0xFFFFFF & myPixels[pixel])));

                selectedColor.setOnTouchListener(new SwipeDismissTouchListener(selectedColor, null, new SwipeDismissTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(Object token) {
                        return true;
                    }

                    @Override
                    public void onDismiss(View view, Object token, SwipeDismissTouchListener.SwipeDirection direction) {

                        selectedColor.setVisibility(View.GONE);
                        Log.i("DEVELOPER", direction.toString());
                    }
                }));

                selectedColor.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i("DEVELOPER", "dasdsadas");
                    }
                });

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                llMain.addView(selectedColor, params);
                return false;
            }
        });

        new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(R.id.llMain, this))
                .setContentTitle("ShowcaseView")
                .setContentText("This is highlighting the Home button")
                .hideOnTouchOutside()
                .build();
    }

    public int[] decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {

        // here we're using our own internal PImage attributes
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                // use interal buffer instead of pixels for UX reasons
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }

        return rgb;
    }

    /**
     * Helper method to access the camera returns null if it cannot get the
     * camera or does not exist
     *
     * @return Camera
     */
    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            // cannot get camera or does not exist
        }
        return camera;
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }
}
