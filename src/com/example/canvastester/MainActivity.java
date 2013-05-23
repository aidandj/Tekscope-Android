package com.example.canvastester;

import java.util.Locale;

import android.os.Bundle;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.v4.view.GestureDetectorCompat;
import android.util.FloatMath;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.PopupMenu;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.GestureDetector;
import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public abstract class MainActivity extends Activity implements TextToSpeech.OnInitListener {  
	
	private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector; 
    private TextToSpeech tts;
	
    @Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void openContextMenu(View view) {
		// TODO Auto-generated method stub
		super.openContextMenu(view);
	}

	private DrawingPanel mDrawingPanel;
    private DrawingThread mDrawingThread;
    private SurfaceView mSurfaceView;
    private Touch mTouch;
    
    private float totalGraphShiftx = 0;
    private float totalGraphShifty = 0;
    private float totalGraphScale = 1;
	float scale = 1;
    
    
	Matrix matrix = new Matrix();
	Matrix savedMatrix = new Matrix();


	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// Remember some things for zooming
	   PointF start = new PointF();
	   PointF mid = new PointF();
	   float oldDist = 1f;

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        Log.d("CanvasTester", "onCreate");
		
        tts = new TextToSpeech(this, this);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        mDrawingPanel = (DrawingPanel)findViewById(R.id.scopeView);
        mDrawingThread = mDrawingPanel.getThread();  
        
        registerForContextMenu(findViewById(R.id.scopeView)); // Allow for a context menu for long click on the screen
		
              
        if (savedInstanceState == null) {
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }
        
        /**Create the onClick Listeners for the buttons on the app**/
        
        Button buttonChannel1 = (Button) findViewById(R.id.buttonChannel1);
        buttonChannel1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
            	if(mDrawingPanel.getChannel1()){
            		mDrawingPanel.setChannel1(false);
            		Log.w(this.getClass().getName(), "Channel 1 Pressed");
                	tts.speak("channel one on", TextToSpeech.QUEUE_FLUSH, null);

            	} else {
            		mDrawingPanel.setChannel1(true);
            		Log.w(this.getClass().getName(), "Channel 1 Pressed");
                	tts.speak("channel one off", TextToSpeech.QUEUE_FLUSH, null);

            	}
            }
        });
        
        Button buttonChannel2 = (Button) findViewById(R.id.buttonChannel2);
        buttonChannel2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
            	if(mDrawingPanel.getChannel2()){
            		mDrawingPanel.setChannel2(false);
            		Log.w(this.getClass().getName(), "Channel 2 Pressed");
                	tts.speak("channel two on", TextToSpeech.QUEUE_FLUSH, null);

            	} else {
            		mDrawingPanel.setChannel2(true);
            		Log.w(this.getClass().getName(), "Channel 2 Pressed");
                	tts.speak("channel two off", TextToSpeech.QUEUE_FLUSH, null);

            	}
            }
        });
        
        Button buttonChannelMath = (Button) findViewById(R.id.buttonChannelMath);
        buttonChannelMath.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
            	if(mDrawingPanel.getChannelMath()){
            		mDrawingPanel.setChannelMath(false);
            		Log.w(this.getClass().getName(), "Channel Math Pressed");
            	} else {
            		mDrawingPanel.setChannelMath(true);
            		Log.w(this.getClass().getName(), "Channel Math Pressed");
            	}
            }
        });
        
        Button buttonAutoset = (Button) findViewById(R.id.buttonAutoset);
        buttonAutoset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
            	totalGraphShiftx = 0;
                totalGraphShifty = 100;
                totalGraphScale = 1;
                mDrawingPanel.setGraphScaleY(1);
                mDrawingPanel.setGraphScaleX(1);
            	mDrawingPanel.setGraphShiftChannelMath(0, 300);
            	mDrawingPanel.setGraphShiftChannel1(0, 100);
            	mDrawingPanel.setGraphShiftChannel2(0, 500);  
            }
        });
    
    }
 
    /**Current implementation of the LongPress Context Menu. It is connected to the surface view**/
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	Log.w(this.getClass().getName(), "onCreateContextMenu()");
   	
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.waveform, menu);
    	
    }

    @Override
    public void onPause(){
    	super.onPause();
    	Log.d("CanvasTester", "onPause");
		
    	
    }

    @Override
	public void onStart(){
    	super.onStart();
    	Log.d("CanvasTester", "onStart");
		
    	
    }
    
    @Override
	public void onResume(){
    	super.onResume();
    	Log.d("CanvasTester", "onResume");
		
    		
    }
    
    @Override
	public void onRestart(){
    	super.onRestart();
    	Log.d("CanvasTester", "onRestart");
		
    	
    	
    	
    	
    }
    @Override
	public void onInit(int status) {
		// TODO Auto-generated method stub

		if (status == TextToSpeech.SUCCESS) {

			int result = tts.setLanguage(Locale.US);

			tts.setPitch((float) 1); // set pitch level

			 tts.setSpeechRate((float) 1.25); // set speech speed rate

			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("TTS", "Language is not supported");
			
			}

		} else {
			Log.e("TTS", "Initilization Failed");
		}

	}

}