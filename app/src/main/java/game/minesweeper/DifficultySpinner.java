package game.minesweeper;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DifficultySpinner extends androidx.appcompat.widget.AppCompatSpinner {

    public DifficultySpinner(@NonNull Context context) {
        super(context);
        init(context);
    }

    public DifficultySpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DifficultySpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        String[] difficulties = getResources().getStringArray(R.array.difficulties);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.difficulties, R.layout.spinner_item);
        this.setAdapter(adapter);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                GameView gameView = ((MainActivity) context).findViewById(R.id.gameView);
                setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        switch (difficulties[position]) {
                            case "Easy":
                                gameView.newGame(GameView.Difficulty.EASY);
                                break;
                            case "Normal":
                                gameView.newGame(GameView.Difficulty.NORMAL);
                                break;
                            case "Hard":
                                gameView.newGame(GameView.Difficulty.HARD);
                                break;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                String currentDifficulty = "";
                switch (((MainActivity) getContext()).getLastDifficulty()) {
                    case EASY:
                        currentDifficulty = "Easy";
                        break;
                    case NORMAL:
                        currentDifficulty = "Normal";
                        break;
                    case HARD:
                        currentDifficulty = "Hard";
                        break;
                }
                for (int i = 0; i < difficulties.length; i++) {
                    if (difficulties[i].equals(currentDifficulty)) {
                        setSelection(i);
                        break;
                    }
                }
            }
        });
    }
}
