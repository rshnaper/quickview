package com.aplombee;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;

/**
 * A custom implementation of {@link QuickView} repeater that tracks
 * the last max and min pages visited and prepends/appends new items based on those
 * tracked page values when user scrolls up or down 
 * @author Rostislav Shnaper
 * @param <T>
 */
public abstract class LazyPageLoadingView<T> extends QuickView<T> {
	private Integer minVisitedPage;
	private Integer maxVisitedPage;

	/**
	 * {@inheritDoc}
	 * @param id
	 * @param dataProvider
	 * @param itemsPerRequest
	 */
	public LazyPageLoadingView(String id, IDataProvider<T> dataProvider, int itemsPerRequest) {
		super(id, dataProvider, new DefaultPagingNavigationStrategy(), itemsPerRequest);
	}


	/**
	 * {@inheritDoc}
	 * @param id
	 * @param dataProvider
	 */
	public LazyPageLoadingView(String id, IDataProvider<T> dataProvider) {
		super(id, dataProvider, new DefaultPagingNavigationStrategy());
	}


	@Override
	protected void _setCurrentPage(int page) {
		int current = getCurrentPage();
		super._setCurrentPage(page);
		
		if(current != page) {
			if(maxVisitedPage == null || page > maxVisitedPage) {
				maxVisitedPage = page;
			}
			if(minVisitedPage == null || page < minVisitedPage) {
				minVisitedPage = page;
			}
		}
	}

	@Override
	public List<Item<T>> addItemsForNextPage() {
        List<Item<T>> list = new ArrayList<Item<T>>();
        
        // page for which new items have to created
        int next = getMaxVisitedPage() + 1;
        if (next < getPageCount()) {
            list = addItemsForPage(next);
           _setCurrentPage(next);
        }
        return list;
	}

	@Override
	public List<Item<T>> addItemsForPreviousPage() {
        List<Item<T>> list = new ArrayList<Item<T>>();
        
        // page for which new items have to created
        int previous = getMinVisitedPage() - 1;
        if (previous >= 0) {
            list = addItemsForPreviousPage(previous);
           _setCurrentPage(previous);
        }
        return list;
	}

	/**
	 * Returns the smallest page number that
	 * was previously visited
	 * @return
	 */
	protected int getMinVisitedPage() {
		return minVisitedPage == null ? getCurrentPage() : Math.min(minVisitedPage, getCurrentPage());
	}

	/**
	 * Returns the largest page number that was previously visited
	 * @return
	 */
	protected int getMaxVisitedPage() {
		return maxVisitedPage == null ? getCurrentPage() : Math.max(maxVisitedPage, getCurrentPage());
	}
	
	static class DefaultPagingNavigationStrategy extends ItemsNavigationStrategy {
		@Override
		public boolean isAlwaysZeroPageCreatedOnRender() {
			return false;
		}
	}
}
