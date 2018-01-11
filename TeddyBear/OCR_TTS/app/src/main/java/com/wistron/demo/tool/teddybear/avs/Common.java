package com.wistron.demo.tool.teddybear.avs;

/**
 * Time：16-10-25 15:00
 * Author：bob
 */
public class Common {
    public final static String TAG = "Bob_AVS";

    //message
    public final static int MSG_SYNC_SUCCESS = 1;
    public final static int MSG_SYNC_ERROR = 2;
    public final static int MSG_REFRESH_TOKEN = 3;

    //get Access token
    public final static String ARG_GRANT_TYPE = "grant_type";
    public final static String ARG_CODE = "code";
    public final static String ARG_REDIRECT_URI = "redirect_uri";
    public final static String ARG_CLIENT_ID = "client_id";
    public final static String ARG_CODE_VERIFIER = "code_verifier";

    //refresh token
    public final static String PREF_ACCESS_TOKEN = "accessToken";
    public final static String PREF_REFRESH_TOKEN = "refreshToken";
    public final static String PREF_TOKEN_EXPIRES = "tokenTxpires";
    public static final String TOKEN_PREFERENCE_KEY = "token";


    public final static String CODE_VERIFIER = "code_verifier";
    public final static String CODE_CHALLENGE = "CodeChallenge";
    public final static String PRODUCT_DSN = "dsn";
    public final static String PRODUCT_MATEDATA = "product_metadata";
    public final static String PRODUCT_ID = "product_id";

    public final static String AUTHORIZE_INFO = "authorize_info";
    public final static String AUTHORIZE_CODE = "authorize_code";
    public final static String CLIENT_ID = "client_id";
    public final static String REDIRECT_URI = "redirect_uri";


    public final static String SYNC_STATE1 = "{\"event\":{\"payload\":{\"format" +
            "\":\"AUDIO_L16_RATE_16000_CHANNELS_1\",\"profile\":\"CLOSE_TALK\"}," +
            "\"header\":{\"dialogRequestId\":\"DialogRequestID123\",\"messageId\":\"messageId123\"," +
            "\"name\":\"Recognize\",\"namespace\":\"SpeechRecognizer\"}}," +
            "\"context\":[{\"payload\":{\"activeAlerts\":[],\"allAlerts\":[]}," +
            "\"header\":{\"name\":\"AlertsState\",\"namespace\":\"Alerts\"}}," +
            "{\"payload\":{\"playerActivity\":\"IDLE\",\"offsetInMilliseconds\":0,\"token\":\"\"}," +
            "\"header\":{\"name\":\"PlaybackState\",\"namespace\":\"AudioPlayer\"}}," +
            "{\"payload\":{\"muted\":false,\"volume\":0},\"header\":{\"name\":\"VolumeState\"," +
            "\"namespace\":\"Speaker\"}},{\"payload\":{\"playerActivity\":\"FINISHED\"," +
            "\"offsetInMilliseconds\":0,\"token\":\"\"},\"header\":{\"name\":\"SpeechState\"," +
            "\"namespace\":\"SpeechSynthesizer\"}}]}";

    public final static String SYNC_STATE = "{\n" +
            "           \"context\":\n" +
            "      [{\n" +
            "           \"header\":       {                                                            " +
            "\"namespace\":    \"AudioPlayer\",                                                            " +
            " \"name\": \"PlaybackState\"                                     },                           " +
            "         \"payload\":      {                                                             " +
            "\"token\":        \"\",                                                             " +
            "\"offsetInMilliseconds\": 0,                                                             " +
            "\"playerActivity\":       \"IDLE\"                                    }                       " +
            "  },\n" +
            "      {\n" +
            "           \"header\":       {                                                             " +
            "\"namespace\":    \"Alerts\",                                                            " +
            "\"name\": \"AlertsState\"                                                    },               " +
            "                       \"payload\":      {                                                    " +
            "            \"allAlerts\":    [{                                                              " +
            "                          \"token\":        \"\",                                     " +
            "                                                   \"type\": \"ALARM\",                       " +
            "                                                                  \"scheduledTime\":        " +
            "\"2017-01-16T11:34:51+00:00\"                                                                 " +
            "}],                                                                \"activeAlerts\": []       " +
            "                                 }                          },                         {      " +
            "                             \"header\":       {                                              " +
            "                \"namespace\":    \"Speaker\",                                                " +
            "              \"name\": \"VolumeState\"                                  },                   " +
            "               \"payload\":      {                                                            " +
            "    \"volume\":       25,                                                               " +
            "\"muted\":        false                                  }                      },            " +
            "          {                                    \"header\":       {                            " +
            "                                \"namespace\":    \"SpeechSynthesizer\",                      " +
            "                                      \"name\": \"SpeechState\"                               " +
            "     },                                   \"payload\":      {                                 " +
            "                          \"token\":        \"\",                                     " +
            "                      \"offsetInMilliseconds\": 0,                                            " +
            "               \"playerActivity\":       \"FINISHED\"                                    }    " +
            "  }],      \"event\":        {                                   \"header\":       {          " +
            "                                                  \"namespace\":    \"System\",               " +
            "                                             \"name\": \"SynchronizeState\",                  " +
            "                                          \"messageId\":    \"messageId-100\"                 " +
            "                  },                                   \"payload\":      {\n" +
            "        }      } }";
}
