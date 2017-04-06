package com.sap.yesboss;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by I326482 on 3/31/2017.
 */

public class FloatingAgent extends Service implements TextToSpeech.OnInitListener,RecognitionListener,Dictionary.OnProcessDone{

    private View mCollapseView;
    private View mExpandView;
    private View mFloatingView;
    private View mListenView;
    private TextView mListenText;
    private ImageView mActiveImage;

    private Context mContext;
    private WindowManager mWindowManager;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private AnimationDrawable speakAnimation;
    private AnimationDrawable blinkAnimation;
    private Handler mainHandler;
    private Dictionary dictionary;


    private static enum STATE{INIT,READY,LISTENING,ERROR, DONE, STOPPED};
    private STATE LAST_STATE=STATE.INIT;
    private TalkRes lestTalkRes;
    private TalkReq lestTalkReq;

    @Override
    public void onCreate() {
        super.onCreate();

        mainHandler = new Handler(this.getMainLooper());
        dictionary = new Dictionary(this,this);

        initTextToSpeach();
        initSpeachToText();
        initView();
    }

    private void initView(){
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.ybagent, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;
        mCollapseView = mFloatingView.findViewById(R.id.collapse_view);
        mExpandView = mFloatingView.findViewById(R.id.expanded_container);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);
        mActiveImage =(ImageView)mFloatingView.findViewById(R.id.agent_in_action);
        mListenView =mFloatingView.findViewById(R.id.listen_layout);
        mListenText =(TextView)mFloatingView.findViewById(R.id.listening_now);

        mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        if (Xdiff < 10 && Ydiff < 10) {
                            boolean isCollapsed = mFloatingView == null || mFloatingView.findViewById(R.id.collapse_view).getVisibility() == View.VISIBLE;

                            if (isCollapsed) {
                                showAgent();
                            }else{
                                tappedAgent();
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void initTextToSpeach(){
        tts = new TextToSpeech(this, this);
        tts.setLanguage(Locale.ENGLISH);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                stopBlinkAnim();
                startSpeakAnim();
            }

            @Override
            public void onDone(String utteranceId) {
                stopSpeakAnim();
                startBlinkAnim();
                if(lestTalkRes!=null && lestTalkRes.isQuestion){
                    startListen();
                }
            }

            @Override
            public void onError(String utteranceId) {
                stopSpeakAnim();
                startBlinkAnim();
            }
        });
    }

    private void initSpeachToText(){
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
    }

    @Override
    public void commandProcessed(TalkRes talkRes) {
        lestTalkRes = talkRes;

        startSpeak(talkRes.replyText);

        if(lestTalkRes.exit){
            lestTalkRes=null;
            lestTalkReq = null;
            hideAgent();
            SwitchToHome();
        }
    }

    public void SwitchToHome(){
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    private void tappedAgent(){
        TalkReq talkReq=new TalkReq();
        talkReq.last=lestTalkRes;
        dictionary.sayWokeMessage(talkReq);
    }

    private void showAgent(){
        mCollapseView.setVisibility(View.GONE);
        mExpandView.setVisibility(View.VISIBLE);
        dictionary.sayWelcomeMessage();
    }

    private void hideAgent(){
        mCollapseView.setVisibility(View.VISIBLE);
        mExpandView.setVisibility(View.GONE);
        lestTalkRes = null;
        lestTalkReq = null;
    }

    private void startSpeakAnim(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                mActiveImage.clearAnimation();
                mActiveImage.setImageResource(R.drawable.speak_text);
                speakAnimation = (AnimationDrawable) mActiveImage.getDrawable();
                speakAnimation.start();
            }
        });

    }

    private void stopSpeakAnim(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if(speakAnimation!=null && speakAnimation.isRunning()){
                    speakAnimation.stop();
                    mActiveImage.clearAnimation();
                    mActiveImage.setImageResource(R.drawable.speak_1);
                }
            }
        });

    }

    private void startBlinkAnim(){
        mainHandler.post(new Runnable() {
             @Override
             public void run() {
                 mActiveImage.clearAnimation();
                 mActiveImage.setImageResource(R.drawable.blink);
                 blinkAnimation = (AnimationDrawable) mActiveImage.getDrawable();
                 blinkAnimation.start();
             }
         });

    }

    private void stopBlinkAnim(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (blinkAnimation != null && blinkAnimation.isRunning()) {
                    blinkAnimation.stop();
                    mActiveImage.clearAnimation();
                    mActiveImage.setImageResource(R.drawable.speak_1);
                }
            }
        });
    }

    private void startListen(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if(LAST_STATE.equals(STATE.ERROR)){
                    speechRecognizer.stopListening();
                    speechRecognizer.cancel();
                }else if(LAST_STATE.equals(STATE.INIT)) {
                    speechRecognizer.setRecognitionListener(FloatingAgent.this);
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                    speechRecognizer.startListening(intent);
                }
            }
        });
    }

    private void stopListen(){
        speechRecognizer.stopListening();
        speechRecognizer.cancel();
    }

    private void startSpeak(String text){
        mListenView.setVisibility(View.GONE);
        if(text==null || text.trim().length()==0)
            return;

        if(tts.isSpeaking())
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsGreater21(text);
        } else {
            ttsUnder20(text);
        }
    }

    private void stopSpeak(){
        if(tts.isSpeaking()){
            tts.stop();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onInit(int status) {
        LAST_STATE=STATE.INIT;
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        LAST_STATE=STATE.READY;
    }

    @Override
    public void onBeginningOfSpeech() {
        LAST_STATE=STATE.LISTENING;
        mListenView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        LAST_STATE=STATE.STOPPED;
    }

    @Override
    public void onError(int error) {
        LAST_STATE = STATE.ERROR;

        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                break;
            case SpeechRecognizer.ERROR_SERVER:
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                break;
            default:
                break;
        }

        mListenText.setText("ERROR");
    }

    @Override
    public void onResults(Bundle bundle) {
        LAST_STATE = STATE.INIT;
        final ArrayList<String> matches =bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size()>0) {
           String mSearchQuery = matches.get(0);
            mListenText.setText(mSearchQuery);


            TalkReq talkReq=new TalkReq();
            talkReq.last = lestTalkRes;
            talkReq.text = mSearchQuery;

            try {
                dictionary.processCommand(talkReq);
            }catch (Exception e){}
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {
        final ArrayList<String> matches =bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size()>0) {
            String mSearchQuery = matches.get(0);
            mListenText.setText(mSearchQuery);
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId=this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }
}
