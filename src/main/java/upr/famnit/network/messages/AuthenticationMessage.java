package upr.famnit.network.messages;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AuthenticationMessage extends Message {
    @SerializedName("body")
    private Body body;

    public AuthenticationMessage(String type, Body body) {
        super(type);
        this.body = body;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public static class Body {
        @SerializedName("token")
        private String token;

        @SerializedName("HW")
        private List<HW> hw;

        public Body(String token, List<HW> hw) {
            this.token = token;
            this.hw = hw;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public List<HW> getHw() {
            return hw;
        }

        public void setHw(List<HW> hw) {
            this.hw = hw;
        }

        public static class HW {
            @SerializedName("GPU_model")
            private String gpuModel;

            @SerializedName("GPU_VRAM")
            private int gpuVram;

            @SerializedName("driver")
            private String driver;

            @SerializedName("CUDA")
            private String cuda;

            public HW(String gpuModel, int gpuVram, String driver, String cuda) {
                this.gpuModel = gpuModel;
                this.gpuVram = gpuVram;
                this.driver = driver;
                this.cuda = cuda;
            }

            public String getGpuModel() {
                return gpuModel;
            }

            public void setGpuModel(String gpuModel) {
                this.gpuModel = gpuModel;
            }

            public int getGpuVram() {
                return gpuVram;
            }

            public void setGpuVram(int gpuVram) {
                this.gpuVram = gpuVram;
            }

            public String getDriver() {
                return driver;
            }

            public void setDriver(String driver) {
                this.driver = driver;
            }

            public String getCuda() {
                return cuda;
            }

            public void setCuda(String cuda) {
                this.cuda = cuda;
            }
        }
    }
}
