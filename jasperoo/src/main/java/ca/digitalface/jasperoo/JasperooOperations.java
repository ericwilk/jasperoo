package ca.digitalface.jasperoo;

import org.springframework.roo.model.JavaType;

/**
 * Interface of operations this add-on offers.
 *
 * @since 0.1.0
 * @author Waldo Rochow
 */
public interface JasperooOperations {

	/**
	 * Indicate commands should be available
	 * 
	 * @return true if it should be available, otherwise false
	 */
	boolean isCommandAvailable(String commandName);

	/**
	 * Add a report based on the provided Java type.
	 * 
	 * @param type The entity to list in this report.
	 * @param finder help = "If specified, this finder will produce the resultset to be passed to the report.
	 * @param entityPackage The location of the Entities. Default: "~.domain".
	 * @param controllerPackage The location of the Web Controllers. Default: "~.web".
	 */
	void addReportByType(JavaType type, String finder, String entityPackage, String controllerPackage);

	/**
	 * Add a List report for all Java types.
	 * @param entityPackage The location of the Entities. Default: "~.domain".
	 * @param controllerPackage The location of the Web Controllers. Default: "~.web".
	 */
	void addReportsForAll(String entityPackage, String controllerPackage);

	/**
	 * Setup all add-on artifacts
	 * @param controllerPackage The location of the Web Controllers. Default: "~.web".
	 * @param formats A comma separated list of report formats to support. 
	 * 					Options: pdf, xls, csv, html, odt, xml, and rtf. Default: "pdf,xls"
	 */
	void setup(String controllerPackage, String formats);

	/**
	 * Extend Jasperoo to support additional formats.
	 * 
	 * @param formats A comma separated list of report formats to support. 
	 * 					Options: pdf, xls, csv, html, odt, xml, and rtf. Default: "pdf,xls"
	 */
	void extend(String formats);
}