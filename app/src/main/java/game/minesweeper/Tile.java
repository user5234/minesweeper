package game.minesweeper;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RotateDrawable;
import android.os.SystemClock;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import androidx.core.content.res.ResourcesCompat;
import java.util.Random;

public class Tile {

    private int minesAround;
    private final int column, row, squareSize;
    private boolean isMine, isRevealed, isMarked, isHighlighted, isMarkedCorrectly;
    private final AnimationDrawable markTile;
    private final RotateDrawable unMarkTile, revealTile;
    private final GameView gameView;
    private final BitmapDrawable squareBitmap;
    private final Drawable mine, markedIncorrectly;

    private final static Random random = new Random();
    private final static LinearInterpolator linearInterpolator = new LinearInterpolator();
    private final static PathInterpolator pathInterpolator = new PathInterpolator(.22F, -0.41F, .79F, .19F);

    public Tile(GameView gameView, int column, int row) {
        this.gameView = gameView;
        this.squareSize = gameView.getUnitSize();
        this.column = column;
        this.row = row;
        minesAround = -1;
        isMine = false;
        isRevealed = false;
        isMarked = false;
        isMarkedCorrectly = true;
        Bitmap bitmap = Bitmap.createBitmap(100 ,100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(column % 2 != row % 2 ? Color.rgb(162, 209, 73) : Color.rgb(170, 215, 81));
        squareBitmap = new BitmapDrawable(gameView.getResources(), bitmap);
        revealTile = (RotateDrawable) ResourcesCompat.getDrawable(gameView.getResources(), R.drawable.reveal_tile, null);
        unMarkTile = (RotateDrawable) ResourcesCompat.getDrawable(gameView.getResources(), R.drawable.flag_remove, null);
        markTile = (AnimationDrawable) ResourcesCompat.getDrawable(gameView.getResources(), R.drawable.flag_plant, null);
        mine = ResourcesCompat.getDrawable(gameView.getResources(), R.drawable.mine_image, null);
        markedIncorrectly = ResourcesCompat.getDrawable(gameView.getResources(), R.drawable.incorrect_flag, null);
    }

    public void draw(Canvas canvas, Paint paint, Paint.FontMetrics fm) {
        if (isMine) {
            mine.setBounds((column + 1) * squareSize, row * squareSize, (column + 2) * squareSize, (row + 1) * squareSize);
            mine.draw(canvas);
        }
        else  {
            if (column % 2 != row % 2) paint.setColor(Color.rgb(215, 184, 153));
            else paint.setColor(Color.rgb(229, 194, 159));
            canvas.drawRect((column + 1) * squareSize, row * squareSize, (column + 2) * squareSize, (row + 1) * squareSize, paint);
            switch (minesAround) {
                case 1:
                    paint.setColor(Color.rgb(25, 118, 210));
                    break;
                case 2:
                    paint.setColor(Color.rgb(56, 142, 60));
                    break;
                case 3:
                    paint.setColor(Color.rgb(211, 47, 47));
                    break;
                case 4:
                    paint.setColor(Color.rgb(123, 31, 162));
                    break;
                case 5:
                    paint.setColor(Color.rgb(255, 143, 1));
                    break;
                case 7:
                    paint.setColor(Color.rgb(68, 66, 68));
                    break;
            }
            canvas.drawText(getMinesAround() + "", (column + 1) * squareSize + squareSize / 2F - paint.measureText(getMinesAround() + "") / 2F, (row + 1) * squareSize - squareSize / 2F - fm.ascent / 2F, paint);
        }
    }

    public void drawDrawable(Canvas canvas) {
        if (isMarked)
            markTile.draw(canvas);
        else if (unMarkTile.getBounds().right != unMarkTile.getBounds().left) {
            unMarkTile.draw(canvas);
        }
        if (!isMarkedCorrectly && gameView.getGameOver()) {
            markedIncorrectly.draw(canvas);
        }
        if (isRevealed && revealTile.getBounds().right != revealTile.getBounds().left) {
            revealTile.draw(canvas);
        }
    }

    private void animateSetMarked(boolean marked) {
        if (marked) {
            markTile.setBounds((column + 1) * squareSize, row * squareSize, (column + 2) * squareSize, (row + 1) * squareSize);
            markTile.setVisible(true, true);
            markTile.setCallback(callback);
            markTile.start();
        }
        else if (isMarked) {
            unMarkTile.setVisible(true, true);
            markTile.setVisible(false, true);
            ValueAnimator linearAnim = ValueAnimator.ofPropertyValuesHolder(
                    PropertyValuesHolder.ofInt("x", (column + 1) * squareSize, (column + random.nextInt(8) - 3) * squareSize),
                    PropertyValuesHolder.ofInt("scale", squareSize, 0),
                    PropertyValuesHolder.ofInt("level", 0, 10000));
            linearAnim.setRepeatCount(0);
            linearAnim.setDuration(1000);
            linearAnim.setInterpolator(linearInterpolator);
            ValueAnimator accelerateAnim = ValueAnimator.ofInt(row * squareSize, (row + 20) * squareSize);
            accelerateAnim.setRepeatCount(0);
            accelerateAnim.setDuration(1000);
            accelerateAnim.setInterpolator(pathInterpolator);
            accelerateAnim.addUpdateListener((v) -> {
                unMarkTile.setLevel((Integer) linearAnim.getAnimatedValue("level"));
                unMarkTile.setBounds(
                        (Integer) linearAnim.getAnimatedValue("x"),
                        (Integer) accelerateAnim.getAnimatedValue(),
                        (Integer) linearAnim.getAnimatedValue("x") + (Integer) linearAnim.getAnimatedValue("scale"),
                        (Integer) accelerateAnim.getAnimatedValue() + (Integer) linearAnim.getAnimatedValue("scale"));
                gameView.postInvalidate(
                        unMarkTile.getBounds().left,
                        unMarkTile.getBounds().top,
                        unMarkTile.getBounds().right,
                        unMarkTile.getBounds().bottom);
            });
            linearAnim.start();
            accelerateAnim.start();
        }
    }

    public RotateDrawable getRevealDrawable() {
        if (revealTile.getDrawable() != squareBitmap) {
            revealTile.setDrawable(squareBitmap);
            revealTile.setToDegrees(random.nextInt(2) == 1 ? 180 : - 180);
            revealTile.setBounds((column + 1) * squareSize, row * squareSize, (column + 2) * squareSize, (row + 1) * squareSize);
        }
        return revealTile;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public int getMinesAround() {
        return minesAround;
    }

    public void setMinesAround(int minesAround) {
        this.minesAround = minesAround;
    }

    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public boolean isMine() {
        return isMine;
    }

    public void setMine(boolean mine) {
        isMine = mine;
    }

    public boolean isRevealed() {
        return isRevealed;
    }

    public void setRevealed(boolean revealed) {
        isRevealed = revealed;
    }

    public boolean isMarked() {
        return isMarked;
    }

    public void setMarked(boolean marked) {
        animateSetMarked(marked);
        isMarked = marked;
    }

    public void setMarkedCorrectly(boolean correctly) {
        if (!correctly) {
            isMarkedCorrectly = false;
            markedIncorrectly.setBounds((column + 1) * squareSize, row * squareSize, (column + 2) * squareSize, (row + 1) * squareSize);
        }
    }

    private final Drawable.Callback callback = new Drawable.Callback() {
        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            gameView.removeCallbacks(what);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            gameView.postDelayed(what, when - SystemClock.uptimeMillis());
        }

        @Override
        public void invalidateDrawable(Drawable who) {
            gameView.postInvalidate(
                    who.getBounds().left,
                    who.getBounds().top,
                    who.getBounds().right,
                    who.getBounds().bottom
            );
        }
    };
}
