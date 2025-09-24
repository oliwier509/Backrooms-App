package com.example.backroomsescapeelgrante;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.google.androidgamesdk.GameActivity;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class MainActivity extends GameActivity {
    private MediaPlayer mediaPlayer;
    private MediaPlayer walkingPlayer;
    private MediaPlayer death;
    private RelativeLayout gameOverScreen;
    private TextView survivalTimeText;
    private TextView leaderboardText;
    private Button resetButton;
    static {
        System.loadLibrary("backroomsescapeelgrante");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        death = MediaPlayer.create(this, R.raw.death);
        walkingPlayer = MediaPlayer.create(this, R.raw.walk);
        walkingPlayer.setLooping(true);
        walkingPlayer.setVolume(0.5f, 0.5f);
        mediaPlayer = MediaPlayer.create(this, R.raw.ambience);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.seekTo(0);
            mp.start();
        });
        GameView gameView = findViewById(R.id.gameView);
        JoystickView joystick = findViewById(R.id.joystick);
        gameView.setJoystick(joystick);
        gameView.setWalkingPlayer(walkingPlayer);
        gameView = findViewById(R.id.gameView);
        gameOverScreen = findViewById(R.id.gameOverScreen);
        survivalTimeText = findViewById(R.id.survivalTimeText);
        leaderboardText = findViewById(R.id.leaderboardText);
        resetButton = findViewById(R.id.resetButton);
        gameView.setJoystick(joystick);
        gameView.setWalkingPlayer(walkingPlayer);
        gameView.setGameOverCallback(this::showGameOverScreen);
        resetButton.setOnClickListener(v -> restartGame());
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }
    private void hideSystemUi() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }
    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (walkingPlayer != null) {
            walkingPlayer.stop();
            walkingPlayer.release();
            walkingPlayer = null;
        }
        super.onDestroy();
    }
    private void showGameOverScreen(long survivalTime, float distanceTravelled) {
        runOnUiThread(() -> {
            stopAllSounds();
            death.start();
            mediaPlayer = MediaPlayer.create(this, R.raw.ambience);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.seekTo(0);
                mp.start();
            });
            survivalTimeText.setText("Time: " + (survivalTime / 1000f) + "s\nDistance: " + (int) distanceTravelled + " units");
            SharedPreferences prefs = getSharedPreferences("game_scores", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            int newScoreCount = prefs.getInt("score_count", 0) + 1;
            editor.putFloat("score_" + newScoreCount + "_distance", distanceTravelled);
            editor.putLong("score_" + newScoreCount + "_time", survivalTime);
            editor.putInt("score_count", newScoreCount);
            editor.apply();
            List<String> scoreList = new ArrayList<>();
            for (int i = 1; i <= newScoreCount; i++) {
                float d = prefs.getFloat("score_" + i + "_distance", 0);
                long t = prefs.getLong("score_" + i + "_time", 0);
                scoreList.add(String.format("Distance: %d  Time: %.1fs", (int) d, t / 1000f));
            }
            Collections.sort(scoreList, (a, b) -> {
                int distA = Integer.parseInt(a.split(" ")[1]);
                int distB = Integer.parseInt(b.split(" ")[1]);
                return Integer.compare(distB, distA);
            });
            StringBuilder sb = new StringBuilder("Leaderboard:\n");
            for (int i = 0; i < Math.min(3, scoreList.size()); i++) {
                sb.append(i + 1).append(". ").append(scoreList.get(i)).append("\n");
            }
            leaderboardText.setText(sb.toString());
            JoystickView joystick = findViewById(R.id.joystick);
            joystick.setVisibility(View.GONE);
            gameOverScreen.setVisibility(View.VISIBLE);
        });
    }
    public void stopAllSounds() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (walkingPlayer != null) {
            walkingPlayer.stop();
            walkingPlayer.release();
            walkingPlayer = null;
        }
    }
    public void restartGame() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (walkingPlayer != null) {
            walkingPlayer.stop();
            walkingPlayer.release();
            walkingPlayer = null;
        }
        recreate();
    }
}