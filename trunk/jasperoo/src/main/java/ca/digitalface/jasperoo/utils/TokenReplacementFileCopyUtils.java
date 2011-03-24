/**
 * 
 */
package ca.digitalface.jasperoo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;

/**
 * A utility class to copy template files to the host project, and replace parameter Strings.
 * Taken from the typicalsecurity addon for Roo. (Thanks Rohit)
 * http://code.google.com/p/spring-roo-addon-typical-security/ 
 * @author Rohit Ghatol
 * 
 */
public class TokenReplacementFileCopyUtils extends FileCopyUtils {

	public static int replaceAndCopy(InputStream in, OutputStream out) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		Assert.notNull(out, "No OutputStream specified");
		try {
			int byteCount = 0;
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				byteCount += bytesRead;
			}
			out.flush();
			return byteCount;
		} finally {
			try {
				in.close();
			} catch (IOException ex) {
				throw ex;
			}
			try {
				out.close();
			} catch (IOException ex) {
				throw ex;
			}
		}
	}
	
	
	public static int replaceAndCopy(InputStream in, OutputStream out,	Properties replacement) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		Assert.notNull(out, "No OutputStream specified");
		StringBuffer sb = new StringBuffer();
		try {
			int byteCount = 0;
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, bytesRead));
				byteCount += bytesRead;
			}
			String txt = sb.toString();
			for (Entry<Object, Object> entry : replacement.entrySet()) {
				txt = txt.replaceAll(entry.getKey().toString(), entry
						.getValue().toString());
			}
			out.write(txt.getBytes());
			out.flush();
			return byteCount;
		} finally {
			try {
				in.close();
			} catch (IOException ex) {
				throw ex;
			}
			try {
				out.close();
			} catch (IOException ex) {
				throw ex;
			}
		}
	}
}
