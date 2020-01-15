package com.ibm.runtimes.events.coffeeshop;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

public class EventDrivenBaristaTest {

    public void shouldListenForOrders() {
        
    }

    @Test
    public void shouldMakeCoffee() throws InterruptedException, ExecutionException {
        EventEmitter emitter = mock(EventEmitter.class);
        EventDrivenBarista barista = new EventDrivenBarista(emitter, new SynchronousExecutor());
       
        barista.handle(CoffeeEventType.ORDER, "{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"}");

        verify(emitter).sendEvent("{\"beverage\":{\"beverage\":\"espresso\",\"customer\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"preparedBy\":\"Fred\"},\"order\":{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"},\"state\":\"READY\"}");
    }

    @Test
    public void shouldOnlyPrepareOrderOnceGivenMultipleOrderMessages() throws InterruptedException, ExecutionException {
        EventEmitter emitter = mock(EventEmitter.class);
        EventDrivenBarista barista = new EventDrivenBarista(emitter, new SynchronousExecutor());
       
        barista.handle(CoffeeEventType.ORDER,
                "{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"}");
        barista.handle(CoffeeEventType.ORDER, "{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"}");

        verify(emitter).sendEvent("{\"beverage\":{\"beverage\":\"espresso\",\"customer\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"preparedBy\":\"Fred\"},\"order\":{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"},\"state\":\"READY\"}");
    }

    @Test
    public void shouldNotPrepareOrderIfSomeoneElsePreparedItAlready() throws InterruptedException, ExecutionException {
        EventEmitter emitter = mock(EventEmitter.class);
        EventDrivenBarista barista = new EventDrivenBarista(emitter, new SynchronousExecutor());
       
        barista.handle(CoffeeEventType.BEVERAGE, "{\"beverage\":{\"beverage\":\"espresso\",\"customer\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"preparedBy\":\"Joe\"},\"order\":{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"},\"state\":\"READY\"}");
        barista.handle(CoffeeEventType.ORDER, "{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"}");

        verify(emitter, never()).sendEvent("{\"beverage\":{\"beverage\":\"espresso\",\"customer\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"preparedBy\":\"Fred\"},\"order\":{\"name\":\"Demo-1\",\"orderId\":\"22929b18-9116-4125-8141-07855b992219\",\"product\":\"espresso\"},\"state\":\"READY\"}");
    }

    class SynchronousExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
           command.run();
        }
    }
}