/*
 * Copyright (C) 2012-2015 Adrian Ulrich <adrian@blinkenlights.ch>
 * Copyright (C) 2010, 2011 Christopher Eby <kreed@kreed.org>
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

package com.namh.jidae

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.backup.BackupManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.namh.jidae.ui.MiniPlaybackActivity
import com.namh.jidae.ui.PlaybackActivity

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.util.ArrayList
import kotlin.platform.platformStatic


/**
 * Handles music playback and pretty much all the other work.
 */
public class PlaybackService : Service(),
        Handler.Callback
//        MediaPlayer.OnCompletionListener,
//        MediaPlayer.OnErrorListener,
//        SharedPreferences.OnSharedPreferenceChangeListener,
//        SongTimeline.Callback,
//        SensorEventListener,
//        AudioManager.OnAudioFocusChangeListener
{

    override fun onBind(intents: Intent): IBinder? {
        return null
    }


    /**
     * The PlaybackService state, indicating if the service is playing,
     * repeating, etc.

     * The format of this is 0b00000000_00000000_0000000ff_feeedcba,
     * where each bit is:
     * a:   [PlaybackService.FLAG_PLAYING]
     * b:   [PlaybackService.FLAG_NO_MEDIA]
     * c:   [PlaybackService.FLAG_ERROR]
     * d:   [PlaybackService.FLAG_EMPTY_QUEUE]
     * eee: [PlaybackService.MASK_FINISH]
     * fff: [PlaybackService.MASK_SHUFFLE]
     */
    var mState: Int = 0

    /**
     * How many broken songs we did already skip
     */
    var mSkipBroken: Int = 0

    /**
     * Object used for state-related locking.
     */
    val mStateLock = arrayOfNulls<Any>(0)

    /**
     * Elapsed realtime at which playback was paused by idle timeout. -1
     * indicates that no timeout has occurred.
     */
    private var mIdleStart: Long = -1

    private var mErrorMessage: String? = null


    //---------------------------------------------------------------------------- Notification
    /**
     * If true, the notification should not be hidden when pausing regardless
     * of user settings.
     */
    private var mForceNotificationVisible: Boolean = false
    /**
     * Behaviour of the notification
     */
    private var mNotificationMode: Int = 0

    private var mNotificationManager: NotificationManager? = null

    public val NEVER: Int = 0
    public val WHEN_PLAYING: Int = 1
    public val ALWAYS: Int = 2

    private val NOTIFICATION_ID = 2

    /**
     * The intent for the notification to execute, created by
     * [PlaybackService.createNotificationAction].
     */
    private var mNotificationAction: PendingIntent? = null

    /**
     * If true, create a notification with ticker text or heads up display
     */
    private var mNotificationNag: Boolean = false





    private var mLooper: Looper? = null
    private var mHandler: Handler? = null
    var mMediaPlayer: PodMediaPlayer? = null
    var mPreparedMediaPlayer: PodMediaPlayer? = null
    private var mMediaPlayerInitialized: Boolean = false

    var mTimeline: SongTimeline? = null
    private var mCurrentSong: Song? = null










    public val SHIFT_FINISH: Int = 4
    /**
     * These three bits will be one of SongTimeline.FINISH_*.
     */
    public val MASK_FINISH: Int = 7 shl SHIFT_FINISH
    public val SHIFT_SHUFFLE: Int = 7
    /**
     * These three bits will be one of SongTimeline.SHUFFLE_*.
     */
    public val MASK_SHUFFLE: Int = 7 shl SHIFT_SHUFFLE


    /**
     * If a user action is triggered within this time (in ms) after the
     * idle time fade-out occurs, playback will be resumed.
     */
    private val IDLE_GRACE_PERIOD = 60000
    /**
     * Defer entering deep sleep for this time (in ms).
     */
    private val SLEEP_STATE_DELAY = 60000

    public override fun onCreate() {
        val thread = HandlerThread("PlaybackService", Process.THREAD_PRIORITY_DEFAULT)
        thread.start()



        mMediaPlayer = getNewMediaPlayer()
        mPreparedMediaPlayer = getNewMediaPlayer()
        // We only have a single audio session
        mPreparedMediaPlayer!!.setAudioSessionId(mMediaPlayer!!.getAudioSessionId())


        // Notification
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        mNotificationAction = createNotificationAction(settings)
        mNotificationAction = createNotificationAction()

//        mNotificationMode = Integer.parseInt(settings.getString(PrefKeys.NOTIFICATION_MODE, "1"))
//        mNotificationNag = settings.getBoolean(PrefKeys.NOTIFICATION_NAG, false)
        mNotificationMode = 1
        mNotificationNag = false


        mLooper = thread.getLooper()
        mHandler = Handler(mLooper, this)


        val state = loadState()
        updateState(state)
        setCurrentSong(0)


        sInstance = this
        synchronized (sWait) {
            (sWait as Object).notifyAll()
        }

    }

    public override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.getAction()

            if (ACTION_TOGGLE_PLAYBACK == action) {
                playPause()
            } else if (ACTION_PLAY == action) {
                play()
            } else if (ACTION_PAUSE == action) {
                pause()

            } else if (ACTION_TOGGLE_PLAYBACK_NOTIFICATION == action) {
                mForceNotificationVisible = true
                synchronized (mStateLock) {
                    if ((mState and FLAG_PLAYING) != 0)
                        pause()
                    else
                        play()
                }
            } else if (ACTION_TOGGLE_PLAYBACK_DELAYED == action) {
//                if (mHandler!!.hasMessages(CALL_GO, Integer.valueOf(0))) {
//                    mHandler!!.removeMessages(CALL_GO, Integer.valueOf(0))
//                    val launch = Intent(this, javaClass<LibraryActivity>())
//                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    launch.setAction(Intent.ACTION_MAIN)
//                    startActivity(launch)
//                } else {
//                    mHandler!!.sendMessageDelayed(mHandler!!.obtainMessage(CALL_GO, 0, 0, Integer.valueOf(0)), 400)
//                }
            }

        }

        return Service.START_NOT_STICKY
    }

    public override fun onDestroy() {
        sInstance = null

        mLooper!!.quit()

        // clear the notification
        stopForeground(true)



        if (mMediaPlayer != null) {
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }

        if (mPreparedMediaPlayer != null) {
            mPreparedMediaPlayer!!.release()
            mPreparedMediaPlayer = null
        }


        super<Service>.onDestroy()
    }


    /**
     * Initializes the service state, loading songs saved from the disk into the
     * song timeline.

     * @return The loaded value for mState.
     */
    public fun loadState(): Int {
        var state = 0

//        try {
//            val din = DataInputStream(openFileInput(STATE_FILE))
//
//            if (din.readLong() == STATE_FILE_MAGIC && din.readInt() == STATE_VERSION) {
//                mPendingSeek = din.readInt()
//                mPendingSeekSong = din.readLong()
//                mTimeline!!.readState(din)
//                state = state or (mTimeline!!.getShuffleMode() shl SHIFT_SHUFFLE)
//                state = state or (mTimeline!!.getFinishAction() shl SHIFT_FINISH)
//            }
//
//            din.close()
//        } catch (e: EOFException) {
//            Log.w("VanillaMusic", "Failed to load state", e)
//        } catch (e: IOException) {
//            Log.w("VanillaMusic", "Failed to load state", e)
//        }


        return state
    }

    /**
     * Returns the current service state. The state comprises several individual
     * flags.
     */
    public fun getState(): Int {
        synchronized (mStateLock) {
            return mState
        }
    }

    /**
     * Modify the service state.

     * @param state Union of PlaybackService.STATE_* flags
     * *
     * @return The new state
     */
    private fun updateState(state: Int): Int {
        var state = state
        if ((state and (FLAG_NO_MEDIA or FLAG_ERROR or FLAG_EMPTY_QUEUE)) != 0
                //|| mHeadsetOnly && isSpeakerOn()
        )
            state = state and FLAG_PLAYING.inv()

        val oldState = mState
        mState = state

        if (state != oldState) {
            mHandler!!.sendMessage(mHandler!!.obtainMessage(PROCESS_STATE, oldState, state))
            mHandler!!.sendMessage(mHandler!!.obtainMessage(BROADCAST_CHANGE, state, 0))
        }

        return state
    }

    /**
     * Change the end action (e.g. repeat, random).

     * @param action The new action. One of SongTimeline.FINISH_*.
     * *
     * @return The new state after this is called.
     */
    public fun setFinishAction(action: Int): Int {
        synchronized (mStateLock) {
            return updateState(mState and MASK_FINISH.inv() or (action shl SHIFT_FINISH))
        }
    }


    /**
     * Returns the finish action for the given state.

     * @param state The PlaybackService state to process.
     * *
     * @return The finish action. One of SongTimeline.FINISH_*.
     */
    public fun finishAction(state: Int): Int {
        return (state and MASK_FINISH) shr SHIFT_FINISH
    }




    public fun createNotificationAction(): PendingIntent {
        val intent = Intent(this, javaClass<MiniPlaybackActivity>())
        return PendingIntent.getActivity(this, 0, intent, 0)
    }
    /**
     * Create a PendingIntent for use with the notification.

     * @param prefs Where to load the action preference from.
     */
    public fun createNotificationAction(prefs: SharedPreferences): PendingIntent {
        val intent = Intent(this, javaClass<MiniPlaybackActivity>())
        return PendingIntent.getActivity(this, 0, intent, 0)

//        when (Integer.parseInt(prefs.getString(PrefKeys.NOTIFICATION_ACTION, "0"))) {
//            NOT_ACTION_NEXT_SONG -> {
//                val intent = Intent(this, javaClass<PlaybackService>())
//                intent.setAction(PlaybackService.ACTION_NEXT_SONG_AUTOPLAY)
//                return PendingIntent.getService(this, 0, intent, 0)
//            }
//            NOT_ACTION_MINI_ACTIVITY -> {
//                val intent = Intent(this, javaClass<MiniPlaybackActivity>())
//                return PendingIntent.getActivity(this, 0, intent, 0)
//            }
//            else -> {
//                Log.w("VanillaMusic", "Unknown value for notification_action. Defaulting to 0.")
//                run {
//                    val intent = Intent(this, javaClass<LibraryActivity>())
//                    intent.setAction(Intent.ACTION_MAIN)
//                    return PendingIntent.getActivity(this, 0, intent, 0)
//                }
//            }
//        // fall through
//            NOT_ACTION_MAIN_ACTIVITY -> {
//                val intent = Intent(this, javaClass<LibraryActivity>())
//                intent.setAction(Intent.ACTION_MAIN)
//                return PendingIntent.getActivity(this, 0, intent, 0)
//            }
//            NOT_ACTION_FULL_ACTIVITY -> {
//                val intent = Intent(this, javaClass<FullPlaybackActivity>())
//                intent.setAction(Intent.ACTION_MAIN)
//                return PendingIntent.getActivity(this, 0, intent, 0)
//            }
//        }
    }

    /**
     * Move to the next or previous song or album in the timeline.

     * @param delta One of SongTimeline.SHIFT_*. 0 can also be passed to
     * * initialize the current song with media player, notification,
     * * broadcasts, etc.
     * *
     * @return The new current song
     */
    private fun setCurrentSong(delta: Int): Song? {
        if (mMediaPlayer == null)
            return null

        if (mMediaPlayer!!.isPlaying())
            mMediaPlayer!!.stop()

        val song = getSongFromTimeline(delta)
        mCurrentSong = song
        if (song == null || song.id == -1L || song.path == null) {
            if (MediaUtils.isSongAvailable(getContentResolver())) {
                val flag = if (finishAction(mState) == SongTimeline.FINISH_RANDOM) FLAG_ERROR else FLAG_EMPTY_QUEUE
                synchronized (mStateLock) {
                    updateState((mState or flag) and FLAG_NO_MEDIA.inv())
                }
                return null
            } else {
                // we don't have any songs : /
                synchronized (mStateLock) {
                    updateState((mState or FLAG_NO_MEDIA) and FLAG_EMPTY_QUEUE.inv())
                }
                return null
            }
        } else if ((mState and (FLAG_NO_MEDIA or FLAG_EMPTY_QUEUE)) != 0) {
            synchronized (mStateLock) {
                updateState(mState and (FLAG_EMPTY_QUEUE or FLAG_NO_MEDIA).inv())
            }
        }

        mHandler!!.removeMessages(PROCESS_SONG)

        mMediaPlayerInitialized = false
        mHandler!!.sendMessage(mHandler!!.obtainMessage(PROCESS_SONG, song))
        mHandler!!.sendMessage(mHandler!!.obtainMessage(BROADCAST_CHANGE, -1, 0, song))
        return song
    }

    /**
     * Returns the song `delta` places away from the current
     * position.

     * @see SongTimeline.getSong
     */
    public fun getSong(delta: Int): Song? {

//        if (mTimeline == null)
//            return null
//        if (delta == 0)
//            return mCurrentSong
//        return mTimeline!!.getSong(delta)
        return mCurrentSong
    }


    private fun getSongFromTimeline(delta: Int): Song? {
//        val song: Song?
//        if (delta == 0)
//            song = mTimeline!!.getSong(0)
//        else
//            song = mTimeline!!.shiftCurrentSong(delta)
//        return song
        val song = Song(0)
        song.album = "Download";
        song.title = "liveeco-0420-1-8363"
        song.path = "/mnt/sdcard/Download/liveeco-0420-1-8363.mp3"
        song.duration = 2806464


        return song
    }


    /**
     * Set a state flag.
     */
    public fun setFlag(flag: Int) {
        synchronized (mStateLock) {
            updateState(mState or flag)
        }
    }

    /**
     * Resets the idle timeout countdown. Should be called by a user action
     * has been triggered (new song chosen or playback toggled).

     * If an idle fade out is actually in progress, aborts it and resets the
     * volume.
     */
    public fun userActionTriggered() {
        mHandler!!.removeMessages(FADE_OUT)
        mHandler!!.removeMessages(IDLE_TIMEOUT)
//        if (mIdleTimeout != 0)
//            mHandler!!.sendEmptyMessageDelayed(IDLE_TIMEOUT, (mIdleTimeout * 1000).toLong())
//
//        if (mFadeOut != 1.0f) {
//            mFadeOut = 1.0f
//            refreshReplayGainValues()
//        }

        val idleStart = mIdleStart
        if (idleStart != -1L && SystemClock.elapsedRealtime() - idleStart < IDLE_GRACE_PERIOD) {
            mIdleStart = -1
            setFlag(FLAG_PLAYING)
        }
    }


    /**
     * Create a song notification. Call through the NotificationManager to
     * display it.

     * @param song The Song to display information about.
     * *
     * @param state The state. Determines whether to show paused or playing icon.
     */
    public fun createNotification(song: Song?, state: Int): Notification {
        val playing = (state and FLAG_PLAYING) != 0

        val views = RemoteViews(getPackageName(), R.layout.notification)
        val expanded = RemoteViews(getPackageName(), R.layout.notification_expanded)

        val cover = song!!.getCover(this)
        if (cover == null) {
            views.setImageViewResource(R.id.cover, R.drawable.fallback_cover)
            expanded.setImageViewResource(R.id.cover, R.drawable.fallback_cover)
        } else {
            views.setImageViewBitmap(R.id.cover, cover)
            expanded.setImageViewBitmap(R.id.cover, cover)
        }

        //val playButton = ThemeHelper.getPlayButtonResource(playing)
        val playButton = getPlayButtonResource(playing)

        views.setImageViewResource(R.id.play_pause, playButton)
        expanded.setImageViewResource(R.id.play_pause, playButton)

        val service = ComponentName(this, javaClass<PlaybackService>())

        val previous = Intent(PlaybackService.ACTION_PREVIOUS_SONG)
        previous.setComponent(service)
        expanded.setOnClickPendingIntent(R.id.previous, PendingIntent.getService(this, 0, previous, 0))

        val playPause = Intent(PlaybackService.ACTION_TOGGLE_PLAYBACK_NOTIFICATION)
        playPause.setComponent(service)
        views.setOnClickPendingIntent(R.id.play_pause, PendingIntent.getService(this, 0, playPause, 0))
        expanded.setOnClickPendingIntent(R.id.play_pause, PendingIntent.getService(this, 0, playPause, 0))

        val next = Intent(PlaybackService.ACTION_NEXT_SONG)
        next.setComponent(service)
        views.setOnClickPendingIntent(R.id.next, PendingIntent.getService(this, 0, next, 0))
        expanded.setOnClickPendingIntent(R.id.next, PendingIntent.getService(this, 0, next, 0))

        val close = Intent(PlaybackService.ACTION_CLOSE_NOTIFICATION)
        close.setComponent(service)
        views.setOnClickPendingIntent(R.id.close, PendingIntent.getService(this, 0, close, 0))
        expanded.setOnClickPendingIntent(R.id.close, PendingIntent.getService(this, 0, close, 0))

        views.setTextViewText(R.id.title, song!!.title)
        views.setTextViewText(R.id.artist, song!!.artist)
        expanded.setTextViewText(R.id.title, song!!.title)
        expanded.setTextViewText(R.id.album, song!!.album)
        expanded.setTextViewText(R.id.artist, song!!.artist)

        val notification = Notification()
        notification.contentView = views
        notification.icon = R.drawable.status_icon
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
        notification.contentIntent = mNotificationAction
        if (isOnVersionOrNewer(16)) {   // Jellybean : 16
            // expanded view is available since 4.1
            notification.bigContentView = expanded
        }
        if (lollipopOrNewer()) {
            notification.visibility = Notification.VISIBILITY_PUBLIC
        }

        if (mNotificationNag) {
            if (lollipopOrNewer()) {
                notification.priority = Notification.PRIORITY_MAX
                notification.vibrate = LongArray(0) // needed to get headsup
            } else {
                notification.tickerText = song!!.title + " - " + song!!.artist
            }
        }

        return notification
    }

    private fun getPlayButtonResource(playing: Boolean): Int {
        var playButton = 0

        if (lollipopOrNewer()) {
            // Android >= 5.0 uses the dark version of this drawable
            playButton = if (playing) R.drawable.widget_pause else R.drawable.widget_play
        } else {
            playButton = if (playing) R.drawable.pause else R.drawable.play
        }
        return playButton
    }

    private fun updateNotification() {
        if ((mForceNotificationVisible || mNotificationMode == ALWAYS || mNotificationMode == WHEN_PLAYING && (mState and FLAG_PLAYING) != 0) && mCurrentSong != null)
            mNotificationManager!!.notify(NOTIFICATION_ID, createNotification(mCurrentSong, mState))
        else
            mNotificationManager!!.cancel(NOTIFICATION_ID)
    }

    /**
     * Start playing if currently paused.

     * @return The new state after this is called.
     */
    public fun play(): Int {
        synchronized (mStateLock) {
            if ((mState and FLAG_EMPTY_QUEUE) != 0) {
                setFinishAction(4) //SongTimeline.FINISH_RANDOM
                setCurrentSong(0)
                Toast.makeText(this, R.string.random_enabling, Toast.LENGTH_SHORT).show()
            }

            val state = updateState(mState or FLAG_PLAYING)
            userActionTriggered()
            return state
        }
    }

    /**
     * Pause if currently playing.

     * @return The new state after this is called.
     */
    public fun pause(): Int {
        synchronized (mStateLock) {
            val state = updateState(mState and FLAG_PLAYING.inv())
            userActionTriggered()
            return state
        }
    }

    /**
     * If playing, pause. If paused, play.

     * @return The new state after this is called.
     */
    public fun playPause(): Int {
//        mForceNotificationVisible = false
        synchronized (mStateLock) {
            if ((mState and FLAG_PLAYING) != 0)
                return pause()
            else
                return play()
        }
    }
    /**
     * Returns a new MediaPlayer object
     */
    private fun getNewMediaPlayer(): PodMediaPlayer {
        val mp = PodMediaPlayer(this)
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
//        mp.setOnCompletionListener(this)
//        mp.setOnErrorListener(this)
        return mp
    }


    throws(IOException::class)
    public fun prepareMediaPlayer(mp: PodMediaPlayer?, path: String) {
        mp!!.setDataSource(path)
        mp!!.prepare()
        // applyReplayGain(mp)
    }

    private fun processNewState(oldState: Int, state: Int) {
        val toggled = oldState xor state

        if (((toggled and FLAG_PLAYING) != 0) && mCurrentSong != null) {
            // user requested to start playback AND we have a song selected
            if ((state and FLAG_PLAYING) != 0) {

                // We get noisy: Acquire a new AudioFX session if required
//                if (mMediaPlayerAudioFxActive == false) {
//                    mMediaPlayer.openAudioFx()
//                    mMediaPlayerAudioFxActive = true
//                }

                if (mMediaPlayerInitialized)
                    mMediaPlayer?.start()


                if (mNotificationMode != NEVER) // player on notification bar
                    startForeground(NOTIFICATION_ID, createNotification(mCurrentSong, mState))

                // for unplugging the headset
//                mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

                mHandler!!.removeMessages(ENTER_SLEEP_STATE)
//                try {
//                    if (mWakeLock != null && mWakeLock.isHeld() == false)
//                        mWakeLock.acquire()
//                } catch (e: SecurityException) {
//                    // Don't have WAKE_LOCK permission
//                }

            } else {
                if (mMediaPlayerInitialized)
                    mMediaPlayer?.pause()

                if (mNotificationMode == ALWAYS || mForceNotificationVisible) {
                    stopForeground(false)
                    mNotificationManager!!.notify(NOTIFICATION_ID, createNotification(mCurrentSong, mState))
                } else {
                    stopForeground(true)
                }

                // Delay entering deep sleep. This allows the headset
                // button to continue to function for a short period after
                // pausing and keeps the AudioFX session open
                mHandler!!.sendEmptyMessageDelayed(ENTER_SLEEP_STATE, SLEEP_STATE_DELAY.toLong())
            }


        }

        // for saved position for that song
//        if ((toggled and FLAG_NO_MEDIA) != 0 && (state and FLAG_NO_MEDIA) != 0) {
//            val song = mCurrentSong
//            if (song != null && mMediaPlayerInitialized) {
//                mPendingSeek = mMediaPlayer.getCurrentPosition()
//                mPendingSeekSong = song!!.id
//            }
//        }


    }

    private fun broadcastChange(state: Int, song: Song?, uptime: Long) {
        if (state != -1) {
            val list = sActivities
            var i = list.size()
            while (--i != -1)
                list.get(i).setState(uptime, state)
        }

        if (song != null) {
            val list = sActivities
            var i = list.size()
            while (--i != -1)
                list.get(i).setSong(uptime, song)
        }

//        updateWidgets()
//
//        if (mReadaheadEnabled)
//            triggerReadAhead()
//
//        RemoteControl.updateRemote(this, mCurrentSong, mState)
//
//        if (mStockBroadcast)
//            stockMusicBroadcast()
//        if (mScrobble)
//            scrobble()

    }

    /**
     * Return the error message set when FLAG_ERROR is set.
     */
    public fun getErrorMessage(): String? {
        return mErrorMessage
    }



    companion object{

        /**
         * The appplication-wide instance of the PlaybackService.
         */
        public var sInstance: PlaybackService? = null


        /**
         * Object used for PlaybackService startup waiting.
         */
        private val sWait = arrayOfNulls<java.lang.Object>(0)

        /**
         * Static referenced-array to PlaybackActivities, used for callbacks
         */
        private val sActivities = ArrayList<PlaybackActivity>(5)

        /**
         * If set, music will play.
         */
        public val FLAG_PLAYING: Int = 1
        /**
         * Set when there is no media available on the device.
         */
        public val FLAG_NO_MEDIA: Int = 2
        /**
         * Set when the current song is unplayable.
         */
        public val FLAG_ERROR: Int = 4
        /**
         * Set when the user needs to select songs to play.
         */
        public val FLAG_EMPTY_QUEUE: Int = 8


        //-------------------------------------------------------------------------------- Action


        /**
         * Action for startService: toggle playback on/off.
         */
        public val ACTION_TOGGLE_PLAYBACK: String = "com.namh.jidae.action.TOGGLE_PLAYBACK"
        /**
         * Action for startService: start playback if paused.
         */
        public val ACTION_PLAY: String = "com.namh.jidae.action.PLAY"
        /**
         * Action for startService: pause playback if playing.
         */
        public val ACTION_PAUSE: String = "com.namh.jidae.action.PAUSE"
        /**
         * Action for startService: toggle playback on/off.

         * Unlike [PlaybackService.ACTION_TOGGLE_PLAYBACK], the toggle does
         * not occur immediately. Instead, it is delayed so that if two of these
         * actions are received within 400 ms, the playback activity is opened
         * instead.
         */
        public val ACTION_TOGGLE_PLAYBACK_DELAYED: String = "com.namh.jidae.action.TOGGLE_PLAYBACK_DELAYED"
        /**
         * Action for startService: toggle playback on/off.

         * This works the same way as ACTION_PLAY_PAUSE but prevents the notification
         * from being hidden regardless of notification visibility settings.
         */
        public val ACTION_TOGGLE_PLAYBACK_NOTIFICATION: String = "com.namh.jidae.action.TOGGLE_PLAYBACK_NOTIFICATION"
        /**
         * Action for startService: advance to the next song.
         */
        public val ACTION_NEXT_SONG: String = "com.namh.jidae.action.NEXT_SONG"
        /**
         * Action for startService: advance to the next song.

         * Unlike [PlaybackService.ACTION_NEXT_SONG], the toggle does
         * not occur immediately. Instead, it is delayed so that if two of these
         * actions are received within 400 ms, the playback activity is opened
         * instead.
         */
        public val ACTION_NEXT_SONG_DELAYED: String = "com.namh.jidae.action.NEXT_SONG_DELAYED"
        /**
         * Action for startService: advance to the next song.

         * Like ACTION_NEXT_SONG, but starts playing automatically if paused
         * when this is called.
         */
        public val ACTION_NEXT_SONG_AUTOPLAY: String = "com.namh.jidae.action.NEXT_SONG_AUTOPLAY"
        /**
         * Action for startService: go back to the previous song.
         */
        public val ACTION_PREVIOUS_SONG: String = "com.namh.jidae.action.PREVIOUS_SONG"
        /**
         * Action for startService: go back to the previous song OR just rewind if it played for less than 5 seconds
         */
        public val ACTION_REWIND_SONG: String = "com.namh.jidae.action.REWIND_SONG"
        /**
         * Change the shuffle mode.
         */
        public val ACTION_CYCLE_SHUFFLE: String = "com.namh.jidae.CYCLE_SHUFFLE"
        /**
         * Change the repeat mode.
         */
        public val ACTION_CYCLE_REPEAT: String = "com.namh.jidae.CYCLE_REPEAT"
        /**
         * Pause music and hide the notifcation.
         */
        public val ACTION_CLOSE_NOTIFICATION: String = "com.namh.jidae.CLOSE_NOTIFICATION"



        /**
         * Return the PlaybackService instance, creating one if needed.
         */
        public platformStatic fun get(context: Context): PlaybackService? {
            if (sInstance == null) {
                context.startService(Intent(context, javaClass<PlaybackService>()))

                while (sInstance == null) {
                    try {
                        synchronized (sWait) {
                            (sWait as java.lang.Object).wait()
                        }
                    } catch (ignored: InterruptedException) {
                    }

                }
            }

            return sInstance
        }

        /**
         * Returns true if a PlaybackService instance is active.
         */
        public platformStatic fun hasInstance(): Boolean {
            return sInstance != null
        }


        /**
         * Add an Activity to the registered PlaybackActivities.

         * @param activity The Activity to be added
         */
        public platformStatic fun addActivity(activity: PlaybackActivity) {
            sActivities.add(activity)
        }

        /**
         * Remove an Activity from the registered PlaybackActivities

         * @param activity The Activity to be removed
         */
        public platformStatic fun removeActivity(activity: PlaybackActivity) {
            sActivities.remove(activity)
        }




    }


    /**
     * Returns the position of the current song in the song timeline.
     */
    public fun getTimelinePosition(): Int {
        return mTimeline!!.getPosition()
    }

    private fun processSong(song: Song?) {
        /* Save our 'current' state as the try block may set the ERROR flag (which clears the PLAYING flag */
        val playing = (mState and FLAG_PLAYING) != 0

        try {
            mMediaPlayerInitialized = false
            mMediaPlayer!!.reset()

            if (mPreparedMediaPlayer!!.isPlaying()) {
                // The prepared media player is playing as the previous song
                // reched its end 'naturally' (-> gapless)
                // We can now swap mPreparedMediaPlayer and mMediaPlayer
                val tmpPlayer = mMediaPlayer
                mMediaPlayer = mPreparedMediaPlayer
                mPreparedMediaPlayer = tmpPlayer // this was mMediaPlayer and is in reset() state
                Log.v("pod player", "Swapped media players")
            } else if (song!!.path != null) {
                prepareMediaPlayer(mMediaPlayer, song!!.path)
            }


            mMediaPlayerInitialized = true
            // Cancel any pending gapless updates and re-send them
            mHandler!!.removeMessages(GAPLESS_UPDATE)
            mHandler!!.sendEmptyMessage(GAPLESS_UPDATE)

            // When the seeking bar is used
//            if (mPendingSeek != 0 && mPendingSeekSong == song.id) {
//                mMediaPlayer!!.seekTo(mPendingSeek)
//                mPendingSeek = 0
//            }

            if ((mState and FLAG_PLAYING) != 0)
                mMediaPlayer!!.start()

            if ((mState and FLAG_ERROR) != 0) {
                mErrorMessage = null
                updateState(mState and FLAG_ERROR.inv())
            }
            mSkipBroken = 0 /* File not broken, reset skip counter */
        } catch (e: IOException) {
            mErrorMessage = getResources().getString(R.string.song_load_failed, song!!.path)
            updateState(mState or FLAG_ERROR)
            Toast.makeText(this, mErrorMessage, Toast.LENGTH_LONG).show()
            Log.e("VanillaMusic", "IOException", e)

            /* Automatically advance to next song IF we are currently playing or already did skip something
			 * This will stop after skipping 10 songs to avoid endless loops (queue full of broken stuff */
            if (mTimeline!!.isEndOfQueue() === false
                    && getSong(1) != null
                    && (playing || (mSkipBroken > 0 && mSkipBroken < 10))) {
                mSkipBroken++
                mHandler!!.sendMessageDelayed(
                        mHandler!!.obtainMessage(SKIP_BROKEN_SONG, getTimelinePosition(), 0), 1000)
            }

        }

        updateNotification()
//
//        mTimeline!!.purge()
    }



    //-------------------------------------------------------------------- Handler.Callback
    /**
     * Releases mWakeLock and closes any open AudioFx sessions
     */
    private val ENTER_SLEEP_STATE = 1
    /**
     * Run the given query and add the results to the timeline.

     * obj is the QueryTask. arg1 is the add mode (one of SongTimeline.MODE_*)
     */
    private val QUERY = 2
    /**
     * This message is sent with a delay specified by a user preference. After
     * this delay, assuming no new IDLE_TIMEOUT messages cancel it, playback
     * will be stopped.
     */
    private val IDLE_TIMEOUT = 4
    /**
     * Decrease the volume gradually over five seconds, pausing when 0 is
     * reached.

     * arg1 should be the progress in the fade as a percentage, 1-100.
     */
    private val FADE_OUT = 7
    /**
     * If arg1 is 0, calls [PlaybackService.playPause].
     * Otherwise, calls [PlaybackService.setCurrentSong] with arg1.
     */
    private val CALL_GO = 8
    private val BROADCAST_CHANGE = 10
    private val SAVE_STATE = 12
    private val PROCESS_SONG = 13
    private val PROCESS_STATE = 14
    private val SKIP_BROKEN_SONG = 15
    private val GAPLESS_UPDATE = 16

    override fun handleMessage(message: Message): Boolean {
        when (message.what) {
            PROCESS_SONG-> {
                processSong(message.obj as Song?)
            }
            PROCESS_STATE -> {
                processNewState(message.arg1, message.arg2)
            }

            BROADCAST_CHANGE->{
                broadcastChange(message.arg1, message.obj as Song?, message.getWhen())
            }


        }
        return true;
    }
}
/**
 * Return the error message set when FLAG_ERROR is set.
 */
