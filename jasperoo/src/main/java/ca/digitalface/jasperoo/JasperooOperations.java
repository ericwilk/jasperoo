package ca.digitalface.jasperoo;

import org.springframework.roo.model.JavaType;

/**
 * Interface of operations this add-on offers. Typically used by a command type or an external add-on.
 *
 * @since 1.1
 */
public interface JasperooOperations {

	/**
	 * Indicate commands should be available
	 * 
	 * @return true if it should be available, otherwise false
	 */
	boolean isCommandAvailable();

	/**
	 * Add a report based on the provided Java type.
	 */
	void addReportByType(JavaType type, Long id, String finder, String entityPackage, String controllerPackage);
	
	/**
	 * Add a List report for all Java types.
	 */
	void addListReportForAll(String entityPackage, String controllerPackage);
	
	/**
	 * Setup all add-on artifacts
	 */
	void setup(String controllerPackage);
}