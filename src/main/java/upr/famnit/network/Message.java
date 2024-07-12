package upr.famnit.network;
import com.google.gson.annotations.SerializedName;

public class Message {

    @SerializedName("type")
    private String type;

    public Message(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
