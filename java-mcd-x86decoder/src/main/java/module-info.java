/**
 * module-info
 */
module de.carne.mcd.x86decoder {

	requires java.xml;

	requires org.eclipse.jdt.annotation;

	requires de.carne;
	requires transitive de.carne.mcd;

	exports de.carne.mcd.x86decoder;

}