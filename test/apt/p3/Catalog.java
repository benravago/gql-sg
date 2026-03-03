package apt.p3;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;

import reactor.core.publisher.Flux;

public class Catalog {

  @SubscriptionMapping
  public Flux<BookPrice> notifyBookPriceChange(@Argument Integer bookId) { return null; }

  @QueryMapping
  public Book bookById(@Argument Integer id) { return null; }

}