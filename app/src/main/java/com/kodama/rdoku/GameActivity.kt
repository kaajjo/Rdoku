package com.kodama.rdoku

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.kodama.rdoku.customview.SudokuBoardView
import com.kodama.rdoku.gamelogic.BestTimeManager
import com.kodama.rdoku.gamelogic.Solver
import com.kodama.rdoku.model.GameDifficulty
import com.kodama.rdoku.model.BoardState
import com.kodama.rdoku.model.GameType
import com.kodama.rdoku.model.SudokuCell
import java.io.Serializable
import kotlin.random.Random

class GameActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.app_name)

        val bundle: Bundle? = intent.extras

        if(bundle != null){
            gameDifficulty = bundle.getSerializable("game_difficulty") as GameDifficulty
            gameType = bundle.getSerializable("game_type") as? GameType
        }

        size = when(gameType){
            GameType.classic_9x9 -> 9
            GameType.classic_6x6 -> 6
            else -> 9
        }

        boardView = findViewById(R.id.sudokuBoard)
        boardView.gridSize = size

        cmTimer = findViewById(R.id.cmTimer)

        initPrefs()

        startGame()
    }

    lateinit var cmTimer: Chronometer

    private lateinit var solvedBoard: Array<Array<SudokuCell>>
    private lateinit var boardView: SudokuBoardView
    private lateinit var gameDifficulty: GameDifficulty
    private var gameType: GameType? = null
    private var size = 9


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.game_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.restart_menu ->{
                startGame()
                true
            }
            R.id.about_menu -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            R.id.solve_menu ->{
                showAlertDialogGiveUp()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            android.R.id.home -> {
                finish()
                true
            }
            else -> {
                super.onContextItemSelected(item)
            }
        }
    }

    private fun startGame(){
        val tvDifficulty = findViewById<TextView>(R.id.tvDifficulty)
        when(gameDifficulty){
            GameDifficulty.Easy -> tvDifficulty.text = getString(R.string.difficulty_easy)
            GameDifficulty.Moderate -> tvDifficulty.text = getString(R.string.difficulty_moderate)
            GameDifficulty.Hard -> tvDifficulty.text = getString(R.string.difficulty_hard)
        }
        setBoard()
        solveBoard()
        cmTimer.base = SystemClock.elapsedRealtime()
        cmTimer.start()

        enableGameKeyboard(true)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()

        editor.putInt("games_started", prefs.getInt("games_started", 0) + 1)
        editor.apply()
    }

    private fun setBoard(){
        boardView.resetBoard()

        val boards: Array<String> =
            if(gameType == GameType.classic_9x9){
                when(gameDifficulty){
                    GameDifficulty.Easy -> resources.getStringArray(R.array.easyBoards9x9)
                    GameDifficulty.Moderate -> resources.getStringArray(R.array.moderateBoards9x9)
                    GameDifficulty.Hard -> resources.getStringArray(R.array.hardBoards9x9)
                }
            } else{
                resources.getStringArray(R.array.easyBoards6x6)
            }

        val randomBoardString = boards[Random.nextInt(0, boards.size)]

        val randomBoard = Array(size) {Array(size){ SudokuCell(0) }}

        for(i in 0 until size){
            for(j in 0 until size){
                randomBoard[i][j].value = randomBoardString[i * size + j].digitToInt()
                if(randomBoard[i][j].value != 0){
                    randomBoard[i][j].locked = true
                }
            }
        }
        boardView.setBoard(randomBoard)
    }

    override fun onDestroy() {
        cmTimer.stop()
        super.onDestroy()
    }

    fun onBtnNumberClick(view: View){
        val number: Int = when(view.id){
            R.id.btnOne -> 1
            R.id.btnTwo -> 2
            R.id.btnThree -> 3
            R.id.btnFour -> 4
            R.id.btnFive -> 5
            R.id.btnSix -> 6
            R.id.btnSeven -> 7
            R.id.btnEight -> 8
            R.id.btnNine -> 9
            else -> 0
        }
        if(!boardView.getBoard()[boardView.selectedRow][boardView.selectedCol].locked){
            boardView.setNum(number)
            boardView.setNumberWrong(!isValidMove(number))
        }

        checkForComplete()

        hideFullyUsedNumber()
    }

    private fun showCompleteActivity(cmTimer: Chronometer){
        cmTimer.stop()


        val completeActivityIntent = Intent(this, CompleteActivity::class.java)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isTimerEnabled = prefs.getBoolean("enable_timer", true)

        // save best time only if timer was enabled
        if(isTimerEnabled){

            // get minutes and seconds from the timer
            val seconds = (SystemClock.elapsedRealtime() - cmTimer.base) / 1000 % 60
            val minutes = (SystemClock.elapsedRealtime() - cmTimer.base) / 1000 / 60 % 60

            val bestTimeManager = BestTimeManager(this)

            var bestTime = bestTimeManager.getBestTime(gameDifficulty, gameType ?: GameType.classic_9x9)
            var bestSeconds = bestTime.first
            var bestMinutes = bestTime.second

            var isNewBestTime = false
            var isFirstBestTime = false

            if(bestMinutes == 0L && bestSeconds == 0L){
                isFirstBestTime = true
            }

            var prevBestSeconds = 0L
            var prevBestMinutes = 0L
            if(minutes <= bestMinutes && seconds < bestSeconds || bestMinutes == 0L && bestSeconds == 0L){
                prevBestSeconds = bestSeconds
                prevBestMinutes = bestMinutes

                bestTimeManager.saveBestTime(seconds, minutes, gameDifficulty, gameType ?: GameType.classic_9x9)
                isNewBestTime = true
            }

            bestTime = bestTimeManager.getBestTime(gameDifficulty,gameType ?: GameType.classic_9x9)

            bestSeconds = bestTime.first
            bestMinutes = bestTime.second

            completeActivityIntent.putExtra("time_seconds", seconds)
            completeActivityIntent.putExtra("time_minutes", minutes)
            completeActivityIntent.putExtra("best_time_seconds", bestSeconds)
            completeActivityIntent.putExtra("best_time_minutes", bestMinutes)

            completeActivityIntent.putExtra("prev_best_seconds", prevBestSeconds)
            completeActivityIntent.putExtra("prev_best_minutes", prevBestMinutes)

            completeActivityIntent.putExtra("new_best_time", isNewBestTime)
            completeActivityIntent.putExtra("first_best_time", isFirstBestTime)
        }

        completeActivityIntent.putExtra("game_difficulty", gameDifficulty as Serializable)
        completeActivityIntent.putExtra("game_type", gameType as Serializable)

        completeActivityIntent.putExtra("is_timer_enabled", isTimerEnabled)

        startActivity(completeActivityIntent)
    }

    private fun showAlertDialogGiveUp(){
        val builder = AlertDialog.Builder(this).setTitle(R.string.give_up_alert_title)
            .setMessage(R.string.give_up_alert_text)
            .setPositiveButton(R.string.alert_dialog_yes){ _, _ ->
                giveUpSolve()
                enableGameKeyboard(false)
            }
            .setNegativeButton(R.string.alert_dialog_no){ dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()

        alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getColor(R.color.alert_dialog_button_text))
        alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(getColor(R.color.alert_dialog_button_text))
    }

    // init preferences
    private fun initPrefs(){
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if(!prefs.getBoolean("enable_timer", true)){
            findViewById<LinearLayout>(R.id.linearLayoutTimer).visibility = View.GONE
        }
    }

    // erase button click
    fun onBtnEraseClick(view: View){
        if(boardView.boardState != BoardState.Complete && boardView.selectedRow != -1 && boardView.selectedCol != -1
            && !boardView.getBoard()[boardView.selectedRow][boardView.selectedCol].locked){
            boardView.setNum(0)
            hideFullyUsedNumber()
        }
    }

    // hint button click
    fun onBtnHintClick(view: View){
        if(boardView.boardState != BoardState.Complete){
            boardView.setNum(solvedBoard[boardView.selectedRow][boardView.selectedCol].value)
            boardView.setNumberWrong(false)
            hideFullyUsedNumber()
            checkForComplete()
        }
    }

    // checks if sudoku is completed
    private fun checkForComplete(){
        val board = boardView.getBoard()
        for(i in 0 until size){
            for(j in 0 until size){
                if(board[i][j].value == 0){
                    return
                }
            }
        }

        for(i in 0 until size){
            for(j in 0 until size){
                if(board[i][j].value != solvedBoard[i][j].value){
                    return
                }
            }
        }

        boardView.boardState = BoardState.Complete

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()
        editor.putInt("games_completed", prefs.getInt("games_completed", 0) + 1)
        editor.apply()

        showCompleteActivity(cmTimer)
    }

    private fun enableGameKeyboard(isVisible: Boolean){
        val visibility: Int = if(isVisible){
            View.VISIBLE
        } else{
            View.GONE
        }

        val gameKeyboard = findViewById<LinearLayout>(R.id.gameKeyboard)
        for(i in 0 until gameKeyboard.childCount){
            val btn = gameKeyboard.getChildAt(i) as Button
            btn.visibility = visibility
            if(i >= size){
                btn.visibility = View.GONE
            }
        }
    }

    // hides numbers that have been fully used
    private fun hideFullyUsedNumber(){
        for(i in 1..size){
            val button: Button = when(i){
                1 -> findViewById(R.id.btnOne)
                2 -> findViewById(R.id.btnTwo)
                3 -> findViewById(R.id.btnThree)
                4 -> findViewById(R.id.btnFour)
                5 -> findViewById(R.id.btnFive)
                6 -> findViewById(R.id.btnSix)
                7 -> findViewById(R.id.btnSeven)
                8 -> findViewById(R.id.btnEight)
                9 -> findViewById(R.id.btnNine)
                else -> return
            }
            if(isNumberFullyUsed(i)){
                button.visibility = View.GONE
            }
            else{
                if(button.visibility == View.GONE){
                    button.visibility = View.VISIBLE
                }
            }
        }
    }

    // fills all cells with correct numbers
    private fun giveUpSolve(){
        boardView.setBoard(solvedBoard)
    }

    private fun solveBoard(){
        val solver = Solver()
        solvedBoard = solver.solve(boardView.getBoard(), size)
    }

    private fun isValidMove(number: Int):Boolean{
        val solver = Solver()
        return solver.validMove(boardView.selectedRow,
            boardView.selectedCol,
            number, boardView.getBoard(), size)
    }

    private fun isNumberFullyUsed(num: Int):Boolean{
        if(num == 0){
            return false
        }

        var count = 0

        for(i in 0 until size){
            for(j in 0 until size){
                if(boardView.getBoard()[i][j].value == num){
                    count += 1
                }
            }
        }

        if(count != size){
            return false
        }

        return true
    }
}