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
import android.os.Message
import android.text.format.DateUtils
import android.view.View
import android.view.Window
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TableLayout
import android.widget.TextView
import com.namh.jidae.PlaybackService
import com.namh.jidae.R
import com.namh.jidae.Song

/**
 * Compact playback activity that displays itself like a dialog. That is, the
 * window is not fullscreen but only as large as it needs to be. Includes a
 * CoverView and control buttons.
 */
public class MiniPlaybackActivity : PlaybackActivity(), SeekBar.OnSeekBarChangeListener {


    private var mSeekBarTracking: Boolean = false
    private var mPaused: Boolean = false;


    /**
     * Current song duration in milliseconds.
     */
    private var mDuration: Long = 0
    /**
     * Cached StringBuilder for formatting track position.
     */
    private val mTimeBuilder = StringBuilder()

    /**
     * The currently playing song.
     */
    private var mCurrentSong: Song? = null







    override fun onCreate(state: Bundle?) {
        super<PlaybackActivity>.onCreate(state)

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

        mSeekBar!!.setMax(1000) // set seekbar range
        mSeekBar!!.setOnSeekBarChangeListener(this)


        setDuration(0)
    }

    public override fun onResume(){
        super<PlaybackActivity>.onResume()
        mPaused = false
        updateElapsedTime()

    }



    public override fun onPause() {
        super<PlaybackActivity>.onPause()
        mPaused = true
    }

    override fun onClick(view: View) {
        when (view.getId()) {
//            R.id.cover_view -> {
//                startActivity(Intent(this, javaClass<FullPlaybackActivity>()))
//                finish()
//            }
            else -> super<PlaybackActivity>.onClick(view)
        }
    }


    protected override fun onStateChange(state: Int, toggled: Int) {
        super<PlaybackActivity>.onStateChange(state, toggled)

//        if ((toggled and (PlaybackService.FLAG_NO_MEDIA or PlaybackService.FLAG_EMPTY_QUEUE)) !== 0) {
//            if ((state and PlaybackService.FLAG_NO_MEDIA) !== 0) {
//                showOverlayMessage(R.string.no_songs)
//            } else if ((state and PlaybackService.FLAG_EMPTY_QUEUE) !== 0) {
//                showOverlayMessage(R.string.empty_queue)
//            } else {
//                hideMessageOverlay()
//            }
//        }

        if ((state and PlaybackService.FLAG_PLAYING) !== 0)
            updateElapsedTime()

//        if (mQueuePosView != null)
//            updateQueuePosition()
    }


    protected override fun onSongChange(song: Song?) {
        super<PlaybackActivity>.onSongChange(song)

        setDuration((if (song == null) 0 else song!!.duration).toLong())

//        if (mTitle != null) {
//            if (song == null) {
//                mTitle.setText(null)
//                mAlbum.setText(null)
//                mArtist.setText(null)
//            } else {
//                mTitle.setText(song!!.title)
//                mAlbum.setText(song!!.album)
//                mArtist.setText(song!!.artist)
//            }
//            updateQueuePosition()
//        }

        mCurrentSong = song
        updateElapsedTime()

//        if (mExtraInfoVisible) {
//            mHandler!!.sendEmptyMessage(MSG_LOAD_EXTRA_INFO)
//        }
    }

    /**
     * Update the current song duration fields.

     * @param duration The new duration, in milliseconds.
     */
    private fun setDuration(duration: Long) {
        mDuration = duration
        mDurationView!!.setText(DateUtils.formatElapsedTime(mTimeBuilder, duration / 1000))
    }

    /**
     * Update seek bar progress and schedule another update in one second
     */
    private fun updateElapsedTime() {
        val position
                = (if (PlaybackService.hasInstance()) PlaybackService.get(this)!!.getPosition() else 0).toLong()

        if (!mSeekBarTracking) {
            val duration = mDuration
            mSeekBar!!.setProgress(if (duration == 0L) 0 else (1000 * position / duration).toInt())
        }

        mElapsedView!!.setText(DateUtils.formatElapsedTime(mTimeBuilder, position / 1000))

        if ( // mControlsVisible &&
            !mPaused && (mState and PlaybackService.FLAG_PLAYING) !== 0) {
            // Try to update right after the duration increases by one second
            val next = 1050 - position % 1000
            mUiHandler.removeMessages(MSG_UPDATE_PROGRESS)
            mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, next)
        }
    }


    //---------------------------------------------------------  SeekBar.OnSeekBarChangeListener
    //
    override fun onStartTrackingTouch(seekBar: SeekBar) {
        mSeekBarTracking = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        mSeekBarTracking = false
    }


    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            mElapsedView!!.setText(DateUtils.formatElapsedTime(mTimeBuilder, progress * mDuration / 1000000))
            mUiHandler.removeMessages(MSG_SEEK_TO_PROGRESS)
            mUiHandler.sendMessageDelayed(mUiHandler.obtainMessage(MSG_SEEK_TO_PROGRESS, progress, 0), 150)
        }
    }



    //------------------------------------------------------------------------ handleMessage

    /**
     * Update the seekbar progress with the current song progress. This must be
     * called on the UI Handler.
     */
    private val MSG_UPDATE_PROGRESS = 10
    /**
     * Calls {{@link .seekToProgress}.
     */
    private val MSG_SEEK_TO_PROGRESS = 18


    public override fun handleMessage(message: Message): Boolean {
        when (message.what) {
//            MSG_SAVE_CONTROLS -> {
//                val editor = PlaybackService.getSettings(this).edit()
//                editor.putBoolean("visible_controls", mControlsVisible)
//                editor.putBoolean("visible_extra_info", mExtraInfoVisible)
//                editor.commit()
//            }
            MSG_UPDATE_PROGRESS -> updateElapsedTime()
//            MSG_LOAD_EXTRA_INFO -> loadExtraInfo()
//            MSG_COMMIT_INFO -> {
//                mGenreView.setText(mGenre)
//                mTrackView.setText(mTrack)
//                mYearView.setText(mYear)
//                mComposerView.setText(mComposer)
//                mFormatView.setText(mFormat)
//                mReplayGainView.setText(mReplayGain)
//            }
//            MSG_UPDATE_POSITION -> updateQueuePosition()
            MSG_SEEK_TO_PROGRESS -> {
                PlaybackService.get(this)!!.seekToProgress(message.arg1)
                updateElapsedTime()
            }
            else -> return super<PlaybackActivity>.handleMessage(message)
        }

        return true
    }
}
