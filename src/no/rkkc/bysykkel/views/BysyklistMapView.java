package no.rkkc.bysykkel.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;

import com.google.android.maps.MapView;

public class BysyklistMapView extends MapView {
	boolean isPressed = true;
	
	public BysyklistMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public BysyklistMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public BysyklistMapView(Context context, String apiKey) {
		super(context, apiKey);
	}
	
//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		if (event.getAction() == MotionEvent.ACTION_DOWN) {
//			new Thread(new Runnable(){
//				public void run() {
////					Looper.prepare();
//					try {
//						isPressed = true;
//						Thread.sleep(1000);
//						if (isPressed) {
//							Log.v("Test", "longpress!");
//							
//						}
//					} catch (InterruptedException e) {
//						// Don't do anything, let the finally clause handle it.
//					} finally {
//						isPressed = false;
//					}
//				}
//
//				}).start();
//		} else {
//			isPressed = false; 
//		}
//		
//		return super.onTouchEvent(event);
//	}
	
}
