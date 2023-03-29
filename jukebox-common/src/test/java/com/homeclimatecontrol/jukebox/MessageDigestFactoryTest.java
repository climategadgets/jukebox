package com.homeclimatecontrol.jukebox;

import com.homeclimatecontrol.jukebox.util.MessageDigestFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
class MessageDigestFactoryTest {

    private static final String message = "I know the word";

    @Test
    void testMD5() {

        String digest = new MessageDigestFactory().getMD5(message);
        assertThat(digest).isEqualTo("084848e5ff80a02c58ced8d7307aa7b6");
    }

    @Test
    void testSHA() {

        String digest = new MessageDigestFactory().getSHA(message);
        assertThat(digest).isEqualTo("876d1e8893c76b77429faa02ee33d3312ce447c0");
    }
}
