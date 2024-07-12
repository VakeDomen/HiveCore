package upr.famnit.network.messages;

import com.google.gson.annotations.SerializedName;

public class SubmitEmbedMessage extends Message {
    @SerializedName("taskId")
    private int taskId;

    @SerializedName("body")
    private Body body;

    public SubmitEmbedMessage(String type, int taskId, Body body) {
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

        @SerializedName("polling")
        private String polling;

        @SerializedName("data")
        private String data;

        public Body(String model, String polling, String data) {
            this.model = model;
            this.polling = polling;
            this.data = data;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getPolling() {
            return polling;
        }

        public void setPolling(String polling) {
            this.polling = polling;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}