package io.smallrye.reactive.messaging.blocking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.WeldTestBaseWithoutTails;
import io.smallrye.reactive.messaging.blocking.beans.IncomingCompletionStageMessageBlockingBean;
import io.smallrye.reactive.messaging.blocking.beans.IncomingCompletionStagePayloadBlockingBean;
import io.smallrye.reactive.messaging.blocking.beans.IncomingCustomBlockingBean;
import io.smallrye.reactive.messaging.blocking.beans.IncomingCustomTwoBlockingBean;
import io.smallrye.reactive.messaging.blocking.beans.IncomingCustomUnorderedBlockingBean;
import io.smallrye.reactive.messaging.blocking.beans.IncomingDefaultBlockingBean;
import io.smallrye.reactive.messaging.blocking.beans.IncomingDefaultUnorderedBlockingBean;
import io.smallrye.reactive.messaging.blocking.beans.IncomingUniMessageBlockingBean;
import io.smallrye.reactive.messaging.blocking.beans.IncomingUniPayloadBlockingBean;

class BlockingSubscriberTest extends WeldTestBaseWithoutTails {

    @BeforeAll
    static void setupConfig() {
        installConfig("src/test/resources/config/worker-config.properties");
    }

    @AfterAll
    static void clear() {
        releaseConfig();
    }

    @Test
    void testIncomingBlockingWithDefaults() {
        addBeanClass(ProduceIn.class);
        addBeanClass(IncomingDefaultBlockingBean.class);
        initialize();

        IncomingDefaultBlockingBean bean = container.getBeanManager().createInstance().select(IncomingDefaultBlockingBean.class)
                .get();

        await().until(() -> bean.list().size() == 6);
        assertThat(bean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = bean.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isTrue();
        }
    }

    @Test
    void testIncomingBlockingUnordered() {
        addBeanClass(ProduceIn.class);
        addBeanClass(IncomingDefaultUnorderedBlockingBean.class);
        initialize();

        IncomingDefaultUnorderedBlockingBean bean = container.getBeanManager().createInstance()
                .select(IncomingDefaultUnorderedBlockingBean.class).get();

        await().until(() -> bean.list().size() == 6);
        assertThat(bean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = bean.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isTrue();
        }
    }

    @Test
    void testIncomingBlockingCustomPool() {
        addBeanClass(ProduceIn.class);
        addBeanClass(IncomingCustomBlockingBean.class);
        initialize();

        IncomingCustomBlockingBean bean = container.getBeanManager().createInstance().select(IncomingCustomBlockingBean.class)
                .get();

        await().until(() -> bean.list().size() == 6);
        assertThat(bean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = bean.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.size()).isLessThanOrEqualTo(2);
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("my-pool-")).isTrue();
        }
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isFalse();
        }
    }

    @Test
    void testIncomingBlockingCustomPoolUnordered() {
        addBeanClass(ProduceIn.class);
        addBeanClass(IncomingCustomUnorderedBlockingBean.class);
        initialize();

        IncomingCustomUnorderedBlockingBean bean = container.getBeanManager().createInstance()
                .select(IncomingCustomUnorderedBlockingBean.class).get();

        await().until(() -> bean.list().size() == 6);
        assertThat(bean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = bean.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.size()).isLessThanOrEqualTo(2);
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("my-pool-")).isTrue();
        }
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isFalse();
        }
    }

    @Test
    void testIncomingBlockingCustomPoolTwo() {
        addBeanClass(ProduceIn.class);
        addBeanClass(IncomingCustomTwoBlockingBean.class);
        initialize();

        IncomingCustomTwoBlockingBean bean = container.getBeanManager().createInstance()
                .select(IncomingCustomTwoBlockingBean.class).get();

        await().until(() -> bean.list().size() == 6);
        assertThat(bean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = bean.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.size()).isLessThanOrEqualTo(5);
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("another-pool-")).isTrue();
        }
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isFalse();
        }
    }

    @Test
    void testIncomingBlockingCompletionStageWithPayload() {
        addBeanClass(ProduceIn.class);
        addBeanClass(IncomingCompletionStagePayloadBlockingBean.class);
        initialize();

        IncomingCompletionStagePayloadBlockingBean bean = container.getBeanManager().createInstance()
                .select(IncomingCompletionStagePayloadBlockingBean.class).get();

        await().until(() -> bean.list().size() == 6);
        assertThat(bean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = bean.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isTrue();
        }
    }

    @Test
    void testIncomingBlockingCompletionStageWithMessage() {
        addBeanClass(ProduceIn.class);
        addBeanClass(IncomingCompletionStageMessageBlockingBean.class);
        initialize();

        IncomingCompletionStageMessageBlockingBean bean = container.getBeanManager().createInstance()
                .select(IncomingCompletionStageMessageBlockingBean.class).get();

        await().until(() -> bean.list().size() == 6);
        assertThat(bean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = bean.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isTrue();
        }
    }

    @Test
    void testIncomingBlockingUniWithPayload() {
        addBeanClass(ProduceIn.class);
        addBeanClass(IncomingUniPayloadBlockingBean.class);
        initialize();

        IncomingUniPayloadBlockingBean bean = container.getBeanManager().createInstance()
                .select(IncomingUniPayloadBlockingBean.class).get();

        await().until(() -> bean.list().size() == 6);
        assertThat(bean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = bean.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isTrue();
        }
    }

    @Test
    void testIncomingBlockingUniWithMessage() {
        addBeanClass(ProduceIn.class);
        addBeanClass(IncomingUniMessageBlockingBean.class);
        initialize();

        IncomingUniMessageBlockingBean bean = container.getBeanManager().createInstance()
                .select(IncomingUniMessageBlockingBean.class).get();

        await().until(() -> bean.list().size() == 6);
        assertThat(bean.list()).contains("a", "b", "c", "d", "e", "f");

        List<String> threadNames = bean.threads().stream().distinct().collect(Collectors.toList());
        assertThat(threadNames.contains(Thread.currentThread().getName())).isFalse();
        for (String name : threadNames) {
            assertThat(name.startsWith("vert.x-worker-thread-")).isTrue();
        }
    }

    @ApplicationScoped
    public static class ProduceIn {
        @Outgoing("in")
        public Publisher<String> produce() {
            return Multi.createFrom().items("a", "b", "c", "d", "e", "f");
        }
    }

}
