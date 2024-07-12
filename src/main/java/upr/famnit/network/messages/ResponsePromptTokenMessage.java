package upr.famnit.network.messages;

import com.google.gson.annotations.SerializedName;


public class ResponsePromptTokenMessage extends Message {
    @SerializedName("taskId")
    private int taskId;

    @SerializedName("body")
    private Body body;

    public ResponsePromptTokenMessage(String type, int taskId, Body body) {
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
        @SerializedName("model")
        private String model;

        @SerializedName("token")
        private String token;

        public Body(String model, String token) {
            this.model = model;
            this.token = token;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
