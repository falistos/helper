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

package me.lucko.helper.config;

import com.google.gson.JsonElement;

import io.leangen.geantyref.TypeToken;
import me.lucko.helper.config.typeserializers.*;
import me.lucko.helper.datatree.DataTree;
import me.lucko.helper.gson.GsonSerializable;

import net.kyori.text.Component;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;

/**
 * Misc utilities for working with Configurate
 */
public abstract class ConfigFactory<N extends ConfigurationNode, L extends ConfigurationLoader<?>> {

    private static final ConfigFactory<ConfigurationNode, YamlConfigurationLoader> YAML = new ConfigFactory<ConfigurationNode, YamlConfigurationLoader>() {
        @Nonnull
        @Override
        public YamlConfigurationLoader loader(@Nonnull Path path) {
            YamlConfigurationLoader.Builder builder = YamlConfigurationLoader.builder()
                    .nodeStyle(NodeStyle.BLOCK)
                    .indent(2)
                    .source(() -> Files.newBufferedReader(path, StandardCharsets.UTF_8))
                    .sink(() -> Files.newBufferedWriter(path, StandardCharsets.UTF_8));

            builder.defaultOptions(builder.defaultOptions().serializers(TYPE_SERIALIZERS));
            return builder.build();
        }
    };

    private static final ConfigFactory<ConfigurationNode, GsonConfigurationLoader> GSON = new ConfigFactory<ConfigurationNode, GsonConfigurationLoader>() {
        @Nonnull
        @Override
        public GsonConfigurationLoader loader(@Nonnull Path path) {
            GsonConfigurationLoader.Builder builder = GsonConfigurationLoader.builder()
                    .indent(2)
                    .source(() -> Files.newBufferedReader(path, StandardCharsets.UTF_8))
                    .sink(() -> Files.newBufferedWriter(path, StandardCharsets.UTF_8));

            builder.defaultOptions(builder.defaultOptions().serializers(TYPE_SERIALIZERS));
            return builder.build();
        }
    };

    private static final ConfigFactory<CommentedConfigurationNode, HoconConfigurationLoader> HOCON = new ConfigFactory<CommentedConfigurationNode, HoconConfigurationLoader>() {
        @Nonnull
        @Override
        public HoconConfigurationLoader loader(@Nonnull Path path) {
            HoconConfigurationLoader.Builder builder = HoconConfigurationLoader.builder()
                    .source(() -> Files.newBufferedReader(path, StandardCharsets.UTF_8))
                    .sink(() -> Files.newBufferedWriter(path, StandardCharsets.UTF_8));

            builder.defaultOptions(builder.defaultOptions().serializers(TYPE_SERIALIZERS));
            return builder.build();
        }
    };

    private static final TypeSerializerCollection TYPE_SERIALIZERS;
    static {
        TypeSerializerCollection.Builder helperSerializers = TypeSerializerCollection.builder();
        helperSerializers.register(TypeToken.get(JsonElement.class), GsonTypeSerializer.INSTANCE);
        helperSerializers.register(TypeToken.get(GsonSerializable.class), HelperTypeSerializer.INSTANCE);
        helperSerializers.register(TypeToken.get(ConfigurationSerializable.class), BukkitTypeSerializer.INSTANCE);
        helperSerializers.register(TypeToken.get(DataTree.class), JsonTreeTypeSerializer.INSTANCE);
        helperSerializers.register(TypeToken.get(String.class), ColoredStringTypeSerializer.INSTANCE);
        helperSerializers.register(TypeToken.get(me.lucko.helper.text.Component.class), TextTypeSerializer.INSTANCE);
        helperSerializers.register(TypeToken.get(Component.class), Text3TypeSerializer.INSTANCE);

        TYPE_SERIALIZERS = helperSerializers.build();
    }

    @Nonnull
    public static TypeSerializerCollection typeSerializers() {
        return TYPE_SERIALIZERS;
    }

    @Nonnull
    public static ConfigFactory<ConfigurationNode, YamlConfigurationLoader> yaml() {
        return YAML;
    }

    @Nonnull
    public static ConfigFactory<ConfigurationNode, GsonConfigurationLoader> gson() {
        return GSON;
    }

    @Nonnull
    public static ConfigFactory<CommentedConfigurationNode, HoconConfigurationLoader> hocon() {
        return HOCON;
    }

    private ConfigFactory() {

    }

    @Nonnull
    public abstract L loader(@Nonnull Path path);

    @Nonnull
    public ConfigurationNode load(@Nonnull Path path) {
        try {
            return loader(path).load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(@Nonnull Path path, @Nonnull ConfigurationNode node) {
        try {
            loader(path).save(node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void load(@Nonnull Path path, T object) {
        try {
            L loader = loader(path);
            ObjectMapper<T> objectMapper = objectMapper(object);

            if (!Files.exists(path)) {
                // create a new empty node
                ConfigurationNode node = loader.createNode();
                // write the content of the object to the node
                objectMapper.save(object, node);
                // save the node
                loader.save(node);
            } else {
                // load the node from the file
                ConfigurationNode node = loader.load();
                // populate the config object
                objectMapper.load(node);
            }
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    @Nonnull
    public L loader(@Nonnull File file) {
        return loader(file.toPath());
    }

    @Nonnull
    public ConfigurationNode load(@Nonnull File file) {
        return load(file.toPath());
    }

    public void save(@Nonnull File file, @Nonnull ConfigurationNode node) {
        save(file.toPath(), node);
    }

    public <T> void load(@Nonnull File file, T object) {
        load(file.toPath(), object);
    }

    @Nonnull
    public static <T> ObjectMapper<T> classMapper(@Nonnull Class<T> clazz) {
        try {
            return ObjectMapper.factory().get(clazz);
        } catch (SerializationException e) {
            throw new ConfigurationException(e);
        }
    }

    @Nonnull
    public static <T> ObjectMapper<T> objectMapper(@Nonnull T object) {
        try {
            return (ObjectMapper<T>) ObjectMapper.factory().get(object.getClass());
        } catch (SerializationException e) {
            throw new ConfigurationException(e);
        }
    }

    @Nonnull
    public static <T> T generate(@Nonnull Class<T> clazz, @Nonnull ConfigurationNode node) {
        try {
            return classMapper(clazz).load(node);
        } catch (SerializationException e) {
            throw new ConfigurationException(e);
        }
    }

    @Nonnull
    public static <T> T populate(@Nonnull T object, @Nonnull ConfigurationNode node) {
        try {
            return objectMapper(object).load(node);
        } catch (SerializationException e) {
            throw new ConfigurationException(e);
        }
    }
}
