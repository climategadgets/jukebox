package net.sf.jukebox.jmx;

import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * A facility to expose objects presented to it via JMX.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2008-2009
 */
public final class JmxWrapper {

    public final Logger logger = Logger.getLogger(getClass());
    
    /**
     * Create an instance.
     */
    public JmxWrapper() {
        
    }
    
    /**
     * Create an instance and register all given objects.
     * 
     * @param targetSet Set of objects to register.
     */
    public JmxWrapper(Set<?> targetSet) {
        
        for (Iterator<?> i = targetSet.iterator(); i.hasNext(); ) {
            register(i.next());
        }
    }


    /**
     * Register the object with the JMX server.
     *
     * @param target Object to register.
     */
    public void register(Object target) {

        if (target == null) {
            throw new IllegalArgumentException("target can't be null");
        }

        ObjectName name = null;
        MBeanServer mbs = null;

        NDC.push("jmxRegister");

        try {

            if (!(target instanceof JmxAware)) {
                logger.warn("Not JmxAware, ignored: " + target);
                return;
            }

            JmxDescriptor jmxDescriptor = ((JmxAware) target).getJmxDescriptor();

            mbs = ManagementFactory.getPlatformMBeanServer();
            StringBuilder sb = new StringBuilder();

            sb.append(jmxDescriptor.domainName).append(":");
            sb.append("name=").append(jmxDescriptor.name).append(",");
            sb.append("instance=").append(jmxDescriptor.instance);

            String pattern = sb.toString();
            logger.info("name: " + pattern);

            name = new ObjectName(pattern);

            expose(target, name, jmxDescriptor.description);

        } catch (InstanceAlreadyExistsException ex) {

            logger.info("Already registered: ", ex);

            NDC.push("again");
            try {
                mbs.unregisterMBean(name);
                expose(this, name, "FIXME");
            } catch (Throwable t) {
                logger.error("Failed", t);
            } finally {
                NDC.pop();
            }
            
        } catch (Throwable t) {
            logger.error("Failed", t);
        } finally {
            NDC.pop();
        }
    }

    /**
     * Expose the object via JMX, as appropriate.
     *
     * If the object doesn't have any {@link JmxAttribute JMX properties}, the method will silently return.
     *
     * @param target Object to analyze and expose.
     * @param name Object name.
     * @param description Object description.
     *
     * @exception IllegalArgumentException if there are inconsistencies in the way the {@link JmxAttribute properties}
     * are described, or the {@code target} is {@code null}.
     * @throws MBeanRegistrationException if there's a problem registering the {@code target}.
     * @throws NotCompliantMBeanException if the {@code target} is not quite what's needed.
     * @throws InstanceAlreadyExistsException if the {@code name} has already been registered.
     */
    public void expose(Object target, ObjectName name, String description) throws NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException {

        if (target == null) {
            throw new IllegalArgumentException("target can't be null");
        }

        NDC.push("expose(" + target.getClass().getSimpleName() + ')');

        try {

            List<MBeanAttributeInfo> operations = new LinkedList<MBeanAttributeInfo>();
            Class<?> targetClass = target.getClass();

            for (Method method : targetClass.getMethods()) {

                logger.debug("analyzing " + method);

                Annotation annotation = getAnnotation(targetClass, method, JmxAttribute.class);

                if (annotation != null) {
                    operations.addAll(expose(target, method, (JmxAttribute) annotation));
                }
            }

            MBeanAttributeInfo[] attributeArray = operations.toArray(new MBeanAttributeInfo[] {});
            MBeanInfo mbInfo = new MBeanInfo(target.getClass().getName(),
                    description,
                    attributeArray,
                    null,
                    null,
                    null,
                    null);
            Proxy proxy = new Proxy(target, mbInfo);

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            mbs.registerMBean(proxy, name);

        } finally {
            NDC.pop();
        }
    }

    private Annotation getAnnotation(Class<?> targetClass, Method method, Class<? extends Annotation> annotationClass) {

        NDC.push("isAnnotationPresent");

        try {

            logger.debug("method " + method);
            logger.debug("annotation " + annotationClass.getSimpleName());

            Annotation annotation = method.getAnnotation(annotationClass);

            if (annotation != null) {
                // This is the simple case...
                logger.debug("simple case");
                return annotation;
            }

            // Well, simple case didn't work. Three options are possible now:
            // a) the annotation is present on the interface;
            // b) the annotation is present on the superclass (FIXME: make sure both abstract and concrete are covered);
            // c) it is not present at all.

            Class<?>[] interfaces = targetClass.getInterfaces();

            for (int offset = 0; offset < interfaces.length; offset++) {
                Class<?> anInterface = interfaces[offset];
                annotation = getAnnotation(anInterface, method.getName(), annotationClass);

                if (annotation != null) {
                    return annotation;
                }
            }

            Class<?> superClass = targetClass.getSuperclass();

            return superClass == null ? null : getAnnotation(superClass, method.getName(), annotationClass);

        } finally {
            NDC.pop();
        }
    }

    private Annotation getAnnotation(Class<?> targetClass, String methodName, Class<? extends Annotation> annotationClass) {
        NDC.push("isAnnotationPresent(" + methodName + ')');
        try {
            return getAnnotation(targetClass, targetClass.getMethod(methodName), annotationClass);
        } catch (NoSuchMethodException ignored) {
            // Oh well...
            logger.debug("no");
            return null;
        } finally {
            NDC.pop();
        }
    }

    /**
     * Expose a method off the target.
     *
     * @param target Object to expose the method of.
     * @param method The method to expose.
     *
     * @param annotation Annotation to extract the metadata from.
     * @return Operation signature.
     */
    private List<MBeanAttributeInfo> expose(Object target, Method method, JmxAttribute annotation) {

        NDC.push("exposeMethod");

        logger.debug(target.getClass().getName() + '#' + method.getName() + " found to be JmxAttribute");

        try {

            String name = resolveAccessorName(method);

            logger.info(target.getClass().getName() + '#' + method.getName() + ": exposed as " + name);
            logger.info("description: " + annotation.description());

            return expose(target, name, annotation.description(), method, resolveMutator(target, method, name));

            // VT: FIXME: It might be a good idea to further check the method sanity. Or not
            //method.invoke(target, property);

        } finally {
            NDC.pop();
        }
    }

    /**
     * Expose the method[s] via JMX.
     *
     * @param target Object to expose the method[s] of.
     * @param name Name to expose as.
     * @param description Human readable description.
     * @param accessor Accessor method.
     * @param mutator Matching mutator method, or {@code null} if none available.
     *
     * @return Operation signature.
     */
    private List<MBeanAttributeInfo> expose(Object target,
            String name,
            String description,
            Method accessor, Method mutator) {
        NDC.push("expose+");

        try {

            logger.info("name:     " + name);
            logger.info("type:     " + accessor.getReturnType().getName());
            logger.info("accessor: " + accessor);
            logger.info("mutator:  " + mutator);

            List<MBeanAttributeInfo> result = new LinkedList<MBeanAttributeInfo>();

            MBeanAttributeInfo accessorInfo = new MBeanAttributeInfo(
                    name,
                    accessor.getReturnType().getName(),
                    description,
                    true,
                    mutator != null,
                    accessor.getName().startsWith("is"));

            logger.debug("accessor: " + accessorInfo);

            result.add(accessorInfo);

            /*
      if (false)
      if (mutator != null) {

        //MBeanAttributeInfo mutatorInfo = new MBeanAttributeInfo(description, mutator);

        //logger.debug("mutator: " + mutatorInfo);

        //result.add(mutatorInfo);
      }
             */

            return result;

        } finally {
            NDC.pop();
        }
    }

    /**
     * Resolve a mutator method matching the accessor method in type and name.
     *
     * @param target Object to resolve the method on.
     * @param accessor Accessor method to find the match for.
     * @param name Exposed method name. @return Matching mutator method, or {@code null}, if none is found.
     *
     * @return The method found, or {@code null} if none.
     */
    private Method resolveMutator(Object target, Method accessor, String name) {
        NDC.push("resolveMutator(" + name + ")");
        try {

            Class<?> returnType = accessor.getReturnType();
            String mutatorName = "set" + upperFirst(name);

            logger.debug("trying " + mutatorName + '(' + returnType.getSimpleName() + ')');

            Method mutator = target.getClass().getMethod(mutatorName, returnType);

            logger.debug("got method: " + mutator);

            return mutator;

        } catch (NoSuchMethodException e) {
            // This is normal
            logger.info("No mutator found: " + e.getMessage());
            return null;
        } finally {
            NDC.pop();
        }
    }

    /**
     * Resolve the attribute name.
     *
     * @param method Method to resolve the exposed name of.
     *
     * @return Name to expose as.
     *
     * @exception IllegalStateException if the method is not an accessor (is void or has arguments).
     */
    private String resolveAccessorName(Method method) {

        if (!isAccessor(method)) {
            throw new IllegalStateException("Shouldn't have ended up here (see the code");
        }

        String name = method.getName();

        if (name.startsWith("is")) {
            return lowerFirst(name.substring(2));
        }

        if (name.startsWith("get")) {
            return lowerFirst(name.substring(3));
        }

        if (name.startsWith("set")) {
            throw new IllegalStateException("Method name doesn't conform to accessor pattern (need isX or getX): " + name);
        }

        return name;
    }

    /**
     * Check if the method is an accessor method.
     *
     * @param method Method to check.
     *
     * @return {@code true} if the method is indeed an accessor.
     *
     * @exception IllegalArgumentException if the method is not an accessor.
     */
    private boolean isAccessor(Method method) {

        if (method.getParameterTypes().length != 0) {
            throw new IllegalArgumentException(method.getName() + "() is not an accessor (takes arguments)");
        }

        logger.debug("returns " + method.getReturnType().getName());

        if (method.getReturnType().getName().equals("void")) {
            throw new IllegalArgumentException(method.getName() + "() is not an accessor (returns void)");
        }

        return true;
    }

    /**
     * Convert the first character to lower case.
     *
     * No sanity checks beyond {@code null} or empty are done - if the string is all caps, the result will be exactly
     * what you'd think it is.
     *
     * @param source Source string.
     * @return The source string with the first character converted to lower case, or {@code null}
     * or empty string, if the source was {@code null} or empty.
     */
    public static String lowerFirst(String source) {

        if (source == null || source.isEmpty()) {
            return source;
        }

        if (source.length() == 1) {
            return source.toLowerCase();
        }

        return source.substring(0, 1).toLowerCase() + source.substring(1);
    }

    /**
     * Convert the first character to upper case.
     *
     * @param source Source string.
     * @return The source string with the first character converted to upper case, or {@code null}
     * or empty string, if the source was {@code null} or empty.
     */
    public static String upperFirst(String source) {

        if (source == null || source.isEmpty()) {
            return source;
        }

        if (source.length() == 1) {
            return source.toUpperCase();
        }

        return source.substring(0, 1).toUpperCase() + source.substring(1);
    }

    private class Proxy implements DynamicMBean {

        private final Object target;
        private final Class<?> targetClass;
        private final MBeanInfo mbInfo;
        private final Map<String, MBeanAttributeInfo> name2attribute = new TreeMap<String, MBeanAttributeInfo>();
        private final Map<String, Method> name2accessor = new TreeMap<String, Method>();

        private Proxy(Object target, MBeanInfo mbInfo) {

            if (target == null) {
                throw new IllegalArgumentException("target can't be null");
            }

            NDC.push("constructor");
            try {

                this.target = target;
                this.mbInfo = mbInfo;

                targetClass = target.getClass();

                MBeanAttributeInfo[] attributes = mbInfo.getAttributes();
                for (int offset = 0; offset < attributes.length; offset++) {
                    MBeanAttributeInfo attributeInstance = attributes[offset];
                    String attributeName = attributeInstance.getName();
                    logger.debug("attribute: " + attributeName);

                    name2attribute.put(attributeName, attributeInstance);
                    name2accessor.put(attributeName, resolve(attributeName, attributeInstance.isIs()));
                }

                for (Iterator<String> i = name2accessor.keySet().iterator(); i.hasNext(); ) {
                    String name = i.next();
                    logger.debug("accessor resolved for " + name + ": " + name2accessor.get(name));
                }

            } finally {
                NDC.pop();
            }
        }

        private Method resolve(String methodName, boolean isIs) {

            NDC.push("resolve(" + methodName + ')');

            try {

                if (isIs) {
                    return resolve("is" + upperFirst(methodName), false);
                }

                try {

                    // Try straight (this is the last resort for the case of getX)

                    try {

                        Method targetMethod = targetClass.getMethod(methodName);

                        logger.debug("resolved " + targetMethod);

                        return targetMethod;

                    } catch (NoSuchMethodException e) {

                        throw e;
                    }

                } catch (NoSuchMethodException e) {

                    // If the name is isX or getX, we're screwed

                    if (methodName.startsWith("is") || methodName.startsWith("get") ) {
                        throw new IllegalStateException("Unable to find method '" + methodName + "', is* and get* tried too");
                    }

                    // Didn't work, try getX
                    logger.debug("Invocation failed: " + e.getMessage());
                    return resolve("get" + upperFirst(methodName), false);
                }
            } finally {
                NDC.pop();
            }
        }

        public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {

            NDC.push("getAttribute(" + attribute + ')');
            try {

                Method method = name2accessor.get(attribute);

                if (method == null) {
                    throw new AttributeNotFoundException(attribute);
                }

                return method.invoke(target);

            } catch (IllegalAccessException e) {
                throw new ReflectionException(e, "oops");
            } catch (InvocationTargetException e) {
                throw new ReflectionException(e, "oops");
            } finally {
                NDC.pop();
            }
        }

        public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
            throw new UnsupportedOperationException();
        }

        public AttributeList getAttributes(String[] attributes) {

            NDC.push(getAttributeListString(attributes));
            try {

                AttributeList attributeList = new AttributeList();

                for (int offset = 0; offset < attributes.length; offset++) {
                    String attributeName = attributes[offset];
                    Method method = name2accessor.get(attributeName);

                    try {

                        Object value = method.invoke(target);
                        attributeList.add(new Attribute(attributeName, value));

                        logger.debug(attributeName + ": " + value);

                    } catch (IllegalAccessException e) {
                        logger.error("invocation failed, attribute skipped: " + attributeName, e);
                    } catch (InvocationTargetException e) {
                        logger.error("invocation failed, attribute skipped: " + attributeName, e);
                    }
                }

                return attributeList;

            } finally {
                NDC.pop();
            }
        }

        private String getAttributeListString(String[] attributes) {

            StringBuilder sb = new StringBuilder("getAttributes(");

            for (int offset = 0; offset < attributes.length; offset++) {
                if (offset != 0) {
                    sb.append(", ");
                }
                sb.append(attributes[offset]);
            }
            sb.append(')');

            return sb.toString();
        }

        public AttributeList setAttributes(AttributeList attributes) {
            throw new UnsupportedOperationException();
        }

        public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
            throw new UnsupportedOperationException();
        }

        public MBeanInfo getMBeanInfo() {
            return mbInfo;
        }
    }
}
