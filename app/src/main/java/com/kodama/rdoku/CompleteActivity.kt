package com.kodama.rdoku

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.kodama.rdoku.model.GameDifficulty
import com.kodama.rdoku.model.GameType
import java.io.Serializable

class CompleteActivity : AppCompatActivity() {
    lateinit var currentGameDifficulty: GameDifficulty
    lateinit var currentGameType: GameType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete)

        initExtras()

        findViewById<ImageButton>(R.id.btnCompleteRestart).setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("game_difficulty", currentGameDifficulty as Serializable)
            intent.putExtra("game_type", currentGameType as Serializable)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnCompleteHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    private fun initExtras() {
        val extras: Bundle? = intent.extras

        if (extras != null) {

            // game difficulty
            currentGameDifficulty = extras.getSerializable("game_difficulty") as GameDifficulty

            val difficultyText: String =
                when (currentGameDifficulty) {
                    GameDifficulty.Easy -> getString(R.string.difficulty_easy)
                    GameDifficulty.Moderate -> getString(R.string.difficulty_moderate)
                    GameDifficulty.Hard -> getString(R.string.difficulty_hard)
                }
            findViewById<TextView>(R.id.tvCompleteDifficulty).text = getString(R.string.complete_difficulty, difficultyText)

            currentGameType = extras.getSerializable("game_type") as GameType

            findViewById<TextView>(R.id.tvCompleteGameType).text = when(currentGameType){
                GameType.classic_9x9 -> getString(R.string.classic_9x9)
                GameType.classic_6x6 -> getString(R.string.classic_6x6)
            }

            val tvCompleteTime = findViewById<TextView>(R.id.tvCompleteTime)
            val tvCompleteBest = findViewById<TextView>(R.id.tvCompleteBestTime)
            val tvCompleteCongrats =  findViewById<TextView>(R.id.tvCompleteCongrats)
            val isTimerEnabled = extras.getBoolean("is_timer_enabled", true)

            // show times only if timer was enabled
            if(isTimerEnabled){

                // time to complete
                val timeMinutes = extras.getLong("time_minutes", 0L).toString()
                val timeSeconds = extras.getLong("time_seconds", 0L).toString()

                tvCompleteTime.text = getString(
                    R.string.complete_time,
                    timeMinutes.padStart(2, '0'),
                    timeSeconds.padStart(2, '0')
                )

                // best time
                val bestMinutes = extras.getLong("best_time_minutes", 0L).toString()
                val bestSeconds = extras.getLong("best_time_seconds", 0L).toString()

                // previous best time
                val prevBestMinutes = extras.getLong("prev_best_minutes", 0L).toString()
                val prevBestSeconds = extras.getLong("prev_best_seconds", 0L).toString()

                tvCompleteBest.text = getString(
                    R.string.complete_best_time,
                    bestMinutes.padStart(2, '0'),
                    bestSeconds.padStart(2, '0')
                )

                // congrats text
                // where args: 1 - difficulty, 2:3 - time to complete, 4:5 - best time
                if (extras.getBoolean("new_best_time", false)) {

                    tvCompleteCongrats.text = getString(
                        R.string.complete_congrats_faster,
                        difficultyText.lowercase(),
                        timeMinutes.padStart(2, '0'),
                        timeSeconds.padStart(2, '0'),
                        prevBestMinutes.padStart(2, '0'),
                        prevBestSeconds.padStart(2, '0')
                    )
                } else {
                    tvCompleteCongrats.text = getString(
                        R.string.complete_congrats_slower,
                        difficultyText.lowercase(),
                        timeMinutes.padStart(2, '0'),
                        timeSeconds.padStart(2, '0'),
                        bestMinutes.padStart(2, '0'),
                        bestSeconds.padStart(2, '0')
                    )
                }

                if(extras.getBoolean("first_best_time", false)){
                    tvCompleteCongrats.text = getString(
                        R.string.complete_congrats_first,
                        difficultyText.lowercase(),
                        timeMinutes.padStart(2, '0'),
                        timeSeconds.padStart(2, '0'),
                    )
                }
            }
            else{
                tvCompleteBest.visibility = View.GONE
                tvCompleteTime.visibility = View.GONE

                tvCompleteCongrats.text = getString(R.string.complete_congrats_no_timer, difficultyText.lowercase())
            }
        }
    }
}