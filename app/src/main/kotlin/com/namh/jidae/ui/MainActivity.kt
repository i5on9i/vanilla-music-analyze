package com.namh.jidae.ui

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.namh.jidae.R
import kotlin.properties.Delegates

public class MainActivity : AppCompatActivity(),
        View.OnClickListener {


    private var mOpenActivityButton: Button by Delegates.notNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super<AppCompatActivity>.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mOpenActivityButton = findViewById(R.id.btn_play_pause) as Button;
        mOpenActivityButton.setOnClickListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.getItemId()

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true
        }

        return super<AppCompatActivity>.onOptionsItemSelected(item)
    }

    //------------------------------------------------------------------------ View.OnClickListener



    override fun onClick(view: View) {
        when (view.getId()) {
//            R.id.next -> shiftCurrentSong(SongTimeline.SHIFT_NEXT_SONG)
            R.id.btn_play_pause -> openActivity()
//            R.id.previous -> shiftCurrentSong(SongTimeline.SHIFT_PREVIOUS_SONG)
//            R.id.end_action -> cycleFinishAction()
//            R.id.shuffle -> cycleShuffle()
        }
    }

    private fun openActivity() {
        val intent = Intent(this, javaClass<MiniPlaybackActivity>())
        startActivity(intent)

    }
}
