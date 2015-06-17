package net.sf.jukebox.jmx;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2000-2015
 */
public class JmxWrapperTest extends TestCase {

    private final Logger logger = Logger.getLogger(getClass());
    private final Random rg = new Random();

    private ObjectName getObjectName() throws MalformedObjectNameException {
        Hashtable<String, String> properties = new Hashtable<String, String>();

        properties.put("id", Double.toString(rg.nextGaussian()));
        return new ObjectName("testDomain", properties);
    }

    public void testExposeNull() throws Throwable {
        
        try {
        
            new JmxWrapper().expose(null, getObjectName(), "null");
            
            fail("Should've failed by now");

        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "target can't be null", ex.getMessage());
        }
    }

    public void testLiteral() throws Throwable {
        new JmxWrapper().expose(new LiteralAccessor(), getObjectName(), "Literal accessor");
        assertTrue("We've made it", true);
    }

    public void testGoodAccessor() throws Throwable {
        new JmxWrapper().expose(new GoodAccessor(), getObjectName(), "Properly named accessor");
        assertTrue("We've made it", true);
    }

    public void testAccessorMutator() throws Throwable {
        new JmxWrapper().expose(new AccessorMutator(), getObjectName(), "Accessor & mutator");
        assertTrue("We've made it", true);
    }

    public void testAccessorBadMutator() throws Throwable {
        new JmxWrapper().expose(new AccessorBadMutator(), getObjectName(), "Good accessor, bad mutator");
        assertTrue("We've made it", true);
    }

    public void testBadAccessorHasArguments() throws Throwable {
        try {
            new JmxWrapper().expose(new BadAccessorHasArguments(), getObjectName(), "Bad accessor signature - takes arguments");
        } catch (IllegalArgumentException e) {
            logger.info(e);
            assertTrue("Null exception message", e.getMessage() != null);
            assertEquals("Unexpected exception message", "name() is not an accessor (takes arguments)", e.getMessage());
        }
    }

    public void testBadAccessorReturnsVoid() throws Throwable {
        try {
            new JmxWrapper().expose(new BadAccessorReturnsVoid(), getObjectName(), "Bad accessor signature - returns void");
        } catch (IllegalArgumentException e) {
            logger.info(e);
            assertTrue("Null exception message", e.getMessage() != null);
            assertEquals("Unexpected exception message", "name() is not an accessor (returns void)", e.getMessage());
        }
    }

    public void testInterfaceDefined() throws Throwable {
        new JmxWrapper().expose(new TheImplementation(), getObjectName(), "Annotation on the interface");
        assertTrue("We've made it", true);
    }
    
    public void testCollectionConstructor() {
        
        Set<Object> targets = new HashSet<Object>();
        
        targets.add(new LiteralAccessor());
        targets.add(new GoodAccessor());
        targets.add(new AccessorMutator());
        targets.add(new AccessorBadMutator());
        targets.add(new BadAccessorHasArguments());
        targets.add(new BadAccessorReturnsVoid());
        targets.add(new TheImplementation());
        targets.add(new TheConcreteSuperclass());
        targets.add("something that is definitely not @JmxAware");
        targets.add(new SimpleJmxAware());
        
        new JmxWrapper(targets);
        assertTrue("We've made it", true);
    }
    
    public void testRegisterNull() {
        
        try {
        
            new JmxWrapper().register(null);
            
            fail("Should've failed by now");

        } catch (IllegalArgumentException ex) {
            
            assertEquals("Wrong exception message", "target can't be null", ex.getMessage());
        }
    }
    
    public void testRegisterNotJmxAware() {
        
        new JmxWrapper().register("something that is definitely not @JmxAware");
        assertTrue("We've made it", true);
    }

    public void testRegisterJmxAware() {
        
        new JmxWrapper().register(new SimpleJmxAware());
        assertTrue("We've made it", true);
    }

    public void testRegisterTwice() {
        
        JmxWrapper w = new JmxWrapper();
        Object target = new SimpleJmxAware();
        
        w.register(target);
        w.register(target);

        assertTrue("We've made it", true);
    }

    static class LiteralAccessor {

        @JmxAttribute(description="just the name")
        public String name() {
            return "name";
        }
    }

    static class GoodAccessor {

        @JmxAttribute(description="just the name")
        public String getName() {
            return "name";
        }
    }

    static class AccessorMutator {

        @JmxAttribute(description="just the name")
        public String getName() {
            return "name";
        }

        public void setName(String name) {
        }
    }

    static class AccessorBadMutator {

        @JmxAttribute(description="just the name")
        public String getName() {
            return "name";
        }

        public void setName(Set<?> name) {
        }
    }

    static class BadAccessorHasArguments {

        @JmxAttribute(description="just the name")
        //@ConfigurableProperty(
        //  propertyName="name",
        //  description="name given"
        //)
        public String name(String key) {
            return "name";
        }
    }

    static class BadAccessorReturnsVoid {

        @JmxAttribute(description="just the name")
        public void name() {
        }
    }

    interface TheInterface {

        @JmxAttribute(description="defined in the interface")
        String getInterfaceDefined();
    }

    class TheConcreteSuperclass {

        @JmxAttribute(description = "defined in the concrete superclass")
        public String getConcreteSuperclassDefined() {
            return "concrete superclass";
        }
    }

    abstract class TheAbstractSuperclass extends TheConcreteSuperclass {

        @JmxAttribute(description = "defined in the abstract superclass")
        public abstract String getAbstractSuperclassDefined();
    }

    class TheImplementation extends TheAbstractSuperclass implements TheInterface {

        public String getInterfaceDefined() {
            return "must be exposed though the annotation is present only on the interface";
        }

        @Override
        public String getAbstractSuperclassDefined() {
            return "must be exposed through the annotation is present only on the abstract superclass";
        }

        @Override
        public String getConcreteSuperclassDefined() {
            return "must be exposed through the annotation is present only on the concrete superclass";
        }
    }
    
    class SimpleJmxAware implements JmxAware {

        @Override
        public JmxDescriptor getJmxDescriptor() {
            return new JmxDescriptor("jukebox", getClass().getSimpleName(), Integer.toHexString(hashCode()), "test case");
        }
    }
}
