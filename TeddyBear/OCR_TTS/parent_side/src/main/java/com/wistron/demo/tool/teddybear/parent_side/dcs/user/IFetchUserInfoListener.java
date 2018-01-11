package com.wistron.demo.tool.teddybear.parent_side.dcs.user;

/**
 * Created by ivanjlzhang on 17-9-28.
 */

public interface IFetchUserInfoListener {
    void OnSuccess(String uname);

    void OnFailed(String errro);
}
