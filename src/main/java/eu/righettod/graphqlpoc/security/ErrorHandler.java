package eu.righettod.graphqlpoc.security;

import graphql.GraphQLError;
import graphql.servlet.GenericGraphQLError;
import graphql.servlet.GraphQLErrorHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to handle unexpected error.
 * See the link below (v5.3) for technical explanation.
 * <p>
 * Due to a bug (issue #177) in graphql-spring-boot version 5.3+ we cannot use the @ExceptionHandler annotation way so we use the Interface impl way
 * and we stay in version 5.2 of graphql-spring-boot until the fix.
 *
 * Uncomment the annotation "@Component" to enable the handler.
 *
 * @see "https://github.com/graphql-java-kickstart/graphql-spring-boot/releases/tag/v5.3"
 * @see "https://github.com/graphql-java-kickstart/graphql-spring-boot/issues/177"
 *
 */
//@Component
public class ErrorHandler implements GraphQLErrorHandler {

    @Override
    public List<GraphQLError> processErrors(List<GraphQLError> list) {
        //Log all errors from the variable "list" on server side
        //....
        //We return a generic error
        List<GraphQLError> error = new ArrayList<>(1);
        error.add(new GenericGraphQLError("Query cannot be processed!"));

        return error;
    }

    @Override
    public boolean errorsPresent(List<GraphQLError> errors) {
        return (errors != null && !errors.isEmpty());
    }

}
