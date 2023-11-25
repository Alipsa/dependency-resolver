package test.alipsa.groovy.resolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.junit.jupiter.api.Test;
import se.alipsa.groovy.resolver.DependencyResolver
import se.alipsa.groovy.resolver.ResolvingException

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager;

import static org.junit.jupiter.api.Assertions.*;

class DependencyResolverTest {

  private static final Logger log = LogManager.getLogger()

  @Test
  void testResolveDependency() throws ResolvingException {
    DependencyResolver resolver = new DependencyResolver()

    List<File> dependencies = resolver.resolve("org.apache.commons:commons-lang3:3.13.0")
    assertEquals(1, dependencies.size())
  }

  @Test
  void testResolveNonLocalDependency() throws IOException, ResolvingException {
    DependencyResolver resolver = new DependencyResolver()
    File libPhoneNumberDir = new File(System.getProperty("user.home"), "/.m2/repository/com/googlecode/libphonenumber")
    if (libPhoneNumberDir.exists()) {
      log.info("Deleting existing " + libPhoneNumberDir)
      libPhoneNumberDir.deleteDir()
    }

    log.info("Resolving libphonenumber:8.13.26");
    List<File> dependencies = resolver.resolve("com.googlecode.libphonenumber:libphonenumber:8.13.26")
    File dir = new File(libPhoneNumberDir, "/libphonenumber/8.13.26")
    List<String> fileNames = List.of("libphonenumber-8.13.26.jar", "libphonenumber-8.13.26.jar.sha1", "libphonenumber-8.13.26.pom")
    assertEquals(1, dependencies.size())
    Arrays.asList(dir.listFiles()).forEach(f -> {
      if (!"_remote.repositories".equals(f.getName())) {
        assertTrue(fileNames.contains(f.getName()), f.getName() + " was unexpected");
      }
    })
  }

  @Test
  void testAddToClasspath() {
    String depScript = '''
    import se.alipsa.groovy.resolver.DependencyResolver
    def resolver = new DependencyResolver(this)
    resolver.addDependency('com.googlecode.libphonenumber:libphonenumber:8.13.26')    
    '''
    String script = '''
    import com.google.i18n.phonenumbers.PhoneNumberUtil
    def numberUtil = PhoneNumberUtil.getInstance()
    def phoneNumber = numberUtil.parse('+46 70 12 23 198', 'SE')
    println "Phone number is ${numberUtil.isValidNumber(phoneNumber) ? '' : 'NOT '}valid"
    '''
    def shell = new GroovyShell()
    shell.evaluate(depScript)
    shell.evaluate(script)

    ScriptEngineManager factory = new ScriptEngineManager()
    ScriptEngine engine = factory.getEngineByName("groovy")
    engine.eval(depScript)
    engine.eval(script)

    GroovyScriptEngineImpl groovyScriptEngine = new GroovyScriptEngineImpl()
    groovyScriptEngine.eval(depScript)
    groovyScriptEngine.eval(script)
  }
}
