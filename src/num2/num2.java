package num2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class OrderProcessor {
    private final BlockingQueue<String> orderQueue;
    private final ReentrantLock lock;
    private final AtomicInteger processedOrders;

    public OrderProcessor() {
        this.orderQueue = new LinkedBlockingQueue<>();
        this.lock = new ReentrantLock();
        this.processedOrders = new AtomicInteger(0);
    }

    public void addOrder(String order) {
        lock.lock();
        try {
            orderQueue.offer(order);
            System.out.println("Добавлен заказ: " + order);
        } finally {
            lock.unlock();
        }
    }

    public String processOrder() throws InterruptedException {
        String order = orderQueue.poll(1, TimeUnit.SECONDS);
        if (order != null) {
            Thread.sleep(1000);
            processedOrders.incrementAndGet();
            return order;
        }
        return null;
    }

    public int getProcessedOrdersCount() {
        return processedOrders.get();
    }
}

class OrderAdder implements Runnable {
    private final OrderProcessor orderProcessor;
    private final CountDownLatch latch;

    public OrderAdder(OrderProcessor orderProcessor, CountDownLatch latch) {
        this.orderProcessor = orderProcessor;
        this.latch = latch;
    }

    @Override
    public void run() {
        for (int i = 1; i <= 10; i++) {
            String order = "Заказ #" + i;
            orderProcessor.addOrder(order);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        latch.countDown();
    }
}

class OrderHandler implements Callable<String> {
    private final OrderProcessor orderProcessor;
    private final CountDownLatch latch;

    public OrderHandler(OrderProcessor orderProcessor, CountDownLatch latch) {
        this.orderProcessor = orderProcessor;
        this.latch = latch;
    }

    @Override
    public String call() throws InterruptedException {
        latch.await();
        String processedOrder = orderProcessor.processOrder();
        if (processedOrder != null) {
            System.out.println("Обработан " + processedOrder);
            return processedOrder;
        }
        return null;
    }
}

class ECommerceOrderSystem {
    public static void main(String[] args) {
        OrderProcessor orderProcessor = new OrderProcessor();
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        CountDownLatch latch = new CountDownLatch(1);

        Runnable orderAdder = new OrderAdder(orderProcessor, latch);

        executorService.submit(orderAdder);

        List<Future<String>> futures = new ArrayList<>();


        for (int i = 0; i < 10; i++) {
            Future<String> future = executorService.submit(new OrderHandler(orderProcessor, latch));
            futures.add(future);
        }

        executorService.shutdown();

        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);

            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    if (result != null) {
                        System.out.println("Результат: " + result);
                    }
                } catch (ExecutionException e) {
                    System.err.println("Ошибка при обработке заказа: " + e.getMessage());
                }
            }

            System.out.println("Общее количество обработанных заказов: " + orderProcessor.getProcessedOrdersCount());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}