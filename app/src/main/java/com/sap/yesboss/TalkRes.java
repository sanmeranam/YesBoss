package com.sap.yesboss;

import org.json.JSONArray;

/**
 * Created by I326482 on 3/31/2017.
 */

public class TalkRes {
    public String replyText = "";
    public boolean exit =false;
    public boolean isQuestion =false;
    public int stage=0;
    public JSONArray options;
}
