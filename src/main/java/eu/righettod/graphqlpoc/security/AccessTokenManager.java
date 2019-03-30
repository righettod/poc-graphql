package eu.righettod.graphqlpoc.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import eu.righettod.graphqlpoc.repository.BusinessDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Handle the creation and the validation of the JWT access token
 */
@Component
public class AccessTokenManager {

    /**
     * Accessor to business data
     */
    @Autowired
    private BusinessDataRepository businessDataRepository;

    /**
     * Issue a JWT token for a veterinary
     *
     * @param veterinaryName Veterinary name
     * @return KWT token
     * @throws Exception If any error occurs
     */
    public String issueToken(String veterinaryName) throws Exception {
        Algorithm algorithm = Algorithm.HMAC256(businessDataRepository.loadCfgParam("ACCESS_TOKEN_SECRET"));
        Date validityFrame = Date.from(LocalDateTime.now().plusMinutes(60).atZone(ZoneId.systemDefault()).toInstant());
        return JWT.create().withIssuer("AuthSystem").withAudience("poc").withExpiresAt(validityFrame).withSubject(veterinaryName).sign(algorithm);
    }

    /**
     * Verify the validity of a JWT token
     *
     * @param token JWT token
     * @throws Exception If token is not valid
     */
    public void verifyToken(String token) throws Exception {
        Algorithm algorithm = Algorithm.HMAC256(businessDataRepository.loadCfgParam("ACCESS_TOKEN_SECRET"));
        JWTVerifier verifier = JWT.require(algorithm).withIssuer("AuthSystem").withAudience("poc").build();
        verifier.verify(token);
    }
}
