/*
 * This file is part of helper, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.helper.config.typeserializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import io.leangen.geantyref.TypeToken;
import me.lucko.helper.gson.GsonProvider;
import me.lucko.helper.text.Component;
import me.lucko.helper.text.serializer.ComponentSerializers;
import me.lucko.helper.text.serializer.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

@Deprecated
public class TextTypeSerializer implements TypeSerializer<Component> {
    private static final GsonComponentSerializer DELEGATE = (GsonComponentSerializer) ComponentSerializers.JSON;

    public static final TextTypeSerializer INSTANCE = new TextTypeSerializer();

    private TextTypeSerializer() {
    }

    @Override
    public Component deserialize(Type type, ConfigurationNode node) throws SerializationException {
        JsonElement json = node.get(TypeToken.get(JsonElement.class));
        return DELEGATE.deserialize(json, type, new GsonContext());
    }

    @Override
    public void serialize(Type type, @Nullable Component component, ConfigurationNode node) throws SerializationException {
        JsonElement element = DELEGATE.serialize(component, type, new GsonContext());
        node.set(TypeToken.get(JsonElement.class), element);
    }

    private static final class GsonContext implements JsonSerializationContext, JsonDeserializationContext {
        @Override
        public JsonElement serialize(Object src) {
            return GsonProvider.standard().toJsonTree(src);
        }

        @Override
        public JsonElement serialize(Object src, Type typeOfSrc) {
            return GsonProvider.standard().toJsonTree(src, typeOfSrc);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> R deserialize(JsonElement json, Type typeOfT) throws JsonParseException {
            return (R) GsonProvider.standard().fromJson(json, typeOfT);
        }
    }
}
