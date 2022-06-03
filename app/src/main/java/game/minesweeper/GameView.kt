package game.minesweeper

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.core.view.doOnLayout
import java.util.*
import kotlin.math.abs

class GameView : View {

    enum class Difficulty { EASY, NORMAL, HARD }

    var gameOver = false; private set
    var unitSize: Int = 0; private set

    private var seconds = 0
    private var flagsLeft = 0
    private var tilesLeftToWin = 0
    private var won = false
    private var firstTouch = false
    private var shouldAddPlayScreen = false

    private var timer = Timer()
    private var currentDifficulty = Difficulty.EASY

    private val tilesToAnimate = mutableSetOf<Tile>()
    private val focusedSquare = Rect()
    private val fillPaint = Paint()
    private val strokePaint = Paint()

    private lateinit var tiles: Array<Array<Tile>>
    private lateinit var mainLayout : ViewGroup
    private lateinit var regOptions : View
    private lateinit var markedOptions : View
    private lateinit var revealedOptions : View
    private lateinit var playScreen : View
    private lateinit var playButton : ThemeButton
    private lateinit var fm : Paint.FontMetrics
    private lateinit var backgroundBitmap : Bitmap

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr)

    init {
        fillPaint.style = Paint.Style.FILL
        fillPaint.typeface = ResourcesCompat.getFont(context, R.font.roboto_bold)
        strokePaint.color = Color.rgb(78, 129, 42)
        strokePaint.style = Paint.Style.STROKE
        doOnLayout {
            mainLayout = (context as MainActivity).findViewById(R.id.mainLayout)
            playScreen = mainLayout.findViewById(R.id.playAgainScreen)
            playButton = mainLayout.findViewById(R.id.playAgainButton)
            playButton.text = "START GAME"
            playButton.setAction { removePlayScreen() }
            regOptions = LayoutInflater.from(context).inflate(R.layout.options, mainLayout, false)
            markedOptions = LayoutInflater.from(context).inflate(R.layout.options_marked, mainLayout, false)
            revealedOptions = LayoutInflater.from(context).inflate(R.layout.options_revealed, mainLayout, false)
            regOptions.findViewById<View>(R.id.shovel).setOnClickListener(digClickListener)
            regOptions.findViewById<View>(R.id.flag).setOnClickListener(markClickListener)
            markedOptions.findViewById<View>(R.id.remove_flag).setOnClickListener(unMarkClickListener)
            revealedOptions.findViewById<View>(R.id.digRevealed).setOnClickListener(digClickListener)
            newGame((context as MainActivity).getLastDifficulty())
        }
    }

    fun newGame(difficulty: Difficulty) {
        fun addMines(minesAround : Int) {
            val r = Random()
            var i = 0
            while (i < minesAround) {
                val column = r.nextInt(tiles.size)
                val row = r.nextInt(tiles[0].size)
                if (!tiles[column][row].isMine) {
                    tiles[column][row].isMine = true
                    i++
                }
            }
        }
        fun findUnitSize(targetSquaresAmount : Int) : Int {
            var unitSize: Int
            var y: Float
            var x = 2F
            do {
                x++
                unitSize = (width / x).toInt()
                y = (height - unitSize).toFloat() / unitSize
            } while (y * (x - 2) <= targetSquaresAmount)
            //check if you should go one step backwards (if it's closer to the target)
            if (y.toInt() * (x - 2) - targetSquaresAmount > targetSquaresAmount - ((height - width / (x - 1)).toInt() / (width / (x - 1))) * (x - 3)) {
                x--
                unitSize = (width / x).toInt()
            }
            return unitSize
        }
        (context as MainActivity).saveLastDifficulty(difficulty)
        if ((context as MainActivity).getHighScoreAt(difficulty) != -1) {
            post { (playScreen.findViewById<View>(R.id.highScoreText) as TextView).text = (context as MainActivity).getHighScoreAt(difficulty).toString() }
        } else if (difficulty != currentDifficulty) {
            post { (playScreen.findViewById<View>(R.id.highScoreText) as TextView).text = "---" }
        }
        iterateOptions { mainLayout.removeView(it) }
        focusedSquare.set(-1000, -1000, -1000, -1000)
        currentDifficulty = difficulty
        firstTouch = true
        gameOver = false
        shouldAddPlayScreen = true
        seconds = 0
        timer.cancel()
        val minesAmount = when (difficulty) {
            Difficulty.EASY -> {
                unitSize = findUnitSize(100)
                10
            }
            Difficulty.NORMAL -> {
                unitSize = findUnitSize(200)
                30
            }
            Difficulty.HARD -> {
                unitSize = findUnitSize(400)
                90
            }
        }
        fillPaint.textSize = 0.8F * unitSize
        strokePaint.strokeWidth = unitSize / 10F
        fm = fillPaint.fontMetrics
        tiles = Array(width / unitSize - 2) { x -> Array(height / unitSize - 1) { y -> Tile(this, x, y) } }
        addMines(minesAmount)
        backgroundBitmap = BackgroundCreator.create(width, height, unitSize)
        flagsLeft = minesAmount
        tilesLeftToWin = tiles.size * tiles[0].size - minesAmount
        post {
            (mainLayout.findViewById<View>(R.id.timeText) as TextView).text = "0"
            (mainLayout.findViewById<View>(R.id.flagsAmount) as TextView).text = "$flagsLeft"
        }
        println("new game")
        postInvalidate()
    }

    private fun dig(column : Int, row : Int) {
        val tile = tiles[column][row]
        if (!tile.isRevealed) {
            tile.isRevealed = true
            tilesLeftToWin--
            tilesToAnimate.add(tile)
        }
        if (tile.isMine) {
            won = false
            gameOver = true
            animateTiles()
            gameOver()
            return
        }
        if (tile.isMarked) {
            tile.isMarked = false
            flagsLeft++
            post { (mainLayout.findViewById(R.id.flagsAmount) as TextView).text = "$flagsLeft" }
        }
        var counter = 0
        iterateAround(column, row) { t, _, _ -> if (t.isMine) counter++ }
        tile.minesAround = counter
        if (counter == 0) {
            iterateAround(column, row) {
                    t, x, y ->
                if (!t.isMine && !t.isRevealed) {
                    if (t.isMarked) {
                        t.isMarked = false
                        flagsLeft++
                        post {
                            (mainLayout.findViewById(R.id.flagsAmount) as TextView).text = "$flagsLeft"
                        }
                    }
                    dig(x, y)
                }
            }
        }
        iterateAround(column, row) { t, x, y -> if (t.isHighlighted) { t.isHighlighted = false; dig(x, y) } }
        animateTiles()
        if (tilesLeftToWin == 0) {
            won = true
            gameOver = true
        }
        if (gameOver) gameOver()
    }

    private fun clearStartArea(column : Int, row : Int, widthRad: Int = 1, heightRad: Int = 1) {
        fun replaceMine(x : Int, y : Int) {
            val r = Random()
            while (true) {
                val randX = r.nextInt(tiles.size)
                val randY = r.nextInt(tiles[randX].size)
                if (!tiles[randX][randY].isMine && (abs(randX - column) > widthRad || abs(randY - row) > heightRad)) {
                    tiles[randX][randY].isMine = true
                    tiles[x][y].isMine = false
                    return
                }
            }
        }
        iterateAround(column, row, widthRad, heightRad) { t, x, y -> if (t.isMine) replaceMine(x, y) }
    }

    private fun addHighlight(column: Int, row: Int) : Boolean {
        var counter = 0
        iterateAround(column, row) { t, _, _ -> if (t.isMarked) counter++ }
        if (counter == tiles[column][row].minesAround) {
            var counter2 = 0
            iterateAround(column, row) {
                    t, _, _ ->
                if (!t.isMarked && !t.isRevealed) {
                    t.isHighlighted = true
                    counter2++
                }
            }
            return counter2 != 0
        }
        return false
    }

    private fun addPlayScreen() {
        if (shouldAddPlayScreen) {
            shouldAddPlayScreen = false
            postDelayed( {
                playButton.text = ("PLAY  AGAIN")
                mainLayout.addView(playScreen)
                val alphaAnimation = AlphaAnimation(0F, 1F)
                val scaleAnimation = ScaleAnimation(0F, 1F, 0F, 1F, Animation.RELATIVE_TO_SELF, 0.5F, Animation.RELATIVE_TO_SELF, 0.5F)
                scaleAnimation.interpolator = PathInterpolatorCompat.create(0.725F, 0F, 0.195F, 1.460F)
                alphaAnimation.fillAfter = true
                alphaAnimation.duration = 500
                scaleAnimation.fillAfter = true
                scaleAnimation.duration = 500
                playScreen.startAnimation(alphaAnimation)
                playScreen.findViewById<View>(R.id.playAgainView).startAnimation(scaleAnimation)
            } , 1000)
        }
    }

    private fun removePlayScreen() {
        post {
            val alphaAnimation = AlphaAnimation(1F, 0F)
            alphaAnimation.duration = 500
            alphaAnimation.startOffset = 300
            alphaAnimation.fillAfter = true
            val scaleAnimation = ScaleAnimation(1F, 0F, 1F, 0F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            scaleAnimation.duration = 500
            scaleAnimation.startOffset = 300
            scaleAnimation.fillAfter = true
            playScreen.startAnimation(alphaAnimation)
            playScreen.findViewById<View>(R.id.playAgainView).startAnimation(scaleAnimation)
            (playScreen.parent as ViewGroup).removeView(playScreen)
        }
        postDelayed({ newGame(currentDifficulty) }, 300)
    }

    private fun startTimer() {
        timer = Timer()
        timer.scheduleAtFixedRate( object : TimerTask() {
            override fun run() {
                seconds++
                post { mainLayout.findViewById<TextView>(R.id.timeText).text = "$seconds" }
            }
        }, 1000, 1000)
    }

    private fun gameOver() {
        timer.cancel()
        timer = Timer()
        post { (playScreen.findViewById<TextView>(R.id.scoreText)).text = "$seconds" }
        if (!won) {
            val unMarkedMines = mutableListOf<Tile>()
            val markedIncorrectlyTiles = mutableListOf<Tile>()
            val r = Random()
            iterateAll {
                    t, _, _ ->
                if (t.isMine && !t.isRevealed && !t.isMarked)
                    unMarkedMines.add(t)
                else if (!t.isMine && !t.isRevealed && t.isMarked)
                    markedIncorrectlyTiles.add(t)
            }
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    when {
                        unMarkedMines.size > 0 -> {
                            post {
                                val i: Int = r.nextInt(unMarkedMines.size)
                                unMarkedMines[i].isRevealed = true
                                tilesToAnimate.add(unMarkedMines[i])
                                unMarkedMines.removeAt(i)
                                animateTiles()
                            }
                        }
                        markedIncorrectlyTiles.size > 0 -> {
                            post {
                                val i: Int = r.nextInt(markedIncorrectlyTiles.size)
                                markedIncorrectlyTiles[i].isMarked = false
                                markedIncorrectlyTiles[i].setMarkedCorrectly(false)
                                markedIncorrectlyTiles.removeAt(i)
                            }
                        }
                        else -> {
                            this.cancel()
                            addPlayScreen()
                        }
                    }
                }
            }, 500, 500)
            return
        }
        if ((context as MainActivity).getHighScoreAt(currentDifficulty) > seconds || (context as MainActivity).getHighScoreAt(currentDifficulty) == -1) {
            (context as MainActivity).saveHighScore(currentDifficulty, seconds)
            post { (playScreen.findViewById<View>(R.id.highScoreText) as TextView).text = "$seconds" }
        }
        addPlayScreen()
    }

    /**
     * animates the tiles getting revealed
     * in this class for performance reasons
     */
    private fun animateTiles(pathsAmount : Int = 4) {
        val a: Array<MutableList<Tile>> = Array(pathsAmount) { mutableListOf() }
        val r = Random()
        val toRemove = mutableSetOf<Tile>()
        for (t in tilesToAnimate) {
            a[r.nextInt(pathsAmount)].add(t)
            toRemove.add(t)
        }
        tilesToAnimate.removeAll(toRemove)
        val linearAnim = ValueAnimator.ofPropertyValuesHolder(
            PropertyValuesHolder.ofInt("level", 0, 10000),
            PropertyValuesHolder.ofInt("dx", 0, unitSize),
            PropertyValuesHolder.ofInt("scale", unitSize, 0)
        )
        val accelerateAnim = ValueAnimator.ofInt(0, unitSize * 4)
        accelerateAnim.addUpdateListener {
            for (i in 0 until pathsAmount) {
                for (t in a[i]) {
                    t.revealDrawable.level = linearAnim.getAnimatedValue("level") as Int
                    t.revealDrawable.setBounds(
                        (t.column + 1) * unitSize + linearAnim.getAnimatedValue("dx") as Int * (i - 1),
                        t.row * unitSize + it.animatedValue as Int,
                        (t.column + 1) * unitSize + linearAnim.getAnimatedValue("dx") as Int * (i - 1) + linearAnim.getAnimatedValue(
                            "scale"
                        ) as Int,
                        t.row * unitSize + it.animatedValue as Int + linearAnim.getAnimatedValue("scale") as Int
                    )
                    postInvalidate()
                }
            }
        }
        linearAnim.repeatCount = 0
        linearAnim.duration = 1000
        linearAnim.interpolator = LinearInterpolator()
        accelerateAnim.repeatCount = 0
        accelerateAnim.duration = 1000
        accelerateAnim.interpolator = PathInterpolator(.22f, -0.41f, .79f, .19f)
        linearAnim.start()
        accelerateAnim.start()
    }

    private fun iterateAround(column : Int, row : Int, widthRad: Int = 1, heightRad: Int = 1, action : (Tile, column : Int, row : Int) -> Unit) {
        for (x in column - widthRad..column + widthRad) {
            for (y in row - heightRad..row + heightRad) {
                if (x >= 0 && y >= 0 && x < tiles.size && y < tiles[x].size) {
                    action(tiles[x][y], x, y)
                }
            }
        }
    }

    private fun iterateAll(action: (Tile, column: Int, row: Int) -> Unit) {
        for (x in tiles.indices)
            for (y in tiles[x].indices)
                action(tiles[x][y], x, y)
    }

    private fun iterateOptions( action : (View) -> Unit ) {
        action(regOptions)
        action(markedOptions)
        action(revealedOptions)
    }

    private val digClickListener : (View) -> Unit = {
        dig(focusedSquare.left / unitSize - 1, focusedSquare.top / unitSize)
        iterateOptions { mainLayout.removeView(it) }
        focusedSquare.set(-1000, -1000, -1000, -1000)
        postInvalidate()
    }

    private val markClickListener : (View) -> Unit = {
        if (flagsLeft > 0) {
            tiles[focusedSquare.left / unitSize - 1][focusedSquare.top / unitSize].isMarked = true
            flagsLeft--
            post { (mainLayout.findViewById(R.id.flagsAmount) as TextView).text = "$flagsLeft" }
        }
        mainLayout.removeView(regOptions)
        focusedSquare.set(-1000, -1000, -1000, -1000)
        postInvalidate()
    }

    private val unMarkClickListener : (View) -> Unit = {
        tiles[focusedSquare.left / unitSize - 1][focusedSquare.top / unitSize].isMarked = false
        flagsLeft++
        post { (mainLayout.findViewById(R.id.flagsAmount) as TextView).text = "$flagsLeft" }
        mainLayout.removeView(markedOptions)
        focusedSquare.set(-1000, -1000, -1000, -1000)
        postInvalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if (e != null && e.action == MotionEvent.ACTION_DOWN) {
            if (!gameOver) {
                val column = e.x.toInt() / unitSize - 1
                val row = e.y.toInt() / unitSize
                iterateAll { t, _, _ -> t.isHighlighted = false }
                if (e.x > unitSize && e.x < width - unitSize - width % unitSize && e.y < height - unitSize - height % unitSize && (tiles[column][row].minesAround == -1 || addHighlight(column, row))) {
                    val t = tiles[column][row]
                    if (firstTouch) {
                        when (currentDifficulty) {
                            Difficulty.EASY -> clearStartArea(t.column, t.row)
                            else -> { clearStartArea(t.column, t.row, 2, 2) }
                        }
                        dig(t.column, t.row)
                        firstTouch = false
                        startTimer()
                    }
                    else {
                        iterateOptions { mainLayout.removeView(it) }
                        //options for a marked tile
                        if (t.isMarked)
                            mainLayout.addView(markedOptions)
                        //options for a tile with the same amount of flags around it and mines around it
                        else if (t.isRevealed && addHighlight(t.column, t.row))
                            mainLayout.addView(revealedOptions)
                        //options for a regular tile
                        else
                            mainLayout.addView(regOptions)
                        //double click events
                        val xInUnitSizeJumps = (e.x / unitSize).toInt() * unitSize
                        val yInUnitSIzeJumps = (e.y / unitSize).toInt() * unitSize
                        if (focusedSquare.left == xInUnitSizeJumps && focusedSquare.top == yInUnitSIzeJumps) {
                            when {
                                t.isMarked -> markedOptions.findViewById<View>(R.id.remove_flag).performClick()
                                t.isRevealed -> revealedOptions.findViewById<View>(R.id.digRevealed).performClick()
                                else -> regOptions.findViewById<View>(R.id.shovel).performClick()
                            }
                        }
                        //non double click events
                        else {
                            focusedSquare.set(xInUnitSizeJumps, yInUnitSIzeJumps, xInUnitSizeJumps + unitSize, yInUnitSIzeJumps + unitSize)
                            iterateOptions {
                                it.x = focusedSquare.right.toFloat()
                                it.y = focusedSquare.top + y - (regOptions.height - unitSize) / 2F
                            }
                        }
                    }
                }
                //tile is a 0 or clicked outside map
                else {
                    focusedSquare.set(-1000, -1000, -1000, -1000)
                    iterateOptions { mainLayout.removeView(it) }
                }
            }
            //game over
            else addPlayScreen()
        }
        postInvalidate()
        return super.onTouchEvent(e)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(backgroundBitmap, 0F, 0F, null)
        iterateAll {
                t, x, y ->
            if (t.isHighlighted) {
                fillPaint.color = Color.rgb(185, 221, 119)
                canvas?.drawRect((x + 1F) * unitSize, (y + 0F) * unitSize, (x + 2F) * unitSize, (y + 1F) * unitSize, fillPaint)
            }
            if (t.isMine) {
                fillPaint.color = Color.BLACK
                canvas?.drawRect((x + 1F) * unitSize, (y + 0F) * unitSize, (x + 2F) * unitSize, (y + 1F) * unitSize, fillPaint)
            }
            else if (t.isRevealed) t.draw(canvas, fillPaint, fm)
        }
        canvas?.drawRect(focusedSquare, strokePaint)
        iterateAll {
                t, _, _ ->
            t.drawDrawable(canvas)
        }
    }
}