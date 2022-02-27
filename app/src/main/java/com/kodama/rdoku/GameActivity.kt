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
import com.kodama.rdoku.model.GameDifficulty
import com.kodama.rdoku.gamelogic.SudokuGame
import com.kodama.rdoku.model.BoardState
import com.kodama.rdoku.model.GameType
import java.io.Serializable

class GameActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //supportActionBar?.setDisplayShowTitleEnabled(true)
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


        sudokuGame = SudokuGame(this, size)

        sudokuBoard = findViewById(R.id.sudokuBoard)
        sudokuBoard.gridSize = size

        cmTimer = findViewById(R.id.cmTimer)

        startGame()

        initPrefs()
    }

    lateinit var cmTimer: Chronometer

    private lateinit var sudokuGame: SudokuGame
    private lateinit var sudokuBoard: SudokuBoardView
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
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.settings_menu ->{
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
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
        sudokuGame.setBoard(gameDifficulty, gameType ?: GameType.classic_9x9)

        cmTimer.base = SystemClock.elapsedRealtime()
        cmTimer.start()

        enableGameKeyboard(true)
        sudokuBoard.invalidate()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()

        editor.putInt("games_started", prefs.getInt("games_started", 0) + 1)
        editor.apply()
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
        sudokuGame.setNumberBoard(number)

        checkForComplete()

        hideFullyUsedNumber()
        sudokuBoard.invalidate()
    }

    private fun showCompleteActivity(cmTimer: Chronometer){
        cmTimer.stop()

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


        val intent = Intent(this, CompleteActivity::class.java)

        intent.putExtra("time_seconds", seconds)
        intent.putExtra("time_minutes", minutes)
        intent.putExtra("best_time_seconds", bestSeconds)
        intent.putExtra("best_time_minutes", bestMinutes)

        intent.putExtra("prev_best_seconds", prevBestSeconds)
        intent.putExtra("prev_best_minutes", prevBestMinutes)

        intent.putExtra("new_best_time", isNewBestTime)
        intent.putExtra("first_best_time", isFirstBestTime)

        intent.putExtra("game_difficulty", gameDifficulty as Serializable)
        intent.putExtra("game_type", gameType as Serializable)
        startActivity(intent)
    }

    private fun showAlertDialogGiveUp(){
        val builder = AlertDialog.Builder(this).setTitle(R.string.give_up_alert_title)
            .setMessage(R.string.give_up_alert_text)
            .setPositiveButton(R.string.alert_dialog_yes){ _, _ ->
                sudokuGame.debugSolve()
                enableGameKeyboard(false)
                sudokuBoard.invalidate()
            }
            .setNegativeButton(R.string.alert_dialog_no){ dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()

        alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getColor(R.color.alert_dialog_button_text))
        alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(getColor(R.color.alert_dialog_button_text))
    }

    // initializing preferences
    private fun initPrefs(){
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if(!prefs.getBoolean("enable_timer", true)){
            findViewById<LinearLayout>(R.id.linearLayoutTimer).visibility = View.GONE
        }
    }

    fun onBtnEraseClick(view: View){
        if(sudokuGame.boardState != BoardState.Complete){
            sudokuGame.eraseNumber()

            hideFullyUsedNumber()
            sudokuBoard.invalidate()
        }
    }

    fun onBtnHintClick(view: View){
        if(sudokuGame.boardState != BoardState.Complete){
            sudokuGame.useHint()

            hideFullyUsedNumber()
            checkForComplete()

            sudokuBoard.invalidate()
        }
    }

    private fun checkForComplete(){
        if(sudokuGame.checkForComplete()){
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = prefs.edit()
            editor.putInt("games_completed", prefs.getInt("games_completed", 0) + 1)
            editor.apply()

            showCompleteActivity(cmTimer)
        }
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

    // hiding numbers that have been fully used
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
            if(sudokuGame.fullyUsedNumber(i)){
                button.visibility = View.GONE
            }
            else{
                if(button.visibility == View.GONE){
                    button.visibility = View.VISIBLE
                }
            }
        }
    }
}