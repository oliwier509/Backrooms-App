package com.example.backroomsescapeelgrante;
import android.content.Context;
import android.graphics.*;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private Thread thread;
    private boolean running;
    private GameWorld gameWorld;
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        gameWorld = new GameWorld(context);
    }
    public void setJoystick(JoystickView joystick) {
        gameWorld.setJoystick(joystick);
    }
    public void setWalkingPlayer(MediaPlayer walkingPlayer) {
        gameWorld.setWalkingPlayer(walkingPlayer);
    }
    public void setGameOverCallback(GameWorld.GameOverListener listener) {
        gameWorld.setGameOverListener(listener);
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gameWorld.init(getResources());
        running = true;
        thread = new Thread(this);
        thread.start();
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException ignored) {}
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        while (running) {
            if (!getHolder().getSurface().isValid()) continue;
            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1_000_000_000f;
            lastTime = now;
            gameWorld.update(deltaTime, getWidth(), getHeight());
            Canvas canvas = null;
            try {
                canvas = getHolder().lockCanvas();
                if (canvas != null) {
                    synchronized (getHolder()) {
                        gameWorld.draw(canvas, getWidth(), getHeight());
                    }
                }
            } finally {
                if (canvas != null) {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}