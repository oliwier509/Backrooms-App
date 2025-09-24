package com.example.backroomsescapeelgrante;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.media.MediaPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class GameWorld {
    private float worldOffsetX = 0;
    private float worldOffsetY = 0;
    private Bitmap carpetBitmap;
    private Paint carpetPaint;
    private Bitmap playerBitmap;
    private Bitmap wallBitmap;
    private MediaPlayer walkingPlayer;
    private JoystickView joystick;
    private static final int TILE_SIZE = 150;
    private static final int CHUNK_SIZE = 15;
    private final HashMap<Point, MazeGrid> chunkMap = new HashMap<>();
    private Point currentChunk = new Point(0, 0);
    private final List<RectF> walls = new ArrayList<>();
    private static final int ENEMY_SPAWN_DELAY = 60;
    private long startTime;
    private final List<Enemy> enemies = new ArrayList<>();
    private final Random enemyRandom = new Random();
    private Bitmap[] enemyBitmaps;
    private long lastBreadcrumbTime = 0;
    private boolean isGameOver = false;
    private long survivalTime = 0;
    private GameOverListener gameOverListener;
    private MediaPlayer[] growlSounds;
    private Context context;
    public interface GameOverListener {
        void onGameOver(long survivalTimeMillis, float distanceTravelled);
    }
    private float getDistanceFromSpawn() {
        float playerX = -worldOffsetX;
        float playerY = -worldOffsetY;
        return (float) Math.sqrt(playerX * playerX + playerY * playerY);
    }
    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }
    private static class Breadcrumb {
        PointF position;
        long timestamp;
        Breadcrumb(float x, float y, long timestamp) {
            this.position = new PointF(x, y);
            this.timestamp = timestamp;
        }
    }
    private final List<Breadcrumb> playerPathNodes = new ArrayList<>();
    public GameWorld(Context context) {
        this.context = context;
    }
    public void setJoystick(JoystickView joystick) {
        this.joystick = joystick;
    }
    public void setWalkingPlayer(MediaPlayer walkingPlayer) {
        this.walkingPlayer = walkingPlayer;
    }
    public void init(Resources resources) {
        playerBitmap = BitmapFactory.decodeResource(resources, R.drawable.player);
        carpetBitmap = BitmapFactory.decodeResource(resources, R.drawable.carpet);
        enemyBitmaps = new Bitmap[] {
                BitmapFactory.decodeResource(resources, R.drawable.enemy1),
                BitmapFactory.decodeResource(resources, R.drawable.enemy2),
                BitmapFactory.decodeResource(resources, R.drawable.enemy3),
                BitmapFactory.decodeResource(resources, R.drawable.enemy4),
                BitmapFactory.decodeResource(resources, R.drawable.enemy5)
        };
        startTime = System.currentTimeMillis();
        Bitmap wallRaw = BitmapFactory.decodeResource(resources, R.drawable.wall);
        wallBitmap = Bitmap.createScaledBitmap(wallRaw, 150, 150, true);
        Shader shader = new BitmapShader(carpetBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        carpetPaint = new Paint();
        carpetPaint.setShader(shader);
        generateChunksAround(currentChunk);

        growlSounds = new MediaPlayer[] {
                MediaPlayer.create(context, R.raw.growl1),
                MediaPlayer.create(context, R.raw.growl2),
                MediaPlayer.create(context, R.raw.growl3),
                MediaPlayer.create(context, R.raw.growl4)
        };
    }

    public void update(float deltaTime, int screenWidth, int screenHeight) {
        if (!isGameOver && joystick != null) {
            int playerTileX = (int) ((-worldOffsetX + screenWidth / 2f) / TILE_SIZE);
            int playerTileY = (int) ((-worldOffsetY + screenHeight / 2f) / TILE_SIZE);
            Point newChunk = new Point(playerTileX / CHUNK_SIZE, playerTileY / CHUNK_SIZE);
            if (!newChunk.equals(currentChunk)) {
                currentChunk = newChunk;
                generateChunksAround(currentChunk);
            }
            float speed = 300;
            float xInput = joystick.getXPercent();
            float yInput = joystick.getYPercent();
            boolean isMoving = Math.abs(xInput) > 0.1f || Math.abs(yInput) > 0.1f;
            float proposedOffsetX = worldOffsetX - xInput * speed * deltaTime;
            float proposedOffsetY = worldOffsetY - yInput * speed * deltaTime;
            if (isMoving) {
                if (!isColliding(proposedOffsetX, worldOffsetY, screenWidth, screenHeight)) {
                    worldOffsetX = proposedOffsetX;
                }
                if (!isColliding(worldOffsetX, proposedOffsetY, screenWidth, screenHeight)) {
                    worldOffsetY = proposedOffsetY;
                }
                if (walkingPlayer != null && !walkingPlayer.isPlaying()) {
                    walkingPlayer.start();
                }
                long now = System.currentTimeMillis();
                if (now - lastBreadcrumbTime > 50) {
                    float px = screenWidth / 2f - worldOffsetX;
                    float py = screenHeight / 2f - worldOffsetY;
                    playerPathNodes.add(new Breadcrumb(px, py, now));
                    lastBreadcrumbTime = now;
                }
                playerPathNodes.removeIf(b -> now - b.timestamp > 15000);
            } else {
                if (walkingPlayer != null && walkingPlayer.isPlaying()) {
                    walkingPlayer.pause();
                    walkingPlayer.seekTo(0);
                }
            }
            long currentTime = System.currentTimeMillis();
            if ((currentTime - startTime) / 500 >= ENEMY_SPAWN_DELAY) {
                maybeSpawnEnemy(screenWidth, screenHeight);
            }
            updateEnemies(deltaTime, screenWidth, screenHeight);
            checkEnemyCollision(screenWidth, screenHeight);
        }
    }
    private void generateChunksAround(Point centerChunk) {
        int radius = 2;
        chunkMap.keySet().removeIf(chunk ->
                Math.abs(chunk.x - centerChunk.x) > radius || Math.abs(chunk.y - centerChunk.y) > radius
        );
        walls.clear();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                Point chunkPos = new Point(centerChunk.x + dx, centerChunk.y + dy);
                boolean isCenter = chunkPos.equals(new Point(0, 0));
                if (!chunkMap.containsKey(chunkPos)) {
                    MazeGrid maze = new MazeGrid(CHUNK_SIZE + 1, CHUNK_SIZE + 1);
                    maze.generate();
                    if (isCenter) {
                        int centerCol = CHUNK_SIZE / 2;
                        int centerRow = CHUNK_SIZE / 2;
                        for (int cx = centerCol - 2; cx <= centerCol + 2; cx++) {
                            for (int cy = centerRow - 6; cy <= centerRow - 2; cy++) {
                                maze.clearWall(cx, cy);
                            }
                        }
                    }
                    if (!maze.hasPathToEdge(CHUNK_SIZE / 2, CHUNK_SIZE / 2)) {
                        maze.forcePathToEdge(CHUNK_SIZE / 2, CHUNK_SIZE / 2);
                    }
                    chunkMap.put(chunkPos, maze);
                }
                MazeGrid maze = chunkMap.get(chunkPos);
                int baseX = chunkPos.x * CHUNK_SIZE * TILE_SIZE;
                int baseY = chunkPos.y * CHUNK_SIZE * TILE_SIZE;
                for (int y = 0; y < CHUNK_SIZE; y++) {
                    for (int x = 0; x < CHUNK_SIZE; x++) {
                        if (maze.isWall(x, y)) {
                            float left = baseX + x * TILE_SIZE;
                            float top = baseY + y * TILE_SIZE;
                            walls.add(new RectF(left, top, left + TILE_SIZE, top + TILE_SIZE));
                        }
                    }
                }
            }
        }
    }
    private void maybeSpawnEnemy(int screenWidth, int screenHeight) {
        if (enemyRandom.nextFloat() > 0.005f) return;
        float screenLeft = -worldOffsetX;
        float screenTop = -worldOffsetY;
        float screenRight = screenLeft + screenWidth;
        float screenBottom = screenTop + screenHeight;
        List<Point> validChunks = new ArrayList<>();
        for (Point chunk : chunkMap.keySet()) {
            float chunkLeft = chunk.x * CHUNK_SIZE * TILE_SIZE;
            float chunkTop = chunk.y * CHUNK_SIZE * TILE_SIZE;
            float chunkRight = chunkLeft + CHUNK_SIZE * TILE_SIZE;
            float chunkBottom = chunkTop + CHUNK_SIZE * TILE_SIZE;
            boolean isOutsideScreen = chunkRight < screenLeft || chunkLeft > screenRight ||
                    chunkBottom < screenTop || chunkTop > screenBottom;
            if (isOutsideScreen) {
                validChunks.add(chunk);
            }
        }
        if (validChunks.isEmpty()) return;
        Point spawnChunk = validChunks.get(enemyRandom.nextInt(validChunks.size()));
        MazeGrid grid = chunkMap.get(spawnChunk);
        for (int attempts = 0; attempts < 1000; attempts++) {
            int x = enemyRandom.nextInt(CHUNK_SIZE);
            int y = enemyRandom.nextInt(CHUNK_SIZE);
            if (!grid.isWall(x, y)) {
                float worldX = spawnChunk.x * CHUNK_SIZE * TILE_SIZE + x * TILE_SIZE + TILE_SIZE / 2f;
                float worldY = spawnChunk.y * CHUNK_SIZE * TILE_SIZE + y * TILE_SIZE + TILE_SIZE / 2f;
                if (worldX < screenLeft || worldX > screenRight || worldY < screenTop || worldY > screenBottom) {
                    Bitmap skin = enemyBitmaps[enemyRandom.nextInt(enemyBitmaps.length)];
                    Enemy.Behavior behavior = Enemy.Behavior.values()[enemyRandom.nextInt(2)];
                    enemies.add(new Enemy(worldX, worldY, skin, behavior, growlSounds));
                    break;
                }
            }
        }
    }

    private void updateEnemies(float deltaTime, int screenWidth, int screenHeight) {
        if(!isGameOver){
            float playerX = screenWidth / 2f - worldOffsetX;
            float playerY = screenHeight / 2f - worldOffsetY;
            List<Point> visibleChunks = new ArrayList<>();
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    Point chunkPos = new Point(currentChunk.x + dx, currentChunk.y + dy);
                    visibleChunks.add(chunkPos);
                }
            }
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);
                Point enemyChunk = new Point((int) (enemy.x / (CHUNK_SIZE * TILE_SIZE)), (int) (enemy.y / (CHUNK_SIZE * TILE_SIZE)));
                if (!visibleChunks.contains(enemyChunk)) {
                    enemies.remove(i);
                    continue;
                }
                RectF enemyRect = enemy.getBounds(0, 0);
                boolean insideWall = false;
                for (RectF wall : walls) {
                    if (RectF.intersects(enemyRect, wall)) {
                        insideWall = true;
                        break;
                    }
                }
                if (insideWall) {
                    enemies.remove(i);
                    continue;
                }
                List<PointF> breadcrumbPoints = new ArrayList<>();
                for (Breadcrumb b : playerPathNodes) {
                    breadcrumbPoints.add(b.position);
                }
                enemy.update(deltaTime, playerX, playerY, walls, breadcrumbPoints);
            }
        }
    }
    private void checkEnemyCollision(int screenWidth, int screenHeight) {
        float playerSize = playerBitmap.getWidth();
        float playerX = screenWidth / 2f;
        float playerY = screenHeight / 2f;
        RectF playerRect = new RectF(
                playerX - playerSize / 2f,
                playerY - playerSize / 2f,
                playerX + playerSize / 2f,
                playerY + playerSize / 2f
        );
        for (Enemy enemy : enemies) {
            RectF eRect = enemy.getBounds(worldOffsetX, worldOffsetY);
            if (RectF.intersects(playerRect, eRect)) {
                isGameOver = true;
                survivalTime = System.currentTimeMillis() - startTime;
                float distanceTravelled = getDistanceFromSpawn();
                if (gameOverListener != null) {
                    gameOverListener.onGameOver(survivalTime, distanceTravelled);
                }
                break;
            }
        }
    }
    private boolean isColliding(float proposedOffsetX, float proposedOffsetY, int screenWidth, int screenHeight) {
        if (playerBitmap == null) return false;
        float playerSize = playerBitmap.getWidth();
        float playerWorldX = screenWidth / 2f - proposedOffsetX;
        float playerWorldY = screenHeight / 2f - proposedOffsetY;
        RectF playerRect = new RectF(
                playerWorldX - playerSize / 2f,
                playerWorldY - playerSize / 2f,
                playerWorldX + playerSize / 2f,
                playerWorldY + playerSize / 2f
        );
        for (RectF wall : walls) {
            if (RectF.intersects(playerRect, wall)) {
                return true;
            }
        }
        return false;
    }
    public void draw(Canvas canvas, int screenWidth, int screenHeight) {
        if (carpetBitmap == null) return;
        int bmpW = carpetBitmap.getWidth();
        int bmpH = carpetBitmap.getHeight();
        float offsetX = worldOffsetX % bmpW;
        float offsetY = worldOffsetY % bmpH;
        if (offsetX > 0) offsetX -= bmpW;
        if (offsetY > 0) offsetY -= bmpH;
        canvas.save();
        canvas.translate(offsetX, offsetY);
        for (int y = 0; y <= screenHeight + bmpH; y += bmpH) {
            for (int x = 0; x <= screenWidth + bmpW; x += bmpW) {
                canvas.drawBitmap(carpetBitmap, x, y, null);
            }
        }
        canvas.restore();
        drawWalls(canvas);
        drawPlayer(canvas, screenWidth, screenHeight);
        drawEnemies(canvas);
    }
    private void drawEnemies(Canvas canvas) {
        for (Enemy enemy : enemies) {
            enemy.draw(canvas, worldOffsetX, worldOffsetY);
        }
    }
    private void drawWalls(Canvas canvas) {
        if (wallBitmap == null) return;
        canvas.save();
        canvas.translate(worldOffsetX, worldOffsetY);
        for (RectF wall : walls) {
            canvas.drawBitmap(wallBitmap, null, wall, null);
        }
        canvas.restore();
    }
    private void drawPlayer(Canvas canvas, int screenWidth, int screenHeight) {
        if (playerBitmap == null || joystick == null) return;
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        float angleDegrees = (float) Math.toDegrees(getJoystickAngleRadians());
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(angleDegrees);
        canvas.drawBitmap(playerBitmap, -playerBitmap.getWidth() / 2f, -playerBitmap.getHeight() / 2f, null);
        canvas.restore();
    }
    private float getJoystickAngleRadians() {
        float x = joystick.getXPercent();
        float y = joystick.getYPercent();
        return (float) Math.atan2(y, x);
    }
}