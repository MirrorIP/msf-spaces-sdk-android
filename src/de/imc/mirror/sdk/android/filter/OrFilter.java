package de.imc.mirror.sdk.android.filter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.DataObjectFilter;

/**
 * This meta filter represents a logical OR. If a single child filter validates the data object,
 * this filter will validate it too.
 * 
 * @author simon.schwantzer(at)im-c.de
 */
public class OrFilter implements de.imc.mirror.sdk.filter.OrFilter {
	private Set<DataObjectFilter> childFilters;
	
	/**
	 * Creates an OR filter with the given child filters.
	 * @param filters Child filters to add.
	 */
	public OrFilter(DataObjectFilter ... filters) {
		childFilters = new HashSet<DataObjectFilter>();
		for (DataObjectFilter filter : filters) {
			childFilters.add(filter);
		}
	}

	@Override
	public boolean isDataObjectValid(DataObject dataObject) {
		for (DataObjectFilter filter : childFilters) {
			if (filter.isDataObjectValid(dataObject)) return true;
		}
		return false;
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
