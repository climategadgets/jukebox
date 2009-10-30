package net.sf.jukebox.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A simple utility class to produce an SHA message digest of a message.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class MessageDigestFactory {

    /**
     * Produce an SHA message digest.
     * 
     * @param message Message to get SHA digest for.
     * @return A string representation of the message digest.
     * @throws NoSuchAlgorithmException if by some miracle SHA algorithm is not
     * available.
     */
    public String getSHA(String message) {

        try {
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MessageDigest sha = MessageDigest.getInstance("SHA");
            DigestOutputStream dos = new DigestOutputStream(baos, sha);
            PrintWriter pw = new PrintWriter(dos);

            pw.print(message);
            pw.flush();

            byte md[] = dos.getMessageDigest().digest();

            StringBuffer sb = new StringBuffer();

            for (int offset = 0; offset < md.length; offset++) {

                byte b = md[offset];

                if ((b & 0xF0) == 0) {

                    sb.append("0");
                }

                sb.append(Integer.toHexString(b & 0xFF));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            
            // Can't afford to throw a checked exception,
            // and this is a pretty rare, but pretty severe problem,
            // it's OK to fast fail here
            throw new IllegalStateException("Can't create a SHA message disgest", ex);
        }
    }
}
