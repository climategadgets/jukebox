package net.sf.jukebox.datastream.logger.impl.udp;

import net.sf.jukebox.util.network.HostHelper;
import net.sf.jukebox.datastream.logger.impl.AbstractLogger;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.conf.ConfigurableProperty;
import net.sf.jukebox.jmx.JmxAttribute;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.NDC;

/**
 * An abstract logger that provides logging by broadcasting UDP packets.
 * Provides a base for {@link XapLogger xAP logger} and {@link XplLogger xPL logger}.
 *
 * @param <E> Data type to log.
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2009
 */
public abstract class UdpLogger<E extends Number> extends AbstractLogger<E> {

    /**
     * Date format to use.
     */
    private static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * Socket port to broadcast on.
     */
    private int port;

    /**
     * Socket to broadcast to.
     */
    private DatagramSocket socket;

    /**
     * Host name. It is resolved at {@link #startup startup}, to avoid
     * repetitive invocations.
     */
    private String hostname;

    /**
     * Mapping of a host address to a network address. Used in
     * {@link #resolveNetworkAddress resolveNetworkAddress()}.
     */
    private final Map<InetAddress, InetAddress> host2network = new HashMap<InetAddress, InetAddress>();

    /**
     * Set of local addresses we can't support.
     */
    private final Set<InetAddress> unsupported = new HashSet<InetAddress>();

    /**
     * Create an instance with no listeners.
     * 
     * @param port Port to bind to.
     */
    public UdpLogger(int port) {
	
	this(null, port);
    }
    
    /**
     * Create an instance listening to given data sources.
     * 
     * @param producers Data sources to listen to.
     * @param port Port to bind to.
     */
    public UdpLogger(Set<DataSource<E>> producers, int port) {
        super(producers);
        
        // VT: FIXME: not good enough, no checks. setPort() would be a better option,
        // but it's not yet implemented.
        this.port = port;
    }

    /**
     * Get the default port to broadcast on.
     * 
     * @return The port.
     */
    @JmxAttribute(description = "Default port")
    public abstract int getDefaultPort();

    /**
     * Get the port to broadcast on.
     * 
     * @return The port.
     */
    @JmxAttribute(description = "Current port")
    public final int getPort() {
	return port;
    }
    
    @ConfigurableProperty(
	    propertyName = "port",
	    description = "Port to bind to"
		)
    public final void setPort(int port) {
	
	// Theoretically, it's a good idea. Practically, it's a pain - need to shutdown and start again.
	
	throw new IllegalStateException("Not Implemented");
    }

    /**
     * Get a string that will be used as a source signature.
     * 
     * @return {@link #hostname host name}.
     */
    @JmxAttribute(description = "Source signature")
    public final String getSource() {
	return hostname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void startup() throws Throwable {
	
	NDC.push("startup");
	
	try {

	    socket = new DatagramSocket();
	    socket.setBroadcast(true);

	    hostname = InetAddress.getLocalHost().getHostName();

	    int dotOffset = hostname.indexOf(".");

	    if (dotOffset != -1) {

		hostname = hostname.substring(0, dotOffset);
	    }

	    logger.info("Using host name: " + hostname);

	    // There's nothing we can possibly do at startup in subclasses
	} finally {
	    NDC.pop();
	}

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final synchronized void shutdown() throws Throwable {

	// There's nothing we can possibly do at shutdown in subclasses

	socket.close();
	socket = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void consume(String signature, DataSample<E> value) {

	if (!isEnabled()) {

	    // Oops... Slipped through the cracks. Can't afford to send() -
	    // the socket may be null by this time.

	    logger.warn("update() ignored - not enabled");
	    return;
	}

	StringBuilder sb = new StringBuilder();

	writeHeader(sb);
	writeData(sb, signature, value);

	String packet = sb.toString();

	logger.debug("Packet:\n" + packet);
	logger.debug("Packet size: " + packet.length());

	try {

	    send(packet);

	} catch (IOException ex) {

	    // There's nothing we can do about it
	    logger.warn("send() failed:", ex);
	}
    }

    /**
     * Write a protocol header.
     * 
     * @param sb String buffer to write the header to.
     */
    protected abstract void writeHeader(StringBuilder sb);

    /**
     * Write a protocol data.
     * 
     * @param sb String buffer to write the header to.
     * @param signature Channel signature to use.
     * @param value Data value.
     */
    protected abstract void writeData(StringBuilder sb, String signature, DataSample<E> value);

    /**
     * Resolve a host address into a network address.
     * 
     * @param address Address to resolve.
     * @return Class C network address corresponding to the given network address.
     * 
     * @throws UnknownHostException if the address cannot be resolved.
     */
    private synchronized InetAddress resolveNetworkAddress(
	    final InetAddress address) throws UnknownHostException {

	if (address == null) {
	    throw new IllegalArgumentException("address can't be null");
	}

	// Let's see if we know how to handle it

	if (!(address instanceof Inet4Address)) {

	    // nope

	    if (unsupported.contains(address)) {

		// I know, I know
		return null;
	    }

	    unsupported.add(address);

	    // Let's tell them so they don't get confused about a disappeared
	    // address
	    logger
		    .warn("Don't know how to handle address " + address
			    + " - it's a " + address.getClass().getName()
			    + ", skipped");

	    // and now bail out
	    return null;
	}

	InetAddress result = host2network.get(address);

	if (result == null) {

	    // VT: FIXME: This is an interface address. We have to make it a
	    // broadcast address. Code below is a copy'n'paste from
	    // SimpleBroadcastServer. This will only work with class C
	    // network.

	    // In some (unknown) circumstances, the algorithm below fails to
	    // parse the address. Let's try to log what's going on

	    try {

		// VT: FIXME: This could've been done in a more elegant way
		// if byte representation was used

		StringTokenizer st = new StringTokenizer(address
			.getHostAddress(), ".");

		// VT: FIXME: Damn it, is it 0 or 255? I remember 255, but
		// why does 0 work???

		String targetAddress = st.nextToken() + "." + st.nextToken()
			+ "." + st.nextToken() + ".0";

		result = InetAddress.getByName(targetAddress);

		logger.info("Host address " + address
			+ " translated into network address " + result);

	    } catch (NoSuchElementException ex) {

		logger.warn("Unable to parse address into components: "
			+ address, ex);
		logger.warn("Packets will be sent to interface (not network) address");

		result = address;
	    }

	    host2network.put(address, result);
	}

	return result;
    }

    /**
     * Send the message out.
     * 
     * @param message Message to broadcast.
     * @throws IOException If there was an I/O error.
     * @throws SocketException If there was a network problem.
     */
    private void send(String message) throws IOException {

	byte[] data = message.getBytes();
	int sent = 0;

	for (Iterator<InetAddress> i = HostHelper.getLocalAddresses()
		.iterator(); i.hasNext();) {

	    InetAddress address = resolveNetworkAddress(i.next());

	    if (address == null) {
		// This is OK, it'll happen if the address is not a v4 address
		continue;
	    }

	    DatagramPacket packet = new DatagramPacket(data, message.length(),
		    address, port);

	    // This try/catch is to improve diagnostics: underlying
	    // exceptions don't tell us much about where exactly the problem
	    // lies, and at least address information is lost. So we catch
	    // it and rethrow it, augmented with what we know about it.

	    try {

		socket.send(packet);
		sent++;

		logger.debug("Sent packet to " + address + ":" + port);

	    } catch (IOException ioex) {

		// We're not going to break it now, there may be more addresses
		// to send stuff to

		logger.warn("socket.send(" + address + ":" + port + ") failed", ioex);
	    }
	}

	if (sent == 0) {

	    logger.error("Couldn't send a packet to any of the addresses, check your network setup: "
			    + HostHelper.getLocalAddresses());
	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void createChannel(String name, String signature,
	    long timestamp) {

	signature2name.put(signature, name);
    }

    /**
     * Resolve channel name by signature.
     * 
     * @param signature
     *            Signature to resolve.
     * @return Channel name, or <code>null</code> if unknown.
     */
    protected final String getChannelName(String signature) {

	return signature2name.get(signature);
    }

    /**
     * Replace newlines with spaces so the message can be stuffed into a single
     * line.
     * 
     * @param message Original message.
     * @return Message converted to a single line.
     */
    protected final String normalize(String message) {

	if (message == null) {
	    return null;
	}

	if (!message.contains("\n")) {
	    return message;
	}

	StringBuilder sb = new StringBuilder();

	for (int offset = 0; offset < message.length(); offset++) {

	    char c = message.charAt(offset);

	    if (c == '\n') {
		sb.append(" ");
	    } else {
		sb.append(c);
	    }
	}

	return sb.toString();
    }

    /**
     * Get the value string.
     * 
     * @param sample Data sample.
     * @return The value as as a string representation of a number, or empty
     * string if the sample is an error sample.
     */
    protected final String getValueString(DataSample<E> sample) {

	if (sample.isError()) {

	    return "";
	}

	return Double.toString(sample.sample.doubleValue());
    }

    /**
     * Get the error string.
     * 
     * @param sample Data sample.
     * 
     * @return Normalized exception message, or literal @code null} if there's no message.
     * 
     * @exception IllegalStateException
     * if the sample is not an error sample. This is done to
     * prevent wasting valuable UDP packet space - I don't want
     * to fragment the packets...
     */
    protected final String getErrorString(DataSample<E> sample) {

	if (!sample.isError()) {

	    throw new IllegalStateException(
		    "You're not supposed to get the error string if the sample is not an error sample, call isError() first");
	}

	return normalize(sample.error.getMessage());
    }

    /**
     * Get a timestamp for the given format.
     * 
     * @param time Time to get the timestamp for.
     * @return Timestamp as a string, according to {@link #dateFormat date format used}.
     */
    protected final String getTimestamp(long time) {

	String timestamp;
	Date date = new Date(time);

	try {
	    
	    timestamp = new SimpleDateFormat(dateFormat).format(date);
	    
	} catch (Throwable t) {
	    // This is bad, but better than nothing
	    timestamp = date.toString();
	}

	return timestamp;
    }

    /**
     * Get a human readable description of the class functionality.
     * 
     * @return The description.
     */
    protected abstract String getDescription();
}
