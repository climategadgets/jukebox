package net.sf.jukebox.jmx;

/**
 * The template definition for the handler that handles the {@link
 * javax.management.DynamicMBean#invoke
 * javax.management.DynamicMBean#invoke} method.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2000
 * @version $Id: JmxInvocationHandler.java,v 1.2 2007-06-14 04:32:12 vtt Exp $
 * @since Jukebox 2.0p5
 */
abstract public class JmxInvocationHandler {

    /**
     * Perform the <code>invoke()</code> operation on the target object.
     *
     * The action name is absent because this object is invoked as a target
     * from a {@link JmxHelper dispatcher}, and the action name is
     * implicitly present there.
     *
     * @param target The object to get the information from.
     *
     * @param params Call parameters
     *
     * @param signature Call signature
     *
     * @return The object that will be returned as a result of
     * <code>invoke()</code> call.
     */
    abstract public Object invoke(Object target, Object params[], String signature[]);
}
