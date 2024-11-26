package num3;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class Order {
    private static final AtomicInteger ingredients = new AtomicInteger(3);
    private static int number = 0;
    public final int num;
    private final String dishName;
    private final int preparationTime;
    private final double price;

    public Order(String dishName, int preparationTime, double price) {
        this.dishName = dishName;
        this.preparationTime = preparationTime;
        this.price = price;
        synchronized (Order.class) {
            number += 1;
            this.num = number;
        }
    }

    public String getDishName() {
        return dishName;
    }

    public int getPreparationTime() {
        return preparationTime;
    }

    public double getPrice() {
        return price;
    }

    public static boolean checkIngredients() {
        return ingredients.get() > 0 && ingredients.decrementAndGet() >= 0;
    }

    public void run(){
        Order order = this;
        CompletableFuture<Void> orderProcess = CompletableFuture.supplyAsync(() -> {
            if (Order.checkIngredients()) {
                System.out.println("Номер заказа " + order.num + ": Достаточно ингредиентов для " + order.getDishName());
                return order;
            } else {
                System.out.println("Номер заказа " + order.num + ": Недостаточно ингредиентов для " + order.getDishName());
                return null;
            }
        }).thenCompose(orderResult -> {
            if (orderResult != null) {
                return CompletableFuture.runAsync(() -> {
                    try {
                        System.out.println("Номер заказа " + orderResult.num + ": Приготовление " + orderResult.getDishName() + "...");
                        Thread.sleep(orderResult.getPreparationTime());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).thenApply(v -> orderResult);
            } else {
                return CompletableFuture.completedFuture(null);
            }
        }).thenApply(orderResult -> {
            if (orderResult != null) {
                double totalPrice = orderResult.getPrice();
                if (totalPrice > 500) {
                    totalPrice *= 0.9;
                    System.out.println("Номер заказа " + orderResult.num + ": Скидка применена. Новая цена: " + totalPrice);
                } else {
                    System.out.println("Номер заказа " + orderResult.num + ": Цена: " + totalPrice);
                }
                return totalPrice;
            }
            return null;
        }).thenAccept(totalPrice -> {
            if (totalPrice != null) {
                System.out.println("Блюдо готово! Общая стоимость: " + totalPrice);
            } else {
                System.out.println("Заказ не был обработан из-за недостатка ингредиентов.");
            }
        });


        try {
            orderProcess.join();
        } catch (Exception e) {
            System.err.println("Ошибка при обработке заказа: " + e.getMessage());
        }
    }
}

class RestaurantOrderSystem {

    public static void main(String[] args) {
        Order order = new Order("Паста", 3000, 600);
        order.run();
        Order order2 = new Order("Паста", 3000, 600);
        order2.run();
        Order order3 = new Order("Паста", 3000, 600);
        order3.run();
        Order order4 = new Order("Паста", 3000, 600);
        order4.run();

        System.out.println("Обработка заказов завершена.");
    }
}