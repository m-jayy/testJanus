package in.minewave.janusvideoroom;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import in.minewave.janusvideoroom.PeerConnectionClient.PeerConnectionParameters;
import in.minewave.janusvideoroom.PeerConnectionClient.PeerConnectionEvents;

public class MainActivity extends AppCompatActivity implements JanusRTCInterface, PeerConnectionEvents, View.OnClickListener {
    private static final String TAG = "MainActivity";

    private PeerConnectionClient peerConnectionClient;
    private PeerConnectionParameters peerConnectionParameters;

    private SurfaceViewRenderer localRender;
    private VideoCapturer videoCapturer;
    private EglBase rootEglBase;
    private WebSocketChannel mWebSocketChannel;
    LinearLayout rootView;
    int count = 1;

    boolean loudSpeaker = false;

    CircleImageView btnLoud;
    CircleImageView btnMute;
    CircleImageView btnSwitchCamera;
    CircleImageView btnvideo;

    boolean muteMicro = false;

    ConstraintLayout mainConstraint;

    ConstraintLayout bottomPanel;

    LinearLayout llMain;

    LinearLayout.LayoutParams childViewParam;
    LinearLayout.LayoutParams param;

    LinearLayout Ll;
    int weight = 2;

    boolean capturingVideo = true;

    CircleImageView btnHangUp;
    HashMap<BigInteger, SurfaceViewRenderer> HMSurfaceViews;

    int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootView = (LinearLayout) findViewById(R.id.activity_main);

        if (checkAndRequestPermissions()) {
            init();
        }
    }

    public void init() {
        mWebSocketChannel = new WebSocketChannel();
        mWebSocketChannel.initConnection("wss://janus.conf.meetecho.com/ws");
        mWebSocketChannel.setDelegate(this);

        createLocalRender();

        peerConnectionParameters = new PeerConnectionParameters(false, 360, 480, 20, "H264", true, 0, "opus", false, false, false, false, false);
        peerConnectionClient = PeerConnectionClient.getInstance();
        peerConnectionClient.createPeerConnectionFactory(this, peerConnectionParameters, this);

        btnLoud = (CircleImageView) findViewById(R.id.btnLoud);
        btnLoud.setOnClickListener(this);
        btnMute = (CircleImageView) findViewById(R.id.btnMute);
        btnMute.setOnClickListener(this);
        btnSwitchCamera = (CircleImageView) findViewById(R.id.btnSwitchCamera);
        btnSwitchCamera.setOnClickListener(this);
        btnvideo = (CircleImageView) findViewById(R.id.btnvideo);
        btnvideo.setOnClickListener(this);

        Ll = (LinearLayout) findViewById(R.id.Ll);

        mainConstraint = (ConstraintLayout) findViewById(R.id.mainConstraint);
        mainConstraint.setOnClickListener(this);

        bottomPanel = (ConstraintLayout) findViewById(R.id.bottomPanel);
        bottomPanel.postDelayed(new Runnable() {
            public void run() {
                bottomPanel.setVisibility(View.GONE);
            }
        }, 3000);

        llMain = (LinearLayout) findViewById(R.id.llMain);

        btnHangUp = (CircleImageView) findViewById(R.id.btnHangUp);
        btnHangUp.setOnClickListener(this);

        HMSurfaceViews = new HashMap<BigInteger, SurfaceViewRenderer>();

        AudioManager audioManager =
                ((AudioManager) getSystemService(AUDIO_SERVICE));

        audioManager.setMicrophoneMute(muteMicro);
        audioManager.setWiredHeadsetOn(false);
        audioManager.setSpeakerphoneOn(loudSpeaker);
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }

    private boolean checkAndRequestPermissions() {
        int permissionSendMessage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        List<String> listPermissionsNeeded = new ArrayList<>();
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "Permission callback called-------");
        switch (requestCode) {
            case 1: {

                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Map & location services permission granted");
                        init();
                        // process the normal flow
                        //else any one or both the permissions are not granted
                    } else {
                        finish();
                        Toast.makeText(this, "permission Required to Continue App", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            peerConnectionClient.startVideoSource();
        } catch (Exception e) {
            e.getLocalizedMessage();
        }
    }

    private void createLocalRender() {
        localRender = (SurfaceViewRenderer) findViewById(R.id.local_video_view);
        rootEglBase = EglBase.create();
        localRender.init(rootEglBase.getEglBaseContext(), null);
        localRender.setEnableHardwareScaler(true);
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }

    private boolean captureToTexture() {
        return true;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Log.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer = null;
        if (useCamera2()) {
            Log.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            Log.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            Log.e(TAG, "Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    private void switchCamera() {

        if (videoCapturer != null) {
            if (videoCapturer instanceof CameraVideoCapturer) {
                CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
                cameraVideoCapturer.switchCamera(null);
                btnSwitchCamera.setVisibility(View.VISIBLE);

            } else {
                // Will not switch camera, video capturer is not a camera
            }
        }
    }


    private void offerPeerConnection(BigInteger handleId) {
        videoCapturer = createVideoCapturer();
        peerConnectionClient.createPeerConnection(rootEglBase.getEglBaseContext(), localRender, videoCapturer, handleId);
        peerConnectionClient.createOffer(handleId);
    }

    // interface JanusRTCInterface
    @Override
    public void onPublisherJoined(final BigInteger handleId) {
        offerPeerConnection(handleId);
    }

    @Override
    public void onPublisherRemoteJsep(BigInteger handleId, JSONObject jsep) {
        SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"));
        String sdp = jsep.optString("sdp");
        SessionDescription sessionDescription = new SessionDescription(type, sdp);
        peerConnectionClient.setRemoteDescription(handleId, sessionDescription);
    }

    @Override
    public void subscriberHandleRemoteJsep(BigInteger handleId, JSONObject jsep) {
        SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(jsep.optString("type"));
        String sdp = jsep.optString("sdp");
        SessionDescription sessionDescription = new SessionDescription(type, sdp);
        peerConnectionClient.subscriberHandleRemoteJsep(handleId, sessionDescription);
    }

    @Override
    public void onLeaving(final BigInteger handleId) {

        Log.e("aya", "leaving" + handleId.abs());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SurfaceViewRenderer lastSurface = HMSurfaceViews.get(handleId.abs());

                try {
                    lastSurface.release();
                } catch (Exception e) {
                    e.getLocalizedMessage();
                }
                try {
                    lastSurface.setBackground(getResources().getDrawable(R.drawable.surfaceview_layover));
                } catch (Exception e) {
                    e.getLocalizedMessage();
                }

            }
        });
    }

    // interface PeerConnectionClient.PeerConnectionEvents
    @Override
    public void onLocalDescription(SessionDescription sdp, BigInteger handleId) {
        Log.e(TAG, sdp.type.toString());
        mWebSocketChannel.publisherCreateOffer(handleId, sdp);

    }

    @Override
    public void onRemoteDescription(SessionDescription sdp, BigInteger handleId) {
        Log.e(TAG, sdp.type.toString());
        mWebSocketChannel.subscriberCreateAnswer(handleId, sdp);
    }

    @Override
    public void onIceCandidate(IceCandidate candidate, BigInteger handleId) {
        Log.e(TAG, "=========onIceCandidate========");
        if (candidate != null) {
            mWebSocketChannel.trickleCandidate(handleId, candidate);
        } else {
            mWebSocketChannel.trickleCandidateComplete(handleId);
        }
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {

        Log.d("dds", "1");
    }

    @Override
    public void onIceConnected() {

        Log.d("dds", "2");
    }

    @Override
    public void onIceDisconnected() {
        // call onLeaving tag
        Log.d("dds", "3");
    }

    @Override
    public void onPeerConnectionClosed() {
        Log.d("dds", "4");
    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {
        Log.d("dds", "5");
    }

    @Override
    public void onPeerConnectionError(String description) {

        Toast.makeText(this, description, Toast.LENGTH_LONG).show();
        Log.d("dds", "6");
    }

    @Override
    public void onRemoteRender(final JanusConnection connection) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //adding SurfaceViewRenderer every time when candidate join the room.

                Log.e("aya", "onRemoteRender:abs " + connection.handleId.abs());

//                Log.d("dd",connection.peerConnection.getRemoteDescription()+"");
//                remoteRender = new SurfaceViewRenderer(MainActivity.this);
//                remoteRender.init(rootEglBase.getEglBaseContext(), null);
//                LinearLayout.LayoutParams params  = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                rootView.addView(remoteRender, params);
//                ConstraintLayout parentLayout = (ConstraintLayout)findViewById(R.id.mainConstraint);
//                ConstraintSet set = new ConstraintSet();
                if(count==1)
                {
                    addIceCandidateVertically(connection, connection.handleId.abs(),true);
                    count++;
                }
//                else if(count==2)
//                {
//                    Ll.setWeightSum(2);
//                    if(localRender.getParent() != null) {
//                        ((ViewGroup)localRender.getParent()).removeView(localRender); // <- fix
//                    }
//
//                    Ll.addView(localRender);
//                    addIceCandidateVertically(connection, connection.handleId.abs());
//                    count++;
//                    weight++;
//                }
//                else if (count % 2 == 0) {
//                    llMain.setWeightSum(weight);
//                    addIceCandidateVertically(connection, connection.handleId.abs());
//                    count++;
//                    weight++;
//                }
//                else if (count % 2 == 1) {
//                    addIceCandidateHorizontally(connection, connection.handleId.abs());
//                    count++;
//                }

            }
        });
    }

    private void addIceCandidateVertically(JanusConnection connection, BigInteger senderID) {
        SurfaceViewRenderer childView = addChildView(new SurfaceViewRenderer(getApplicationContext()));
        HMSurfaceViews.put(senderID, childView);
        Ll = addLinearLayout(new LinearLayout(getApplicationContext()));

        Ll.addView(childView, childViewParam);

        llMain.addView(Ll, param);

        connection.videoTrack.addRenderer(new VideoRenderer(childView));
    }


    private void addIceCandidateVertically(JanusConnection connection, BigInteger senderID,boolean SingleCandidateCase) {
        SurfaceViewRenderer childView = addChildView(new SurfaceViewRenderer(getApplicationContext()));
        childView.setZOrderOnTop(false);
        childView.setZOrderMediaOverlay(false);
        localRender.setZOrderMediaOverlay(false);
        localRender.setZOrderOnTop(false);

        localRender.bringToFront();
        HMSurfaceViews.put(senderID, childView);



        Ll.addView(childView, childViewParam);

        connection.videoTrack.addRenderer(new VideoRenderer(childView));
    }

    private void addIceCandidateHorizontally(JanusConnection connection, BigInteger senderID) {

        SurfaceViewRenderer childView = addChildView(new SurfaceViewRenderer(getApplicationContext()));
        HMSurfaceViews.put(senderID, childView);

        Ll.setWeightSum(2);
        Ll.addView(childView, childViewParam);

        connection.videoTrack.addRenderer(new VideoRenderer(childView));
    }

    private LinearLayout addLinearLayout(LinearLayout linearLayout) {
        linearLayout.setId(View.generateViewId());
        linearLayout.setWeightSum(1);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );

        return linearLayout;
    }

    private SurfaceViewRenderer addChildView(SurfaceViewRenderer childView) {
        childView.setId(View.generateViewId());
        childView.init(rootEglBase.getEglBaseContext(), null);
        childViewParam = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        );

        return childView;
    }

    @SuppressLint("ResourceType")
    @Override
    public void onClick(View view) {
        if (view == btnLoud) {
            if (loudSpeaker == false) {
                btnLoud.setImageResource(R.drawable.audiocall_speaker_on_icon);
                loudSpeaker = true;

                MySpeakerTask myTask = new MySpeakerTask();
                myTask.execute();


            } else {
                btnLoud.setImageResource(R.drawable.audiocall_speaker_off_icon);
                loudSpeaker = false;
                MySpeakerTask myTask = new MySpeakerTask();
                myTask.execute();

            }

        } else if (view == btnMute) {

            if (muteMicro == false) {

                btnMute.setImageResource(R.drawable.audiocall_mic_off_icon);
                muteMicro = true;
                MyMuteTask myMuteTask = new MyMuteTask();
                myMuteTask.execute();
            } else {

                btnMute.setImageResource(R.drawable.audiocall_mic_on_icon);
                muteMicro = false;
                MyMuteTask myMuteTask = new MyMuteTask();
                myMuteTask.execute();
            }
        } else if (view == btnSwitchCamera) {
            switchCamera();
        } else if (view == btnvideo) {
            if (capturingVideo) {
                btnvideo.setImageResource(R.drawable.video_camera_off);
                btnSwitchCamera.setVisibility(View.GONE);

                try {
                    videoCapturer.stopCapture();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                capturingVideo = false;

            } else {
                btnvideo.setImageResource(R.drawable.video_camera);
                btnSwitchCamera.setVisibility(View.VISIBLE);
                videoCapturer.startCapture(360, 480, 20);
                capturingVideo = true;
            }


        } else if (view == mainConstraint) {
            bottomPanel.setVisibility(View.VISIBLE);
            bottomPanel.postDelayed(new Runnable() {
                public void run() {
                    bottomPanel.setVisibility(View.GONE);
                }
            }, 3000);
        } else if (view == btnHangUp) {
            peerConnectionClient.close();
            finish();

            localRender.release();
//            localRender.setAlpha((float) 0.9);


        }
    }

    class MySpeakerTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            if (loudSpeaker == true) {


                AudioManager audioManager =
                        ((AudioManager) getSystemService(AUDIO_SERVICE));

                audioManager.setWiredHeadsetOn(false);
                audioManager.setSpeakerphoneOn(true);
                audioManager.setMode(AudioManager.MODE_NORMAL);
            } else {
                AudioManager audioManager =
                        ((AudioManager) getSystemService(AUDIO_SERVICE));

                audioManager.setWiredHeadsetOn(false);
                audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(AudioManager.MODE_NORMAL);

            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
        }
    }

    class MyMuteTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            if (muteMicro == true) {

                AudioManager audioManager =
                        ((AudioManager) getSystemService(AUDIO_SERVICE));


                audioManager.setMicrophoneMute(true);

            } else {
                AudioManager audioManager =
                        ((AudioManager) getSystemService(AUDIO_SERVICE));


                audioManager.setMicrophoneMute(false);

            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
        }
    }

}
