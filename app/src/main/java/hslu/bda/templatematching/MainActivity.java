package hslu.bda.templatematching;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final int SELECT_PICTURE = 1;
    private String selectedImagePath;
    private Mat sampledImage;
    private Mat originalImage;
    private String filename;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        //This is the callback method called once the OpenCV //manager is connected
        public void onManagerConnected(int status) {
            switch (status) {
                //Once the OpenCV manager is successfully connected we can enable the camera interaction with the defined OpenCV camera view
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("Example Loaded", "OpenCV loaded successfully");
                    //mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


    }

    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloVisionView);
        //Set the view as visible
        //mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        //Register your activity as the callback object to handle //camera frames
        //mOpenCvCameraView.setCvCameraViewListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Context context = getApplicationContext();
        CharSequence text = "You need to load an image first!";
        int duration = Toast.LENGTH_SHORT;

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_openGallary) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select Picture"), SELECT_PICTURE);
            return true;
        } else if (id==R.id.action_sobel){
            if(sampledImage==null)
            {
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                return true;
            }
            setSobel();
            return true;
        } else if (id == R.id.action_canny){
            if(sampledImage==null)
            {
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                return true;
            }
            setCanny();
            return true;
        } else if (id == R.id.action_HTL){
            if(sampledImage==null)
            {
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                return true;
            }
            setHoughTransformation();
            return true;
        } else if (id == R.id.action_CHT){
            if(sampledImage==null)
            {
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                return true;
            }
            setHoughCircle();
            return true;
        } else if (id == R.id.action_Color){
            setColorDetection();

        }

        return super.onOptionsItemSelected(item);
    }

    public void setSobel(){
        Mat blurredImage=new Mat();
        Size size=new Size(7,7);
        Imgproc.GaussianBlur(sampledImage, blurredImage, size, 0,0);

        Mat gray = new Mat();
        Imgproc.cvtColor(blurredImage, gray, Imgproc.COLOR_RGB2GRAY);

        Mat xFirstDervative =new Mat(),yFirstDervative =new Mat();
        int ddepth=CvType.CV_16S;

        Imgproc.Sobel(gray, xFirstDervative,ddepth , 1,0);
        Imgproc.Sobel(gray, yFirstDervative,ddepth , 0,1);

        Mat absXD=new Mat(),absYD=new Mat();

        Core.convertScaleAbs(xFirstDervative, absXD);
        Core.convertScaleAbs(yFirstDervative, absYD);

        Mat edgeImage=new Mat();
        Core.addWeighted(absXD, 0.5, absYD, 0.5, 0, edgeImage);

        displayImage(edgeImage);

    }

    public void setCanny(){
        Mat gray = new Mat();
        Imgproc.cvtColor(sampledImage, gray, Imgproc.COLOR_RGB2GRAY);

        Mat edgeImage=new Mat();
        Imgproc.Canny(gray, edgeImage, 100, 200);

        displayImage(edgeImage);
    }

    public void setHoughTransformation(){
        Mat binaryImage=new Mat();
        Imgproc.cvtColor(sampledImage, binaryImage, Imgproc.COLOR_RGB2GRAY);

        Imgproc.Canny(binaryImage, binaryImage, 80, 100);

        Mat lines = new Mat();
        int threshold = 50;

        Imgproc.HoughLinesP(binaryImage, lines, 1, Math.PI / 180, threshold);

        Imgproc.cvtColor(binaryImage, binaryImage, Imgproc.COLOR_GRAY2RGB);
        for (int i = 0; i < lines.cols(); i++) {
            double[] line = lines.get(0, i);
            double xStart = line[0], yStart = line[1], xEnd = line[2], yEnd = line[3];
            org.opencv.core.Point lineStart = new org.opencv.core.Point(xStart, yStart);
            org.opencv.core.Point lineEnd = new org.opencv.core.Point(xEnd, yEnd);

            Imgproc.line(binaryImage, lineStart, lineEnd, new Scalar(0, 0, 255), 3);
        }
        displayImage(binaryImage);
    }

    public void setHoughCircle(){
        Mat grayImage=new Mat();
        Imgproc.cvtColor(sampledImage, grayImage, Imgproc.COLOR_RGB2GRAY);

        double minDist=20;
        int thickness=2;
        double cannyHighThreshold=150;
        double accumlatorThreshold=50;
        Mat circles = new Mat();
        Imgproc.HoughCircles(grayImage, circles, Imgproc.CV_HOUGH_GRADIENT, 1, minDist, cannyHighThreshold, accumlatorThreshold, 0, 0);

        Imgproc.cvtColor(grayImage, grayImage, Imgproc.COLOR_GRAY2RGB);
        for (int i = 0; i < circles.cols(); i++)
        {
            double[] circle = circles.get(0, i);
            double centerX = circle[0], centerY = circle[1], radius = circle[2];
            org.opencv.core.Point center = new org.opencv.core.Point(centerX, centerY);
            Imgproc.circle(grayImage, center, (int) radius, new Scalar(0, 0, 255), thickness);
        }
        displayImage(grayImage);
    }

    public void setColorDetection(){
        Scalar min = new Scalar(0, 0, 130, 0);//BGR-A
        Scalar max= new Scalar(140, 110, 255, 0);//BGR-A
        //IplImage orgImg = cvLoadImage("colordetectimage.jpg");
        //create binary image of original size
        Mat binaryImage=new Mat();
        Imgproc.cvtColor(sampledImage, binaryImage, Imgproc.COLOR_RGB2GRAY);
        //Imgproc.threshold(sampledImage,dst, 123,255, Imgproc.THRESH_BINARY);
        //IplImage imgThreshold = cvCreateImage(cvGetSize(orgImg), 8, 1);
        //apply thresholding
        Imgproc.threshold(sampledImage, binaryImage, 0, 255, Imgproc.THRESH_BINARY);
        //InRangeS(orgImg, min, max, imgThreshold);
        //smooth filter- median
        Mat blurredImage=new Mat();
        int kernelDim=7;
        Imgproc.medianBlur(sampledImage,blurredImage , kernelDim);
        //cvSmooth(imgThreshold, imgThreshold, CV_MEDIAN, 13);
        //save
        displayImage(blurredImage);
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = getPath(selectedImageUri);
                filename = selectedImagePath.substring(selectedImagePath.lastIndexOf("/")+1);
                Log.i("Bildpfad", "selectedImagePath: " + selectedImagePath);
                Log.i("Bildname", "selectedFileName: " + filename);
                loadImage(selectedImagePath);
                displayImage(sampledImage);
            }
        }
    }

    private String getPath(Uri uri) {
        // just some safety built in
        if(uri == null ) {
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if(cursor != null ){
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    private void loadImage(String path) {
        originalImage = Imgcodecs.imread(path);
        Mat rgbImage=new Mat();

        Imgproc.cvtColor(originalImage, rgbImage, Imgproc.COLOR_BGR2RGB);

        Display display = getWindowManager().getDefaultDisplay();
        //This is "android graphics Point" class
        android.graphics.Point size = new Point();
        display.getSize(size);

        int width = (int) size.x;
        int height = (int) size.y;
        sampledImage=new Mat();

        double downSampleRatio= calculateSubSampleSize(rgbImage, width, height);

        Imgproc.resize(rgbImage, sampledImage, new Size(),downSampleRatio,downSampleRatio,Imgproc.INTER_AREA);

        try {
            ExifInterface exif = new ExifInterface(selectedImagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            switch (orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    //get the mirrored image
                    sampledImage=sampledImage.t();
                    //flip on the y-axis
                    Core.flip(sampledImage, sampledImage, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    //get up side down image
                    sampledImage=sampledImage.t();
                    //Flip on the x-axis
                    Core.flip(sampledImage, sampledImage, 0);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double calculateSubSampleSize(Mat srcImage, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = srcImage.height();
        final int width = srcImage.width();
        double inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of requested height and width to the raw
            //height and width
            final double heightRatio = (double) reqHeight / (double) height;
            final double widthRatio = (double) reqWidth / (double) width;

            // Choose the smallest ratio as inSampleSize value, this will
            //guarantee final image with both dimensions larger than or
            //equal to the requested height and width.
            inSampleSize = heightRatio<widthRatio ? heightRatio :widthRatio;
        }
        return inSampleSize;
    }

    private void displayImage(Mat image) {
        // create a bitMap
        Bitmap bitMap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        // convert to bitmap:
        Utils.matToBitmap(image, bitMap);

        // find the imageview and draw it!
        ImageView iv = (ImageView) findViewById(R.id.IODarkRoomImageView);
        iv.setImageBitmap(bitMap);
    }
}
