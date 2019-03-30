package eu.righettod.graphqlpoc.resolvers;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import eu.righettod.graphqlpoc.repository.BusinessDataRepository;
import eu.righettod.graphqlpoc.security.AccessTokenManager;
import eu.righettod.graphqlpoc.types.Dog;
import eu.righettod.graphqlpoc.types.Veterinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolvers for all Queries defined in the Schema.
 */
@Component
public class Query implements GraphQLQueryResolver {

    /**
     * Accessor to business data
     */
    @Autowired
    private BusinessDataRepository businessDataRepository;

    /** Token manager */
    @Autowired
    private AccessTokenManager accessTokenManager;

    /** Resolver linked to query "auth" defined in the schema. */
    public String auth(String veterinaryName) throws Exception
    {
        return this.accessTokenManager.issueToken(veterinaryName);
    }

    /**
     * Resolver linked to query "allDogs" defined in the schema.
     */
    public List<Dog> allDogs(boolean onlyFree, int limit) throws Exception {
        return this.businessDataRepository.findAllDogs(onlyFree, limit);
    }

    /**
     * Resolver linked to query "dogs" defined in the schema.
     */
    public List<Dog> dogs(String namePrefix, int limit) throws Exception {
        //[VULN]: There an SQLi here because the repository using string concatenation for this query
        //Payload is:
        /*
            query sqli {
              dogs(namePrefix:"ab%' UNION ALL SELECT 50 AS ID, C.CFGVALUE AS NAME, NULL AS VETERINARY_ID FROM CONFIG C LIMIT ? -- ",limit: 1000){
                id,name
              }
            }
         */
        return this.businessDataRepository.findByNamePrefix(namePrefix, limit);
    }

    /**
     * Resolver linked to query "myDogs" defined in the schema.
     */
    public List<Dog> myDogs(String accessToken, int veterinaryId) throws Exception {
        //[VULN]: There an access control issue here because the verification of the access token do not verify that the token belong to the veterinary passed in "veterinaryId"
        accessTokenManager.verifyToken(accessToken);
        return this.businessDataRepository.findVeterinaryById(veterinaryId).getDogs();
    }

    /**
     * Resolver linked to query "myInfo" defined in the schema.
     */
    public Veterinary myInfo(String accessToken, int veterinaryId) throws Exception {
        //[VULN]: There an access control issue here because the verification of the access token do not verify that the token belong to the veterinary passed in "veterinaryId"
        accessTokenManager.verifyToken(accessToken);
        return this.businessDataRepository.findVeterinaryById(veterinaryId);
    }
}
