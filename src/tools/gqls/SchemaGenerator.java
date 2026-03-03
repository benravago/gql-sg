package tools.gqls;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.tools.StandardLocation;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SchemaGenerator extends AbstractProcessor {

  // annotation processor features

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(
      $QueryMapping, // method
      $MutationMapping, // method
      $SubscriptionMapping, // method
      $SchemaMapping, // method,type
      $DirectiveMapping, // type
      $ScalarMapping // type
      // InterfaceMapping
      // UnionMapping
    );
  }

  final static String /* constants */

    // annotations used as APT selectors

    $SchemaMapping =        "org.springframework.graphql.data.method.annotation.SchemaMapping",
    $QueryMapping =         "org.springframework.graphql.data.method.annotation.QueryMapping",
    $MutationMapping =      "org.springframework.graphql.data.method.annotation.MutationMapping",
    $SubscriptionMapping =  "org.springframework.graphql.data.method.annotation.SubscriptionMapping",
    $BatchMapping =         "org.springframework.graphql.data.method.annotation.BatchMapping",
    $DirectiveMapping =     "tools.gqls.annotation.DirectiveMapping",
    $ScalarMapping =        "tools.gqls.annotation.ScalarMapping",
    $TypeMapping =          "tools.gqls.annotation.TypeMapping",

    // other annotations and classes

    $Argument =             "org.springframework.graphql.data.method.annotation.Argument",
    $Alias =                "tools.gqls.annotation.Alias",

    $JsonIgnore =           "com.fasterxml.jackson.annotation.JsonIgnore",
    $JsonIgnoreProperties = "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
    $JsonIgnoreType =       "com.fasterxml.jackson.annotation.JsonIgnoreType",
    $JsonProperty =         "com.fasterxml.jackson.annotation.JsonProperty",
    $JsonRootName =         "com.fasterxml.jackson.annotation.JsonRootName",
    $JsonTypeName =         "com.fasterxml.jackson.annotation.JsonTypeName",

    $ScalarType =           "graphql.schema.GraphQLScalarType",
    $Coercing =             "graphql.schema.Coercing",

    $Map =                  "java.util.Map",
    $Collection =           "java.util.Collection",
    $Enum =                 "java.lang.Enum",
    $Object =               "java.lang.Object";


  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    builtins();
    nullType = processingEnv.getTypeUtils().getNullType();
    objectType = typeMirror($Object);
    enumType = erasure(typeMirror($Enum));
    collectionType = erasure(typeMirror($Collection));
    coercingType = erasure(typeMirror($Coercing));
  }

  TypeMirror nullType;
  TypeMirror enumType;
  TypeMirror objectType;
  TypeMirror collectionType;
  TypeMirror coercingType;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return roundEnv.processingOver() ? generate() : collect(annotations,roundEnv);
  }

  boolean collect(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    collect(roundEnv, $QueryMapping,        this::queryMapping        );
    collect(roundEnv, $MutationMapping,     this::mutationMapping     );
    collect(roundEnv, $SubscriptionMapping, this::subscriptionMapping );
    collect(roundEnv, $SchemaMapping,       this::schemaMapping       );
    collect(roundEnv, $DirectiveMapping,    this::directiveMapping    );
    collect(roundEnv, $ScalarMapping,       this::scalarMapping       );
    return false; // no claims, allow others
  }

  void collect(RoundEnvironment env, String name, Consumer<Element> handler) {
    env.getElementsAnnotatedWith(typeElement(name)).forEach(handler);
  }

  boolean generate() {
    objects();
    schema();
    return false;
  }

  class SchemaType {
    String fqcn;
    String name; int flags; TypeElement ref; AnnotationMirror am; List<SchemaField> fields;

    SchemaType(String q, String n, int f, TypeElement t) { fqcn=q; name=n; flags=f; ref=t; }
    SchemaType(String q, String n, int f, TypeElement t, AnnotationMirror a) { this(q,n,f,t); am=a; }
    SchemaType(String q, String n, int f, TypeElement t, List<SchemaField> b) { this(q,n,f,t); fields=b; }
  }

  record SchemaField(String name, Element ref, TypeMirror type, List<VariableElement> parameters) {}

  Map<String,SchemaType> schema = new HashMap<>(); // className, [schemaName, flags, ref]
  SchemaType put(SchemaType v) { schema.put(v.fqcn,v); return v; }

  static final int // flags

    // schema parts     -- 0x00ff
    DIRECTIVE = 0x0080, // Directive
    SCALAR    = 0x0040, // Scalar
    ENUM      = 0x0020, // Enumeration
    ROOT      = 0x0010, // root type - Query, Mutation, Subscription
    INTERFACE = 0x0008, // Interface
    UNION     = 0x0004, // Union
    TYPE      = 0x0001, // Data Object

    // indicators       -- 0xff00
    INPUT     = 0x4000, // 'input' type
    OUTPUT    = 0x2000, // output 'type'
    WRITE     = 0x0400, // write-able property
    READ      = 0x0200, // read-able property
    WIRING    = 0x1000, // directive wiring
    COERCING  = 0x0100, // scalar coercing
    BUILTIN   = 0x0800; // built-in type

  static int schemaPart(SchemaType s) { return s != null ? (s.flags & 0x0ff) : 0; }

  static boolean isType(SchemaType s) { return s != null && (s.flags & TYPE) != 0; }
  static boolean isInput(SchemaType s) { return s != null && (s.flags & INPUT) != 0; }
  static boolean isOutput(SchemaType s) { return s != null && (s.flags & OUTPUT) != 0; }
  static boolean isScalar(SchemaType s) { return s != null && (s.flags & SCALAR) != 0; }
  static boolean isDirective(SchemaType s) { return s != null && (s.flags & DIRECTIVE) != 0; }
  static boolean isBuiltin(SchemaType s) { return s != null && (s.flags & BUILTIN) != 0; }

  static boolean is(int flags, int mask) { return (flags & mask) != 0; }

  PrintWriter writer(String folder, String file) {
    try {
      return new PrintWriter(processingEnv.getFiler()
        .createResource(StandardLocation.SOURCE_OUTPUT, folder, file)
        .openWriter()
      );
    }
    catch (IOException e) { throw new UncheckedIOException(e); }
  }

  PrintWriter sdl(String kind, String name) {
    var out = writer("graphql", kind+'_'+name+".gqls");
    out.format("# %s%n%n", do_not_edit());
    return out;
  }

  PrintWriter bean(String pkg, String name) {
    return writer(pkg, name+".java");
  }

  static String[] beanSpec(String fqcn, String suffix) {
    var p = fqcn.lastIndexOf('.');
    return new String[] {
      fqcn.substring(0,p), // packageName
      fqcn.substring(p+1)+suffix // className
    };
  }

  String do_not_edit() {
    return "@Generated(value="+getClass().getName()+", date=\""+Instant.now()+"\", message=\"DO NOT EDIT!\")";
  }

  Map<String,String> typeNames = new TreeMap<>();

  Map<String,String> typeNames() {
    for (var e:schema.entrySet()) typeNames.put(e.getValue().name,e.getKey());
    return typeNames;
  }

  String inputName(String n) {
    var k = typeNames.get(n);
    return k != null && isInput(schema.get(k)) ? n+"Input" : n;
  }

  // Phase 3: generate artifacts

  void schema() {
    for (var e:typeNames().entrySet()) {
      var s = schema.get(e.getValue());
      switch (schemaPart(s)) {
        case DIRECTIVE -> directives(s);
        case SCALAR    -> scalars(s);
        case ENUM      -> enumerations(s);
        case ROOT      -> roots(s);
        case INTERFACE -> interfaces(s);
        case UNION     -> unions(s);
        case TYPE      -> types(s);
        default -> {} // "unknown "+Integer.toHexString(s.flags);
      };
    }
  }

  /*   directive @name( ... ) on LOCATION | ...   */

  void directives(SchemaType s) {
    if (builtin(s.am)) return;
    directive_schema(s);
    directive_wiring(s);
  }

  void directive_schema(SchemaType s) {
    sdl("directive",s.name)
      .format("# ref = %s%ndirective @%s %s %s %n%n", s.fqcn, s.name, arguments(s.ref), locations(s.am))
      .close();
  }

  CharSequence arguments(TypeElement e) {
    var b = new StringBuilder("(");
    for (var i:e.getEnclosedElements()) if (i instanceof ExecutableElement x){
      if (has(x,$Alias)) continue;
      b.append(' ').append(name(x)).append(':').append(type(returnType(x),false));
      // TODO: // <argumentName>: <ArgumentType> = <defaultValue>
    }
    return b.length() > 1 ? b.append(" )") : "";
  }
  CharSequence locations(AnnotationMirror am) {
    var b = new StringBuilder();
    if (actualValue(am,"location") instanceof AnnotationValue av && av.getValue() instanceof List list) {
      for (var i:list) {
        b.append(" | ").append(i);
      }
    }
    return b.isEmpty() ? b : b.replace(0, 2, "on");
  }

  void directive_wiring(SchemaType s) {
    var w = wiring(s.am);
    if (w != null) {
      var a = beanSpec(s.fqcn,"_Bean");
      bean(a[0],a[1])
        .format(directive_wiring, do_not_edit(), a[0], a[1], s.name, w)
        .close();
    }
  }

  final static String directive_wiring = """
    package %2$s;
    %1$s
    @org.springframework.stereotype.Component
    public class %3$s implements org.springframework.graphql.execution.RuntimeWiringConfigurer {
      @Override public void configure(graphql.schema.idl.RuntimeWiring.Builder builder) {
        builder.directive("%4$s", %5$s);
      }
    } """; // @Generated, packageName, className, directiveName, directiveInstance

  static String wiring(AnnotationMirror am) {
    var i = instance(am);
    if (i != null) return i;
    var w = actualValue(am, "wiring");
    if (w != null) return ctor(w.getValue());
    return null;
  }

  // TODO: handle @Repeatable(DirectiveMapping.class)

  /*   scalar Name   */

  void scalars(SchemaType s) {
    if (isBuiltin(s)) return;
    scalar_schema(s);
    scalar_wiring(s);
  }

  void scalar_schema(SchemaType s) {
    sdl("scalar",s.name)
      .format("# ref = %s%nscalar %s%n%n", s.fqcn, s.name)
      .close();
  }

  void scalar_wiring(SchemaType s) {
    s.fqcn = s.ref.getQualifiedName().toString(); // use annotated class name
    var c = coercing(s);
    if (c != null) {
      var a = beanSpec(s.fqcn,"_Bean");
      bean(a[0],a[1])
        .format(scalar_wiring, do_not_edit(), a[0], a[1], c)
        .close();
    }
  }

  final static String scalar_wiring = """
    package %2$s;
    %1$s
    @org.springframework.stereotype.Component
    public class %3$s implements org.springframework.graphql.execution.RuntimeWiringConfigurer {
      @Override public void configure(graphql.schema.idl.RuntimeWiring.Builder builder) {
        builder.scalar(%4$s);
      }
    } """; // @Generated, packageName, className, scalarInstance

  String coercing(SchemaType s) {
    if (builtin(s.am)) {
      return instance(s.am);
    } else {
      var i = instance(s.am);
      if (i != null) return scalar_builder(s.name, i);
      var c = actualValue(s.am, "coercing");
      if (c != null) return scalar_builder(s.name, ctor(c.getValue()));
      var b = isA(s.ref.asType(), coercingType);
      if (b) return scalar_builder(s.name, ctor(s.fqcn));
    }
    return null;
  }

  static String instance(AnnotationMirror am) { return str(actualValue(am, "instance")); }
  static boolean builtin(AnnotationMirror am) { return isTrue(am,"builtin"); }

  static String ctor(Object o) { return "new "+o+"()"; }

  static String scalar_builder(String a, String b) { return scalar_builder.formatted(a,b); }

  final static String scalar_builder =
    "graphql.schema.GraphQLScalarType.newScalar().name(\"%1$s\").coercing(%2$s).build()";

  /*   enum Name { ... }   */

  void enumerations(SchemaType s) {
    var f = enumValues(s.ref);
    var out = sdl("enum",s.name);
    out.format("# ref = %s%nenum %s {%n  %s%n}%n%n", s.fqcn, s.name, lines(f));
    out.close();
  }

  List<String> enumValues(TypeElement t) {
    var list = new ArrayList<String>();
    for (var e:t.getEnclosedElements()) {
      if (e.getKind().equals(ElementKind.ENUM_CONSTANT)) {
        list.add(e.toString());
      }
    }
    return list;
  }

  /*   type [ Query | Mutation | Subscription ] { ... }   */

  void roots(SchemaType s) {
    s.fqcn = s.fqcn.substring(1);
    var f = fields(s.fields, 0, _ -> true);
    var out = sdl("root",s.name);
    out.format("# root %s%ntype %s {%n  %s%n}%n%n", s.fqcn, s.name, lines(f) );
    out.close();
  }

  /*   type Name { ... }         */
  /*   input NameInput { ... }   */

  void types(SchemaType s) {
    var out = sdl("type",s.name);
    out.format("# ref = %s%n", s.fqcn);
    if (isOutput(s)) outputs(s, out);
    if (isInput(s)) inputs(s, out);
    out.close();
  }

  void outputs(SchemaType s, PrintWriter out) {
    var f = fields(s.fields, 0, i -> !setter(i.name));
    var d = directives(s.ref, "OBJECT");  // type Name @foo { A B }
    out.format("type %s%s {%n  %s%n}%n%n", s.name, d, lines(f));
  }
  void inputs(SchemaType s, PrintWriter out) {
    var f = fields(s.fields, INPUT, i -> !getter(i.name));
    var d = directives(s.ref, "INPUT_OBJECT");  // input NameInput @foo { A B }
    out.format("input %sInput%s {%n  %s%n}%n%n", s.name, d, lines(f));
  }

  List<String> fields(List<SchemaField> list, int flag, Predicate<SchemaField> accessor) {
    var a = new ArrayList<String>();
    for (var f:list) {
      if (accessor.test(f)) {
        var t = typeOf(f.ref, f.type, flag);
        if (t != null) {
          var n = f.parameters != null ? field(f.name) + signature(f.parameters) : f.name;
          a.add(n+": "+t);
        }
      }
    }
    return a;
  }

  CharSequence signature(List<VariableElement> a) {
    if (a == null || a.isEmpty()) return "";
    var b = new StringBuilder().append("( ");
    for (var v:a) {
      var n = nameOf(v);
      var t = typeOf(v, v.asType(), INPUT);
      if (t != null) b.append(n).append(": ").append(t).append(", ");
    }
    b.setLength(b.length()-2);
    return b.append(" )");
  }

  String nameOf(Element e) {
    var a = annotationValue(e,$Argument,"name","value");
    var n = a != null ? a.getValue().toString() : "";
    return n.isEmpty() ? name(e) : n;
  }

  String typeOf(Element e, TypeMirror m, int f) {
    var n = type(m, is(f,INPUT));
    var t = annotationMirror(e,$TypeMapping);
    if (t != null) {
      if (isTrue(t,"ignore")) return null;
      if (isTrue(t,"id")) n = "ID";
      if (isTrue(t,"require") && n.charAt(n.length()-1) != '!') n += '!';
    }
    return n;
  }

  String type(TypeMirror m, boolean i) {
    return switch (m.getKind()) {
      // built-in graphql types
      case INT -> "Int!";
      case FLOAT, DOUBLE -> "Float!";
      // compatible with java boxed-primitive classes
      case BOOLEAN -> "Boolean!";
      case BYTE -> "Byte!";
      case CHAR -> "Character!";
      case SHORT -> "Short!";
      case LONG -> "Long!";
      // a type[] class
      case ARRAY -> "["+type(componentOf(m),i)+"]";
      // a class or interface
      case DECLARED -> isList(m) ? type(arrayOf(m),i) : nameOf(m,i);
      // <?> and variants
      case WILDCARD -> type(wildcardBound(m),i);
      // EXECUTABLE, MODULE, PACKAGE
      // TYPEVAR, INTERSECTION, UNION
      // NULL, VOID
      // ERROR, NONE, OTHER
      default -> "#"+m;
    };
  }

  TypeMirror componentOf(TypeMirror m) {
    return ((ArrayType)m).getComponentType();
  }
  TypeMirror arrayOf(TypeMirror m) {
    return processingEnv.getTypeUtils().getArrayType(typeArgument(m,0));
  }

  String nameOf(TypeMirror m, boolean input) {
    m = declaredType((DeclaredType)m); // handle Container<T>
    var fqcn = erasure(m).toString();
    var s = schema.get(fqcn);
    var n = s != null ? s.name : fqcn.substring(fqcn.lastIndexOf('.')+1);
    if (input && isType(s)) n += "Input";
    return n;
  }

  /*   interface Name { ... }   */

  void interfaces(SchemaType s) {
    var out = sdl("interface",s.name);
    out.format("# ref = %s%ninterface %s { ... }%n%n", s.fqcn, s.name);
    out.close();
  }

  /*   union Name = Impl | ...   */

  void unions(SchemaType s) {
    var out = sdl("union",s.name);
    out.format("# ref = %s%nunion %s = ... %n%n", s.fqcn, s.name);
    out.close();
  }

  CharSequence directives(Element e, String loc) { // TODO: check if LOCATION matches
    var b = new StringBuilder();
    for (var a:e.getAnnotationMirrors()) if (location(a,loc)) {;
      var s = schema.get(type(a));
      if (isDirective(s)) b.append(" @").append(s.name).append(arguments(a));
    }
    return b;
  }

  CharSequence arguments(AnnotationMirror am) {
    var b = new StringBuilder("(");
    var p = keys(am);
    for (var a:attributes(am)) {
      var k = a.getKey(); // ExecutableElement
      var v = a.getValue(); // AnnotationValue
      var n = str(annotationValue(k,$Alias,"value"));
      if (n != null) {
        if (p.contains(n)) continue; // primary has precedence
      } else {
        n = name(k);
      }
      b.append(' ').append(n).append(':').append(v);
    }
    return b.length() > 1 ? b.append(" )") : "";
  }

  Set<String> keys(AnnotationMirror am) {
    var set = new HashSet<String>();
    for (var k:am.getElementValues().keySet()) set.add(name(k));
    return set;
  }

  boolean location(AnnotationMirror a, String loc) {
    for (var m:annotations(a)) if (type(m).equals($DirectiveMapping)) {
      for (var e:attributes(m)) if (name(e.getKey()).equals("location")) {
        for (var l:list(e.getValue())) if (l.toString().equals(loc)) {
          return true;
        }
      }
    }
    return false;
  }

  // Phase 2: discover types

  void objects() {
    rootType("Query");
    rootType("Mutation");
    rootType("Subscription");
    dataFetchers();
  }

  void dataFetchers() {
    for (var e:mappings.entrySet()) {
      for (var m:e.getValue()) if (m.ref instanceof ExecutableElement x) {
        var s = schema.get(e.getKey());
        if (s != null) s.fields.add(methodElement(x, m.field));
      } // TODO: if not found, is type unused ??
    }
  }

  void rootType(String rootName) {
    var p = rootElement(rootName);
    if (p != null) put(new SchemaType("."+rootName, rootName, ROOT, null, p ));
  }

  List<SchemaField> rootElement(String rootName) {
    var list = mappings.remove(rootName);
    return list != null ? operations(list) : null;
  }

  List<SchemaField> operations(List<Mapping> a) {
    var list = new ArrayList<SchemaField>();
    for (var m:a) if (m.ref instanceof ExecutableElement e) {
      add(list, methodElement(e, fieldName(e)));
    }
    return list;
  }

  TypeMirror outputType(TypeMirror m) {
    objectType(componentType(m), TYPE|OUTPUT);
    return m;
  }

  VariableElement inputType(VariableElement v) {
    objectType(componentType(v.asType()), TYPE|INPUT);
    return v;
  }

  void objectType(TypeElement t, int flag) {
    var fqcn = fqcn(t);
    if (fqcn.startsWith("java.")) return;
    switch (t.getKind()) {
      case ENUM -> enumType(t,fqcn);
      case CLASS -> objectType(t,flag,fqcn);
      default -> {} // ignore
    }
  }

  void enumType(TypeElement t, String fqcn) {
    if (!schema.containsKey(fqcn)) {
      put(new SchemaType(fqcn, mappingName(fqcn), ENUM, t ));
    }
  }

  void objectType(TypeElement t, int flag, String fqcn) {
    var s = schema.get(fqcn);
    if (s != null) {
      if (isScalar(s)) return;
      s.flags |= flag;
      schemaFields(t,flag,null); // to cascade flag to nested types
    } else { // add early to reserve className
      var se = put(new SchemaType(fqcn, mappingName(fqcn), flag, t, new ArrayList<>() ));
      schemaFields(t,flag,se.fields); // fill in class fields/methods
    }
  }

  void schemaFields(TypeElement y, int flag, List<SchemaField> list) {
    for (var p:accessibleElements(y)) switch (p) {
      case TypeElement t -> objectType(t,flag); // CLASS
      case VariableElement v -> add(list, fieldElement(v,flag)); // FIELD
      case ExecutableElement e -> { switch (e.getKind()) {
        case CONSTRUCTOR -> {}
        case METHOD -> add(list, methodElement(e,flag));
        default -> {}
      }}
      default -> {}
    }
  }

  SchemaField fieldElement(VariableElement v, int flag) {
    var fieldName = fieldName(v);
    if (fieldName == null) return null;
    var fieldType = v.asType();
    objectType(componentType(fieldType),flag);
    return fieldName != null ? new SchemaField(fieldName, v, fieldType, null) : null;
  }

  String fieldName(Element e) {
    if (isPublic(e)) {
      return name(e);
    }
    return null;
  }

  SchemaField methodElement(ExecutableElement e, int flag) {
    var methodName = methodName(e);
    return methodName != null ? methodElement(e, methodName) : null;
  }

  SchemaField methodElement(ExecutableElement e, String methodName) {
    var methodType = setter(name(e)) ? argumentType(e) : returnType(e);
    outputType(methodType);
    return new SchemaField(methodName, e, methodType, parameters(e));
  }

  List<VariableElement> parameters(ExecutableElement e) {
    var list = new ArrayList<VariableElement>();
    for (var p:e.getParameters()) {
      if (has(p,$Argument)) add(list, inputType(p));
    }
    return list;
  }

  static String methodName(ExecutableElement e) {
    if (isPublic(e)) {
      var n = name(e);
      if (getter(n) || setter(n)) return n;
    }
    return null;
  }

  static boolean getter(String n) { return prefix(n, "get", "is" ); }
  static boolean setter(String n) { return prefix(n, "set" ); }

  static boolean prefix(String s, String...a) {
    for (var p:a) if (s.startsWith(p)) return true;
    return false;
  }

  static String field(String n) {
    if (getter(n) || setter(n)) {
      for (var i = 0; i < n.length(); i++) {
        var c = n.charAt(i);
        if (Character.isUpperCase(c)) {
          return Character.toLowerCase(c) + n.substring(++i);
        }
      }
    }
    return n;
  }

  static boolean isPublic(Element e) {
    return e.getModifiers().contains(Modifier.PUBLIC);
  }
  static boolean readOnly(Element e) {
    var m = e.getModifiers();
    return m.contains(Modifier.FINAL) || m.contains(Modifier.STATIC);
  }

  String mappingName(String fqcn) {
    var p = fqcn.lastIndexOf('.');
    var f = p < 0 ? fqcn : fqcn.substring(p+1);
    var list = mappings.get(fqcn);
    if (list != null) {
      var i = list.iterator();
      while (i.hasNext()) {
        var m = i.next();
        if (m.ref instanceof TypeElement) {
          f = m.field;
          i.remove();
          break;
        }
      }
      if (list.isEmpty()) {
        mappings.remove(fqcn);
      }
    }
    return f;
  }

  // Phase 1: collect metadata

  void directiveMapping(Element e) {
    if (e instanceof TypeElement te) {
      var am = annotationMirror(te,$DirectiveMapping);
      if (am != null) {
        var directiveName = camelCase(schemaName(te,am,"Wiring"));
        put(new SchemaType(fqcn(te), directiveName, DIRECTIVE, te, am ));
      }
    }
  }

  void scalarMapping(Element e) {
    if (e instanceof TypeElement te) {
      var am = annotationMirror(te,$ScalarMapping);
      if (am != null) {
        var scalarName = schemaName(te,am,"Coercing");
        var scalarType = scalarType(te,am);
        put(new SchemaType(scalarType, scalarName, SCALAR, te, am ));
      }
    }
  }

  String schemaName(TypeElement e, AnnotationMirror am, String suffix) {
    var v = str(actualValue(am,"name"));
    if (v != null) return v;
    var n = name(e);
    return n.endsWith(suffix)? n.substring(0,n.length()-suffix.length()) : n;
  }

  String scalarType(TypeElement e, AnnotationMirror am) {
    var v = str(actualValue(am,"type"));
    return v != null ? v : typeParameter(e,$Coercing);
  }

  String typeParameter(TypeElement e, String interfaceName) {
    for (var i:e.getInterfaces()) {
      var s = i.toString().split("[<,>]");
      if (s[0].equals(interfaceName)) {
        for (var j=1; j < s.length; j++) {
          if (!s[j].startsWith("java.lang.")) return s[j];
        }
      }
    }
    return e.getQualifiedName().toString(); // TODO: check if this is sufficient
  }

  record Mapping(String field, Element ref) {}
  Map<String,List<Mapping>> mappings = new HashMap<>(); // typeName, [field,Element]

  void typeMapping(Element e, String typeName, String fieldName) {
    mappings
      .computeIfAbsent(typeName, _ -> new ArrayList<>())
      .add(new Mapping(fieldName,e));
  }

  void schemaMapping(Element e) {
    var typeName = str(annotationValue(e,$SchemaMapping,"typeName"));
    var field = str(annotationValue(e,$SchemaMapping,"field","value"));
    switch (e) {
      case TypeElement t -> {
        switch (typeName) {
          case "Query", "Mutation", "Subscription" -> System.out.println("TODO: @SchemaMapping on class "+t);
          default -> typeMapping(t, t.getQualifiedName().toString(), typeName ); // use annotated class fqcn as key
        }
      }
      case ExecutableElement x -> {
        if (typeName == null) typeName = argumentType(x).toString();
        if (field == null) field = name(x);
        typeMapping(x, typeName, field ); // use @SchemaMapping(typeName) as key
        objectType(componentType(returnType(x)), TYPE|OUTPUT); // also use x.returnType
      }
      default -> System.out.println("TODO: @SchemaMapping on element "+e);
    }
  }

  TypeMirror returnType(ExecutableElement x) {
    return x.getReturnType();
  }
  TypeMirror argumentType(ExecutableElement x) {
    var p = x.getParameters();
    return p != null && p.size() > 0 ? p.get(0).asType() : nullType;
  }

  void queryMapping(Element e) {
    typeMapping(e,$QueryMapping);
  }
  void mutationMapping(Element e) {
    typeMapping(e,$MutationMapping);
  }
  void subscriptionMapping(Element e) {
    typeMapping(e,$SubscriptionMapping);
  }

  void typeMapping(Element e, String fqcn) {
    var p = fqcn.lastIndexOf('.'); // package.name.[Query,Mutation,Subscription]Mapping
    var typeName = fqcn.substring(p+1,fqcn.length()-7); // clip suffix
    var field = str(annotationValue(e,fqcn,"field","value"));
    typeMapping(e, typeName, field);
  }

  // helpers

  final static String[] BUILTINS = {
    "java.lang.String",  "String",
    "java.lang.Boolean", "Boolean",
    "java.lang.Double",  "Float",
    "java.lang.Float",   "Float",
    "java.lang.Integer", "Int",
    "java.lang.Long",    "Int"
  };

  void builtins() {
    var i=0; while (i < BUILTINS.length) {
      put(new SchemaType(BUILTINS[i++], BUILTINS[i++], BUILTIN|SCALAR, null ));
    }
  }

  TypeElement typeElement(PrimitiveType prim) {
    return processingEnv.getTypeUtils().boxedClass(prim);
  }
  TypeElement typeElement(TypeMirror decl) {
    return (TypeElement) processingEnv.getTypeUtils().asElement(decl);
  }
  TypeElement typeElement(CharSequence name) {
    return processingEnv.getElementUtils().getTypeElement(name);
  }
  TypeMirror typeMirror(CharSequence name) {
    return typeElement(name).asType();
  }

  TypeElement componentType(TypeMirror m) {
    return switch (m) {
      case PrimitiveType p -> typeElement(p);
      case ArrayType a -> typeElement(a.getComponentType());
      case DeclaredType d -> typeElement(declaredType(d)); // isList(d) ? typeArgument(d,0) : d);
      default -> null; // ExecutableType, IntersectionType, NullType,  UnionType
    };                 // ErrorType, NoType, ReferenceType, TypeVariable, WildcardType
  }

  TypeMirror declaredType(DeclaredType d) {
    var t = d.getTypeArguments();
    return t.isEmpty() ? d : t.getFirst(); // optimistic
  }

  boolean isA(TypeMirror a, TypeMirror b) { return processingEnv.getTypeUtils().isSubtype(a,b); }
  boolean isList(TypeMirror m) { return isA(m,collectionType); }
  boolean isEnum(TypeMirror m) { return isA(m,enumType); } // or .isAssignable(m,collectionType);

  Collection<? extends Element> accessibleElements(TypeElement t) {
    var map = new HashMap<String,Element>();
    for (;;) {
      for (var e:t.getEnclosedElements()) {
        map.putIfAbsent(name(e),e);
      }
      var m = t.getSuperclass();
      if (m.toString().equals($Object) || m instanceof NoType) break;
      t = componentType(m);
    }
    map.remove("");
    return map.values();
  }

  TypeMirror typeArgument(TypeMirror m, int index) {
    var a = ((DeclaredType)m).getTypeArguments(); // only the first is used
    var c = a.isEmpty() ? objectType : a.get(index);
    return c instanceof WildcardType w ? wildcardBound(w) : c;
  }

  TypeMirror erasure(TypeMirror m) {
    return processingEnv.getTypeUtils().erasure(m);
  }

  TypeMirror wildcardBound(TypeMirror m) {
    var b = ((WildcardType)m).getExtendsBound();
    if (b == null) b = ((WildcardType)m).getSuperBound();
    return b != null ? b : objectType;
  }

  static String fqcn(TypeElement te) {
    return te != null ? te.getQualifiedName().toString() : null;
  }

  static String name(Element e) {
    return e.getSimpleName().toString();
  }

  static boolean has(Element e, String an) {
    return annotationMirror(e,an) != null;
  }

  static AnnotationMirror annotationMirror(Element e, String an) {
    for (var m:e.getAnnotationMirrors()) {
      if (type(m).equals(an)) return m;
    }
    return null;
  }

  static AnnotationValue annotationValue(Element e, String an, String...fn) {
    var m = annotationMirror(e,an);
    return m != null ? actualValue(m,fn) : null;
  }

  static AnnotationValue annotationDefaultValue(Element e, String an, String...fn) {
    var m = annotationMirror(e,an);
    return m != null ? defaultValue(m,fn) : null;
  }

  static AnnotationValue actualValue(AnnotationMirror m, String...fn) {
    for (var p:attributes(m)) {
      var k = name(p.getKey());
      for (var n:fn) if (k.equals(n)) return p.getValue();
    }
    return null;
  }

  static AnnotationValue defaultValue(AnnotationMirror m, String...fn) {
    for (var p:attributes(m)) {
      var k = name(p.getKey());
      for (var n:fn) if (k.equals(n)) return p.getKey().getDefaultValue();
    }
    return null;
  }

  static boolean isTrue(AnnotationMirror m, String fn) {
    return actualValue(m,fn) instanceof AnnotationValue v ? Boolean.TRUE.equals(v.getValue()) : false;
  }

  static String str(AnnotationValue v) {
    return v != null ? v.getValue().toString() : null;
  }

  static String type(AnnotationMirror m) {
    return m.getAnnotationType().toString();
  }

  static List<AnnotationMirror> annotations(AnnotationMirror m) {
    return (List) m.getAnnotationType().asElement().getAnnotationMirrors();
  }
  static Set<Map.Entry<ExecutableElement, AnnotationValue>> attributes(AnnotationMirror m) {
    return (Set) m.getElementValues().entrySet();
  }
  static List<AnnotationValue> list(AnnotationValue v) {
    return (List) v.getValue();
  }

  static String camelCase(String s) {
    return Character.toLowerCase(s.charAt(0)) + s.substring(1);
  }

  static <T> void add(List<T> list, T item) {
    if (list != null && item != null) list.add(item);
  }

  static String lines(Iterable<String> i) {
    return String.join("\n  ",i);
  }

}