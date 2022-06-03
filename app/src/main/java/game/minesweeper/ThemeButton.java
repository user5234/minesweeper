package game.minesweeper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class ThemeButton extends View {

    private String text = "";
    private Paint paint;
    private float height, width, top, textHeight, smallHeight;
    private Paint.FontMetrics metrics;
    private Runnable action;

    public ThemeButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ThemeButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ThemeButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ThemeButton.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                height = getHeight();
                smallHeight = 8 * height / 10F - 1;
                width = getWidth();
                top = 0;
                paint.setTypeface(ResourcesCompat.getFont(context, R.font.arcade_font));
                paint.setTextSize(0.4666667F * height);
                metrics = paint.getFontMetrics();
                textHeight = metrics.ascent + metrics.descent + metrics.leading;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.rgb(43, 24, 0));
        canvas.drawRoundRect(0, height / 15F, width - 1, height - 1, height / 15F, height / 15F, paint);
        paint.setColor(Color.rgb(145, 81, 0));
        canvas.drawRoundRect(width / 75F, height / 2F, 74 * width / 75F, 19 * height / 20F - 1, height / 15F, height / 15F, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(width / 75F, top, 74 * width / 75F, smallHeight + top, height / 15F, height / 15F, paint);
        paint.setColor(Color.rgb(255, 161, 43));
        canvas.drawRoundRect(width / 75F, top + height / 50F, 74 * width / 75F, smallHeight + top, height / 15F, height / 15F, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText(text, (width - paint.measureText(text)) / 2F, top + (smallHeight - textHeight) / 2F + height / 90F, paint);
        paint.setColor(Color.BLACK);
        canvas.drawText(text, (width - paint.measureText(text)) / 2F, top + (smallHeight - textHeight) / 2F, paint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                top = height / 8F;
                postInvalidate();
                break;
            case MotionEvent.ACTION_UP:
                top = 0;
                postInvalidate();
                if (action != null) action.run();
                break;
        }
        return true;
    }

    public String getText() { return text; }

    public void setText(String text) { this.text = text; }

    public void setAction(Runnable action) {
        this.action = action;
    }
}
