/*
 * Copyright (C) 2015 Adrian Ulrich <adrian@blinkenlights.ch>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>. 
 */


package com.namh.jidae

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect

import java.io.IOException

public class PodMediaPlayer(private val mContext: Context) : MediaPlayer() {

    private var dataSource: String? = null
    private var mHasNextMediaPlayer: Boolean = false

    /**
     * Resets the media player to an unconfigured state
     */
    public override fun reset() {
        dataSource = null
        mHasNextMediaPlayer = false
        super.reset()
    }

    /**
     * Releases the media player and frees any claimed AudioEffect
     */
    public override fun release() {
        dataSource = null
        mHasNextMediaPlayer = false
        super.release()
    }

    /**
     * Sets the data source to use
     */
    throws(IOException::class, IllegalArgumentException::class, SecurityException::class, IllegalStateException::class)
    public override fun setDataSource(dataSource: String) {
        this.dataSource = dataSource
        super.setDataSource(this.dataSource)
    }

    /**
     * Sets the next media player data source
     */
    public fun setNextMediaPlayer(next: PodMediaPlayer?) {
        super.setNextMediaPlayer(next)
        mHasNextMediaPlayer = (next != null)
    }

    /**
     * Returns true if a 'next' media player has been configured
     * via setNextMediaPlayer(next)
     */
    public fun hasNextMediaPlayer(): Boolean {
        return mHasNextMediaPlayer
    }

    /**
     * Creates a new AudioEffect for our AudioSession
     */
    public fun openAudioFx() {
        val i = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, this.getAudioSessionId())
        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mContext.getPackageName())
        mContext.sendBroadcast(i)
    }

    /**
     * Releases a previously claimed audio session id
     */
    public fun closeAudioFx() {
        val i = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, this.getAudioSessionId())
        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, mContext.getPackageName())
        mContext.sendBroadcast(i)
    }

}
