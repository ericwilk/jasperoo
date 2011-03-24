package ca.digitalface.jasperoo;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * The Spring ROO command class. The command class is registered by the Roo shell following an
 * automatic classpath scan.
 * 
 * @since 0.1.0
 * @author Waldo Rochow
 * 
 */
@Component
@Service
public class JasperooCommands implements CommandMarker {
	
	/**
	 * Get a reference to the JasperooOperations from the underlying OSGi container
	 */
	@Reference private JasperooOperations operations;
	
	/**
	 * This method is optional. It allows automatic command hiding in situations when the command should not be visible.
	 * For example the 'entity' command will not be made available before the user has defined his persistence settings 
	 * in the Roo shell or directly in the project.
	 * 
	 * You can define multiple methods annotated with {@link CliAvailabilityIndicator} if your commands have differing
	 * visibility requirements.
	 * 
	 * @return true (default) if the command should be visible at this stage, false otherwise
	 *  
	 */
	@CliAvailabilityIndicator({"jasperoo setup", "jasperoo add", "jasperoo all" })
	public boolean isCommandAvailable() {
		return operations.isCommandAvailable();
	}

	/**
	 * Generate a report for the entity specified. 
	 * 
	 * @param target <b>mandatory</b>, The entity to list in this report.
	 * @param finder <b>not mandatory</b>, help = "If specified, this finder will produce the resultset to be passed to the List report.
	 * @param entityPackage <b>not mandatory</b>, The location of the Entities. Default: "~.domain".
	 * @param controllerPackage <b>not mandatory</b>, The location of the Web Controllers. Default: "~.web".
	 */
	@CliCommand(value = "jasperoo add", help = "Generate a report for the entity specified.")
	public void add(@CliOption(key = "type", mandatory = true, help = "The entity to list in this report.") JavaType target,
			@CliOption(key = "finder", mandatory = false, help = "If specified, this finder will produce the resultset to be passed to the List report.") String finder,
			@CliOption(key = "entityPackage", mandatory = false, help = "The location of the Entities.", unspecifiedDefaultValue="~.domain") String entityPackage, 
			@CliOption(key = "controllerPackage", mandatory = false, help = "The location of the Web Controllers.", unspecifiedDefaultValue="~.web") String controllerPackage) {
		operations.addReportByType(target, finder, entityPackage, controllerPackage);
	}

	/**
	 * Generate a "List" report for all project entities. Same as calling "<b>jasperoo add</b>" on each entity.
	 * 
	 * @param entityPackage <b>not mandatory</b>, The location of the Entities. Default: "~.domain".
	 * @param controllerPackage <b>not mandatory</b>, The location of the Web Controllers. Default: "~.web".
	 */
	@CliCommand(value = "jasperoo all", help = "Generate a \"List\" report for all project entities. Same as calling \"jasperoo add\" on each entity.")
	public void all(@CliOption(key = "entityPackage", mandatory = false, help = "The location of the Entities.", unspecifiedDefaultValue="~.domain") String entityPackage, 
			@CliOption(key = "controllerPackage", mandatory = false, help = "The location of the Web Controllers.", unspecifiedDefaultValue="~.web") String controllerPackage) {
		operations.addReportsForAll(entityPackage, controllerPackage);
	}

	/**
	 * Integrate Jasper Reports into this Roo application. Must be called before calling "<b>jasperoo add</b>" or "<b>jasperoo all</b>".
	 * 
	 * @param controllerPackage <b>not mandatory</b>, The location of the Web Controllers. Default: "~.web".
	 * @param formats <b>not mandatory</b>, A comma separated list of report formats to support. Options: pdf, xls, csv, html, odt, xml, and rtf. Default: "pdf,xls".
	 */
	@CliCommand(value = "jasperoo setup", help = "Integrate Jasper Reports into this Roo application.")
	public void setup(@CliOption(key = "controllerPackage", mandatory = false, help = "The location of the Web Controllers.", unspecifiedDefaultValue="~.web") String controllerPackage,
			@CliOption(key = "formats", mandatory = false, help = "A comma separated list of report formats to support. Options: pdf, xls, csv, html, odt, xml, and rtf. Default: \"pdf,xls\".", unspecifiedDefaultValue="pdf,xls") String formats) {
		operations.setup(controllerPackage, formats);
	}
}