package upr.famnit.network;

import com.google.gson.*;
import upr.famnit.network.messages.*;

import java.lang.reflect.Type;

public class MessageDeserializer implements JsonDeserializer<Message> {
    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();

        return switch (type) {
            case "Authentication" -> context.deserialize(jsonObject, AuthenticationMessage.class);
            case "SubmitPrompt" -> context.deserialize(jsonObject, SubmitPromptMessage.class);
            case "ResponsePrompt" -> context.deserialize(jsonObject, ResponsePromptMessage.class);
            case "ResponsePromptToken" -> context.deserialize(jsonObject, ResponsePromptTokenMessage.class);
            case "Error" -> context.deserialize(jsonObject, ErrorMessage.class);
            case "SubmitEmbed" -> context.deserialize(jsonObject, SubmitEmbedMessage.class);
            case "ResponseEmbed" -> context.deserialize(jsonObject, ResponseEmbedMessage.class);
            default -> null;
        };
    }
}
