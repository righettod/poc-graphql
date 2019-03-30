package eu.righettod.graphqlpoc.resolvers;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import eu.righettod.graphqlpoc.repository.BusinessDataRepository;
import eu.righettod.graphqlpoc.security.AccessTokenManager;
import eu.righettod.graphqlpoc.types.Dog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Resolvers for all Mutations defined in the Schema.
 */
@Component
public class Mutation implements GraphQLMutationResolver {

    /**
     * Accessor to business data
     */
    @Autowired
    private BusinessDataRepository businessDataRepository;

    /**
     * Token manager
     */
    @Autowired
    private AccessTokenManager accessTokenManager;


    /**
     * Resolver linked to mutation "associateDogToMe" defined in the schema.
     */
    public Dog associateDogToMe(String accessToken, int veterinaryId, int dogId) throws Exception {
        //[VULN]: There an access control issue here because the verification of the access token do not verify that the token belong to the veterinary passed in "veterinaryId"
        accessTokenManager.verifyToken(accessToken);
        return this.businessDataRepository.associatedDogToVeterinary(veterinaryId, dogId);
    }

    /**
     * Resolver linked to mutation "disassociateDogFromMe" defined in the schema.
     */
    public Dog disassociateDogFromMe(String accessToken, int veterinaryId, int dogId) throws Exception {
        //[VULN]: There an access control issue here because the verification of the access token do not verify that the token belong to the veterinary passed in "veterinaryId"
        accessTokenManager.verifyToken(accessToken);
        return this.businessDataRepository.disassociatedDogFromVeterinary(veterinaryId, dogId);
    }


}
