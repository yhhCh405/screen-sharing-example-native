package me.yhh.mediaprojectiontest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    companion object{
        const val TAG = "MediaProjection"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.media_projection_btn).setOnClickListener {
            startActivity(MediaProjectionActivity.getIntent(this))
        }

    }
}

