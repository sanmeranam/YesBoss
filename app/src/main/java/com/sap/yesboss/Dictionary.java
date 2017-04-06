package com.sap.yesboss;

import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by I326482 on 3/30/2017.
 */
public class Dictionary {
    private final String WELCOME_TEXT[]={"Good Morning","Good Afternoon","Good Evening",", How may I help You?"};
    private final String WOKE_TEXT[]={"Yes, Please tell me.","Here I am, please tell me."};

    private JSONObject scriptJSON;
    private Random random;
    private FloatingAgent context;
    private OnProcessDone processDone;

    public Dictionary(FloatingAgent context,OnProcessDone processDone){
        this.context = context;
        this.processDone = processDone;
        this.random=new Random();
        this.scriptJSON = loadScriptJSON();
    }

    public void sayWelcomeMessage(){
        TalkRes talkRes=new TalkRes();

        talkRes.isQuestion=true;
        talkRes.stage=0;
        talkRes.replyText = "Hello there, ";

        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if(timeOfDay >= 0 && timeOfDay < 12){
            talkRes.replyText+=WELCOME_TEXT[0];
        }else if(timeOfDay >= 12 && timeOfDay < 16){
            talkRes.replyText+=WELCOME_TEXT[1];
        }else if(timeOfDay >= 16 && timeOfDay < 21){
            talkRes.replyText+=WELCOME_TEXT[2];
        }

        talkRes.replyText+=WELCOME_TEXT[3];

        this.processDone.commandProcessed(talkRes);
    }

    public void sayWokeMessage(TalkReq req){
        TalkRes talkRes=new TalkRes();

        talkRes.isQuestion=true;
        talkRes.stage=req.last.stage;
        talkRes.replyText=WOKE_TEXT[random.nextInt(WOKE_TEXT.length)];
        this.processDone.commandProcessed(talkRes);
    }

    public void processCommand(TalkReq req)throws  Exception{
        TalkRes talkRes=new TalkRes();

        JSONObject lineObj = searchCommandLine(req,scriptJSON);

        if(lineObj!=null){

            if(lineObj.has("if") && !checkRequestOptions(req.last.stage+"",lineObj.getJSONArray("if"))){
                talkRes = unknown(scriptJSON,req);
            }else{
                talkRes=known(lineObj,req);
            }
        }else if(req.last.options.length()>0 && checkRequestOptions(req.text,req.last.options) && scriptJSON.has("_stage_"+req.last.stage)){
            lineObj = scriptJSON.getJSONObject("_stage_"+req.last.stage);
            talkRes=known(lineObj,req);
        }else{
            talkRes = unknown(scriptJSON,req);
        }

        this.processDone.commandProcessed(talkRes);
    }

    private TalkRes known(JSONObject lineObj,TalkReq req)throws  Exception{
        TalkRes talkRes = new TalkRes();
        if(lineObj.has("url")){
            openAmazon(req.text,lineObj.getString("url"));
        }
        talkRes.isQuestion = lineObj.has("qus");
        talkRes.exit = lineObj.has("e")?lineObj.getBoolean("e"):false;
        talkRes.stage = req.last!=null?req.last.stage:0;
        talkRes.stage = lineObj.has("stage")?lineObj.getInt("stage"):talkRes.stage;
        talkRes.replyText = talkRes.isQuestion?lineObj.getString("qus"):lineObj.getString("ans");
        talkRes.options =lineObj.has("options")?lineObj.getJSONArray("options"):new JSONArray();

        return  talkRes;
    }

    private  TalkRes unknown(JSONObject base,TalkReq req)throws  Exception{
        TalkRes talkRes = new TalkRes();
        JSONObject lineObj= base.getJSONObject("_stage_0");
        talkRes.replyText=lineObj.getString("qus");
        talkRes.isQuestion=true;
        talkRes.stage = req.last!=null?req.last.stage:0;
        return talkRes;
    }

    private boolean checkRequestOptions(String reqText,JSONArray avail)throws  Exception{
        for(int i=0;i<avail.length();i++){
            if(avail.getString(i).toLowerCase().indexOf(reqText.toLowerCase())>-1){
                return true;
            }
        }
        return false;
    }

    private JSONObject searchCommandLine(TalkReq req,JSONObject mainObj)throws Exception{
        JSONObject lineObj =null;
        Iterator<String> keys = mainObj.keys();
        String foundKey=null;
        while (keys.hasNext()){
            String sKey =keys.next();
            String[] start=sKey.split(",");
            for(String m:start){
                if(req.text.toLowerCase().indexOf(m.toLowerCase())==0){
                    foundKey =sKey;
                    break;
                }
            }
            if(foundKey!=null){
                break;
            }
        }
        if(foundKey!=null) {
            lineObj = scriptJSON.getJSONObject(foundKey);
        }
            return lineObj;

    }


    private boolean openAmazon(String searchText,String  url){
        searchText=searchText.replaceAll(" ","+");
        try {
            String sURL=url.replace("{1}",searchText.toLowerCase());
            Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(sURL));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException anfe) {

        }
        return true;
    }


    public static interface OnProcessDone{
        public void commandProcessed(TalkRes talkRes);
    }

    private JSONObject loadScriptJSON(){
        JSONObject obj =null;
        String json ="{}";
        try {
            InputStream is =context.getAssets().open("script.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        try {
            obj = new JSONObject(json);
        }catch (Exception ex){
            obj=new JSONObject();
        }
        return obj;
    }
}
