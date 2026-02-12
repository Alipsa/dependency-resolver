package test.alipsa.groovy.resolver

import groovy.transform.CompileStatic
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.alipsa.groovy.resolver.DependencyResolver
import se.alipsa.groovy.resolver.ResolvingException

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import java.nio.file.Files

import static org.junit.jupiter.api.Assertions.*

/**
 * These tests are primarily intended to verify that the DependencyResolver can be used in various contexts
 * (GroovyShell, JSR223, GroovyScriptEngine) and that the constructor correctly identifies and rejects non-GroovyClassLoader classloaders.
 *
 * The tests that resolves dependencies from remote repositories are tagged as "integration" since they require network access and may be slower.
 */
class DependencyResolverTest {

  private static final Logger log = LoggerFactory.getLogger(DependencyResolverTest)

  String depScript = '''
    import se.alipsa.groovy.resolver.DependencyResolver
    def resolver = new DependencyResolver(this)
    resolver.addDependency('com.googlecode.libphonenumber:libphonenumber:8.13.26')    
    '''

  String script = '''
    import com.google.i18n.phonenumbers.PhoneNumberUtil
    def numberUtil = PhoneNumberUtil.getInstance()
    def phoneNumber = numberUtil.parse('+46 70 12 23 198', 'SE')
    numberUtil.isValidNumber(phoneNumber)
    '''

  @Test
  @Tag("integration")
  void testResolveDependency() throws ResolvingException {
    DependencyResolver resolver = new DependencyResolver()
    List<File> dependencies = resolver.resolve("org.apache.commons:commons-lang3:3.13.0")
    assertEquals(1, dependencies.size())
    assertTrue(dependencies.every { it.exists() })
  }

  @Test
  @Tag("integration")
  void testResolveNonLocalDependency() throws IOException, ResolvingException {
    DependencyResolver resolver = new DependencyResolver()
    log.info("Resolving libphonenumber:8.13.26")
    List<File> dependencies = resolver.resolve("com.googlecode.libphonenumber:libphonenumber:8.13.26")
    assertEquals(1, dependencies.size())
    File jarFile = dependencies.first()
    File pomFile = new File(jarFile.parentFile, jarFile.name.replace(".jar", ".pom"))
    assertTrue(jarFile.exists(), "Expected ${jarFile.absolutePath} to exist")
    assertTrue(pomFile.exists(), "Expected ${pomFile.absolutePath} to exist")
  }

  @Test
  @Tag("integration")
  void testAddToClasspathShell() {
    def shell = new GroovyShell()
    shell.evaluate(depScript)
    def result = shell.evaluate(script)
    assertTrue(result as Boolean, "Expected script to evaluate to true but was ${result}")
  }

  @Test
  @Tag("integration")
  void testAddToClasspathJsr223() {

    ScriptEngineManager factory = new ScriptEngineManager()
    ScriptEngine engine = factory.getEngineByName("groovy")
    engine.eval(depScript)
    def result = engine.eval(script)
    assertTrue(result as Boolean, "Expected script to evaluate to true but was ${result}")

    GroovyScriptEngineImpl gseJsr223 = new GroovyScriptEngineImpl()
    gseJsr223.eval(depScript)
    result = gseJsr223.eval(script)
    assertTrue(result as Boolean, "Expected script to evaluate to true but was ${result}")
  }

  @Test
  @Tag("integration")
  void testAddToClasspathScriptEngine() {
    File tempDir = Files.createTempDirectory("groovy-scripts").toFile()
    File deptScriptFile = new File(tempDir, "deptScript.groovy")
    File scriptFile = new File(tempDir, "script.groovy")
    deptScriptFile.text = depScript
    scriptFile.text = script
    URL[] roots = [tempDir.toURI().toURL()]
    GroovyScriptEngine gse = new GroovyScriptEngine(roots)
    def binding = new Binding()
    def depResult = gse.run(deptScriptFile.getName(), binding)
    assertNull(depResult)
    def scriptResult = gse.run(scriptFile.getName(), binding)
    assertTrue(scriptResult as Boolean, "Expected script to evaluate to true but was ${scriptResult}")
    tempDir.deleteDir()
  }


  @Test
  void testURLClassLoaderConstructor() {
    // When a GroovyClassLoader is held in a URLClassLoader-typed variable,
    // the URLClassLoader constructor should be selected instead of falling
    // through to DependencyResolver(Object), which would incorrectly call
    // cl.getClass().getClassLoader().
    URLClassLoader cl = new GroovyClassLoader()
    DependencyResolver resolver = new DependencyResolver(cl)
    assertNotNull(resolver)
  }

  @Test
  @CompileStatic
  void testObjectConstructorGroovyClassLoader() {
    ClassLoader cl = new GroovyClassLoader()
    DependencyResolver resolver = new DependencyResolver(cl)
    assertNotNull(resolver, "Expected constructor to succeed when given a GroovyClassLoader as a ClassLoader")

    def nonUrlLoader = new ClassLoader() {
      @Override
      protected Class<?> findClass(String name) throws ClassNotFoundException {
        // You can leave this empty for a "rainy day" failure test
        // or implement logic to load bytes from a Map/String.
        throw new ClassNotFoundException(name)
      }
    }

    assertThrows(IllegalArgumentException,
        () -> new DependencyResolver(nonUrlLoader),
        "Expected constructor to throw IllegalArgumentException when given a non-URLClassLoader"
    )
  }

  @Test
  void testObjectCallerConstructor() {
    GroovyClassLoader gcl = new GroovyClassLoader()
    Class<?> clazz = gcl.parseClass("class Dummy {}")
    Object dummyObject = clazz.getDeclaredConstructor().newInstance()
    DependencyResolver resolver = new DependencyResolver(dummyObject)
    assertNotNull(resolver, "The constructor should succeed when given an object whose class is loaded by a GroovyClassLoader")
  }

  @Test
  void testConstructorRejectsNonGroovyClassLoader() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      new DependencyResolver(String.class)
    }
    assertTrue(ex.message.contains("groovy classloader"))

    // Rainy day test to verify that the Object constructor from a non-Groovy class also throws the expected exception
    assertThrows(IllegalArgumentException) {
      new DependencyResolver("Hello")
    }
  }
}
