package com.github.nill14.parsers.dependency;

import java.util.Set;

public interface IDependencyCollector {

	
	/**
	 * a dependsOn(string) or consumes(string)
	 * @return
	 */
	Set<String> getRequiredDependencies();
	
	Set<String> getOptionalDependencies();

	/**
	 * All providers (inversive dependency) are optional. 
	 * Nobody is forced to consume the service.
	 * 
	 * a isPrerequisiteOf(string) or produces(string)
	 * @return
	 */
	Set<String> getOptionalProviders();

	/**
	 * 
	 * @return a name of self-provider (included automatically in {@link #getOptionalProviders()}
	 */
	String getName();

}
