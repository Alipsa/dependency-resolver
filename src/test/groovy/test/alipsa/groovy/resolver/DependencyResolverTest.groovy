package test.alipsa.groovy.resolver

import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.alipsa.groovy.resolver.DependencyResolver
import se.alipsa.groovy.resolver.ResolvingException

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

import static org.junit.jupiter.api.Assertions.*

class DependencyResolverTest {

  private static final Logger log = LoggerFactory.getLogger(DependencyResolverTest)

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
    assert numberUtil.isValidNumber(phoneNumber)
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

  @Test
  void testConstructorRejectsNonGroovyClassLoader() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      new DependencyResolver(String.class)
    }
    assertTrue(ex.message.contains("groovy classloader"))
  }
}
