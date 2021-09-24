package com.wintech.practise

import android.Manifest
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wintech.meetall.R
import java.lang.Exception
import java.lang.RuntimeException
import java.util.HashMap

class VideoChatActivity : AppCompatActivity() {
    private var channelName: String? = null
    private var user: User? = null
    var mLayoutType = LAYOUT_TYPE_DEFAULT
    var mRtcEngine: RtcEngine? = null
    private var mCallBtn: ImageView? = null
    private var mMuteBtn: ImageView? = null
    private var mSwitchVoiceBtn: ImageView? = null
    private var mGridVideoViewContainer: GridVideoViewContainer? = null
    private var isCalling = true
    private var isMuted = false
    private var isVoiceChanged = false
    private val mIsLandscape = false
    private var mSmallVideoViewDock: RelativeLayout? = null
    private var mSmallVideoViewAdapter: SmallVideoViewAdapter? = null
    private var mIsPeerToPeerMode = true
    private var mActualTarget: String? = null
    private val mUidsList: HashMap<Int, SurfaceView?> = HashMap<Int, SurfaceView?>()
    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the onJoinChannelSuccess callback.
        // This callback occurs when the local user successfully joins the channel.
        fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "User: $uid join!", Toast.LENGTH_LONG).show()
                Log.i("agora", "Join channel success, uid: " + (uid and 0xFFFFFFFFL))
                user.setAgoraUid(uid)
                val localView: SurfaceView? = mUidsList.remove(0)
                mUidsList[uid] = localView
            }
        }

        // Listen for the onFirstRemoteVideoDecoded callback.
        // This callback occurs when the first video frame of a remote user is received and decoded after the remote user successfully joins the channel.
        // You can call the setupRemoteVideo method in this callback to set up the remote video view.
        fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            runOnUiThread {
                Log.i("agora", "First remote video decoded, uid: " + (uid and 0xFFFFFFFFL))
                setupRemoteVideo(uid)
            }
        }

        // Listen for the onUserOffline callback.
        // This callback occurs when the remote user leaves the channel or drops offline.
        fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "User: $uid left the room.", Toast.LENGTH_LONG).show()
                Log.i("agora", "User offline, uid: " + (uid and 0xFFFFFFFFL))
                onRemoteUserLeft(uid)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val ab = supportActionBar
        ab?.hide()
        setContentView(R.layout.activity_video_call)
        extras
        initUI()
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            initEngineAndJoinChannel()
        }
    }

    private val extras: Unit
        private get() {
            channelName = intent.extras!!.getString("Channel")
            user = intent.extras!!.getParcelable("User")
            mIsPeerToPeerMode = intent.getBooleanExtra(MessageUtil.INTENT_EXTRA_IS_PEER_MODE, true)
            mActualTarget = intent.extras!!.getString("Actual Target")
        }

    private fun initUI() {
        mCallBtn = findViewById(R.id.start_call_end_call_btn)
        mMuteBtn = findViewById(R.id.audio_mute_audio_unmute_btn)
        mSwitchVoiceBtn = findViewById(R.id.switch_voice_btn)
        mGridVideoViewContainer = findViewById(R.id.grid_video_view_container)
        mGridVideoViewContainer.setItemEventHandler(object : OnItemClickListener() {
            fun onItemClick(view: View?, position: Int) {
                //can add single click listener logic
            }

            fun onItemLongClick(view: View?, position: Int) {
                //can add long click listener logic
            }

            fun onItemDoubleClick(view: View, position: Int) {
                onBigVideoViewDoubleClicked(view, position)
            }
        })
    }

    private fun initEngineAndJoinChannel() {
        initializeEngine()
        setupLocalVideo()
        joinChannel()
    }

    private fun initializeEngine() {
        mRtcEngine = try {
            RtcEngine.create(baseContext, getString(R.string.agora_app_id), mRtcEventHandler)
        } catch (e: Exception) {
            Log.e(TAG, Log.getStackTraceString(e))
            throw RuntimeException("""
    NEED TO check rtc sdk init fatal error
    ${Log.getStackTraceString(e)}
    """.trimIndent())
        }
    }

    private fun setupLocalVideo() {
        runOnUiThread {
            mRtcEngine.enableVideo()
            mRtcEngine.enableInEarMonitoring(true)
            mRtcEngine.setInEarMonitoringVolume(80)
            val surfaceView: SurfaceView = RtcEngine.CreateRendererView(baseContext)
            mRtcEngine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            surfaceView.setZOrderOnTop(false)
            surfaceView.setZOrderMediaOverlay(false)
            mUidsList[0] = surfaceView
            mGridVideoViewContainer.initViewContainer(this@VideoCallActivity, 0, mUidsList, mIsLandscape)
        }
    }

    private fun joinChannel() {
        // Join a channel with a token, token can be null.
        mRtcEngine.joinChannel(null, channelName, "Extra Optional Data", 0)
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode)
            return false
        }
        return true
    }

    private fun onBigVideoViewDoubleClicked(view: View, position: Int) {
        if (mUidsList.size < 2) {
            return
        }
        val user: UserStatusData = mGridVideoViewContainer.getItem(position)
        val uid: Int = if (user.mUid === 0) this.user.getAgoraUid() else user.mUid
        if (mLayoutType == LAYOUT_TYPE_DEFAULT && mUidsList.size != 1) {
            switchToSmallVideoView(uid)
        } else {
            switchToDefaultVideoView()
        }
    }

    private fun switchToSmallVideoView(bigBgUid: Int) {
        val slice: HashMap<Int, SurfaceView?> = HashMap<Int, SurfaceView>(1)
        slice[bigBgUid] = mUidsList[bigBgUid]
        val iterator: Iterator<SurfaceView?> = mUidsList.values.iterator()
        while (iterator.hasNext()) {
            val s: SurfaceView? = iterator.next()
            s.setZOrderOnTop(true)
            s.setZOrderMediaOverlay(true)
        }
        mUidsList[bigBgUid].setZOrderOnTop(false)
        mUidsList[bigBgUid].setZOrderMediaOverlay(false)
        mGridVideoViewContainer.initViewContainer(this, bigBgUid, slice, mIsLandscape)
        bindToSmallVideoView(bigBgUid)
        mLayoutType = LAYOUT_TYPE_SMALL
    }

    private fun bindToSmallVideoView(exceptUid: Int) {
        if (mSmallVideoViewDock == null) {
            val stub: ViewStub = findViewById<View>(R.id.small_video_view_dock) as ViewStub
            mSmallVideoViewDock = stub.inflate() as RelativeLayout
        }
        val twoWayVideoCall = mUidsList.size == 2
        val recycler = findViewById<View>(R.id.small_video_view_container) as RecyclerView
        var create = false
        if (mSmallVideoViewAdapter == null) {
            create = true
            mSmallVideoViewAdapter = SmallVideoViewAdapter(this, user.getAgoraUid(), exceptUid, mUidsList)
            mSmallVideoViewAdapter.setHasStableIds(true)
        }
        recycler.setHasFixedSize(true)
        if (twoWayVideoCall) {
            recycler.layoutManager = RtlLinearLayoutManager(applicationContext, RtlLinearLayoutManager.HORIZONTAL, false)
        } else {
            recycler.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.HORIZONTAL, false)
        }
        recycler.addItemDecoration(SmallVideoViewDecoration())
        recycler.adapter = mSmallVideoViewAdapter
        recycler.addOnItemTouchListener(RecyclerItemClickListener(baseContext, object : OnItemClickListener() {
            fun onItemClick(view: View?, position: Int) {}
            fun onItemLongClick(view: View?, position: Int) {}
            fun onItemDoubleClick(view: View, position: Int) {
                onSmallVideoViewDoubleClicked(view, position)
            }
        }))
        recycler.isDrawingCacheEnabled = true
        recycler.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
        if (!create) {
            mSmallVideoViewAdapter.setLocalUid(user.getAgoraUid())
            mSmallVideoViewAdapter.notifyUiChanged(mUidsList, exceptUid, null, null)
        }
        for (tempUid in mUidsList.keys) {
            if (user.getAgoraUid() !== tempUid) {
                if (tempUid == exceptUid) {
                    mRtcEngine.setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_HIGH)
                } else {
                    mRtcEngine.setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_NORANL)
                }
            }
        }
        recycler.visibility = View.VISIBLE
        mSmallVideoViewDock.setVisibility(View.VISIBLE)
    }

    private fun onSmallVideoViewDoubleClicked(view: View, position: Int) {
        switchToDefaultVideoView()
    }

    private fun onRemoteUserLeft(uid: Int) {
        removeRemoteVideo(uid)
    }

    private fun removeRemoteVideo(uid: Int) {
        runOnUiThread(Runnable {
            val target: Any = mUidsList.remove(uid) ?: return@Runnable
            switchToDefaultVideoView()
        })
    }

    private fun setupRemoteVideo(uid: Int) {
        runOnUiThread {
            val mRemoteView: SurfaceView = RtcEngine.CreateRendererView(applicationContext)
            mUidsList[uid] = mRemoteView
            mRemoteView.setZOrderOnTop(true)
            mRemoteView.setZOrderMediaOverlay(true)
            mRtcEngine.setupRemoteVideo(VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            switchToDefaultVideoView()
        }
    }

    private fun switchToDefaultVideoView() {
        mGridVideoViewContainer.initViewContainer(this@VideoCallActivity, user.getAgoraUid(), mUidsList, mIsLandscape)
        var setRemoteUserPriorityFlag = false
        mLayoutType = LAYOUT_TYPE_DEFAULT
        var sizeLimit = mUidsList.size
        if (sizeLimit > 5) {
            sizeLimit = 5
        }
        for (i in 0 until sizeLimit) {
            val uid: Int = mGridVideoViewContainer.getItem(i).mUid
            if (user.getAgoraUid() !== uid) {
                if (!setRemoteUserPriorityFlag) {
                    setRemoteUserPriorityFlag = true
                    mRtcEngine.setRemoteUserPriority(uid, Constants.USER_PRIORITY_HIGH)
                } else {
                    mRtcEngine.setRemoteUserPriority(uid, Constants.USER_PRIORITY_NORANL)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isCalling) {
            leaveChannel()
        }
        RtcEngine.destroy()
    }

    private fun leaveChannel() {
        // Leave the current channel.
        mRtcEngine.leaveChannel()
    }

    fun onCallClicked(view: View?) {
        if (isCalling) {
            //finish current call
            finishCalling()
            isCalling = false
            mCallBtn!!.setImageResource(R.drawable.btn_startcall)
            finish()
        } else {
            //start the call
            startCalling()
            isCalling = true
            mCallBtn!!.setImageResource(R.drawable.btn_endcall)
        }
    }

    private fun finishCalling() {
        leaveChannel()
        mUidsList.clear()
    }

    private fun startCalling() {
        setupLocalVideo()
        joinChannel()
    }

    fun onSwitchCameraClicked(view: View?) {
        mRtcEngine.switchCamera()
    }

    fun onLocalAudioMuteClicked(view: View?) {
        isMuted = !isMuted
        mRtcEngine.muteLocalAudioStream(isMuted)
        val res: Int = if (isMuted) R.drawable.btn_mute else R.drawable.btn_unmute
        mMuteBtn!!.setImageResource(res)
    }

    fun onSwitchVoiceClicked(view: View?) {
        if (!isVoiceChanged) {
            //start voice change to little girl, can be changed to different voices
            mRtcEngine.setLocalVoiceChanger(3)
            Toast.makeText(this, "Voice changer activate", Toast.LENGTH_SHORT).show()
        } else {
            //disable voice change
            Toast.makeText(this, "Voice back to normal", Toast.LENGTH_SHORT).show()
            mRtcEngine.setLocalVoiceReverbPreset(0)
        }
        val res: Int = if (!isVoiceChanged) R.drawable.ic_change_voice_24dp else R.drawable.ic_change_voice_normal_24dp
        mSwitchVoiceBtn!!.setImageResource(res)
        isVoiceChanged = !isVoiceChanged
    }

    fun onVideoChatClicked(view: View?) {
        jumpToMessageActivity()
    }

    private fun jumpToMessageActivity() {
        val intent = Intent(this, MessageActivity::class.java)
        intent.putExtra(INTENT_EXTRA_IS_PEER_MODE, mIsPeerToPeerMode)
        if (!mIsPeerToPeerMode) {
            intent.putExtra(MessageUtil.INTENT_EXTRA_TARGET_NAME, channelName)
        } else {
            intent.putExtra(MessageUtil.INTENT_EXTRA_TARGET_NAME, mActualTarget)
        }
        intent.putExtra(MessageUtil.INTENT_EXTRA_USER_ID, user)
        startActivity(intent)
    }

    companion object {
        const val LAYOUT_TYPE_DEFAULT = 0
        const val LAYOUT_TYPE_SMALL = 1
        private val TAG: String = VideoCallActivity::class.java.getName()
        private const val PERMISSION_REQ_ID = 22

        // Ask for Android device permissions at runtime.
        private val REQUESTED_PERMISSIONS = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}