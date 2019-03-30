package eu.righettod.graphqlpoc.resolvers;

import com.coxautodev.graphql.tools.GraphQLResolver;
import eu.righettod.graphqlpoc.repository.BusinessDataRepository;
import eu.righettod.graphqlpoc.types.Dog;
import eu.righettod.graphqlpoc.types.Veterinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolve Dog object from a  Veterinary object.
 */
@Component
public class VeterinaryResolver implements GraphQLResolver<Veterinary> {

    /**
     * Accessor to business data
     */
    @Autowired
    private BusinessDataRepository businessDataRepository;

    /**
     * Resolve the Dog instance from a Veterinary instance
     *
     * @param v The Veterinary instance
     * @return The list of Dog instances for a the Veterinary instance
     * @throws Exception if any error occurs
     */
    public List<Dog> dogs(Veterinary v) throws Exception {
        Veterinary vt = businessDataRepository.findVeterinaryById(v.getId());
        return vt != null ? vt.getDogs() : null;
    }
}
