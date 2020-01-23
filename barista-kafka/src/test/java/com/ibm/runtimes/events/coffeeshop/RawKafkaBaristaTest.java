package com.ibm.runtimes.events.coffeeshop;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import com.ibm.runtimes.events.coffeeshop.PreparationState.State;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RawKafkaBaristaTest {

    private EventSource source = mock(EventSource.class);

    private Order order;

    @BeforeEach
    public void setupTestData() {
        order = new Order();
        order.setName("Demo-1");
        order.setOrderId("22929b18-9116-4125-8141-07855b992219");
        order.setProduct("espresso");
    }

    @Test
    public void shouldSubscribeToOrdersTopicUsingEventSource() {
       new RawKafkaBarista(null, new SynchronousExecutor(), source);
        
        verify(source).subscribeToTopic(eq("orders"), any(EventHandler.class), eq(Order.class));
    }

    @Test
    public void shouldSubscribeToQueueTopicUsingEventSource() {
       new RawKafkaBarista(null, new SynchronousExecutor(), source);
        
        verify(source).subscribeToTopic(eq("queue"), any(EventHandler.class), eq(PreparationState.class));
    }

    @Test
    public void shouldMakeCoffee() throws InterruptedException, ExecutionException {
        EventEmitter emitter = mock(EventEmitter.class);
        RawKafkaBarista barista = new RawKafkaBarista(emitter, new SynchronousExecutor(), source);

        barista.handleIncomingOrder(order);

        verify(emitter).sendEvent("{\"beverage\":{\"beverage\":\"espresso\",\"customer\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"preparedBy\":\"Fred\"},\"order\":{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"},\"state\":\"READY\"}");
    }

    @Test
    public void shouldOnlyPrepareOrderOnceGivenMultipleOrderMessages() throws InterruptedException, ExecutionException {
        EventEmitter emitter = mock(EventEmitter.class);
        RawKafkaBarista barista = new RawKafkaBarista(emitter, new SynchronousExecutor(), source);

        barista.handleIncomingOrder(order);
        barista.handleIncomingOrder(order);

        verify(emitter).sendEvent("{\"beverage\":{\"beverage\":\"espresso\",\"customer\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"preparedBy\":\"Fred\"},\"order\":{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"},\"state\":\"READY\"}");
    }

    @Test
    public void shouldNotPrepareOrderIfSomeoneElsePreparedItAlready() throws InterruptedException, ExecutionException {
        EventEmitter emitter = mock(EventEmitter.class);
        RawKafkaBarista barista = new RawKafkaBarista(emitter, new SynchronousExecutor(), source);
       
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

    class SynchronousExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
           command.run();
        }
    }
}