package upr.famnit.network.messages;

import com.google.gson.annotations.SerializedName;

public class ResponsePromptMessage extends Message {
    @SerializedName("taskId")
    private int taskId;

    @SerializedName("body")
    private Body body;

    public ResponsePromptMessage(String type, int taskId, Body body) {
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

        @SerializedName("systemMesage")
        private String systemMessage;

        @SerializedName("mode")
        private String mode;

        @SerializedName("response")
        private String response;

        @SerializedName("tokenizerTime")
        private long tokenizerTime;

        @SerializedName("inferenceTime")
        private long inferenceTime;

        @SerializedName("tokensProcessed")
        private int tokensProcessed;

        @SerializedName("tokensGenerated")
        private int tokensGenerated;

        public Body(String model, String systemMessage, String mode, String response, long tokenizerTime, long inferenceTime, int tokensProcessed, int tokensGenerated) {
            this.model = model;
            this.systemMessage = systemMessage;
            this.mode = mode;
            this.response = response;
            this.tokenizerTime = tokenizerTime;
            this.inferenceTime = inferenceTime;
            this.tokensProcessed = tokensProcessed;
            this.tokensGenerated = tokensGenerated;
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

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public long getTokenizerTime() {
            return tokenizerTime;
        }

        public void setTokenizerTime(long tokenizerTime) {
            this.tokenizerTime = tokenizerTime;
        }

        public long getInferenceTime() {
            return inferenceTime;
        }

        public void setInferenceTime(long inferenceTime) {
            this.inferenceTime = inferenceTime;
        }

        public int getTokensProcessed() {
            return tokensProcessed;
        }

        public void setTokensProcessed(int tokensProcessed) {
            this.tokensProcessed = tokensProcessed;
        }

        public int getTokensGenerated() {
            return tokensGenerated;
        }

        public void setTokensGenerated(int tokensGenerated) {
            this.tokensGenerated = tokensGenerated;
        }
    }
}
