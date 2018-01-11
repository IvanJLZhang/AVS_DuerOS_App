package com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.message;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
/**Alert 数据对象(model)
 * Created by ivanjlzhang on 17-9-21.
 */

public class Alert {
    private final String token;
    private final SetAlertPayload.AlertType type;

    // 触发时间
    private final String scheduledTime;
    @JsonCreator
    public Alert(@JsonProperty("token") String token, @JsonProperty("type") SetAlertPayload.AlertType type,
                 @JsonProperty("scheduledTime") String scheduledTime) {
        this.token = token;
        this.type = type;
        this.scheduledTime = scheduledTime;
    }

    public String getToken() {
        return token;
    }

    public SetAlertPayload.AlertType getType() {
        return type;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    @Override
    public int hashCode() {
        return token == null ? 0 : token.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return  false;
        if(getClass() != obj.getClass())
            return  false;
        Alert other = (Alert)obj;
        if(token == null){
            if(other.token != null)
                return false;
        }else if(!token.equals(other.token))
            return  false;

        return true;
    }
}
