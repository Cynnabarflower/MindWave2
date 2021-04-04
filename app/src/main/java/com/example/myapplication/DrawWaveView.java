package com.example.myapplication;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.myapplication.R.id.editTextNumber;
import static java.lang.Math.abs;

public class DrawWaveView extends View {
	private static final String TAG = "DrawWaveView";
	private Path path;
	public Paint paint = null;
	int VIEW_WIDTH= 0;
	int VIEW_HEIGHT = 0;
	Bitmap cacheBitmap = null;
	Canvas cacheCanvas = null;
	Paint bmpPaint = new Paint();
	private int maxPoint = 0;
	private int currentPoint = 0;
	private int maxValue = 0;
	private int minValue = 0;
	private float x = 0;
	private float y = 0;
	private float prex = 0;
	private float prey = 0;
	private boolean restart = true;
	
	private int mBottom = 0;
	private int mHeight = 0;
	private int mLeft = 0;
	private int mWidth = 0;
	
	private float mPixPerHeight = 0;
	private float mPixPerWidth = 0;
	Drone drone;
	
	public DrawWaveView(Context context, Drone drone){
		super(context);
		this.drone = drone;
	}

	public DrawWaveView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		
	}
	public void setValue(int maxPoint, int maxValue, int minValue){
		this.maxPoint = maxPoint;
		this.maxValue = maxValue;
		this.minValue = minValue;
	}
	
	public void initView(){


		mBottom = this.getBottom();
		mWidth = this.getWidth();
		mLeft = this.getLeft();
		mHeight = this.getHeight();
		
		mPixPerHeight = (float)mHeight/(maxValue  - minValue);
		mPixPerWidth =  (float)mWidth/maxPoint ;
		
//		Log.d(TAG,"initView  mWidth= " + mWidth + " , mHeight= " + mHeight  );
//		Log.d(TAG,"initView  mBottom= " + mBottom + " , mLeft= " + mLeft  );
//		Log.d(TAG,"initView  mPixPerHeight= "+ mPixPerHeight +" ,mPixPerWidth=" +mPixPerWidth);
		cacheBitmap = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_8888);
		cacheCanvas = new Canvas();
		path = new Path();
		cacheCanvas.setBitmap(cacheBitmap);
		
		paint = new Paint(Paint.DITHER_FLAG);
		paint.setColor(Color.GREEN);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(4);
		
		paint.setAntiAlias(true);
		paint.setDither(true);
		currentPoint =0;
	}
	
	public void clear()
	{
		Paint clearPaint = new Paint();
		clearPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
		cacheCanvas.drawPaint(clearPaint);
		clearPaint.setXfermode(new PorterDuffXfermode(Mode.SRC));
		currentPoint =0;
		path.reset();

		invalidate();
	}
	public boolean isReady(){
		return initFlag;
	}

	ArrayList<Integer> lastPoints = new ArrayList<>();

	double avg = 0;
	int lastPointsSum = 0;
	int blinkCounter = 0;
	long previousTime = 0;
	long firstBlinkTime = 0;
	long lastBlinkTime = 0;

	long lastTime = 0;

	public void updateData(int data, int threshold){

		if(!initFlag){
			return;
		}
//		if (cooldownTime > 0 && System.currentTimeMillis() - cooldownTime >= 3000) {
//			cooldownTime = 0;
//			drone.setCommand("land");
//		}
		lastTime = System.currentTimeMillis();

		if (lastPoints.size() > 10) {
			lastPoints.remove(0);
			lastPoints.add(data);
			lastPointsSum += data;
			lastPointsSum -= lastPoints.get(0);
		} else {
			avg += data;
			lastPoints.add(data);
			lastPointsSum += data;
			return;
		}


		if (System.currentTimeMillis() - firstBlinkTime > 2000) {
			if (blinkCounter >= 5) {
				if (drone.isUp)
					drone.setCommand("land");
				else {
					drone.setCommand("takeoff");
					lastBlinkTime = 0;
					firstBlinkTime = 0;
				}
				System.out.println("Blinksssssssssssssssssss");
				lastBlinkTime = 0;
				firstBlinkTime = 0;
			}
			blinkCounter = 0;
		}

		y = translateData2Y(data);
		x = translatePoint2X(currentPoint);

		if (lastPointsSum/10.0 > threshold) {
			if (firstBlinkTime > 0) {
				lastBlinkTime = System.currentTimeMillis();
			} else {
				firstBlinkTime = System.currentTimeMillis();
				lastBlinkTime = firstBlinkTime;
			}
			blinkCounter++;
		}

		if(currentPoint == 0){
			path.moveTo(x, y);
			currentPoint ++;
			prex = x;
			prey = y;
			previousTime = System.currentTimeMillis();
		} else if(currentPoint == maxPoint){
			cacheCanvas.drawPath(path,paint);
			currentPoint = 0;
		} else {
			if (y > threshold) {
				path.quadTo(prex, prey, x, -y);
				currentPoint++;
				prex = x;
				prey = y;
			} else {
				if (System.currentTimeMillis() - lastBlinkTime > 300) {
					lastBlinkTime = 0;
					firstBlinkTime = 0;
					blinkCounter = 0;
				}
				path.quadTo(prex, prey, x, 0);
				currentPoint++;
				prex = x;
				prey = 0;
			}
		}
		invalidate();
		if(currentPoint == 0){
			clear();
		}
	}
	/**
	 * y = top + height - (data -minValue) * height/(2*maxValue)
	 * @param data
	 * @return
	 */
	private float translateData2Y(int data){
		return (float)mBottom - (data - minValue) *mPixPerHeight ;
	}
	/**
	 * x = mLeft + mWidth/
	 * @param point
	 * @return
	 */
	private float translatePoint2X(int point){
		return (float)mLeft + point * mPixPerWidth;
	}
	
    private boolean initFlag = false;
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		canvas.drawBitmap(cacheBitmap, 0, 0, bmpPaint);
		canvas.drawPath(path, paint);
		//super.onDraw(canvas);
		
	}
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		initFlag = false;
		 Log.d(TAG,"onConfigurationChanged");
	}

	// for rotate screen things
	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		 Log.d(TAG,"onAttachedToWindow");
		 initFlag = false;
	}
	// for rotate screen things
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		// TODO Auto-generated method stub
		super.onLayout(changed, left, top, right, bottom);
		Log.d(TAG,"onLayout");
		 if(!initFlag){
			 initView();
			 initFlag = true;
		 }
	}


}
