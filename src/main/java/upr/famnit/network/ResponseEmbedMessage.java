package upr.famnit.network;

import com.google.gson.annotations.SerializedName;
import upr.famnit.network.Message;

import java.util.List;

public class ResponseEmbedMessage extends Message {
    @SerializedName("taskId")
    private int taskId;

    @SerializedName("body")
    private Body body;

    public ResponseEmbedMessage(String type, int taskId, Body body) {
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

        @SerializedName("embeddingVector")
        private List<Double> embeddingVector;

        @SerializedName("tokenizerTime")
        private long tokenizerTime;

        @SerializedName("tokensProcessed")
        private int tokensProcessed;

        public Body(String model, String polling, List<Double> embeddingVector, long tokenizerTime, int tokensProcessed) {
            this.model = model;
            this.polling = polling;
            this.embeddingVector = embeddingVector;
            this.tokenizerTime = tokenizerTime;
            this.tokensProcessed = tokensProcessed;
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

        public List<Double> getEmbeddingVector() {
            return embeddingVector;
        }

        public void setEmbeddingVector(List<Double> embeddingVector) {
            this.embeddingVector = embeddingVector;
        }

        public long getTokenizerTime() {
            return tokenizerTime;
        }

        public void setTokenizerTime(long tokenizerTime) {
            this.tokenizerTime = tokenizerTime;
        }

        public int getTokensProcessed() {
            return tokensProcessed;
        }

        public void setTokensProcessed(int tokensProcessed) {
            this.tokensProcessed = tokensProcessed;
        }
    }
}
