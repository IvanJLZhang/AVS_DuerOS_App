package com.wistron.demo.tool.teddybear.scene.baidu_stt_online;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ivanjlzhang on 17-9-7.
 */

public class RecogResult {
    private static  final int ERROR_NONE = 0;

    private String origalJson;
    private String[] resultRecognition;
    private String origalResult;
    private String sn;// 日志id，请求有问题时带上sn
    private String desc;
    private String resultType;
    private int error = -1;

    public static RecogResult parseJson(String jsonStr)
    {
        RecogResult result = new RecogResult();
        result.setOrigalJson(jsonStr);

        try {
            JSONObject json = new JSONObject(jsonStr);
            int error = json.optInt("error");
            result.setError(error);
            result.setDesc(json.optString("desc"));
            result.setResultType(json.optString("result_type"));

            if(error == ERROR_NONE){
                result.setOrigalResult(json.getString("origin_result"));
                JSONArray arr = json.optJSONArray("results_recognition");
                if(arr != null){
                    int size = arr.length();
                    String[] recogs = new String[size];
                    for (int index = 0; index < size; index++){
                        recogs[index] = arr.getString(index);
                    }
                    result.setResultRecognition(recogs);
                }
            }

        }
        catch (JSONException e){
            e.printStackTrace();
        }

        return result;
    }
    public boolean hasError(){
        return error != ERROR_NONE;
    }

    public boolean isFinalResult(){return "final_result".equals(resultType);}

    public boolean isPartialResult(){return "partial_result".equals(resultType);}


    public String getOrigalJson() {
        return origalJson;
    }

    public void setOrigalJson(String origalJson) {
        this.origalJson = origalJson;
    }


    public String[] getResultRecognition() {
        return resultRecognition;
    }

    public String getOrigalResult() {
        return origalResult;
    }

    public String getSn() {
        return sn;
    }

    public String getDesc() {
        return desc;
    }

    public String getResultType() {
        return resultType;
    }

    public int getError() {
        return error;
    }

    public void setResultRecognition(String[] resultRecognition) {
        this.resultRecognition = resultRecognition;
    }

    public void setOrigalResult(String origalResult) {
        this.origalResult = origalResult;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public void setError(int error) {
        this.error = error;
    }
}
