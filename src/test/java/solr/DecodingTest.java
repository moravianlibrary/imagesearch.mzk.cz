package solr;

import junit.framework.TestCase;
import net.semanticmetadata.lire.imageanalysis.JCD;
import net.semanticmetadata.lire.imageanalysis.PHOG;
import org.apache.commons.codec.binary.Base64;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: mlux
 * Date: 15.07.13
 * Time: 13:13
 * To change this template use File | Settings | File Templates.
 */
public class DecodingTest extends TestCase {
    public void testDecoding() throws IOException {
        String hist;
        hist = "fmxNOiUWFxcWFxocHi1SblUoJSk5NyswMDI3PE1ren9IKx0SFBgWHiUsOjxYZVQ1LzxWYDk4Ojo8OTJdf1xGOyAVFxcWFRgRERooIR4XHR8iJSMqLTMvPVlrRjsvIhgNDw4QDxEWFClPf1omHBsnHxgcHR0iHiw+f2xPPSgXFhMQERMSEh5FWkobHCEwLSgrKiw0PlBsf104IwkGAgEAAQACAAICAAICAAMFBQMJECMlNzBPJiQjHx0WHBUcIzQ5R1FYdXNIOEFef05NOjMqIiEbAgIEAQAAAgMKCw4RFitZf2ApFRseFxILBQMCAQECf29KMTEeGBMUGRMWEBMQDxQUEhEjIyE9UE1MQEZyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAXn44GBQICBYPFBQaJR1GQCYZHy5ANR0aKCEsIBlVRTgyJRgMDhELBwoPDB5Cf1AbGhMfFQ0PDhAbGzA+fmQ4WwkHHQQAARglCD4WKBQGIAENHAwbEBIcFlNVAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAf1o6Kx4VFRITExQRDhgqJBsZHBcXIRwiKispPFhwf2JHLh4XFBQUDxAQDyRSYkYWEhspJCYiKiQ0ODZhX39VSzgWCg0ABAkEAgAFCAkEBQEEEgoMCSMbKSYZAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAf2FZUCMUGx4ZGRwQFBskGx8SHSgrJCwzKTw3QF1lf3NXSy4UGxYQFRoWGyJHaWMoLjRIQTNBNTk5RnB+bkEmJx8mDAYICwgKBAYMDQcLCwQJEwkKFxcfLUd/";
        hist = "fmxNOiUWFxcWFxocHi1SblUoJSk5NyswMDI3PE1ren9IKx0SFBgWHiUsOjxYZVQ1LzxWYDk4Ojo8OTJdf1xGOyAVFxcWFRgRERooIR4XHR8iJSMqLTMvPVlrRjsvIhgNDw4QDxEWFClPf1omHBsnHxgcHR0iHiw+f2xPPSgXFhMQERMSEh5FWkobHCEwLSgrKiw0PlBsf104IwkGAgEAAQACAAICAAICAAMFBQMJECMlNzBPJiQjHx0WHBUcIzQ5R1FYdXNIOEFef05NOjMqIiEbAgIEAQAAAgMKCw4RFitZf2ApFRseFxILBQMCAQECf29KMTEeGBMUGRMWEBMQDxQUEhEjIyE9UE1MQEZyAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAXn44GBQICBYPFBQaJR1GQCYZHy5ANR0aKCEsIBlVRTgyJRgMDhELBwoPDB5Cf1AbGhMfFQ0PDhAbGzA+fmQ4WwkHHQQAARglCD4WKBQGIAENHAwbEBIcFlNVAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAf1o6Kx4VFRITExQRDhgqJBsZHBcXIRwiKispPFhwf2JHLh4XFBQUDxAQDyRSYkYWEhspJCYiKiQ0ODZhX39VSzgWCg0ABAkEAgAFCAkEBQEEEgoMCSMbKSYZAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAf2FZUCMUGx4ZGRwQFBskGx8SHSgrJCwzKTw3QF1lf3NXSy4UGxYQFRoWGyJHaWMoLjRIQTNBNTk5RnB+bkEmJx8mDAYICwgKBAYMDQcLCwQJEwkKFxcfLUd/";
        byte[] bytes = Base64.decodeBase64(hist);
        PHOG p = new PHOG();
        p.setByteArrayRepresentation(bytes);
        System.out.println(Arrays.toString(p.getDoubleHistogram()));

        BufferedImage img = ImageIO.read(new File("munch.jpg"));
//        BufferedImage img = ImageIO.read(new File("test.jpg"));
        PHOG g = new PHOG();
        g.extract(img);
        System.out.println(g.getDistance(p));
    }

    public void testJcdDecoding() {
        String hist = "gYKUk/Siw6GAoYPkxLPG0tXEgcGj08W1xNLl06HCgICClbKRwdCAgA==";
        byte[] bytes = Base64.decodeBase64(hist);
        JCD j = new JCD();
        j.setByteArrayRepresentation(bytes);
    }

}
