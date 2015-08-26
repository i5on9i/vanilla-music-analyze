/*
 * Copyright (C) 2010, 2011 Christopher Eby <kreed@kreed.org>
 * Copyright (C) 2014-2015 Adrian Ulrich <adrian@blinkenlights.ch>
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

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Process
import android.os.SystemClock
import android.support.v7.app.AppCompatActivity
import android.text.format.DateUtils
import android.view.ContextMenu
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import com.namh.jidae.*

import java.io.File

/**
 * Base activity for activities that contain playback controls. Handles
 * communication with the PlaybackService and response to state and song
 * changes.
 */
public abstract class PlaybackActivity : Activity(),
        Handler.Callback, View.OnClickListener {
    private var mUpAction: Action? = null
    private var mDownAction: Action? = null

    /**
     * A Handler running on the UI thread, in contrast with mHandler which runs
     * on a worker thread.
     */
    protected val mUiHandler: Handler = Handler(this)
    /**
     * A Handler running on a worker thread.
     */
    protected var mHandler: Handler? = null
    /**
     * The looper for the worker thread.
     */
    protected var mLooper: Looper? = null

    protected var mCoverView: CoverView? = null
    protected var mPlayPauseButton: ImageButton? = null
    protected var mShuffleButton: ImageButton? = null
    protected var mEndButton: ImageButton? = null

    protected var mState: Int = 0
    private var mLastStateEvent: Long = 0
    private var mLastSongEvent: Long = 0



    protected var mSeekBar: SeekBar? = null
    protected var mInfoTable: TableLayout? = null
    protected var mElapsedView: TextView? = null
    protected var mDurationView: TextView? = null



    override fun onCreate(state: Bundle?) {
        super<Activity>.onCreate(state)

        PlaybackService.addActivity(this)

        setVolumeControlStream(AudioManager.STREAM_MUSIC)

        val thread = HandlerThread(javaClass.getName(), Process.THREAD_PRIORITY_LOWEST)
        thread.start()

        mLooper = thread.getLooper()
        mHandler = Handler(mLooper, this)



    }

    override fun onDestroy() {
        PlaybackService.removeActivity(this)
        mLooper!!.quit()
        super<Activity>.onDestroy()
    }

    override fun onStart() {
        super<Activity>.onStart()

        if (PlaybackService.hasInstance())
            onServiceReady()
        else
            startService(Intent(this, javaClass<PlaybackService>()))

//        val prefs = PlaybackService.getSettings(this)
//        mUpAction = Action.getAction(prefs, PrefKeys.SWIPE_UP_ACTION, Action.Nothing)
//        mDownAction = Action.getAction(prefs, PrefKeys.SWIPE_DOWN_ACTION, Action.Nothing)
//
//        val window = getWindow()
//
//        if (prefs.getBoolean(PrefKeys.DISABLE_LOCKSCREEN, false))
//            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
//        else
//            window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    }

    override fun onResume() {
        super<Activity>.onResume()
        // MediaButtonReceiver.registerMediaButton(this);
        if (PlaybackService.hasInstance()) {
            val service = PlaybackService.get(this)
            service!!.userActionTriggered()
        }

    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        //		switch (keyCode) {
        //		case KeyEvent.KEYCODE_HEADSETHOOK:
        //		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
        //		case KeyEvent.KEYCODE_MEDIA_NEXT:
        //		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
        //			return MediaButtonReceiver.processKey(this, event);
        //		}

        return super<Activity>.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        //		switch (keyCode) {
        //		case KeyEvent.KEYCODE_HEADSETHOOK:
        //		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
        //		case KeyEvent.KEYCODE_MEDIA_NEXT:
        //		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
        //			return MediaButtonReceiver.processKey(this, event);
        //		}

        return super<Activity>.onKeyUp(keyCode, event)
    }

//    override fun shiftCurrentSong(delta: Int) {
//        setSong(PlaybackService.get(this).shiftCurrentSong(delta))
//    }

    public fun playPause() {
        val service = PlaybackService.get(this)
        val state = service!!.playPause()
        if ((state and PlaybackService.FLAG_ERROR) != 0)
            showToast(service.getErrorMessage(), Toast.LENGTH_LONG)
        setState(state)
    }

    override fun onClick(view: View) {
        when (view.getId()) {
        //		case R.id.next:
        //			shiftCurrentSong(SongTimeline.SHIFT_NEXT_SONG);
        //			break;
            R.id.play_pause -> playPause()
        }//		case R.id.previous:
        //			shiftCurrentSong(SongTimeline.SHIFT_PREVIOUS_SONG);
        //			break;
        //		case R.id.end_action:
        //			cycleFinishAction();
        //			break;
        //		case R.id.shuffle:
        //			cycleShuffle();
        //			break;
    }

    /**
     * Called when the PlaybackService state has changed.

     * @param state PlaybackService state
     * *
     * @param toggled The flags that have changed from the previous state
     */
    protected open fun onStateChange(state: Int, toggled: Int) {
        if ((toggled and PlaybackService.FLAG_PLAYING) != 0 && mPlayPauseButton != null) {
            mPlayPauseButton!!.setImageResource(if ((state and PlaybackService.FLAG_PLAYING) == 0) R.drawable.play else R.drawable.pause)
        }
        //		if ((toggled & PlaybackService.MASK_FINISH) != 0 && mEndButton != null) {
        //			mEndButton.setImageResource(SongTimeline.FINISH_ICONS[PlaybackService.finishAction(state)]);
        //		}
        //		if ((toggled & PlaybackService.MASK_SHUFFLE) != 0 && mShuffleButton != null) {
        //			mShuffleButton.setImageResource(SongTimeline.SHUFFLE_ICONS[PlaybackService.shuffleMode(state)]);
        //		}
    }

    protected fun setState(state: Int) {
        mLastStateEvent = SystemClock.uptimeMillis()

        if (mState != state) {
            val toggled = mState xor state
            mState = state
            runOnUiThread(object : Runnable {
                override fun run() {
                    onStateChange(state, toggled)
                }
            })
        }
    }

    /**
     * Called by PlaybackService to update the state.
     */
    public fun setState(uptime: Long, state: Int) {
        if (uptime > mLastStateEvent)
            setState(state)
    }

    /**
     * Sets up components when the PlaybackService is initialized and available to
     * interact with. Override to implement further post-initialization behavior.
     */
    protected fun onServiceReady() {
        val service = PlaybackService.get(this)
        setSong(service!!.getSong(0))
        setState(service.getState())
    }

    /**
     * Called when the current song changes.

     * @param song The new song
     */
    protected open fun onSongChange(song: Song?) {
        if (mCoverView != null)
            mCoverView!!.querySongs(PlaybackService.get(this))
    }

    protected fun setSong(song: Song?) {
        mLastSongEvent = SystemClock.uptimeMillis()
        runOnUiThread(object : Runnable {
            override fun run() {
                onSongChange(song)
            }
        })
    }

//    /**
//     * Called by FileSystem adapter to get the start folder
//     * for browsing directories
//     */
//    protected fun getFilesystemBrowseStart(): File {
//        val prefs = PlaybackService.getSettings(this)
//        val folder = prefs.getString(PrefKeys.FILESYSTEM_BROWSE_START, "")
//        val fs_start = File(if (folder == "") Environment.getExternalStorageDirectory().getAbsolutePath() else folder)
//        return fs_start
//    }

    /**
     * Called by PlaybackService to update the current song.
     */
    public fun setSong(uptime: Long, song: Song) {
        if (uptime > mLastSongEvent)
            setSong(song)
    }

    /**
     * Called by PlaybackService to update an active song (next, previous, or
     * current).
     */
    public fun replaceSong(delta: Int, song: Song) {
        if (mCoverView != null)
            mCoverView!!.setSong(delta + 1, song)
    }

    /**
     * Called when the song timeline position/size has changed.
     */
    public fun onPositionInfoChanged() {
    }

    /**
     * Called when the content of the media store has changed.
     */
    public fun onMediaChange() {
    }

    /**
     * Called when the timeline change has changed.
     */
    public fun onTimelineChanged() {
    }




    override fun handleMessage(message: Message): Boolean {
        when (message.what) {
//            MSG_NEW_PLAYLIST -> {
//                val dialog = message.obj as NewPlaylistDialog
//                if (dialog.isAccepted()) {
//                    val name = dialog.getText()
//                    val playlistId = Playlist.createPlaylist(getContentResolver(), name)
//                    val playlistTask = dialog.getPlaylistTask()
//                    playlistTask.name = name
//                    playlistTask.playlistId = playlistId
//                    mHandler.sendMessage(mHandler.obtainMessage(MSG_ADD_TO_PLAYLIST, playlistTask))
//                }
//            }
//            MSG_ADD_TO_PLAYLIST -> {
//                val playlistTask = message.obj as PlaylistTask
//                addToPlaylist(playlistTask)
//            }
//            MSG_RENAME_PLAYLIST -> {
//                val dialog = message.obj as NewPlaylistDialog
//                if (dialog.isAccepted()) {
//                    val playlistId = dialog.getPlaylistTask().playlistId
//                    Playlist.renamePlaylist(getContentResolver(), playlistId, dialog.getText())
//                }
//            }
//            MSG_DELETE -> {
//                delete(message.obj as Intent)
//            }
            else -> return false
        }
        return true
    }
//
//    /**
//     * Add a set of songs represented by the playlistTask to a playlist. Displays a
//     * Toast notifying of success.
//
//     * @param playlistTask The pending PlaylistTask to execute
//     */
//    protected fun addToPlaylist(playlistTask: PlaylistTask) {
//        var count = 0
//
//        if (playlistTask.query != null) {
//            count += Playlist.addToPlaylist(getContentResolver(), playlistTask.playlistId, playlistTask.query)
//        }
//
//        if (playlistTask.audioIds != null) {
//            count += Playlist.addToPlaylist(getContentResolver(), playlistTask.playlistId, playlistTask.audioIds)
//        }
//
//        val message = getResources().getQuantityString(R.plurals.added_to_playlist, count, count, playlistTask.name)
//        showToast(message, Toast.LENGTH_SHORT)
//    }
//
//    /**
//     * Delete the media represented by the given intent and show a Toast
//     * informing the user of this.
//
//     * @param intent An intent created with
//     * * [LibraryAdapter.createData].
//     */
//    private fun delete(intent: Intent) {
//        val type = intent.getIntExtra("type", MediaUtils.TYPE_INVALID)
//        val id = intent.getLongExtra("id", LibraryAdapter.INVALID_ID)
//        var message: String? = null
//        val res = getResources()
//
//        if (type == MediaUtils.TYPE_FILE) {
//            val file = intent.getStringExtra("file")
//            val success = MediaUtils.deleteFile(File(file))
//            if (!success) {
//                message = res.getString(R.string.delete_file_failed, file)
//            }
//        } else if (type == MediaUtils.TYPE_PLAYLIST) {
//            Playlist.deletePlaylist(getContentResolver(), id)
//        } else {
//            val count = PlaybackService.get(this).deleteMedia(type, id)
//            message = res.getQuantityString(R.plurals.deleted, count, count)
//        }
//
//        if (message == null) {
//            message = res.getString(R.string.deleted_item, intent.getStringExtra("title"))
//        }
//
//        showToast(message, Toast.LENGTH_SHORT)
//    }

    /**
     * Creates and displays a new toast message
     */
    private fun showToast(message: String?, duration: Int) {
        runOnUiThread(object : Runnable {
            override fun run() {
                Toast.makeText(getApplicationContext(), message, duration).show()
            }
        })
    }


//    /**
//     * Cycle shuffle mode.
//     */
//    public fun cycleShuffle() {
//        setState(PlaybackService.get(this).cycleShuffle())
//    }
//
//    /**
//     * Cycle the finish action.
//     */
//    public fun cycleFinishAction() {
//        setState(PlaybackService.get(this).cycleFinishAction())
//    }
//
//    /**
//     * Open the library activity.
//
//     * @param song If non-null, will open the library focused on this song.
//     */
//    public fun openLibrary(song: Song?) {
//        val intent = Intent(this, javaClass<LibraryActivity>())
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        if (song != null) {
//            intent.putExtra("albumId", song.albumId)
//            intent.putExtra("album", song.album)
//            intent.putExtra("artist", song.artist)
//        }
//        startActivity(intent)
//    }
//
//    override fun upSwipe() {
//        PlaybackService.get(this).performAction(mUpAction, this)
//    }
//
//    override fun downSwipe() {
//        PlaybackService.get(this).performAction(mDownAction, this)
//    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo) {
        //		if (view == mShuffleButton) {
        //			menu.add(GROUP_SHUFFLE, SongTimeline.SHUFFLE_NONE, 0, R.string.no_shuffle);
        //			menu.add(GROUP_SHUFFLE, SongTimeline.SHUFFLE_SONGS, 0, R.string.shuffle_songs);
        //			menu.add(GROUP_SHUFFLE, SongTimeline.SHUFFLE_CONTINUOUS, 0, R.string.shuffle_songs_continuously);
        //			menu.add(GROUP_SHUFFLE, SongTimeline.SHUFFLE_ALBUMS, 0, R.string.shuffle_albums);
        //		} else if (view == mEndButton) {
        //		    menu.add(GROUP_FINISH, SongTimeline.FINISH_STOP, 0, R.string.no_repeat);
        //			menu.add(GROUP_FINISH, SongTimeline.FINISH_REPEAT, 0, R.string.repeat);
        //			menu.add(GROUP_FINISH, SongTimeline.FINISH_REPEAT_CURRENT, 0, R.string.repeat_current_song);
        //			menu.add(GROUP_FINISH, SongTimeline.FINISH_STOP_CURRENT, 0, R.string.stop_current_song);
        //			menu.add(GROUP_FINISH, SongTimeline.FINISH_RANDOM, 0, R.string.random);
        //		}
    }

//    override fun onContextItemSelected(item: MenuItem): Boolean {
//        val group = item.getGroupId()
//        val id = item.getItemId()
//        if (group == GROUP_SHUFFLE)
//            setState(PlaybackService.get(this).setShuffleMode(id))
//        else if (group == GROUP_FINISH)
//            setState(PlaybackService.get(this).setFinishAction(id))
//        return true
//    }

    companion object {

        val MENU_SORT = 1
        val MENU_PREFS = 2
        val MENU_LIBRARY = 3
        val MENU_PLAYBACK = 5
        val MENU_SEARCH = 7
        val MENU_ENQUEUE_ALBUM = 8
        val MENU_ENQUEUE_ARTIST = 9
        val MENU_ENQUEUE_GENRE = 10
        val MENU_CLEAR_QUEUE = 11
        val MENU_SONG_FAVORITE = 12
        val MENU_SHOW_QUEUE = 13
        val MENU_SAVE_AS_PLAYLIST = 14
        val MENU_DELETE = 15
        val MENU_EMPTY_QUEUE = 16

        // namh - option menu related
        //	@Override
        //	public boolean onCreateOptionsMenu(Menu menu)
        //	{
        //		menu.add(0, MENU_PREFS, 0, R.string.settings).setIcon(R.drawable.ic_menu_preferences);
        //		return true;
        //	}
        //
        //	@Override
        //	public boolean onOptionsItemSelected(MenuItem item)
        //	{
        //		switch (item.getItemId()) {
        //		case MENU_PREFS:
        //			startActivity(new Intent(this, PreferencesActivity.class));
        //			break;
        //		case MENU_CLEAR_QUEUE:
        //			PlaybackService.get(this).clearQueue();
        //			break;
        //		default:
        //			return false;
        //		}
        //		return true;
        //	}

        /**
         * Call addToPlaylist with the results from a NewPlaylistDialog stored in
         * obj.
         */
        protected val MSG_NEW_PLAYLIST: Int = 0
        /**
         * Call renamePlaylist with the results from a NewPlaylistDialog stored in
         * obj.
         */
        protected val MSG_RENAME_PLAYLIST: Int = 1
        /**
         * Call addToPlaylist with data from the playlisttask object.
         */
        protected val MSG_ADD_TO_PLAYLIST: Int = 2

        protected val MSG_DELETE: Int = 3




        private val GROUP_SHUFFLE = 100
        private val GROUP_FINISH = 101


        /**
         * Update the seekbar progress with the current song progress. This must be
         * called on the UI Handler.
         */
        private val MSG_UPDATE_PROGRESS = 10
    }
}
