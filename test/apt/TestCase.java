package apt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.spi.ToolProvider;

import graphql.language.Document;
import graphql.schema.GraphqlTypeComparatorRegistry;
import graphql.schema.diff.DiffEvent;
import graphql.schema.diff.DiffLevel;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.SchemaDiffSet;
import graphql.schema.diff.reporting.DifferenceReporter;
import graphql.schema.idl.SchemaPrinter;

public class TestCase {

  protected boolean printDocs = false;

  static final String[] args = {
    "-proc:only",
    "-processor", "tools.gqls.SchemaGenerator",
    "-sourcepath","./test"
  }; // -s directory ...sourcefiles

  static String[] args(String directory, String...sourcefiles) {
    var i = args.length;
    var j = sourcefiles.length;
    var a = new String[i + 2 + j];
    System.arraycopy(args,0,a,0,i);
    a[i++] = "-s"; a[i++] = directory;
    System.arraycopy(sourcefiles,0,a,i,j);
    return a;
  }

  protected String make(String targetFolder, String...sourceFiles) {
    ToolProvider.findFirst("javac").get()
      .run(System.out, System.err, args(targetFolder,sourceFiles));
    return targetFolder;
  }

  protected boolean check(String directory) {
    var ref = read("schema.graphql");
    var tgt = read(directory,"**.gqls");
    return diff(ref,tgt) == 0;
  }

  int diff(String a, String b) {
    var diffSet = SchemaDiffSet.diffSetFromSdl(a,b);
    var options = SchemaDiff.Options.defaultOptions().enforceDirectives();
    return new SchemaDiff(options).diffSchema(diffSet,
      new DifferenceReporter() {
        @Override
          public void report(DiffEvent de) {
          if (DiffLevel.INFO.equals(de.getLevel())) return;
          System.out.println(print(de));
        }
        @Override
        public void onEnd() {
          if (printDocs) {
            System.out.println("old -> \n"+print(diffSet.getOldSchemaDefinitionDoc()));
            System.out.println("new -> \n"+print(diffSet.getNewSchemaDefinitionDoc()));
          }
        }
      });
  }

  void print(String tag, String text) {
    System.out.println(tag+" -> "+text);
  }

  String print(DiffEvent de) {
    return de.toString()
      .replace("{ ","{\n  ")
      .replace(", ",",\n  ")
      .replace("}","\n}");
  }

  String print(Document doc) {
    return new SchemaPrinter(SchemaPrinter.Options
      .defaultOptions()
      .descriptionsAsHashComments(true)
      .includeDirectiveDefinitions(false)
      .includeDirectives(false)
      .setComparators(GraphqlTypeComparatorRegistry.BY_NAME_REGISTRY)
      ).print(doc);
  }

  String read(String directory, String glob) { // **.gqls
    try {
      var buffer = new StringBuilder();
      var matcher = FileSystems.getDefault().getPathMatcher("glob:"+glob);
      Files
        .find(Paths.get(directory), 9, (p,_) -> matcher.matches(p)) // attr.isRegularFile()
        .forEach(p -> buffer.append(read(p)));
      return buffer.toString();
    } catch (IOException ioe) { throw new UncheckedIOException(ioe); }
  }

  String read(Path path) {
    try { return new String(Files.readAllBytes(path)); }
    catch (IOException ioe) { throw new UncheckedIOException(ioe); }
  }

  String read(String resourceName) {
    try { return new String(this.getClass().getResourceAsStream(resourceName).readAllBytes()); }
    catch (IOException ioe) { throw new UncheckedIOException(ioe); }
  }

}