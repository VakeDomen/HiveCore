package upr.famnit.network;

import com.google.gson.*;

import java.lang.reflect.Type;

public class MessageDeserializer implements JsonDeserializer<Message> {
    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();

        switch (type) {
            case "authentication":
                return context.deserialize(jsonObject, AuthenticationMessage.class);
            case "submitPrompt":
                return context.deserialize(jsonObject, SubmitPromptMessage.class);
            case "responsePrompt":
                return context.deserialize(jsonObject, ResponsePromptMessage.class);
            case "responsePromptToken":
                return context.deserialize(jsonObject, ResponsePromptTokenMessage.class);
            case "error":
                return context.deserialize(jsonObject, ErrorMessage.class);
            case "submitEmbed":
                return context.deserialize(jsonObject, SubmitEmbedMessage.class);
            case "responseEmbed":
                return context.deserialize(jsonObject, ResponseEmbedMessage.class);
            default:
                throw new JsonParseException("Unknown type: " + type);
        }
    }
}
