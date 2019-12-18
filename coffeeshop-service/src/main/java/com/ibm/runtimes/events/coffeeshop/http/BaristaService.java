package com.ibm.runtimes.events.coffeeshop.http;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.ibm.runtimes.events.coffeeshop.model.Beverage;
import com.ibm.runtimes.events.coffeeshop.model.Order;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.concurrent.CompletionStage;

@Path("/barista")
@RegisterRestClient
public interface BaristaService {

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    Beverage order(Order order);

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    CompletionStage<Beverage> orderAsync(Order order);


}
