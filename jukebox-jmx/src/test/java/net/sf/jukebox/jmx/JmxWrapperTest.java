package net.sf.jukebox.jmx;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

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
        new JmxWrapper().expose(new GoodAccessor(), getObjectName(), "Properly named get* accessor");
        assertTrue("We've made it", true);
    }

    public void testIsAccessor() throws Throwable {
        new JmxWrapper().expose(new IsAccessor(), getObjectName(), "Properly named is* accessor");
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

    public void testBadAccessorWrongName() throws Throwable {
        try {
            new JmxWrapper().expose(new BadAccessorWrongName(), getObjectName(), "Bad accessor signature - returns void");
        } catch (IllegalArgumentException e) {
            logger.info(e);
            assertTrue("Null exception message", e.getMessage() != null);
            assertEquals("Unexpected exception message", "setName(): method name doesn't conform to accessor pattern (need isX or getX)", e.getMessage());
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
    
    public void testLowerFirst() {
        
        assertEquals(null, JmxWrapper.lowerFirst(null));
        assertEquals("", JmxWrapper.lowerFirst(""));
        assertEquals("a", JmxWrapper.lowerFirst("A"));
        assertEquals("1", JmxWrapper.lowerFirst("1"));
        assertEquals("tEST", JmxWrapper.lowerFirst("TEST"));
    }

    public void testUpperFirst() {
        
        assertEquals(null, JmxWrapper.upperFirst(null));
        assertEquals("", JmxWrapper.upperFirst(""));
        assertEquals("A", JmxWrapper.upperFirst("a"));
        assertEquals("1", JmxWrapper.upperFirst("1"));
        assertEquals("Test", JmxWrapper.upperFirst("test"));
    }
    
    public void testAttributes() throws Throwable {
        
        NDC.push("testAttributes");
        
        try {
        
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            SimpleJmxAware target = new SimpleJmxAware();
            JmxWrapper w = new JmxWrapper();
            
            w.register(target);
            
            JmxDescriptor jmxDescriptor = target.getJmxDescriptor();
            
            StringBuilder sb = new StringBuilder();
            
            sb.append(jmxDescriptor.domainName).append(":");
            sb.append("name=").append(jmxDescriptor.name).append(",");
            sb.append("instance=").append(jmxDescriptor.instance);
    
            String pattern = sb.toString();
            logger.info("getAttribute name: " + pattern);
    
            ObjectName name = new ObjectName(pattern);
            
            Set<ObjectInstance> found = mbs.queryMBeans(name, null);
            
            logger.info("found: " + found);
            
            assertEquals("Wrong object instance count", 1, found.size());
            assertEquals("Wrong name found", name, found.iterator().next().getObjectName());
            
            {
                // The good behavior case
                
                Object code = mbs.getAttribute(name, "code");

                logger.info("getAttribute(code): " + code);

                assertEquals("Wrong code extracted", target.getCode(), code);

                mbs.setAttribute(name, new Attribute("code", "DUDE"));

                logger.info("setAttribute(code): " + target.getCode());

                assertEquals("Wrong code set", target.getCode(), "DUDE");
            }

            {
                // The bad behavior case
                
                try {
                    
                    // An unexpected exception in an accessor
                    
                    mbs.getAttribute(name, "error");
                    fail("Should've failed by now");
                    
                } catch (ReflectionException ex) {
                    
                    assertEquals("Wrong exception message", "Nobody expects the Spanish Inquisition!", ex.getCause().getCause().getMessage());
                }

                try {
                    
                    // An unexpected exception in a mutator
                    
                    mbs.setAttribute(name, new Attribute("error", "DUDE"));
                    fail("Should've failed by now");

                } catch (RuntimeMBeanException ex) {
                    
                    assertEquals("Wrong exception message", "NOBODY expects the Spanish Inquisition!", ex.getCause().getCause().getCause().getMessage());
                }

                try {
                    
                    // Nonexistent accessor
                    
                    mbs.getAttribute(name, "nonexistent");
                    fail("Should've failed by now");
                    
                } catch (AttributeNotFoundException ex) {
                    
                    assertEquals("Wrong exception message", "nonexistent", ex.getMessage());
                }

                try {
                    
                    // Nonexistent mutator
                    
                    mbs.setAttribute(name, new Attribute("nonexistent", "DUDE"));
                    fail("Should've failed by now");
                    
                } catch (AttributeNotFoundException ex) {
                    
                    assertEquals("Wrong exception message", "nonexistent", ex.getMessage());
                }

                try {
                    
                    // Inaccessible accessor
                    
                    Object secret = mbs.getAttribute(name, "secret");
                    fail("Should've failed by now");
                    
                } catch (AttributeNotFoundException ex) {
                    
                    assertEquals("Wrong exception message", "secret", ex.getMessage());
                }

                try {
                    
                    // Inaccessible mutator
                    
                    mbs.setAttribute(name, new Attribute("secret", "DUDE"));
                    fail("Should've failed by now");
                    
                } catch (AttributeNotFoundException ex) {
                    
                    assertEquals("Wrong exception message", "secret", ex.getMessage());
                }
            }

            try {
            
                mbs.invoke(name, "hashCode", null, null);
            
            } catch (RuntimeMBeanException ex) {
                assertEquals("Wrong exception message", "Not Supported Yet: invoke(hashCode)", ex.getCause().getMessage());
            }
            
            
        } finally {
            NDC.pop();
        }
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

    static class IsAccessor {

        @JmxAttribute(description="is enabled?")
        public boolean isEnabled() {
            return true;
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

    static class BadAccessorWrongName {

        @JmxAttribute(description="just the name")
        public String setName() {
            return "name";
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

        private String code = Integer.toHexString(rg.nextInt());
                
        @Override
        public JmxDescriptor getJmxDescriptor() {
            return new JmxDescriptor("jukebox", getClass().getSimpleName(), Integer.toHexString(hashCode()), "test case");
        }
        
        @JmxAttribute(description = "random code")
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }

        @JmxAttribute(description = "broken method")
        public String getError() {
            throw new NullPointerException("Nobody expects the Spanish Inquisition!");
        }
        
        public void setError(String error) {
            throw new NullPointerException("NOBODY expects the Spanish Inquisition!");
        }

        @JmxAttribute(description = "inaccessible method")
        private String getSecret() {
            return "secret";
        }
        
        private void setSecret(String secret) {
        }
    }
}
