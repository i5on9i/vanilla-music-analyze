/*
 * Copyright (C) 2010 Christopher Eby <kreed@kreed.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.namh.jidae.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TableLayout
import android.widget.TextView
import com.namh.jidae.R

/**
 * Compact playback activity that displays itself like a dialog. That is, the
 * window is not fullscreen but only as large as it needs to be. Includes a
 * CoverView and control buttons.
 */
public class MiniPlaybackActivity : PlaybackActivity() {


    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.mini_playback)

//        mCoverView = findViewById(R.id.cover_view) as CoverView
//        mCoverView!!.setOnClickListener(this)
//        mCoverView!!.setup(mLooper, this, CoverBitmap.STYLE_OVERLAPPING_BOX)

        val previousButton = findViewById(R.id.previous)
        previousButton.setOnClickListener(this)
        mPlayPauseButton = findViewById(R.id.play_pause) as ImageButton
        mPlayPauseButton!!.setOnClickListener(this)
        val nextButton = findViewById(R.id.next)
        nextButton.setOnClickListener(this)

        mShuffleButton = findViewById(R.id.shuffle) as ImageButton
        mShuffleButton!!.setOnClickListener(this)
        registerForContextMenu(mShuffleButton)
        mEndButton = findViewById(R.id.end_action) as ImageButton
        mEndButton!!.setOnClickListener(this)
        registerForContextMenu(mEndButton)


        mElapsedView = findViewById(R.id.elapsed) as TextView
        mDurationView = findViewById(R.id.duration) as TextView
        mSeekBar = findViewById(R.id.seek_bar) as SeekBar
    }

    override fun onClick(view: View) {
        when (view.getId()) {
//            R.id.cover_view -> {
//                startActivity(Intent(this, javaClass<FullPlaybackActivity>()))
//                finish()
//            }
            else -> super.onClick(view)
        }
    }
}
