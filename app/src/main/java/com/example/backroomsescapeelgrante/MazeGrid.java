package com.example.backroomsescapeelgrante;
import android.graphics.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;
public class MazeGrid {
    private final int cols, rows;
    private final boolean[][] walls;
    private final Random rand = new Random();
    public MazeGrid(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.walls = new boolean[cols][rows];
        for (int x = 0; x < cols; x++)
            for (int y = 0; y < rows; y++)
                walls[x][y] = true;
    }
    public void generate() {
        boolean[][] visited = new boolean[cols][rows];
        Stack<Point> stack = new Stack<>();
        int startX = cols / 2;
        int startY = rows / 2;
        carve(startX, startY);
        visited[startX][startY] = true;
        stack.push(new Point(startX, startY));
        while (!stack.isEmpty()) {
            Point current = stack.peek();
            List<Point> neighbors = new ArrayList<>();
            for (int[] dir : new int[][]{{0, -2}, {2, 0}, {0, 2}, {-2, 0}}) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];
                if (inBounds(nx, ny) && !visited[nx][ny]) {
                    neighbors.add(new Point(nx, ny));
                }
            }
            if (!neighbors.isEmpty()) {
                Point next = neighbors.get(rand.nextInt(neighbors.size()));
                int wallX = (current.x + next.x) / 2;
                int wallY = (current.y + next.y) / 2;
                carve(next.x, next.y);
                carve(wallX, wallY);
                visited[next.x][next.y] = true;
                stack.push(next);
            } else {
                stack.pop();
            }
        }
    }
    public void carve(int x, int y) {
        if (inBounds(x, y)) {
            walls[x][y] = false;
        }
    }
    public void clearWall(int x, int y) {
        if (inBounds(x, y)) walls[x][y] = false;
    }
    public boolean isWall(int x, int y) {
        return inBounds(x, y) && walls[x][y];
    }
    private boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < cols && y < rows;
    }
    public boolean hasPathToEdge(int startX, int startY) {
        boolean[][] visited = new boolean[cols][rows];
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            if (p.x == 0 || p.y == 0 || p.x == cols - 1 || p.y == rows - 1) {
                return true;
            }
            for (int[] dir : new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
                int nx = p.x + dir[0];
                int ny = p.y + dir[1];
                if (inBounds(nx, ny) && !walls[nx][ny] && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.add(new Point(nx, ny));
                }
            }
        }
        return false;
    }
    public void forcePathToEdge(int startX, int startY) {
        Queue<Point> queue = new LinkedList<>();
        boolean[][] visited = new boolean[cols][rows];
        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            if (p.x == 0 || p.y == 0 || p.x == cols - 1 || p.y == rows - 1) {
                return;
            }
            for (int[] dir : new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
                int nx = p.x + dir[0];
                int ny = p.y + dir[1];
                if (inBounds(nx, ny) && !visited[nx][ny]) {
                    if (walls[nx][ny]) {
                        walls[nx][ny] = false;
                    }
                    visited[nx][ny] = true;
                    queue.add(new Point(nx, ny));
                }
            }
        }
    }
}