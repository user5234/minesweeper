package game.minesweeper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class BackgroundCreator {
    public static Bitmap create(int width, int height, int squareSize) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        for (int i = 0; i < height; i += squareSize) {
            for (int g = 0; g < width; g += 2 * squareSize) {
                if ((i / squareSize) % 2 == 0) {
                    paint.setColor(Color.rgb(162, 209, 73));
                    canvas.drawRect(g, i, g + squareSize, i + squareSize, paint);
                    paint.setColor(Color.rgb(170, 215, 81));
                    canvas.drawRect(g + squareSize, i, g + 2 * squareSize, i + squareSize, paint);
                } else {
                    paint.setColor(Color.rgb(162, 209, 73));
                    canvas.drawRect(g + squareSize, i, g + 2 * squareSize, i + squareSize, paint);
                    paint.setColor(Color.rgb(170, 215, 81));
                    canvas.drawRect(g, i, g + squareSize, i + squareSize, paint);
                }
            }
        }
        paint.setColor(Color.rgb(74, 117, 44));
        canvas.drawRect(0, 0, squareSize, height, paint);
        canvas.drawRect(0, height - squareSize - height % squareSize, width, height, paint);
        canvas.drawRect(width - squareSize, 0, width, height, paint);
        paint.setColor(Color.rgb(142, 204, 57));
        canvas.drawRect(squareSize * 3 / 4F, 0, squareSize, height - squareSize - height % squareSize, paint);
        canvas.drawRect(width - squareSize - width % squareSize, 0, width - 3 * squareSize / 4F - width % squareSize, height - squareSize - height % squareSize, paint);
        canvas.drawRect(squareSize * 3 / 4F, height - squareSize - height % squareSize, width - squareSize * 3 / 4F - width % squareSize, height - height % squareSize - squareSize * 3 / 4F , paint);
        return bitmap;
    }
}
