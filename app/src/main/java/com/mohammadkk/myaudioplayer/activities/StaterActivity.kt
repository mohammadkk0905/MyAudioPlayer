package com.mohammadkk.myaudioplayer.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import com.mohammadkk.myaudioplayer.databinding.ActivityStaterBinding

class StaterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStaterBinding
    private val callbackAnim = object : MotionLayout.TransitionListener {
        override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {

        }
        override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
        }
        override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
            startApp()
        }
        override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        })
        binding = ActivityStaterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.addTransitionListener(callbackAnim)
    }
    private fun startApp() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        binding.root.removeTransitionListener(callbackAnim)
    }
}