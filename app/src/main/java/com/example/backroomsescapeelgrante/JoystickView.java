package com.example.backroomsescapeelgrante;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
public class JoystickView extends View {
    private float baseRadius = 100;
    private float hatRadius = 40;
    private float centerX, centerY;
    private float touchX, touchY;
    private boolean isTouching = false;
    private Paint basePaint, hatPaint;
    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    private void init() {
        basePaint = new Paint();
        basePaint.setColor(Color.GRAY);
        basePaint.setStyle(Paint.Style.FILL);
        hatPaint = new Paint();
        hatPaint.setColor(Color.DKGRAY);
        hatPaint.setStyle(Paint.Style.FILL);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        if (!isTouching) {
            centerX = getWidth() / 2f;
            centerY = getHeight() / 2f;
            touchX = centerX;
            touchY = centerY;
        }
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
        canvas.drawCircle(touchX, touchY, hatRadius, hatPaint);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float dx = event.getX() - centerX;
        float dy = event.getY() - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                isTouching = true;
                if (distance < baseRadius) {
                    touchX = event.getX();
                    touchY = event.getY();
                } else {
                    float ratio = baseRadius / distance;
                    touchX = centerX + dx * ratio;
                    touchY = centerY + dy * ratio;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                isTouching = false;
                touchX = centerX;
                touchY = centerY;
                invalidate();
                break;
        }
        return true;
    }
    public float getXPercent() {
        return (touchX - centerX) / baseRadius;
    }
    public float getYPercent() {
        return (touchY - centerY) / baseRadius;
    }
}