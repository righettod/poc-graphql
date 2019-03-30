package eu.righettod.graphqlpoc.resolvers;

import com.coxautodev.graphql.tools.GraphQLSubscriptionResolver;
import eu.righettod.graphqlpoc.publishers.NewAssociationPublisher;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Resolvers for all Subscriptions defined in the Schema.
 */
@Component
public class Subscription implements GraphQLSubscriptionResolver {

    @Autowired
    private NewAssociationPublisher newAssociationPublisher;


    /**
     * Resolver linked to subscription "newAssociation" defined in the schema.
     */
    Publisher<String> newAssociation() {
        return newAssociationPublisher.getPublisher();
    }

}
