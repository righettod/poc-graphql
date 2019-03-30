package eu.righettod.graphqlpoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Project entry point
 */
@SpringBootApplication
public class Application {

    /**
     * Entry point
     * @param args Command line
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /* Uncomment the block blow to register the 2 built-in instrumentation to prevents DOS using abusive query*/
    /*
    @Bean
    public Instrumentation addMaxQueryComplexityInstrumentation(){
        return new MaxQueryComplexityInstrumentation(15);
    }

    @Bean
    public Instrumentation addMaxQueryDepthInstrumentation(){
        return new MaxQueryDepthInstrumentation(10);
    }
    */
}
