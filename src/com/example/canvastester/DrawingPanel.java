package com.example.canvastester;

import com.darvds.ribbonmenu.RibbonMenuCallback;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.view.GestureDetectorCompat;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

enum ScreenItem { NONE, CH1, CH2, TRIGGER, MATH1 }

public class DrawingPanel extends SurfaceView implements
SurfaceHolder.Callback, GestureDetector.OnGestureListener,
GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {	

	
	private final int LEFT_ANIM = 1;
	private final int RIGHT_ANIM = 2;
	private final int CHANNEL_1 = 1;
	private final int CHANNEL_2 = 2;	
	
	
	private static final String DEBUG_TAG = "Gestures";
	private GestureDetectorCompat mDetector;
	private ScaleGestureDetector mScaleDetector;

	private String serverIpAddress = "10.0.1.4";
	Thread cThread = null;


	private DrawingThread _thread;
	private float xlocation;
	private int x_dim, y_dim;
	private int mTextSize;
	private int mBoundary, mGridSize;
	private Paint channel1Paint, channel2Paint, channelMathPaint, generalTextPaint, gridPaint, channel1TextPaint, channel2TextPaint, channelMathTextPaint;
	private boolean channel1Active, channel2Active, channelMathActive;
	private float graphscaleX, graphscaleY, graphshiftx, graphshiftyChannel1, graphshiftyChannel2, graphshiftyMath;
	public PointF[] channel1Points = new PointF[1200];
	public PointF[] channel2Points = new PointF[1200];
	public PointF[] channelMathPoints = new PointF[1200];
	private boolean displayUpdating = true;
	private boolean displayPaused = false;
	
	private Bitmap toDisk;
	public boolean screenShot = false;
	Context context;

	private float totalGraphShiftx = 0;
	private float totalGraphShifty = 0;
	private float totalGraphScale = 1;
	float scale = 1;

	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// Remember some things for zooming
	PointF start = new PointF();
	PointF mid = new PointF();
	float oldDist = 1f;

	// Enumerations/Arrays for knowing what 'finger one' is touching
	ScreenItem activeScreenItems = ScreenItem.NONE;
	

	/**This is the public constructor for a DrawingPanel Object. We had to implement  two constuctors in order to fully implement a SurfaceView which DrawingPanel is an implementation of **/
		
	public DrawingPanel(Context context, AttributeSet attr) {
		super(context, attr); // This is required to make sure that the basics of a surface view that need to be done are done. What follows id specific to DrawingPanel
		this.context = context;
		getHolder().addCallback(this); 
		_thread = new DrawingThread(getHolder(), this); // A Surfaceview has a built in thread object. This creates the thread.
		xlocation = 0; // Used only for autogenerated waves in testing
		mTextSize = 15; // Default size of Oscope text
		mBoundary = 10; // Default distance from the edge of the surface view
		mGridSize = 50; // Default number of pixels between grid lines on the surface view
		graphscaleX = 1; // Default scale for all signals in x dimension (time)
		graphscaleY = 1; // Default scale for all signals in y dimension (volts)
		graphshiftx = 0; // Default shift in x-axis same for both channels (time)
		graphshiftyChannel1 = 100; // Default shift in y dimension (volts) for channel 1
		graphshiftyChannel2 = 500; // Default shift in y dimension (volts) for channel 2
		graphshiftyMath = 300; // Default shift in y dimension (volts) for Math channel

		for (int i = 0; i < channel1Points.length; i++) { // Initialize the array of 1200 points to hold the waveforms.
			channel1Points[i] = new PointF();
			channel2Points[i] = new PointF();
			channelMathPoints[i] = new PointF();
		}

		/**Paint properties for Channel 1 **/
		channel1Paint = new Paint();
		channel1Paint.setDither(true);
		channel1Paint.setColor(0xFF0000FF);
		channel1Paint.setStyle(Paint.Style.STROKE);
		channel1Paint.setStrokeJoin(Paint.Join.ROUND);
		channel1Paint.setStrokeCap(Paint.Cap.ROUND);
		channel1Paint.setStrokeWidth(10);

		/**Paint properties for Channel 2 **/
		channel2Paint = new Paint();
		channel2Paint.setDither(true);
		channel2Paint.setColor(0xFFFF0000);
		channel2Paint.setStyle(Paint.Style.STROKE);
		channel2Paint.setStrokeJoin(Paint.Join.ROUND);
		channel2Paint.setStrokeCap(Paint.Cap.ROUND);
		channel2Paint.setStrokeWidth(10);

		/**Paint properties for Math Channel **/
		channelMathPaint = new Paint();
		channelMathPaint.setDither(true);
		channelMathPaint.setColor(0xFF7F007F);
		channelMathPaint.setStyle(Paint.Style.STROKE);
		channelMathPaint.setStrokeJoin(Paint.Join.ROUND);
		channelMathPaint.setStrokeCap(Paint.Cap.ROUND);
		channelMathPaint.setStrokeWidth(10);

		/**Paint properties for Display Grid**/
		gridPaint = new Paint();
		gridPaint.setDither(true);
		gridPaint.setColor(0x7FFFFFFF);
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeJoin(Paint.Join.ROUND);
		gridPaint.setStrokeCap(Paint.Cap.ROUND);
		gridPaint.setStrokeWidth(1);

		/**Paint properties for General text**/
		generalTextPaint = new Paint();
		generalTextPaint.setDither(true);
		generalTextPaint.setColor(0xFFFFFF00);
		generalTextPaint.setStyle(Paint.Style.STROKE);
		generalTextPaint.setStrokeJoin(Paint.Join.ROUND);
		generalTextPaint.setStrokeCap(Paint.Cap.ROUND);
		generalTextPaint.setStrokeWidth(3);
		generalTextPaint.setTextSize((float) 50);


		/**Paint properties for Channel 1 text**/
		channel1TextPaint = new Paint();
		channel1TextPaint.setDither(true);
		channel1TextPaint.setColor(0xFF0000FF);
		channel1TextPaint.setStyle(Paint.Style.STROKE);
		channel1TextPaint.setStrokeJoin(Paint.Join.ROUND);
		channel1TextPaint.setStrokeCap(Paint.Cap.ROUND);
		channel1TextPaint.setStrokeWidth(3);
		channel1TextPaint.setTextSize((float) 50);
		
		/**Paint properties for Channel 2 text**/
		channel2TextPaint = new Paint();
		channel2TextPaint.setDither(true);
		channel2TextPaint.setColor(0xFFFF0000);
		channel2TextPaint.setStyle(Paint.Style.STROKE);
		channel2TextPaint.setStrokeJoin(Paint.Join.ROUND);
		channel2TextPaint.setStrokeCap(Paint.Cap.ROUND);
		channel2TextPaint.setStrokeWidth(3);
		channel2TextPaint.setTextSize((float) 50);
		
		/**Paint properties for Channel Math text**/
		channelMathTextPaint = new Paint();
		channelMathTextPaint.setDither(true);
		channelMathTextPaint.setColor(0xFF7F007F);
		channelMathTextPaint.setStyle(Paint.Style.STROKE);
		channelMathTextPaint.setStrokeJoin(Paint.Join.ROUND);
		channelMathTextPaint.setStrokeCap(Paint.Cap.ROUND);
		channelMathTextPaint.setStrokeWidth(3);
		channelMathTextPaint.setTextSize((float) 50);

		// Instantiate the gesture detector with the
		// application context and an implementation of
		// GestureDetector.OnGestureListener
		mDetector = new GestureDetectorCompat(context, this);
		// Set the gesture detector as the double tap
		// listener.
		mDetector.setOnDoubleTapListener(this);

		mScaleDetector = new ScaleGestureDetector(context, this);

		//setLongClickable(true);

		/**Create the networking thread**/
		/**Commented out to create non-network test version**/
		/*
		 * cThread = new Thread(new ClientThread());
		 * cThread.start();
		 * Log.d("TestTCPIP", String.valueOf(cThread.getId()));
		 */

	}

	public DrawingPanel(Context context) {
		super(context);// This is required to make sure that the basics of a surface view that need to be done are done. What follows id specific to DrawingPanel
		this.context = context;
		getHolder().addCallback(this); 
		_thread = new DrawingThread(getHolder(), this); // A Surfaceview has a built in thread object. This creates the thread.
		xlocation = 0; // Used only for autogenerated waves in testing
		mTextSize = 15; // Default size of Oscope text
		mBoundary = 10; // Default distance from the edge of the surface view
		mGridSize = 50; // Default number of pixels between grid lines on the surface view
		graphscaleX = 1; // Default scale for all signals in x dimension (time)
		graphscaleY = 1; // Default scale for all signals in y dimension (volts)
		graphshiftx = 0; // Default shift in x-axis same for both channels (time)
		graphshiftyChannel1 = 100; // Default shift in y dimension (volts) for channel 1
		graphshiftyChannel2 = 500; // Default shift in y dimension (volts) for channel 2
		graphshiftyMath = 300; // Default shift in y dimension (volts) for Math channel

		for (int i = 0; i < channel1Points.length; i++) { // Initialize the array of 1200 points to hold the waveforms.
			channel1Points[i] = new PointF();
			channel2Points[i] = new PointF();
			channelMathPoints[i] = new PointF();
		}

		/**Paint properties for Channel 1 **/
		channel1Paint = new Paint();
		channel1Paint.setDither(true);
		channel1Paint.setColor(0xFF0000FF);
		channel1Paint.setStyle(Paint.Style.STROKE);
		channel1Paint.setStrokeJoin(Paint.Join.ROUND);
		channel1Paint.setStrokeCap(Paint.Cap.ROUND);
		channel1Paint.setStrokeWidth(10);

		/**Paint properties for Channel 2 **/
		channel2Paint = new Paint();
		channel2Paint.setDither(true);
		channel2Paint.setColor(0xFFFF0000);
		channel2Paint.setStyle(Paint.Style.STROKE);
		channel2Paint.setStrokeJoin(Paint.Join.ROUND);
		channel2Paint.setStrokeCap(Paint.Cap.ROUND);
		channel2Paint.setStrokeWidth(10);

		/**Paint properties for Math Channel **/
		channelMathPaint = new Paint();
		channelMathPaint.setDither(true);
		channelMathPaint.setColor(0xFF7F007F);
		channelMathPaint.setStyle(Paint.Style.STROKE);
		channelMathPaint.setStrokeJoin(Paint.Join.ROUND);
		channelMathPaint.setStrokeCap(Paint.Cap.ROUND);
		channelMathPaint.setStrokeWidth(10);
		
		/**Paint properties for Display Grid**/
		gridPaint = new Paint();
		gridPaint.setDither(true);
		gridPaint.setColor(0x7FFFFFFF);
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeJoin(Paint.Join.ROUND);
		gridPaint.setStrokeCap(Paint.Cap.ROUND);
		gridPaint.setStrokeWidth(1);

		/**Paint properties for General text**/
		generalTextPaint = new Paint();
		generalTextPaint.setDither(true);
		generalTextPaint.setColor(0xFFFFFF00);
		generalTextPaint.setStyle(Paint.Style.STROKE);
		generalTextPaint.setStrokeJoin(Paint.Join.ROUND);
		generalTextPaint.setStrokeCap(Paint.Cap.ROUND);
		generalTextPaint.setStrokeWidth(3);
		generalTextPaint.setTextSize((float) 50);


		/**Paint properties for Channel 1 text**/
		channel1TextPaint = new Paint();
		channel1TextPaint.setDither(true);
		channel1TextPaint.setColor(0xFF0000FF);
		channel1TextPaint.setStyle(Paint.Style.STROKE);
		channel1TextPaint.setStrokeJoin(Paint.Join.ROUND);
		channel1TextPaint.setStrokeCap(Paint.Cap.ROUND);
		channel1TextPaint.setStrokeWidth(3);
		channel1TextPaint.setTextSize((float) 50);
		
		/**Paint properties for Channel 2 text**/
		channel2TextPaint = new Paint();
		channel2TextPaint.setDither(true);
		channel2TextPaint.setColor(0xFFFF0000);
		channel2TextPaint.setStyle(Paint.Style.STROKE);
		channel2TextPaint.setStrokeJoin(Paint.Join.ROUND);
		channel2TextPaint.setStrokeCap(Paint.Cap.ROUND);
		channel2TextPaint.setStrokeWidth(3);
		channel2TextPaint.setTextSize((float) 50);

		/**Paint properties for Channel Math text**/
		channelMathTextPaint = new Paint();
		channelMathTextPaint.setDither(true);
		channelMathTextPaint.setColor(0xFF7F007F);
		channelMathTextPaint.setStyle(Paint.Style.STROKE);
		channelMathTextPaint.setStrokeJoin(Paint.Join.ROUND);
		channelMathTextPaint.setStrokeCap(Paint.Cap.ROUND);
		channelMathTextPaint.setStrokeWidth(3);
		channelMathTextPaint.setTextSize((float) 50);
		
		// Instantiate the gesture detector with the
		// application context and an implementation of
		// GestureDetector.OnGestureListener
		mDetector = new GestureDetectorCompat(context, this);
		// Set the gesture detector as the double tap
		// listener.
		mDetector.setOnDoubleTapListener(this);
		mDetector.setIsLongpressEnabled(false);

		mScaleDetector = new ScaleGestureDetector(context, this);

		setLongClickable(true);

		
		
		/**Create the networking thread**/
		/**Commented out to create non-network test version**/
		/*
		 * cThread = new Thread(new ClientThread());
		 * cThread.start();
		 * Log.d("TestTCPIP", String.valueOf(cThread.getId()));
		 */
	}

/** Callback to access ribbon menus in main activity **/
	RibbonMenuCallback RibbonMenuCallbackClass;
	
	void registerMenuCallback(RibbonMenuCallback callback){
	  RibbonMenuCallbackClass = callback;
	 }

	//Not sure what this does, i dont think it is necessary any more
	MainActivity theListener;
	
	public void setLongClickListener(MainActivity l) {
        theListener = l;
    }
	
	/** This section is for the overrides for the gesture listener */

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		this.mDetector.onTouchEvent(event);
		// Be sure to call the superclass implementation
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent event) {
		Log.d(DEBUG_TAG, "onDown: " );
		displayUpdating = false;
		return true;
	}

	@Override
	public boolean onFling(MotionEvent event1, MotionEvent event2,
			float velocityX, float velocityY) {
		Log.d(DEBUG_TAG, "onFling: " + event1.getX());
		if (event1.getX() < 40)
			RibbonMenuCallbackClass.ToggleRibbonMenu(LEFT_ANIM);
		if (event1.getX() > 1180)
			RibbonMenuCallbackClass.ToggleRibbonMenu(RIGHT_ANIM);
		return true;
	}


	
	@Override
	public void onLongPress(MotionEvent event) {
		Log.d(DEBUG_TAG, "onLongPress: " + event.getX() + ", " + event.getY());
		//theListener.onLongClick(this);
		int surfWidth = this.getWidth(); 
		int surfHeight = this.getHeight(); 
		int surfX = this.getLeft(); 
		int surfY = this.getTop();
		
//		showPopup(findViewById(R.id.scopeView));
		
		
		for (int i = 0; i < this.channel1Points.length ; i++){ //
			//Log.d(DEBUG_TAG, "XY: " + (int)(event.getX()) + ", " + (int)(event.getY()) +" current Channel1 XY: " + (int)((((this.channel1Points[i].x*graphscaleX)+graphshiftx))) + "," + (int)(((this.channel1Points[i].y+graphshiftyChannel1))) ); 
			if ((int)((event.getX())/30) == (int)((((this.channel1Points[i].x*graphscaleX)+graphshiftx))/(30*graphscaleX))){ //channel1Points[i].x * graphscaleX + graphshiftx
				//Log.d(DEBUG_TAG, "Channel 1 X Match");
				if ((int)((event.getY())/30) == (int)((this.channel1Points[i].y+graphshiftyChannel1)/30)){ 
					Log.d(DEBUG_TAG, "Channel 1 Touch"); 
					activeScreenItems = ScreenItem.CH1;
					//channel1Active = false;
					RibbonMenuCallbackClass.ToggleRibbonWaveformMenu(CHANNEL_1);
					break;
				}
			}

		}
		for (int i = 0; i < this.channel2Points.length ; i++){ //
			//Log.d(DEBUG_TAG, "XY: " + (int)(event.getX()) + ", " + (int)(event.getY()) +" current Channel2 XY: " + (int)((((this.channel2Points[i].x*graphscaleX)+graphshiftx))) + "," + (int)(((this.channel2Points[i].y+graphshiftyChannel2))) ); 
			if ((int)((event.getX())/30) == (int)((((this.channel2Points[i].x*graphscaleX)+graphshiftx))/(30*graphscaleX))){ //channel1Points[i].x * graphscaleX + graphshiftx
				//Log.d(DEBUG_TAG, "Channel 1 X Match");
				if ((int)((event.getY())/30) == (int)((this.channel2Points[i].y+graphshiftyChannel2)/30)){ 
					Log.d(DEBUG_TAG, "Channel 2 Touch"); 
					activeScreenItems = ScreenItem.CH2;
					//channel2Active = false;
					RibbonMenuCallbackClass.ToggleRibbonWaveformMenu(CHANNEL_2);
					break;
				}
			}

		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		Log.d(DEBUG_TAG, "onScroll: ");
		graphshiftx = -distanceX + graphshiftx;
		graphshiftyChannel1 = -distanceY + graphshiftyChannel1;
		return true;
	}

	@Override
	public void onShowPress(MotionEvent event) {
		Log.d(DEBUG_TAG, "onShowPress: " );
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		Log.d(DEBUG_TAG, "onSingleTapUp: ");
		if (displayPaused){
			displayUpdating = false;
			displayPaused = false;
		}
		else{
			displayUpdating = true;
			displayPaused = true;
		}
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent event) {
		Log.d(DEBUG_TAG, "onDoubleTap: ");
		RibbonMenuCallbackClass.ToggleRibbonMenu(LEFT_ANIM);
		RibbonMenuCallbackClass.ToggleRibbonMenu(RIGHT_ANIM);
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent event) {
		Log.d(DEBUG_TAG, "onDoubleTapEvent: ");
		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent event) {
		Log.d(DEBUG_TAG, "onSingleTapConfirmed: " );
		return true;
	}
//Function that captures a screenshot and opens the gallery. It is dirty and uses some global variables connected to main activity and drawing thread.
	public void screenCap(){ 
		
		Calendar calendar = Calendar.getInstance();
		 int date = calendar.get(Calendar.DATE);
		 int month = calendar.get(Calendar.MONTH);
		 int year = calendar.get(Calendar.YEAR);
		 int hour = calendar.get(Calendar.HOUR_OF_DAY);
		 int minute = calendar.get(Calendar.MINUTE);
		 int second = calendar.get(Calendar.SECOND);
		 
		 
		 OutputStream fOut = null;
		 String ourDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "ScholaPics";
		 String imgname = Integer.toString(year) + "." + Integer.toString(month + 1) + "." +  Integer.toString(date) + "-" +  Integer.toString(hour) + ":" +  Integer.toString(minute) + "." +  Integer.toString(second) +  ".jpg";
         File myDir=new File(ourDir);
         myDir.mkdirs();
         Log.d("screenCap", "File saving to: " + ourDir + imgname);
		 File file = new File(ourDir, imgname);
             try {
				fOut = new FileOutputStream(file);
		         toDisk.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
		         Log.d("screenCap", "File saved to: " + ourDir + "/" + imgname);
	             fOut.flush();
	             fOut.close();
	             //MediaStore.Images.Media.insertImage(context.getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
	             screenShot = false;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
             //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    
             //String SCAN_PATH;
             //File[] allFiles ;
             //File folder = new File(ourDir + "/");
             //allFiles = folder.listFiles();
             //Log.d("screenCap", "Opening: " + allFiles[allFiles.length - 1].getAbsolutePath());
             //new SingleMediaScanner(context, allFiles[0]);
             
             // tells your intent to get the contents
          // opens the URI for your image directory on your sdcard
          //intent.setType(ourDir); 
          //((Activity) RibbonMenuCallbackClass).startActivityForResult(intent, 1);


            
	}
	
	 

	/** This section is for the overrides for the SurfaceView */
//Creates a bitmap that onDraw can draw to and be saved
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.x_dim = w;
    	this.y_dim = h;
        toDisk = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        super.onSizeChanged(w, h, oldw, oldh);
    }
	
	@Override
	public void onDraw(Canvas canvas) {
		int i = 0;
		int j = 0;
		x_dim = 1000;//this.getWidth();
		y_dim = this.getHeight();
		generalTextPaint.setTextSize((float) (y_dim / mTextSize));
		channel1TextPaint.setTextSize((float) (y_dim / mTextSize));
		channel2TextPaint.setTextSize((float) (y_dim / mTextSize));
		
		/** This section creates example waveforms.Comment out when examining live data**/
		for (i = 0; i < 1200; i++) {
			channel1Points[i]
					.set((float) i, (float) ((100) * (Math
							.sin(((10*i + xlocation) * (.02))))));
			channel2Points[i].set((float) i,
					(float) ((50) * (Math.sin(((i) * (.02))))));
			channelMathPoints[i].set((float) i, (float) (channel2Points[i].y-channel1Points[i].y));
		}
		if (displayUpdating) {
			xlocation = (xlocation + 20) % 1200;// Makes the waves move
		/** End of Waveform generation**/
		}
		

		//Send all draw commands to this canvas also
		canvas.drawBitmap(toDisk, new Matrix(), new Paint());
		canvas.setBitmap(toDisk);
		
		canvas.drawColor(Color.BLACK);
		j = (this.getWidth() % mGridSize) / 2;
		for (i = (y_dim % mGridSize) / 2; i < y_dim; i = i + mGridSize) {
			canvas.drawLine((float) j, (float) i, (float) (this.getWidth() - j),
					(float) i, gridPaint);
		}
		i = (y_dim % mGridSize) / 2;
		for (j = (this.getWidth() % mGridSize) / 2; j < this.getWidth(); j = j + mGridSize) {
			canvas.drawLine((float) j, (float) i, (float) (j),
					(float) (y_dim - i), gridPaint);
		}
			
		if (channel1Active) {
			for (i = 0; i < (x_dim - 1); i++) {
				canvas.drawLine(channel1Points[i].x * graphscaleX
						+ graphshiftx, channel1Points[i].y * graphscaleY + graphshiftyChannel1,
						channel1Points[i + 1].x * graphscaleX + graphshiftx,
						channel1Points[i + 1].y * graphscaleY + graphshiftyChannel1, channel1Paint);
			}
			channel1TextPaint.setTextAlign(Paint.Align.RIGHT);
			canvas.drawText("CH1", (float) (this.getWidth() - mBoundary), (float) 80,
					channel1TextPaint);
		}

		if (channel2Active) {
			for (i = 0; i < x_dim - 1; i++) {
				canvas.drawLine(channel2Points[i].x * graphscaleX
						+ graphshiftx, channel2Points[i].y + graphshiftyChannel2,
						channel2Points[i + 1].x * graphscaleX + graphshiftx,
						channel2Points[i + 1].y + graphshiftyChannel2, channel2Paint);
			}
			channel2TextPaint.setTextAlign(Paint.Align.RIGHT);
			canvas.drawText("CH2", (float) (this.getWidth() - mBoundary),
					(float) 120, channel2TextPaint);
		}
		
		if (channelMathActive) {
			for (i = 0; i < x_dim - 1; i++) {
				canvas.drawLine(channelMathPoints[i].x * graphscaleX
						+ graphshiftx, channelMathPoints[i].y + graphshiftyMath,
						channelMathPoints[i + 1].x * graphscaleX + graphshiftx,
						channelMathPoints[i + 1].y + graphshiftyMath, channelMathPaint);
			}
			channelMathTextPaint.setTextAlign(Paint.Align.RIGHT);
			canvas.drawText("Math", (float) (this.getWidth() - mBoundary),
					(float) 160, channelMathTextPaint);
		}

			canvas.drawText(
					String.valueOf(x_dim).concat(" x ")
					.concat(String.valueOf(y_dim)), (float) mBoundary,
					(float) y_dim - mBoundary, generalTextPaint);
		

			
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	
//Im not sure why all this works, its the final product that works for me even though i confused the heck out of myself setting it up
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d("test", "surfaceCreated");
		if(_thread.getState() != Thread.State.NEW)
        {
			_thread = new DrawingThread(getHolder(), this);
			//Log.d("test", "before onresume");
            _thread.setRunning(true);
            _thread.start();

        }else{
		_thread.setRunning(true);
		_thread.start();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d("test", "surfaceDestroyed");
		boolean retry = true;
		_thread.setRunning(false);
		while (retry) {
			try {
				_thread.interrupt();
				retry = false;
			} catch (Exception e) {
				// we will try it again and again...
			}
		}
	}



	/** This section is for the getter and setters */

	public void setGraphScaleY(float scale) {
		graphscaleY= scale;
	}

	public void setGraphScaleX(float scale) {
		graphscaleX= scale;
	}

	public void setGraphShiftChannel1(float x, float y) {
		graphshiftx = x;
		graphshiftyChannel1 = y;
	}

	public void setGraphShiftChannel2(float x, float y) {
		graphshiftx = x;
		graphshiftyChannel2 = y;
	}	
	
	public void setGraphShiftChannelMath(float x, float y) {
		graphshiftx = x;
		graphshiftyMath = y;
	}
	
	public DrawingThread getThread() {
		return _thread;
	}

	public void setChannel1(boolean mode) {
		channel1Active = mode;
	}
	
	public boolean getChannel1() {
		return channel1Active;
	}
	
	public void setChannel2(boolean mode) {
		channel2Active = mode;
	}

	public boolean getChannel2() {
		return channel2Active;
	}
	
	public void setChannelMath(boolean mode) {
		channelMathActive = mode;
	}
	
	public boolean getChannelMath() {
		return channelMathActive;
	}

	public ScreenItem getActiveScreenItems(){
		return activeScreenItems;
	}
	
	/** This section is for the overrides for the ScaleListener */

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		// TODO Auto-generated method stub
		Log.d("Scaling", "Scaling: " + graphscaleY);
		if (detector.getCurrentSpanX()*2 < detector.getCurrentSpanY()) // Y bigger than X
			graphscaleY *= detector.getScaleFactor();
		else if (detector.getCurrentSpanY()*2 < detector.getCurrentSpanX()) // X bigger than Y
			graphscaleX *= detector.getScaleFactor();
		else {
			graphscaleY *= detector.getScaleFactor();
			graphscaleX *= detector.getScaleFactor();
		}

		// Don't let the object get too small or too large.
		graphscaleY = Math.max(0.1f, Math.min(graphscaleY, 5.0f));
		graphscaleX = Math.max(0.1f, Math.min(graphscaleX, 5.0f));

		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		// TODO Auto-generated method stub
		Log.d("Scaling", "Beginning Scale: " + graphscaleY);

		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		// TODO Auto-generated method stub
		Log.d("Scaling", "Ending Scale: " + graphscaleY);

	}


	/** This section deals with the network connection **/

	public class ClientThread implements Runnable {

		public void run() {
			try {
				InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
				Log.d("TestTCPIP", "C: Connecting...");
				Socket socket = new Socket(serverAddr, 15151);             

				String channel1 = null;
				char[] channel1chararray = new char[8000];
				String channel2 = null;
				int count;
				
				long oldtime = System.currentTimeMillis();
				int recvpackets = 0;
				
				while (true){    
					try {
						//Log.d("TestTCPIP", "C: Sending command.");
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));   

						channel1 = null;
						channel1chararray = new char[8000];
						channel2 = null;

						while (!(in.ready()));
 
						channel1 = in.readLine();

						//Log.d("DrawingPanel-Data", "Channel 1: " + channel1.substring(0,20));
						StringTokenizer Tok = new StringTokenizer(channel1, ",");

						int i = 0;
						String temp;
						temp = String.valueOf(Tok.nextElement());
						//Log.d("DrawingPanel-Data", temp);
						if (0 == temp.compareTo("CHONE")){
							//Log.d("DrawingPanel-Data", "Channel 1: " + channel1.substring(0,20));
							recvpackets++;
							count = Integer.valueOf(String.valueOf(Tok.nextElement()));
							while (Tok.hasMoreElements()){
								//		Log.d("DrawingPanel-Data", "Count: " + String.valueOf(count));
								count--;
								if (count < 0)
									break;
								channel1Points[i].y = Float.valueOf(String.valueOf(Tok.nextElement()));
								channel1Points[i].x = i++;

							}
						} else if (0 == temp.compareTo("CHTWO")){
							//Log.d("DrawingPanel-Data", "Channel 2: " + channel1.substring(0,20));
							count = Integer.valueOf(String.valueOf(Tok.nextElement()));
							while (Tok.hasMoreElements()){
								//		Log.d("DrawingPanel-Data", "Count: " + String.valueOf(count));
								count--;
								if (count < 0)
									break;
								//Log.d("FLOATING", String.valueOf(Tok.nextElement()));
								channel2Points[i].y = Float.valueOf(String.valueOf(Tok.nextElement()));
								channel2Points[i].x = i++;

							}
						}
						if (recvpackets > 100){
							recvpackets = 0;
							Log.d("DrawingPanel-Data", String.valueOf((System.currentTimeMillis()-oldtime)) + "ms for last 100 values");
							oldtime = System.currentTimeMillis();
						}
						
					} catch (Exception e) {
						Log.e("TestTCPIP", "S: Error", e);
						break;
					}
				}
				socket.close();
				Log.d("TestTCPIP", "C: Closed.");
			} catch (Exception e) {
				Log.e("TestTCPIP", "C: Error", e);
			}
		}

		private float getFloat(BufferedReader in){
			String temp = null;

			try {
				while (!(in.ready()));
				temp = in.readLine();

				while ((temp == null) || (temp.equalsIgnoreCase("")))
					temp = in.readLine();
			} catch (Exception e) {
			}

			return Float.valueOf(temp);
		}

	}
	
	
	
	public class SelectedItems {
		boolean NONE;
		boolean CH1;
		boolean CH2;
		boolean MTH1;
		boolean TRGR;
		
		public void wipe(){
			NONE = CH1 = CH2 = MTH1 = TRGR = false;
		}
		//setters
		public void setNONE(){
			NONE = true;
		}
		public void setCH1(){
			CH1 = true;
		}
		public void setCH2(){
			CH2 = true;
		}
		public void setMTH1(){
			MTH1 = true;
		}
		public void setTRGR(){
			TRGR = true;
		}
		//getters
		public boolean getNONE(){
			return NONE;
		}
		public boolean getCH1(){
			return CH1;
		}
		public boolean getCH2(){
			return CH2;
		}
		public boolean getMTH1(){
			return MTH1;
		}
		public boolean getTRGR(){
			return TRGR;
		}
	}


}



/*
@Override 
public boolean onTouchEvent(MotionEvent event) {


	int surfWidth = this.getWidth(); 
	int surfHeight = this.getHeight(); 
	int surfX = this.getLeft(); 
	int surfY = this.getTop();

	// Handle touch events here... 
	switch (event.getAction() & MotionEvent.ACTION_MASK) { 
	case MotionEvent.ACTION_DOWN:
		start.set(event.getX(), event.getY()); 
		Log.w(this.getClass().getName(), "mode=DRAG"); mode = DRAG;

		for (int i = 0; i < this.channel1Points.length ; i++){ //
			Log.w(this.getClass().getName(), "XY: " + (int)((event.getX()-surfX)/30)
					+ ", " + (int)((event.getY()-surfY-50)/30) +" current Channel1 XY: " + (int)((this.channel1Points[i].x)/30) + "," + (int)((this.channel1Points[i].y)/30) ); 
			if ((int)((event.getX()-surfX)/30) == (int)((this.channel1Points[i].x)/30)){ 
				if ((int)((event.getY()-surfY-50)/30) == (int)((this.channel1Points[i].y)/30)){ 
					Log.w(this.getClass().getName(), "Channel 1 Touch"); }
			}

		}

		break; 
	case MotionEvent.ACTION_POINTER_DOWN: 
		oldDist = spacing(event);
		Log.w(this.getClass().getName(), "oldDist=" + oldDist); if (oldDist > 10f) { //midPoint(mid, event); mode = ZOOM;
			Log.w(this.getClass().getName(), "mode=ZOOM"); 
		} 
		return false; 
		//break;
	case MotionEvent.ACTION_UP: 
		if (mode == DRAG){ 
			totalGraphShiftx = (event.getX() - start.x)+totalGraphShiftx; 
			totalGraphShifty = (event.getY() - start.y)+totalGraphShifty; 
		} 
	case MotionEvent.ACTION_POINTER_UP: 
		if (mode == ZOOM){ 
			Log.w(this.getClass().getName(), "Storing Scale"); 
			totalGraphScale = scale;
			Log.w(this.getClass().getName(), "totalGraphScale= " + totalGraphScale); } mode = NONE;

			Log.w(this.getClass().getName(), "mode=NONE"); 
			break; 
	case MotionEvent.ACTION_MOVE: 
		if (event.getEdgeFlags()==MotionEvent.EDGE_LEFT){
			Log.w(this.getClass().getName(), "Left Edge (Lauch Menu)");
			break; 
		} 
		if (mode == DRAG) { // ... 
			this.setGraphShift((event.getX() - start.x)+totalGraphShiftx, (event.getY() - start.y)+totalGraphShifty); }
		else if (mode == ZOOM) { 
			float newDist = spacing(event);
			Log.w(this.getClass().getName(), "newDist=" + newDist); 
			if (newDist > 10f) { 
				scale = newDist / oldDist;
				this.setGraphScale(scale*totalGraphScale);
				Log.w(this.getClass().getName(), "Scale= " + scale);
				Log.w(this.getClass().getName(), "totalGraphScale= " + totalGraphScale);
			} 
		} 
		break; 
	} 
	return super.onTouchEvent(event);
}
 */
