package me.escoffier.quarkus.coffeeshop;

import java.util.Set;
import java.util.HashSet;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import me.escoffier.quarkus.coffeeshop.dashboard.BoardResource;

@ApplicationPath("/services")
public class CoffeeShopApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
       HashSet<Class<?>> classes = new HashSet<>();
       classes.add(CoffeeShopResource.class);
       classes.add(BoardResource.class);
       classes.add(CoffeeShopHttpResource.class);
       return classes;
    }
}