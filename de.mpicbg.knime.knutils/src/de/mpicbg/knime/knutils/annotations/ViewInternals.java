package de.mpicbg.knime.knutils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to mark fields to be part of the settings 
 * saved for KNIME-views by saveInternals/loadInternals
 * 
 * @author Antje Janosch
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented()
public @interface ViewInternals {
}
