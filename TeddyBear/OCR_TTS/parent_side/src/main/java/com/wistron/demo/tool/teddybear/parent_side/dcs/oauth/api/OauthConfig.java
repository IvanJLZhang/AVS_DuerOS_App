package com.wistron.demo.tool.teddybear.parent_side.dcs.oauth.api;

/**
 * Created by ivanjlzhang on 17-9-19.
 */

public class OauthConfig {
    public static class BundleKey{
        // 在多个activity中传递AccessTokenManager的键值
        public static final String KEY_ACCESS_TOKEN = "baidu_token_manager_access_token";
        public static final String KEY_EXPIRE_TIME = "baidu_token_manager_expire_time";
    }

    public static class PrefenenceKey {
        // 持久化token信息的各种监制
        public static final String SP_ACCESS_TOKEN = "baidu_oauth_config_prop_access_token";
        public static final String SP_CREATE_TIME = "baidu_oauth_config_prop_create_time";
        public static final String SP_EXPIRE_SECONDS = "baidu_oauth_config_prop_expire_secends";
    }

}
