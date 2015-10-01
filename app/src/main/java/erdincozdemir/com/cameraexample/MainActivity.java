package erdincozdemir.com.cameraexample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventListener;

public class MainActivity extends Activity implements SurfaceHolder {

    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private LinearLayout llColor;
    private int[] myPixels;
    private int frameWidth, frameHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        llColor = (LinearLayout) findViewById(R.id.llColor);

        mCamera = getCameraInstance();
        Camera.Parameters cameraParam = mCamera.getParameters();
        cameraParam.setPreviewFormat(ImageFormat.NV21);
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(cameraParam);
        cameraParam = mCamera.getParameters();
        try {
            mCamera.setPreviewDisplay(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.setParameters(cameraParam);
        mCamera.startPreview();

        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
              @Override
              public void onPreviewFrame(byte[] data, Camera camera) {
                  frameHeight = camera.getParameters().getPreviewSize().height;
                  frameWidth = camera.getParameters().getPreviewSize().width;
                  // number of pixels//transforms NV21 pixel data into RGB pixels
                  int rgb[] = new int[frameWidth * frameHeight];
                  // convertion
                  myPixels = decodeYUV420SP(rgb, data, frameWidth, frameHeight);
                  Log.i("DEVELOPER", String.format("%06X", (0xFFFFFF & myPixels[10])));
                  //llColor.setBackgroundColor(Integer.parseInt(String.format("%06X", (0xFFFFFF & myPixels[10])), 16) + 0xFF000000);
              }
          });

        mCameraPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);

        /*mCameraPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);

        mCamera.setDisplayOrientation(90);*/
        mCameraPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(MainActivity.this, String.valueOf(event.getX()) + "," + String.valueOf(event.getY()), Toast.LENGTH_SHORT).show();
                int pixel = (Math.round(event.getY())*frameWidth) + Math.round(event.getX());
                llColor.setBackgroundColor(Integer.parseInt(String.format("%06X", (0xFFFFFF & myPixels[pixel])), 16) + 0xFF000000);
                return false;
            }
        });
    }

    /*private void getColor(Camera camera, int x, int y) {
        int frameHeight = camera.getParameters().getPreviewSize().height;
        int frameWidth = camera.getParameters().getPreviewSize().width;
        int rgb[] = new int[frameWidth * frameHeight];
        decodeYUV420SP(rgb, data, frameWidth, frameHeight);
        Bitmap bmp = Bitmap.createBitmap(rgb, frameWidth, frameHeight, Bitmap.Config.ARGB_8888);
        int pixel = bmp.getPixel( x,y );
        int redValue = Color.red(pixel);
        int blueValue = Color.blue(pixel);
        int greenValue = Color.green(pixel);
        int thiscolor = Color.rgb(redValue, greenValue, blueValue);
        Toast.makeText(MainActivity.this, String.valueOf(thiscolor), Toast.LENGTH_SHORT).show();
    }*/

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

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {

            } catch (IOException e) {
            }
        }
    };

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    @Override
    public void addCallback(Callback callback) {

    }

    @Override
    public void removeCallback(Callback callback) {

    }

    @Override
    public boolean isCreating() {
        return false;
    }

    @Override
    public void setType(int type) {

    }

    @Override
    public void setFixedSize(int width, int height) {

    }

    @Override
    public void setSizeFromLayout() {

    }

    @Override
    public void setFormat(int format) {

    }

    @Override
    public void setKeepScreenOn(boolean screenOn) {

    }

    @Override
    public Canvas lockCanvas() {
        return null;
    }

    @Override
    public Canvas lockCanvas(Rect dirty) {
        return null;
    }

    @Override
    public void unlockCanvasAndPost(Canvas canvas) {

    }

    @Override
    public Rect getSurfaceFrame() {
        return null;
    }

    @Override
    public Surface getSurface() {
        return null;
    }

    /*private void  test(Camera camera) {
        int frameHeight = camera.getParameters().getPreviewSize().height;
        int frameWidth = camera.getParameters().getPreviewSize().width;
        int rgb[] = new int[frameWidth * frameHeight];
        decodeYUV420SP(rgb, data, frameWidth, frameHeight);
        Bitmap bmp = Bitmap.createBitmap(rgb, frameWidth, frameHeight, Bitmap.Config.ARGB_8888);
        int pixel = bmp.getPixel( x,y );
        int redValue = Color.red(pixel);
        int blueValue = Color.blue(pixel);
        int greenValue = Color.green(pixel);
        int thiscolor = Color.rgb(redValue, greenValue, blueValue);
    }*/
}
