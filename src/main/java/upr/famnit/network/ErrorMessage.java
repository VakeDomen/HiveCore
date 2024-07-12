package upr.famnit.network;

import com.google.gson.annotations.SerializedName;
import upr.famnit.network.Message;

public class ErrorMessage extends Message {
    @SerializedName("taskId")
    private int taskId;

    @SerializedName("body")
    private Body body;

    public ErrorMessage(String type, int taskId, Body body) {
        super(type);
        this.taskId = taskId;
        this.body = body;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public static class Body {
        @SerializedName("code")
        private int code;

        @SerializedName("message")
        private String message;

        public Body(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
