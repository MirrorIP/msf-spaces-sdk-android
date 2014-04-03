package de.imc.mirror.sdk.android.filter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.DataObjectFilter;

/**
 * This meta filter represents a logical AND. IF and only if any child filter validates the data object,
 * this filter will validate it too.
 * 
 * @author simon.schwantzer(at)im-c.de
 */
public class AndFilter implements de.imc.mirror.sdk.filter.AndFilter {
	private Set<DataObjectFilter> childFilters;
	
	/**
	 * Creates an AND filter with the given child filters.
	 * @param filters Child filters to add.
	 */
	public AndFilter(DataObjectFilter ... filters) {
		childFilters = new HashSet<DataObjectFilter>();
		for (DataObjectFilter filter : filters) {
			childFilters.add(filter);
		}
	}
	
	@Override
	public boolean isDataObjectValid(DataObject dataObject) {
		for (DataObjectFilter filter : childFilters) {
			if (!filter.isDataObjectValid(dataObject)) return false;
		}
		return true;
	}

	@Override
	public DataObjectFilter addFilter(DataObjectFilter filter) {
		childFilters.add(filter);
		return this;
	}

	@Override
	public Set<DataObjectFilter> getFilters() {
		return Collections.unmodifiableSet(childFilters);
	}
	
}
