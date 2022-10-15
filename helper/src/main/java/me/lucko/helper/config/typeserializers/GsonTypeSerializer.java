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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.leangen.geantyref.TypeToken;
import me.lucko.helper.gson.converter.GsonConverters;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public final class GsonTypeSerializer implements TypeSerializer<JsonElement> {
    private static final TypeToken<JsonElement> TYPE = TypeToken.get(JsonElement.class);

    public static final GsonTypeSerializer INSTANCE = new GsonTypeSerializer();

    private GsonTypeSerializer() {
    }

    @Override
    public JsonElement deserialize(Type type, ConfigurationNode from) throws SerializationException {
        if (from.isNull()) {
            return JsonNull.INSTANCE;
        }

        if (from.isList()) {
            List<? extends ConfigurationNode> childrenList = from.childrenList();
            JsonArray array = new JsonArray();
            for (ConfigurationNode node : childrenList) {
                array.add(node.get(TYPE));
            }
            return array;
        }

        if (from.childrenMap() != null) {
            Map<Object, ? extends ConfigurationNode> childrenMap = from.childrenMap();
            JsonObject object = new JsonObject();
            for (Map.Entry<Object, ? extends ConfigurationNode> ent : childrenMap.entrySet()) {
                object.add(ent.getKey().toString(), ent.getValue().get(TYPE));
            }
            return object;
        }

        Object val = from.raw();
        try {
            return GsonConverters.IMMUTABLE.wrap(val);
        } catch (IllegalArgumentException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void serialize(Type type, @Nullable JsonElement from, ConfigurationNode to) throws SerializationException {
        if (from.isJsonPrimitive()) {
            JsonPrimitive primitive = from.getAsJsonPrimitive();
            to.set(GsonConverters.IMMUTABLE.unwarpPrimitive(primitive));
        } else if (from.isJsonNull()) {
            to.set(null);
        } else if (from.isJsonArray()) {
            JsonArray array = from.getAsJsonArray();
            // ensure 'to' is a list node
            to.set(ImmutableList.of());
            for (JsonElement element : array) {
                serialize(TYPE.getType(), element, to.appendListNode());
            }
        } else if (from.isJsonObject()) {
            JsonObject object = from.getAsJsonObject();
            // ensure 'to' is a map node
            to.set(ImmutableMap.of());
            for (Map.Entry<String, JsonElement> ent : object.entrySet()) {
                serialize(TYPE.getType(), ent.getValue(), to.node(ent.getKey()));
            }
        } else {
            throw new SerializationException("Unknown element type: " + from.getClass());
        }
    }
}
