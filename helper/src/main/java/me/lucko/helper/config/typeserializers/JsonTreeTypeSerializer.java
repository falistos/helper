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

import com.google.gson.JsonElement;

import io.leangen.geantyref.TypeToken;
import me.lucko.helper.datatree.ConfigurateDataTree;
import me.lucko.helper.datatree.DataTree;
import me.lucko.helper.datatree.GsonDataTree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public final class JsonTreeTypeSerializer implements TypeSerializer<DataTree> {
    private static final TypeToken<JsonElement> JSON_ELEMENT_TYPE = TypeToken.get(JsonElement.class);

    public static final JsonTreeTypeSerializer INSTANCE = new JsonTreeTypeSerializer();

    @Override
    public DataTree deserialize(Type type, ConfigurationNode node) throws SerializationException {
        return DataTree.from(node.get(JSON_ELEMENT_TYPE));
    }

    @Override
    public void serialize(Type type, @Nullable DataTree dataTree, ConfigurationNode node) throws SerializationException {
        if (dataTree instanceof GsonDataTree) {
            node.set(JSON_ELEMENT_TYPE, ((GsonDataTree) dataTree).getElement());
        } else if (dataTree instanceof ConfigurateDataTree) {
            node.set(((ConfigurateDataTree) dataTree).getNode());
        } else {
            throw new SerializationException("Unknown type: " + dataTree.getClass().getName());
        }
    }
}
