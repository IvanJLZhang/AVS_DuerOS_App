package com.wistron.demo.tool.teddybear.parent_side.dcs.user;

import com.wistron.demo.tool.teddybear.parent_side.ParentSideApplication;
import com.wistron.demo.tool.teddybear.parent_side.dcs.util.PreferenceUtil;

/**
 * Created by ivanjlzhang on 17-9-28.
 */

public class UserPreferenceUtil extends PreferenceUtil {

    public final static String USER_CONFIG = "dcs_user_info";

    public static String getuName() {
        return (String) get(ParentSideApplication.getContext(), USER_CONFIG, "uName", "");
    }

    public static void setuName(String uName) {
        put(ParentSideApplication.getContext(), USER_CONFIG, "uName", uName);
    }

    public static void setuId(String uId){
        put(ParentSideApplication.getContext(), USER_CONFIG, "uId", uId);
    }

    public static String getUid(){
        return (String) get(ParentSideApplication.getContext(), USER_CONFIG, "uId", "");
    }

    public static void ClearLoginInfo(){
        clear(ParentSideApplication.getContext(), USER_CONFIG);
    }
}
