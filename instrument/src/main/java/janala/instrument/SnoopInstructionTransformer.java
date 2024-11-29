package janala.instrument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

@SuppressWarnings("unused") // Registered via -javaagent
public class SnoopInstructionTransformer implements ClassFileTransformer {
  private static final String instDir = Config.instance.instrumentationCacheDir;
  private static final boolean verbose = Config.instance.verbose;
  private static final boolean instrumentGenerators = Boolean.parseBoolean(System.getProperty("jqf.instrument.INSTRUMENT_GENERATORS", "false"));
  private static final boolean instrumentSplitGenerators = Boolean.parseBoolean(System.getProperty("jqf.instrument.INSTRUMENT_SPLIT_GENERATORS", "false"));

  private static String[] banned = {"[", "java/lang", "org/eclipse/collections", "edu/berkeley/cs/jqf/fuzz/util", "janala", "org/objectweb/asm", "sun", "jdk", "java/util/function"};
  private static String[] excludes = Config.instance.excludeInst;
  private static String[] includes = Config.instance.includeInst;
  private static Pattern[] semantic = new Pattern[Config.instance.semanticAnalysisClasses.length];
  static {
    String[] patterns = Config.instance.semanticAnalysisClasses;
    for (int i = 0; i < patterns.length; i++) {
      semantic[i] = Pattern.compile(patterns[i] + ".*");
    }

    // We don't instrument the generators and test drivers by default, but some guidances (like EI) may require it.
    // On the other hand BeDivFuzz-split only wants to instrument one type of generator.
    List<String> allIncludes = new ArrayList<>(Arrays.asList(includes));
    if (instrumentGenerators) {
      allIncludes.add("edu/berkeley/cs/jqf/examples");
    }
    if (instrumentSplitGenerators) {
      allIncludes.add("de/hub/se/jqf/bedivfuzz/examples");
    }
    includes = allIncludes.toArray(new String[0]);
  }

  public static void premain(String agentArgs, Instrumentation inst) throws ClassNotFoundException {

    preloadClasses();

    inst.addTransformer(new SnoopInstructionTransformer(), true);
    if (inst.isRetransformClassesSupported()) {
      for (Class clazz : inst.getAllLoadedClasses()) {
        try {
          String cname = clazz.getName().replace(".","/");
          if (shouldExclude(cname) == false) {
            if (inst.isModifiableClass(clazz)) {
              inst.retransformClasses(clazz);
            } else {
              println("[WARNING] Could not instrument " + clazz);
            }
          }
        } catch (Exception e){
          if (verbose) {
            println("[WARNING] Could not instrument " + clazz);
            e.printStackTrace();
          }
        }
      }
    }
  }

  private static void preloadClasses() throws ClassNotFoundException {
    Class.forName("java.util.ArrayDeque");
    Class.forName("java.util.LinkedList");
    Class.forName("java.util.LinkedList$Node");
    Class.forName("java.util.LinkedList$ListItr");
    Class.forName("java.util.TreeMap");
    Class.forName("java.util.TreeMap$Entry");
    Class.forName("java.util.zip.ZipFile");
    Class.forName("java.util.jar.JarFile");
  }

  /** packages that should be excluded from the instrumentation */
  private static boolean shouldExclude(String cname) {
    for (String e : banned) {
      if (cname.startsWith(e)) {
        return true;
      }
    }
    for (String e : includes) {
      if (cname.startsWith(e)) {
        return false;
      }
    }
    for (String e : excludes) {
      if (cname.startsWith(e)) {
        return true;
      }
    }
    return false;
  }

  @Override
  synchronized public byte[] transform(ClassLoader loader, String cname, Class<?> classBeingRedefined,
      ProtectionDomain d, byte[] cbuf)
    throws IllegalClassFormatException {

    if(cname == null) {
      // Do not instrument lambdas
      return null;
    }
    boolean toInstrument = !shouldExclude(cname);

    if (toInstrument) {
      print("[INFO] ");
      if (classBeingRedefined != null) {
        print("* ");
      }
      print("Instrumenting: " + cname + "... ");
      GlobalStateForInstrumentation.instance.setCid(cname.hashCode());

      if (instDir != null) {
        File cachedFile = new File(instDir + "/" + cname + ".instrumented.class");
        File referenceFile = new File(instDir + "/" + cname + ".original.class");
        if (cachedFile.exists() && referenceFile.exists()) {
          try {
            byte[] origBytes = Files.readAllBytes(referenceFile.toPath());
            if (Arrays.equals(cbuf, origBytes)) {
              byte[] instBytes = Files.readAllBytes(cachedFile.toPath());
              println(" Found in disk-cache!");
              return instBytes;
            }
          } catch (IOException e) {
            print(" <cache error> ");
          }
        }
      }

      byte[] ret = cbuf;
      try {

        ClassReader cr = new ClassReader(cbuf);
        ClassWriter cw = new SafeClassWriter(cr,  loader,
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new SnoopInstructionClassAdapter(cw, cname, semantic);

        cr.accept(cv, 0);

        ret = cw.toByteArray();
      } catch (Throwable e) {
        println("\n[WARNING] Could not instrument " + cname);
        if (verbose) {
          e.printStackTrace();
        }
        return null;
      }

      println("Done!");

      if (instDir != null) {
        try {
          File cachedFile = new File(instDir + "/" + cname + ".instrumented.class");
          File referenceFile = new File(instDir + "/" + cname + ".original.class");
          File parent = new File(cachedFile.getParent());
          parent.mkdirs();
          try(FileOutputStream out = new FileOutputStream(cachedFile)) {
            out.write(ret);
          }
          try(FileOutputStream out = new FileOutputStream(referenceFile)) {
            out.write(cbuf);
          }
        } catch(Exception e) {
          e.printStackTrace();
        }
      }
      return ret;
    } else {
      return cbuf;
    }
  }

  private static void print(String str) {
    if (verbose) {
      System.out.print(str);
    }
  }

  private static void println(String line) {
    if (verbose) {
      System.out.println(line);
    }
  }
}
