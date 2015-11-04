package at.bitfire.gfxtablet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import at.bitfire.gfxtablet.NetEvent.Type;

@SuppressLint("ViewConstructor")
public class CanvasView extends View implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "GfxTablet.CanvasView";

    final SharedPreferences settings;
    NetworkClient netClient;
	boolean acceptStylusOnly;
	int maxX, maxY;


    // setup

    public CanvasView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        // view is disabled until a network client is set
        setEnabled(false);

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.registerOnSharedPreferenceChangeListener(this);
        setBackground();
        setInputMethods();
    }

    public void setNetworkClient(NetworkClient networkClient) {
        netClient = networkClient;
        setEnabled(true);
    }


    // settings

    protected void setBackground() {
        if (settings.getBoolean(SettingsActivity.KEY_DARK_CANVAS, false))
            setBackgroundColor(Color.BLACK);
        else
            setBackgroundResource(R.drawable.bg_grid_pattern);
    }

    protected void setInputMethods() {
        acceptStylusOnly = settings.getBoolean(SettingsActivity.KEY_PREF_STYLUS_ONLY, false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case SettingsActivity.KEY_PREF_STYLUS_ONLY:
                setInputMethods();
                break;
            case SettingsActivity.KEY_DARK_CANVAS:
                setBackground();
                break;
        }
    }


    // drawing

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.i(TAG, "Canvas size changed: " + w + "x" + h + " (before: " + oldw + "x" + oldh + ")");
		maxX = w;
		maxY = h;
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (isEnabled()) {
			int ptr = event.getPointerCount();

			if (ptr != 0){
					Log.v(TAG, String.format("Generic motion event logged: %f|%f, pressure: %f, ptr: %d", event.getX(0), event.getY(0), event.getPressure(0), ptr));
					if (event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE)
						netClient.getQueue().add(new NetEvent(Type.TYPE_MOTION,
							normalizeX(event.getX(0)),
							normalizeY(event.getY(0)),
							normalizePressure(event.getPressure(0)),
							ptr
						));
				}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		if (isEnabled()) {
			int ptr = event.getPointerCount();

			if (ptr != 0) {
				short nx = normalizeX(event.getX(0)),
					  ny = normalizeY(event.getY(0)),
					  npressure = normalizePressure(event.getPressure(0));
				Log.v(TAG, String.format("Touch event logged: action %d @ %f|%f (pressure: %f, ptr: %d)", event.getActionMasked(), event.getX(0), event.getY(0), event.getPressure(0), ptr));
				switch (event.getActionMasked()) {
					case MotionEvent.ACTION_MOVE:
						netClient.getQueue().add(new NetEvent(Type.TYPE_MOTION, nx, ny, npressure, ptr));
						break;
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_POINTER_DOWN:
						netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, npressure, ptr, ptr, true));
						break;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_POINTER_UP:
					case MotionEvent.ACTION_CANCEL:
						netClient.getQueue().add(new NetEvent(Type.TYPE_BUTTON, nx, ny, npressure, ptr-1, ptr-1, false));
						break;
					}
				}
			return true;
		}
		return false;
	}
	
	
	short normalizeX(float x) {
		return (short)(Math.min(Math.max(0, x), maxX) * Short.MAX_VALUE/maxX);
	}
	
	short normalizeY(float x) {
		return (short)(Math.min(Math.max(0, x), maxY) * Short.MAX_VALUE/maxY);
	}
	
	short normalizePressure(float x) {
		return (short)(Math.min(Math.max(0, x), 2.0) * Short.MAX_VALUE/2.0);
	}

}
