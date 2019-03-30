package eu.righettod.graphqlpoc.publishers;

import eu.righettod.graphqlpoc.repository.BusinessDataRepository;
import eu.righettod.graphqlpoc.types.Dog;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.observables.ConnectableObservable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Publish the events about new associations.
 */
@Component
public class NewAssociationPublisher {

    /**
     * Accessor to business data,
     * will be injected via Spring via the single constructor because BusinessDataRepository is managed by Spring
     */
    private BusinessDataRepository businessDataRepository;

    /** Event publisher*/
    private final Flowable<String> publisher;

    /** Local cache used for the example*/
    private Map<String, String> cacheAssociation = new HashMap<>();

    /**
     * Constructor - Init the publishing of the events
     */
    public NewAssociationPublisher(BusinessDataRepository bDataRepository) {
        this.businessDataRepository = bDataRepository;
        Observable<String> stockPriceUpdateObservable = Observable.create(emitter -> {
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleAtFixedRate(verifyPresenceOfNewAssociation(emitter), 0, 20, TimeUnit.SECONDS);
        });
        ConnectableObservable<String> connectableObservable = stockPriceUpdateObservable.share().publish();
        connectableObservable.connect();
        publisher = connectableObservable.toFlowable(BackpressureStrategy.BUFFER);
    }

    /**
     * Verify if new association has been created
     *
     * @param emitter Event emitter
     * @return A runnable instance
     */
    private Runnable verifyPresenceOfNewAssociation(ObservableEmitter<String> emitter) {
        return () -> {
            try {
                List<Dog> dogs = businessDataRepository.findAllDogs(false, 1000000);
                String vetName;
                for (Dog d : dogs) {
                    vetName = cacheAssociation.get(d.getName());
                    if (vetName == null && d.getVeterinary() != null) {
                        cacheAssociation.put(d.getName(), d.getVeterinary().getName());
                        emitter.onNext(String.format("Dog['%s'] associated with Veterinary['%s'].", d.getName(), d.getVeterinary().getName()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    public Flowable<String> getPublisher() {
        return publisher;
    }
}
