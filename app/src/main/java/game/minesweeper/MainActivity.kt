package game.minesweeper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import game.minesweeper.GameView.Difficulty


private const val PREFERENCES = "app_preferences"
private const val MINESWEEPER_LAST_DIFFICULTY = "minesweeper_difficulty"
private const val MINESWEEPER_EASY_SCORE = "minesweeper_easy"
private const val MINESWEEPER_NORMAL_SCORE = "minesweeper_normal"
private const val MINESWEEPER_HARD_SCORE = "minesweeper_hard"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
    }

    fun saveHighScore(difficulty: Difficulty, highScore: Int) {
        val preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        val editor = preferences.edit()
        when (difficulty) {
            Difficulty.EASY -> editor.putInt(MINESWEEPER_EASY_SCORE, highScore)
            Difficulty.NORMAL -> editor.putInt(MINESWEEPER_NORMAL_SCORE, highScore)
            Difficulty.HARD -> editor.putInt(MINESWEEPER_HARD_SCORE, highScore)
        }
        editor.apply()
    }

    fun saveLastDifficulty(difficulty: Difficulty) {
        val preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        val editor = preferences.edit()
        when (difficulty) {
            Difficulty.EASY -> editor.putInt(MINESWEEPER_LAST_DIFFICULTY, 1)
            Difficulty.NORMAL -> editor.putInt(MINESWEEPER_LAST_DIFFICULTY, 2)
            Difficulty.HARD -> editor.putInt(MINESWEEPER_LAST_DIFFICULTY, 3)
        }
        editor.apply()
    }

    fun getHighScoreAt(difficulty: Difficulty): Int {
        val preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        return when (difficulty) {
            Difficulty.EASY -> preferences.getInt(MINESWEEPER_EASY_SCORE, -1)
            Difficulty.NORMAL -> preferences.getInt(MINESWEEPER_NORMAL_SCORE, -1)
            Difficulty.HARD -> preferences.getInt(MINESWEEPER_HARD_SCORE, -1)
        }
    }

    fun getLastDifficulty() : Difficulty {
        val preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        when (preferences.getInt(MINESWEEPER_LAST_DIFFICULTY, 1)) {
            1 -> return Difficulty.EASY
            2 -> return Difficulty.NORMAL
            3 -> return Difficulty.HARD
        }
        return Difficulty.EASY
    }
}