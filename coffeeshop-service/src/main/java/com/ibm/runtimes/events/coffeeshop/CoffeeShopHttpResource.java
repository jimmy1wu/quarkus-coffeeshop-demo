package com.ibm.runtimes.events.coffeeshop;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.ibm.runtimes.events.coffeeshop.http.BaristaService;
import com.ibm.runtimes.events.coffeeshop.model.Beverage;
import com.ibm.runtimes.events.coffeeshop.model.Order;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class CoffeeShopHttpResource {
    

    @Inject
    @RestClient
    BaristaService barista;
    
    @POST
    @Path("/http")
    public Beverage http(final Order order) {
        return barista.order(order.setOrderId(UUID.randomUUID().toString()));
    }

   
}
