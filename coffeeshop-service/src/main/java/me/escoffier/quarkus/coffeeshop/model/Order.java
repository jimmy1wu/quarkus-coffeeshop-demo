package me.escoffier.quarkus.coffeeshop.model;

public class Order {

    private String product;
    private String name;
    private String orderId;

    public String toString() {
        return "product: " + product + ", name: " + name + ", orderId: " + orderId;
    }

    public String getProduct() {
        return product;
    }

    public Order setProduct(String product) {
        this.product = product;
        return this;
    }

    public String getName() {
        return name;
    }

    public Order setName(String name) {
        this.name = name;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public Order setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }
}
