
.DEFAULT_GOAL := tests


graphql_jar = com/graphql-java/graphql-java/25.0/graphql-java-25.0.jar
junit_jar   = org/junit/platform/junit-platform-console-standalone/6.0.3/junit-platform-console-standalone-6.0.3.jar


mvn = \
  mkdir -p lib ; \
  curl -s -o $@ https://repo1.maven.org/maven2/$(1)


car = \
  rm -fr bin ; \
  javac -d bin $^ ; \
  jar cvf $@ -C bin .

space := $(empty) $(empty)
colon := $(empty):$(empty)

clr = \
  rm -fr bin ; \
  javac -d bin -cp $(subst $(space),$(colon),$(filter %.jar,$^)) $(filter %.java,$^) ; \
  jar cvf $@ -C bin .

tdir := $(empty)test/$(empty)
cdir := $(empty)-C test $(empty)

cpy = \
  jar uvf $@ $(subst $(tdir),$(cdir),$(filter %.graphql,$^))


processor_src = \
  src/tools/gqls/SchemaGenerator.java

annotations_src = \
  src/tools/gqls/annotation/Alias.java \
  src/tools/gqls/annotation/DirectiveMapping.java \
  src/tools/gqls/annotation/ScalarMapping.java \
  src/tools/gqls/annotation/TypeMapping.java

annotations_bin = \
  lib/graphql.jar

mocks_src = \
  mock/org/springframework/graphql/data/method/annotation/Argument.java \
  mock/org/springframework/graphql/data/method/annotation/SubscriptionMapping.java \
  mock/org/springframework/graphql/data/method/annotation/MutationMapping.java \
  mock/org/springframework/graphql/data/method/annotation/SchemaMapping.java \
  mock/org/springframework/graphql/data/method/annotation/QueryMapping.java \
  mock/org/springframework/graphql/data/method/annotation/BatchMapping.java \
  mock/reactor/core/publisher/Flux.java \
  mock/reactor/core/publisher/Mono.java

tests_src = \
  test/apt/TestCase.java \
  test/apt/p1/Author.java \
  test/apt/p1/Book.java \
  test/apt/p1/Catalog.java \
  test/apt/p1/Test_QueryMapping.java \
  test/apt/p1/schema.graphql \
  test/apt/p2/Book.java \
  test/apt/p2/Catalog.java \
  test/apt/p2/Test_MutationMapping.java \
  test/apt/p2/schema.graphql \
  test/apt/p3/Book.java \
  test/apt/p3/BookPrice.java \
  test/apt/p3/Catalog.java \
  test/apt/p3/Test_SubscriptionMapping.java \
  test/apt/p3/schema.graphql \
  test/apt/p4/Author.java \
  test/apt/p4/Book.java \
  test/apt/p4/Catalog.java \
  test/apt/p4/Test_SchemaMapping.java \
  test/apt/p4/schema.graphql \
  test/apt/p5/Day.java \
  test/apt/p5/Days.java \
  test/apt/p5/Test_Enumeration.java \
  test/apt/p5/schema.graphql \
  test/apt/p6/Identity.java \
  test/apt/p6/IdentityCoercing.java \
  test/apt/p6/Question.java \
  test/apt/p6/Test_ScalarMapping.java \
  test/apt/p6/schema.graphql \
  test/apt/p7/Data.java \
  test/apt/p7/Identity.java \
  test/apt/p7/Note.java \
  test/apt/p7/OneOf.java \
  test/apt/p7/Question.java \
  test/apt/p7/Test_DirectiveMapping.java \
  test/apt/p7/schema.graphql \
  test/apt/p8/Book.java \
  test/apt/p8/Books.java \
  test/apt/p8/Rating.java \
  test/apt/p8/Test_TypeMapping.java \
  test/apt/p8/schema.graphql \
  test/apt/p9/Builtins.java \
  test/apt/p9/Test_Primitives.java \
  test/apt/p9/schema.graphql

tests_bin = \
  lib/processor.jar \
  lib/annotations.jar \
  lib/mocks.jar \
  lib/junit.jar \
  lib/graphql.jar


lib/processor.jar: $(processor_src)
	$(call car)

lib/mocks.jar: $(mocks_src)
	$(call car)

lib/annotations.jar: $(annotations_src) $(annotations_bin)
	$(call clr)

lib/tests.jar: $(tests_src) $(tests_bin)
	$(call clr)
	$(call cpy)

lib/graphql.jar:
	$(call mvn,$(graphql_jar))

lib/junit.jar:
	$(call mvn,$(junit_jar))


tests: lib/tests.jar $(tests_bin)
	java \
  -cp lib/annotations.jar:lib/processor.jar:lib/mocks.jar:lib/tests.jar:lib/graphql.jar:lib/junit.jar \
  -cp $(subst $(space),$(colon),$(filter %.jar,$^)) \
  org.junit.platform.console.ConsoleLauncher \
  execute \
  --include-engine=junit-jupiter \
  --select-package=apt \
  --include-classname='.*'


clean:
	rm -fr bin lib tmp

