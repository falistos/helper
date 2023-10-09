package me.lucko.helper.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import me.lucko.helper.Schedulers;
import me.lucko.helper.scheduler.Scheduler;
import me.lucko.helper.scheduler.Task;
import me.lucko.helper.terminable.Terminable;
import me.lucko.helper.terminable.TerminableConsumer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BucketQueue<T> implements Terminable {

    private final Consumer<T> consumer;
    private final List<List<T>> partitions;
    private final boolean async;
    private final int interval;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    private Task task;

    private BucketQueue(Consumer<T> consumer, Collection<T> objects, int partitionSize, boolean async, int interval) {
        this.consumer = consumer;
        this.partitions = Lists.partition(new ArrayList<>(objects), partitionSize);
        this.async = async;
        this.interval = interval;
    }

    public static <T> Builder<T> create(Collection<T> objects) {
        Builder<T> builder = new Builder<>();
        builder.objects = objects;
        return builder;
    }

    public static Builder<Player> onlinePlayers(Predicate<Player> filter) {
        Builder<Player> builder = new Builder<>();
        builder.objects = Players.stream().filter(filter).collect(Collectors.toList());
        return builder;
    }

    public static Builder<Player> onlinePlayers() {
        return create(Players.all());
    }

    public void start() {
        if (task != null && !task.isClosed())
            throw new IllegalStateException("Queue is already running");

        if (partitions.size() == 0)
            return;

        Scheduler scheduler = async ? Schedulers.async() : Schedulers.sync();
        task = scheduler.runRepeating(task -> {
            int i = currentIndex.getAndIncrement();
            if (i >= partitions.size()) {
                close();
                return;
            }

            partitions.get(i).forEach(consumer);
        }, 0, interval);
    }

    @Override
    public void close() {
        if (task != null)
            task.close();
    }

    public static final class Builder<T> {

        private Collection<T> objects;
        private Consumer<T> consumer;
        private int partitionSize = 20;
        private TerminableConsumer terminableConsumer;
        private boolean async;
        private int interval = 1;

        private Builder() { }

        public Builder<T> consumer(Consumer<T> consumer) {
            this.consumer = consumer;
            return this;
        }

        public Builder<T> partitionSize(int size) {
            this.partitionSize = size;
            return this;
        }

        public Builder<T> async(boolean async) {
            this.async = async;
            return this;
        }

        public Builder<T> interval(int interval) {
            this.interval = interval;
            return this;
        }

        public Builder<T> bindWith(TerminableConsumer terminableConsumer) {
            this.terminableConsumer = terminableConsumer;
            return this;
        }

        public BucketQueue<T> build() {
            BucketQueue<T> bucket = new BucketQueue<>(
                    Preconditions.checkNotNull(consumer),
                    Preconditions.checkNotNull(objects),
                    partitionSize,
                    async,
                    interval);

            if (terminableConsumer != null)
                bucket.bindWith(terminableConsumer);

            return bucket;
        }

        public BucketQueue<T> start() {
            BucketQueue<T> bucket = build();
            bucket.start();
            return bucket;
        }
    }
}