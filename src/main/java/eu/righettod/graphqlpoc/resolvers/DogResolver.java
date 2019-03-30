package eu.righettod.graphqlpoc.resolvers;

import com.coxautodev.graphql.tools.GraphQLResolver;
import eu.righettod.graphqlpoc.repository.BusinessDataRepository;
import eu.righettod.graphqlpoc.types.Dog;
import eu.righettod.graphqlpoc.types.Veterinary;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Resolve Veterinary object from a Dog object.
 */
public class DogResolver implements GraphQLResolver<Dog> {

    /**
     * Accessor to business data
     */
    @Autowired
    private BusinessDataRepository businessDataRepository;

    /**
     * Resolve the Veterinary instance from a Dog instance
     *
     * @param d Dog instance
     * @return The Veterinary instance for the Dog instance passed
     * @throws Exception if any error occurs
     */
    public Veterinary veterinary(Dog d) throws Exception {
        Dog dg = businessDataRepository.findDogById(d.getId());
        return dg != null ? dg.getVeterinary() : null;
    }
}
