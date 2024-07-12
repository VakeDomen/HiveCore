package upr.famnit.network;

import com.google.gson.annotations.SerializedName;
import upr.famnit.network.Message;

import java.util.List;

public class SubmitPromptMessage extends Message {
    @SerializedName("taskId")
    private int taskId;

    @SerializedName("body")
    private Body body;

    public SubmitPromptMessage(String type, int taskId, Body body) {
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
        @SerializedName("stream")
        private boolean stream;

        @SerializedName("model")
        private String model;

        @SerializedName("systemMesage")
        private String systemMessage;

        @SerializedName("mode")
        private String mode;

        @SerializedName("history")
        private List<String> history;

        @SerializedName("prompt")
        private String prompt;

        public Body(boolean stream, String model, String systemMessage, String mode, List<String> history, String prompt) {
            this.stream = stream;
            this.model = model;
            this.systemMessage = systemMessage;
            this.mode = mode;
            this.history = history;
            this.prompt = prompt;
        }

        public boolean isStream() {
            return stream;
        }

        public void setStream(boolean stream) {
            this.stream = stream;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getSystemMessage() {
            return systemMessage;
        }

        public void setSystemMessage(String systemMessage) {
            this.systemMessage = systemMessage;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public List<String> getHistory() {
            return history;
        }

        public void setHistory(List<String> history) {
            this.history = history;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
    }
}
