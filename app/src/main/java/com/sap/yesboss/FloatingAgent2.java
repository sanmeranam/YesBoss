//package com.sap.yesboss;
//
//import android.annotation.TargetApi;
//import android.app.AlertDialog;
//import android.app.Service;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.graphics.PixelFormat;
//import android.graphics.drawable.AnimationDrawable;
//import android.media.AudioManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.CountDownTimer;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Message;
//import android.os.Messenger;
//import android.os.RemoteException;
//import android.speech.RecognitionListener;
//import android.speech.RecognizerIntent;
//import android.speech.SpeechRecognizer;
//import android.speech.tts.TextToSpeech;
//import android.speech.tts.UtteranceProgressListener;
//import android.util.Log;
//import android.view.Gravity;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import java.io.BufferedWriter;
//import java.io.IOException;
//import java.lang.ref.WeakReference;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Random;
//
//public class FloatingAgent2 extends Service implements TextToSpeech.OnInitListener, RecognitionListener {
//    private WindowManager mWindowManager;
//    private View mFloatingView;
//    private  TextToSpeech tts;
//    private TextView speacking_now;
//    private ImageView agent_in_action;
//    AnimationDrawable speakAnimation;
//    Handler mainHandler;
//    private Intent meInten;
//    private SpeechRecognizer speechRecognizer;
//    View collapsedView;
//    View expandedView;
//
//    public FloatingAgent2() {
//
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        meInten=intent;
//        return null;
//    }
//
//    public void  stopAnim(String text){
//        speak(text,false);
//        collapsedView.setVisibility(View.VISIBLE);
//        expandedView.setVisibility(View.GONE);
//        speechRecognizer.cancel();
//    }
//
//    public void  toHome(String text){
//        speak(text,false);
//        collapsedView.setVisibility(View.VISIBLE);
//        expandedView.setVisibility(View.GONE);
//        speechRecognizer.cancel();
//
//        Toast.makeText(this,"Order has been placed. Thank you.",Toast.LENGTH_LONG).show();
//
//
//        Intent startMain = new Intent(Intent.ACTION_MAIN);
//        startMain.addCategory(Intent.CATEGORY_HOME);
//        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(startMain);
//    }
//
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        Dictionary.context=this;
//        mainHandler = new Handler(this.getMainLooper());
//        tts = new TextToSpeech(this, this);
//        tts.setLanguage(Locale.ENGLISH);
//        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
//            @Override
//            public void onStart(String utteranceId) {
//
//            }
//
//            @Override
//            public void onDone(String utteranceId) {
//                stopSpeakAnim();
//            }
//
//            @Override
//            public void onError(String utteranceId) {
//
//            }
//        });
//
//        //Inflate the floating view layout we created
//        mFloatingView = LayoutInflater.from(this).inflate(R.layout.ybagent, null);
//
//        //Add the view to the window.
//        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                WindowManager.LayoutParams.TYPE_PHONE,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                PixelFormat.TRANSLUCENT);
//
//        //Specify the view position
//        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
//        params.x = 0;
//        params.y = 100;
//        //The root element of the collapsed view layout
//        collapsedView = mFloatingView.findViewById(R.id.collapse_view);
//        //The root element of the expanded view layout
//        expandedView = mFloatingView.findViewById(R.id.expanded_container);
//
//        //Add the view to the window
//        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
//        mWindowManager.addView(mFloatingView, params);
////        speacking_now =(TextView)expandedView.findViewById(R.id.speacking_now);
//        agent_in_action=(ImageView)mFloatingView.findViewById(R.id.agent_in_action);
//
//
//        //Drag and move floating view using user's touch action.
//        mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
//            private int initialX;
//            private int initialY;
//            private float initialTouchX;
//            private float initialTouchY;
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//
//                        //remember the initial position.
//                        initialX = params.x;
//                        initialY = params.y;
//
//                        //get the touch location
//                        initialTouchX = event.getRawX();
//                        initialTouchY = event.getRawY();
//                        return true;
//                    case MotionEvent.ACTION_UP:
//                        int Xdiff = (int) (event.getRawX() - initialTouchX);
//                        int Ydiff = (int) (event.getRawY() - initialTouchY);
//
//                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
//                        //So that is click event.
//                        if (Xdiff < 10 && Ydiff < 10) {
//                            if (isViewCollapsed()) {
//                                collapsedView.setVisibility(View.GONE);
//                                expandedView.setVisibility(View.VISIBLE);
//                                speak(Dictionary.welcome(),true);
//                            }
//                        }
//                        return true;
//                    case MotionEvent.ACTION_MOVE:
//                        //Calculate the X and Y coordinates of the view.
//                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
//                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
//
//                        //Update the layout with new X & Y coordinate
//                        mWindowManager.updateViewLayout(mFloatingView, params);
//                        return true;
//                }
//                return false;
//            }
//        });
//
//
//
//
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
//        speechRecognizer.setRecognitionListener(this);
//
//
//
//
//    }
//    private boolean listenAgain=false;
//
//    private void speak(String text,boolean listenAgain){
//        if(text==null) {
//            this.listenAgain=false;
//            return;
//        }
//        startSpeakAnim();
//        this.listenAgain=listenAgain;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            ttsGreater21(text);
//        } else {
//            ttsUnder20(text);
//        }
//    }
//
//    private  void startSpeakAnim(){
//        agent_in_action.clearAnimation();
//        agent_in_action.setImageResource(R.drawable.speak_text);
//        speakAnimation = (AnimationDrawable) agent_in_action.getDrawable();
//        speakAnimation.start();
//    }
//
//    private  void stopSpeakAnim(){
//        if(speakAnimation.isRunning()){
//            speakAnimation.stop();
//        }
//        try {
//            agent_in_action.setImageResource(R.drawable.speak2);
//        }catch (Exception e){
//
//        }
//
//        if(this.listenAgain){
//            startRecos();
//        }
//
//    }
//    private  void startRecos(){
//        mainHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                speechRecognizer.cancel();
//                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
//                speechRecognizer.startListening(intent);
//            }
//        });
//    }
//
//
//
//
//    @SuppressWarnings("deprecation")
//    private void ttsUnder20(String text) {
//        HashMap<String, String> map = new HashMap<>();
//        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
//        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
//    }
//
//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    private void ttsGreater21(String text) {
//        String utteranceId=this.hashCode() + "";
//        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
//    }
//
//
//    private boolean isViewCollapsed() {
//        return mFloatingView == null || mFloatingView.findViewById(R.id.collapse_view).getVisibility() == View.VISIBLE;
//
//    }
//
//
//    @Override
//    public void onInit(int status) {
//
//    }
//
//    @Override
//    public void onDestroy()
//    {
//        super.onDestroy();
//
//
//
//        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
//    }
//
//    @Override
//    public void onReadyForSpeech(Bundle params) {
//        Log.d("Speech", "Ready");
//    }
//
//    @Override
//    public void onBeginningOfSpeech() {
//        Log.d("Speech", "Begine");
//    }
//
//    @Override
//    public void onRmsChanged(float rmsdB) {
////        Log.d("Speech", rmsdB+"");
//    }
//
//    @Override
//    public void onBufferReceived(byte[] buffer) {
//        Log.d("Speech", "recive");
//    }
//
//    @Override
//    public void onEndOfSpeech() {
//        Log.d("Speech", "end");
//    }
//
//    @Override
//    public void onError(int error) {
//        Log.d("Speech", "error="+error);
//        speechRecognizer.cancel();
//        startRecos();
//    }
//
//    @Override
//    public void onResults(Bundle results) {
//        Log.d("Speech", "onResults");
//        ArrayList strlist = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//        String text=strlist.get(0).toString();
//        speak(Dictionary.listern(text),true);
//    }
//
//    @Override
//    public void onPartialResults(Bundle partialResults) {
//        ArrayList strlist = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//        for (int i = 0; i < strlist.size();i++ ) {
//            Log.d("Speech", "result Partial=" + strlist.get(i));
//        }
//    }
//
//    @Override
//    public void onEvent(int eventType, Bundle params) {
//
//    }
//}
