package com.ibm.runtimes.events.coffeeshop;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.*;

import com.ibm.runtimes.events.coffeeshop.PreparationState.State;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RawKafkaBaristaTest {

    private KafkaEventSource source = mock(KafkaEventSource.class);

    private Order order;

    @BeforeEach
    public void setupTestData() {
        order = new Order();
        order.setName("Demo-1");
        order.setOrderId("22929b18-9116-4125-8141-07855b992219");
        order.setProduct("espresso");
    }

    @Test
    public void shouldSubscribeToTopicsUsingEventSource() {
       new RawKafkaBarista(null, new SynchronousExecutor(), source, "Fred");

        verify(source).subscribeToTopic(eq("orders"), any(EventHandler.class), eq(Order.class), eq("baristas"));
        verify(source).subscribeToTopic(eq("queue"), any(EventHandler.class), eq(PreparationState.class), eq("Fred"));

    }

    @Test
    public void shouldMakeCoffee() throws InterruptedException, ExecutionException {
        EventEmitter emitter = mock(EventEmitter.class);
        RawKafkaBarista barista = new RawKafkaBarista(emitter, new SynchronousExecutor(), source, "Fred");

        barista.handleIncomingOrder(order);

        verify(emitter).sendEvent("{\"beverage\":{\"beverage\":\"espresso\",\"customer\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"preparedBy\":\"Fred\"},\"order\":{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"},\"state\":\"READY\"}");
    }

    @Test
    public void shouldOnlyPrepareOrderOnceGivenMultipleOrderMessages() throws InterruptedException, ExecutionException {
        EventEmitter emitter = mock(EventEmitter.class);
        RawKafkaBarista barista = new RawKafkaBarista(emitter, new SynchronousExecutor(), source, "Fred");

        barista.handleIncomingOrder(order);
        barista.handleIncomingOrder(order);

        verify(emitter).sendEvent("{\"beverage\":{\"beverage\":\"espresso\",\"customer\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"preparedBy\":\"Fred\"},\"order\":{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"},\"state\":\"READY\"}");
    }

    @Test
    public void shouldNotPrepareOrderIfSomeoneElsePreparedItAlready() throws InterruptedException, ExecutionException {
        EventEmitter emitter = mock(EventEmitter.class);
        RawKafkaBarista barista = new RawKafkaBarista(emitter, new SynchronousExecutor(), source, "Fred");
       
        Beverage beverage = new Beverage();
        beverage.setBeverage(order.getProduct());
        beverage.setCustomer(order.getName());
        beverage.setPreparedBy("Joe");
        beverage.setOrderId(order.getOrderId());
        
        PreparationState result = new PreparationState();
        result.setBeverage(beverage);
        result.setOrder(order);
        result.setState(State.READY);

        barista.handleOrderUpdate(result);
        barista.handleIncomingOrder(order);

        verify(emitter, never()).sendEvent("{\"beverage\":{\"beverage\":\"espresso\",\"customer\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"preparedBy\":\"Fred\"},\"order\":{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"},\"state\":\"READY\"}");
    }

    @Test
    public void shouldDitchOrderIfSomeoneElsePreparedItAfterStarting() throws InterruptedException, ExecutionException {
        EventEmitter emitter = mock(EventEmitter.class);
        RawKafkaBarista barista = new RawKafkaBarista(emitter, Executors.newSingleThreadExecutor(), source, "Fred");

        Beverage beverage = new Beverage();
        beverage.setBeverage(order.getProduct());
        beverage.setCustomer(order.getName());
        beverage.setPreparedBy("Joe");
        beverage.setOrderId(order.getOrderId());

        PreparationState result = new PreparationState();
        result.setBeverage(beverage);
        result.setOrder(order);
        result.setState(State.READY);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            barista.handleOrderUpdate(result);
        });
        barista.handleIncomingOrder(order);

        verify(emitter, never()).sendEvent("{\"beverage\":{\"beverage\":\"espresso\",\"customer\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"preparedBy\":\"Fred\"},\"order\":{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"},\"state\":\"READY\"}");
    }

    class SynchronousExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
           command.run();
        }
    }
}