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
            case "authentication" -> context.deserialize(jsonObject, AuthenticationMessage.class);
            case "submitPrompt" -> context.deserialize(jsonObject, SubmitPromptMessage.class);
            case "responsePrompt" -> context.deserialize(jsonObject, ResponsePromptMessage.class);
            case "responsePromptToken" -> context.deserialize(jsonObject, ResponsePromptTokenMessage.class);
            case "error" -> context.deserialize(jsonObject, ErrorMessage.class);
            case "submitEmbed" -> context.deserialize(jsonObject, SubmitEmbedMessage.class);
            case "responseEmbed" -> context.deserialize(jsonObject, ResponseEmbedMessage.class);
            default -> throw new JsonParseException("Unknown type: " + type);
        };
    }
}
