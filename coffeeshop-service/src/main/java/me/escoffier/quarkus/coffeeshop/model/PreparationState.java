package me.escoffier.quarkus.coffeeshop.model;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

public class PreparationState {

    Beverage beverage;

    Order order;

    State state;

    public enum State {
        IN_QUEUE,
        BEING_PREPARED,
        READY;
    }

    public String toString() {
        return "PreparationState\n"
            + " - Beverage: " + beverage + "\n"
            + " - Order: " + order + "\n"
            + " - State: " + state;
    }

    private static final Jsonb JSON = JsonbBuilder.create();

    public static String ready(Order order, Beverage beverage) {
        return JSON.toJson(new PreparationState().setBeverage(beverage).setOrder(order).setState(State.READY));
    }

    public static String queued(Order order) {
        System.out.println("queued: Order = " + order);
        PreparationState p = new PreparationState();
        System.out.println("queued: p = " + p);
        p = p.setOrder(order);
        System.out.println("queued: p = " + p);
        p = p.setState(State.IN_QUEUE);
        System.out.println("queued: p = " + p);
        String json = JSON.toJson(p);
        System.out.println("queued: Result JSON: " + json);
        //return JSON.toJson(.setOrder(order).setState(State.IN_QUEUE));
        return json;
    }

    public static String underPreparation(Order order) {
        return JSON.toJson(new PreparationState().setOrder(order).setState(State.BEING_PREPARED));
    }


    public Beverage getBeverage() {
        return beverage;
    }

    public PreparationState setBeverage(Beverage beverage) {
        this.beverage = beverage;
        return this;
    }

    public Order getOrder() {
        return order;
    }

    public PreparationState setOrder(Order order) {
        this.order = order;
        return this;
    }

    public State getState() {
        return state;
    }

    public PreparationState setState(State state) {
        this.state = state;
        return this;
    }
}
