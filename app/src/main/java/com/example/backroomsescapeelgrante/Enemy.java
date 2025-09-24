package com.example.backroomsescapeelgrante;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.MediaPlayer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
public class Enemy {
    public enum Behavior { WANDER, IDLE, CHASE }
    float x;
    float y;
    private final Bitmap bitmap;
    private Behavior behavior;
    private final Random rand = new Random();
    private float wanderDirX, wanderDirY;
    private float wanderTimer = 0;
    private long lostSightTime = -1;
    private Behavior originalBehavior;
    private MediaPlayer[] growlSounds;
    private PointF lastSeenBreadcrumb = null;

    public Enemy(float x, float y, Bitmap bitmap, Behavior behavior, MediaPlayer[] growlSounds) {
        this.x = x;
        this.y = y;
        this.bitmap = bitmap;
        this.behavior = behavior;
        this.originalBehavior = behavior;
        this.growlSounds = growlSounds;
        if (behavior == Behavior.WANDER) {
            setRandomDirection();
        }
    }
    private void playMixedGrowl() {
        int randomGrowl = rand.nextInt(4);
        growlSounds[randomGrowl].start();
    }
    private boolean hasLineOfSightToPlayer(float px, float py, List<RectF> walls) {
        for (RectF wall : walls) {
            if (lineIntersectsRect(this.x, this.y, px, py, wall)) {
                return false;
            }
        }
        return true;
    }
    private boolean lineIntersectsRect(float x1, float y1, float x2, float y2, RectF rect) {
        return intersectsLine(x1, y1, x2, y2, rect.left, rect.top, rect.right, rect.top) ||
                intersectsLine(x1, y1, x2, y2, rect.right, rect.top, rect.right, rect.bottom) ||
                intersectsLine(x1, y1, x2, y2, rect.right, rect.bottom, rect.left, rect.bottom) ||
                intersectsLine(x1, y1, x2, y2, rect.left, rect.bottom, rect.left, rect.top);
    }
    private boolean intersectsLine(float x1, float y1, float x2, float y2,
                                   float x3, float y3, float x4, float y4) {
        float denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0) return false;
        float ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
        float ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
        return ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1;
    }
    public void update(float dt, float playerX, float playerY, List<RectF> walls, List<PointF> breadcrumbs) {
        boolean hasLineOfSight = hasLineOfSightToPlayer(playerX, playerY, walls);
        float speed;
        float dx = 0, dy = 0;
        if (hasLineOfSight) {
            behavior = Behavior.CHASE;
            lostSightTime = -1;
            if (!breadcrumbs.isEmpty()) {
                float closestDistSq = Float.MAX_VALUE;
                PointF closestBreadcrumb = null;
                for (PointF b : breadcrumbs) {
                    float distSq = (playerX - b.x) * (playerX - b.x) + (playerY - b.y) * (playerY - b.y);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestBreadcrumb = b;
                    }
                }
                lastSeenBreadcrumb = closestBreadcrumb;
            }
        } else if (behavior == Behavior.CHASE && lostSightTime == -1) {
            lostSightTime = System.currentTimeMillis();
        }

        if (behavior == Behavior.CHASE) {
            playMixedGrowl();
            speed = 250;
            if (hasLineOfSight) {
                float distX = playerX - x;
                float distY = playerY - y;
                float len = (float) Math.sqrt(distX * distX + distY * distY);
                if (len > 1) {
                    dx = (distX / len) * speed * dt;
                    dy = (distY / len) * speed * dt;
                }
            } else if (lastSeenBreadcrumb != null) {
                float distX = lastSeenBreadcrumb.x - x;
                float distY = lastSeenBreadcrumb.y - y;
                float len = (float) Math.sqrt(distX * distX + distY * distY);
                if (len > 10f) {
                    dx = (distX / len) * speed * dt;
                    dy = (distY / len) * speed * dt;
                } else {
                    lastSeenBreadcrumb = null;
                }
            } else if (!breadcrumbs.isEmpty()) {
                int closestIndex = 0;
                float closestDistSq = Float.MAX_VALUE;
                for (int i = 0; i < breadcrumbs.size(); i++) {
                    PointF node = breadcrumbs.get(i);
                    float distX = node.x - x;
                    float distY = node.y - y;
                    float distSq = distX * distX + distY * distY;
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestIndex = i;
                    }
                }

                float threshold = 150f * 150f;
                List<Integer> nearbyIndices = new ArrayList<>();
                for (int i = 0; i < breadcrumbs.size(); i++) {
                    PointF node = breadcrumbs.get(i);
                    float distSq = (node.x - x) * (node.x - x) + (node.y - y) * (node.y - y);
                    if (distSq < threshold) {
                        nearbyIndices.add(i);
                    }
                }

                int targetIndex = closestIndex;
                if (nearbyIndices.size() > 1) {
                    targetIndex = nearbyIndices.stream()
                            .max(Comparator.comparingInt(i -> i))
                            .orElse(closestIndex);
                }

                for (int i = targetIndex; i < breadcrumbs.size(); i++) {
                    PointF target = breadcrumbs.get(i);
                    float distX = target.x - x;
                    float distY = target.y - y;
                    float len = (float) Math.sqrt(distX * distX + distY * distY);
                    if (len > 10f) {
                        dx = (distX / len) * speed * dt;
                        dy = (distY / len) * speed * dt;
                        break;
                    }
                }
            }

            if (lostSightTime != -1 && System.currentTimeMillis() - lostSightTime > 15000) {
                behavior = originalBehavior;
                lostSightTime = -1;
                lastSeenBreadcrumb = null;
            }
        } else if (behavior == Behavior.WANDER) {
            speed = 400;
            dx = wanderDirX * speed * dt;
            dy = wanderDirY * speed * dt;
            wanderTimer -= dt;
            if (wanderTimer <= 0) {
                setRandomDirection();
            }
        }

        move(dx, dy, walls);
    }
    private void move(float dx, float dy, List<RectF> walls) {
        x += dx;
        RectF boundsX = getBounds(0, 0);
        for (RectF wall : walls) {
            if (RectF.intersects(boundsX, wall)) {
                x -= dx;
                break;
            }
        }
        y += dy;
        RectF boundsY = getBounds(0, 0);
        for (RectF wall : walls) {
            if (RectF.intersects(boundsY, wall)) {
                y -= dy;
                break;
            }
        }
    }
    private void setRandomDirection() {
        double angle = rand.nextDouble() * 2 * Math.PI;
        wanderDirX = (float) Math.cos(angle);
        wanderDirY = (float) Math.sin(angle);
        wanderTimer = 1 + rand.nextFloat();
    }
    public void draw(Canvas canvas, float offsetX, float offsetY) {
        float drawX = x + offsetX - bitmap.getWidth() / 2f;
        float drawY = y + offsetY - bitmap.getHeight() / 2f;
        canvas.drawBitmap(bitmap, drawX, drawY, null);
    }
    public RectF getBounds(float offsetX, float offsetY) {
        if (behavior == Behavior.CHASE && lastSeenBreadcrumb != null) {
            float distX = lastSeenBreadcrumb.x - x;
            float distY = lastSeenBreadcrumb.y - y;
            float distSq = distX * distX + distY * distY;
            float threshold = 10f * 10f;
            if (distSq > threshold) {
                return new RectF(x + offsetX, y + offsetY, x + offsetX, y + offsetY);
            }
        }
        float shrinkFactor = 0.8f;
        float width = bitmap.getWidth() * shrinkFactor;
        float height = bitmap.getHeight() * shrinkFactor;
        return new RectF(
                x + offsetX - width / 2f,
                y + offsetY - height / 2f,
                x + offsetX + width / 2f,
                y + offsetY + height / 2f
        );
    }
}
