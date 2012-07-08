/**
 *
 Copyright 2012 Vineet Semwal

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.repeater;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * base class for {@link QuickView}
 *
 * @author Vineet Semwal
 */
public abstract class QuickViewBase<T> extends RepeatingView implements IQuickView {

      //items created per request ,if used with PagingNavigator/AjaxPagingNavigator then it's the items per page
    private int itemsPerRequest=Integer.MAX_VALUE;

    public int getItemsPerRequest() {
        return itemsPerRequest;
    }

    /**
     * for newchildId
     */
    private long index=0;
    public long getIndex(){
        return  index;
    }


    /**
     * increment index by the number passed in argument
     * @param number  number by which index is incremented
     * @return   new index
     */
    protected long incrementIndexByNumber(int number){
        return index=index+number;
    }
    protected void clearChildId(){
        index=0;
    }

    public void setItemsPerRequest(int items) {
        if (items < 1)
        {
            throw new IllegalArgumentException("itemsPerRequest cannot be less than 1");
        }

        if (this.itemsPerRequest != items)
        {
            if (isVersioned())
            {
                addStateChange();
            }
        }

        this.itemsPerRequest = items;

        // because items per page can effect the total number of pages we always
        // reset the current page back to zero
        _setCurrentPage(0);
    }

    private transient long itemsCount=-1;


    private IDataProvider<T> dataProvider;

    public IDataProvider<T> getDataProvider() {
        return dataProvider;
    }

    /**
     * reuse strategy
     */
    private ReUse reuse;

    @Override
    public ReUse getReuse() {
        return reuse;
    }

    /**
     * set reuse strategy
     * <p/>
     * for paging ie. when used with {@link org.apache.wicket.markup.html.navigation.paging.PagingNavigator} or {@link org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator}  the
     * {@link ReUse.DEFAULT_PAGING} is preferred
     * <p/>
     * for rows navigation purpose {@link ReUse.DEFAULT_ROWSNAVIGATOR} is preferred
     *
     * @param reuse
     */
    public void setReuse(ReUse reuse) {
        Args.notNull(reuse,"reuse");
        if(reuse==ReUse.NOT_INITIALIZED){
            throw  new IllegalArgumentException("reuse can't be set to NOT_INITIALIZED ");
        }
        this.reuse = reuse;
    }

    public IRepeaterUtil getRepeaterUtil() {
       return RepeaterUtil.get();
    }

    private long currentPage;


    /**
     * @param id              component id
     * @param dataProvider    dataprovider of objects
     * @param reuse           children are created again on render
     *
     */
    public QuickViewBase(String id, IDataProvider<T> dataProvider, ReUse reuse) {
        super(id);
        Args.notNull(dataProvider, "dataProvider");
        Args.notNull(reuse, "reuse");
        this.reuse = reuse;
         this.dataProvider = dataProvider;
    }


    /**
     * @param object model object
     * @param id     child id
     * @return Child created
     */
    protected Item<T> newItem(long id, T object) {
        Item<T> item = new Item<T>(String.valueOf(id), getRepeaterUtil().safeLongToInt(id), getDataProvider().model(object));
        item.setMarkupId(String.valueOf(id));
        item.setOutputMarkupId(true);
        return item;
    }

    public Item buildCompleteItem(String id, T object) {
        return buildCompleteItem(Long.parseLong(id),object);
    }

    public Item buildCompleteItem(long id, T object) {
        Item<T> item = newItem(id, object);
        item.setMarkupId(String.valueOf(id));
        item.setOutputMarkupId(true);
        populate(item);
        return item;
    }

    public boolean isAjax() {
        return getWebRequest().isAjax();
    }

    /**
     *  it's a simple add,new item is not drawn just added,no js fired
     * @param c component to be added
     * @return this
     */
    public MarkupContainer simpleAdd(Component... c) {
         super.add(c);
        incrementIndexByNumber(c.length)  ;
        return this;
    }




    /**
     * it's a simple remove,the item is just removed from quickview ,no js fired
     * @param c
     * @return this
     */
    public MarkupContainer simpleRemove(Component c) {
        super.remove(c);
        incrementIndexByNumber(-1)        ;
         return this;
    }

    public MarkupContainer simpleRemoveAll() {
        clearChildId();
        return super.removeAll();
    }

    public void reuseNotInitialized(){
        if (ReUse.NOT_INITIALIZED == reuse) {
            throw new RuntimeException("reuse strategy is not set or you have set  ReUse.NOT_INITIALIZED ");
        }
    }

    @Override
    protected void onPopulate() {
        super.onPopulate();
        clearCachedItemCount();
        reuseNotInitialized();
        nonJsRemoveAllIfNotReuse();
        long current=_getCurrentPage();
        if (size() == 0) {

             // all children might have got removed ,if true then create children of last page
             //for first render currentpage will be 0

            if (ReUse.DEFAULT_PAGING == reuse) {
                createChildren(current);
            }


             //   not first render but items were removed

            if ((ReUse.DEFAULT_ROWSNAVIGATOR == reuse) )  {
                createChildren(0);
                _setCurrentPage(0);
            }


             // first render,no children were added,so populated with first page

            if((ReUse.ALL == reuse) || (ReUse.CURRENTPAGE == reuse)){
               createChildren(0);
            }
        }
        /**
         * stategy is reuse and it's not first render
         */
        else {
            /**
             * if reuse=CURRENTPAGE ,reuse the current page elements and remove all others
             */
            if (ReUse.CURRENTPAGE == reuse) {
                /**
                 * remove first page till the current page -1
                 */
                removePages(0, current - 1);
                /**
                 * remove the page after current page till the lastpage is found
                 */
                removePages(current + 1, _getPageCount() - 1);
            }
        }

    }

    public Item<T> getItem(long index) {
        return (Item) get(getRepeaterUtil().safeLongToInt(index));
    }

    @Override
    public String newChildId() {
        return String.valueOf(index);
    }

    /**
     * remove pages from startpage till stop page, including stopPage
     *
     * @param startPage
     * @param stopPage
     */
    public MarkupContainer removePages(long startPage, long stopPage) {
        for (long i = startPage; i <= stopPage; i++) {
            long startIndex = i * itemsPerRequest;
            long endIndex = startIndex + itemsPerRequest;
            for (long itemIndex = startIndex; itemIndex < endIndex; itemIndex++) {
                Item item = getItem(itemIndex);
                if (item != null) {
                    simpleRemove(item);
                }
            }
        }
        return this;
    }

    /**
     * creates children for the page provided
     *
     * @param page
     */
    protected void createChildren(long page) {
        long items = page * getItemsPerRequest();
        Iterator<? extends T> iterator = getDataProvider().iterator(items, getItemsPerRequest());
        for (long i = items; iterator.hasNext(); i++) {
            T obj = iterator.next();
            Component item = buildCompleteItem(i, obj);
            simpleAdd(item);
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
         response.render(JavaScriptHeaderItem.forReference( RepeaterUtilReference.get()));
         }

    public final long getItemsCount(){
        if(itemsCount>=0){
            return itemsCount;
        }
           itemsCount=getDataProvider().size();
            return itemsCount;
    }

    private void clearCachedItemCount()
    {
        itemsCount= -1;
    }

    public long getRowsCount(){
        if(!isVisibleInHierarchy()){
            return 0;
        }
        return getItemsCount();
    }

    /**
     * calculates the number of pages
     *
     *
     * @return  number of pages
     */

    @Override
    public final long getPageCount() {
        return _getPageCount();
    }

    /**
     * don't override ,it's used for testing purpose
     *
     * @return number of pages
     */
    protected long _getPageCount() {
        long total = getRowsCount();

        long count = total / itemsPerRequest;
        if ((itemsPerRequest * count) < total) {
            count++;
        }
        return count;
    }

    /**
     * @see org.apache.wicket.markup.html.navigation.paging.IPageable#getCurrentPage()
     *
     * don't override
     */

    @Override
    public final long getCurrentPage() {
      return   _getCurrentPage();
    }

    /**
     * don't override,it's for internal use
     *
     */
    protected  long _getCurrentPage() {
        long page = currentPage;

        /*
        * trim current page if its out of bounds this can happen if items are added/deleted between
        * requests
        */

        final long count=_getPageCount();
        if (page > 0 && page >= count) {
            page = Math.max(count - 1, 0);
            currentPage = page;
            return page;
        }
        return page;
    }

    /**
     * @see org.apache.wicket.markup.html.navigation.paging.IPageable#setCurrentPage(long)
     */

    public final void setCurrentPage(long page) {
        _setCurrentPage(page);
    }

    /**
     * don't override,it's for internal use
     *
     */
    protected void _setCurrentPage(long page) {
        if (currentPage != page) {
            if (isVersioned()) {
                addStateChange();

            }
        }
        currentPage = page;
    }

    public AjaxRequestTarget getAjaxRequestTarget() {
        return RequestCycle.get().find(AjaxRequestTarget.class);
    }


    protected abstract void populate(Item<T> item);

    /**
     * checks if parent of repeater is added to the components added to
     * A.R.T(ajaxrequesttarget)
     *
     * @return true if parent of repeatingview is added to A.R.T
     */
    public boolean isParentAddedInAjaxRequestTarget() {
        MarkupContainer searchFor = _getParent();
        AjaxRequestTarget target = getAjaxRequestTarget();
        Collection<? extends Component> cs = target.getComponents();
        if (cs == null) {
            return false;
        }
        if (cs.isEmpty()) {
            return false;
        }
        //if repeater's parent is added to component return true
        if (cs.contains(searchFor)) {
            return true;
        }
        //search repeater's parent in children of components added in A.R.T
        boolean found = false;
        for (Component c : cs) {
            if (c instanceof MarkupContainer) {
                MarkupContainer mc = (MarkupContainer) c;
                Boolean result = addNewChildVisitor(mc, searchFor);
                if (Boolean.TRUE.equals(result)) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    /**
     * @param parent    parent on which ChildVisitor is added
     * @param searchFor ,searchFor is the component which visitor search for
     * @return true if searchFor is found
     */
    protected Boolean addNewChildVisitor(MarkupContainer parent, Component searchFor) {
        return parent.visitChildren(new ChildVisitor(searchFor));
    }

    public static class ChildVisitor implements IVisitor<Component, Boolean> {

        private Component searchFor;

        public ChildVisitor(Component searchFor) {
            this.searchFor = searchFor;
        }

        public void component(Component c, IVisit<Boolean> visit) {
            if (searchFor.getPageRelativePath().equals(c.getPageRelativePath())) {
                visit.stop(true);
            }
        }
    }

    /**
     * don't override,it's for internal use
     *
     */
    protected MarkupContainer _getParent() {
        return getParent();
    }



    @Override
    public MarkupContainer add(final Component... c) {
        simpleAdd(c);
        if (!isAjax()) {
            return this;
        }
        if (isParentAddedInAjaxRequestTarget()) {
            return this;
        }
        AjaxRequestTarget target = getAjaxRequestTarget();
        for (int i = 0; i < c.length; i++) {
            MarkupContainer parent = _getParent();
            String script = getRepeaterUtil().insertAfter((Item)c[i], parent);
            target.prependJavaScript(script);
        }
        target.add(c);

        return this;
    }

    /**
     *
     * create and draw children for the provided page ,number of
     * children created are smaller than equal to getItemsPerRequest()
     *
     * @param page
     * @return   list of components created
     */
    @Override
    public List<Item<T>> addComponentsForPage(final long page) {
        long newIndex=page* getItemsPerRequest();
        return addComponentsFromIndex(newIndex);
    }
    /**
     * create and draw children from the provided index ,number of
     * children created are smaller than equal to getItemsPerRequest()
     *
     * @return list of components created
     */


    public List<Item<T>> addComponentsFromIndex(final long index) {
        clearCachedItemCount();
        nonJsRemoveAllIfNotReuse();
        long newIndex=index;
        Iterator<? extends T> iterator = getDataProvider().iterator(newIndex, getItemsPerRequest());
        List<Item<T>> components = new ArrayList<Item<T>>();
        // long i = newIndex;
        while (iterator.hasNext()) {
            T t = iterator.next();
            Item<T> c = buildCompleteItem(newIndex, t);
            components.add(c);
            add(c);
            newIndex++;
        }

        return components;
    }
    @Override
    public MarkupContainer remove(final Component component) {
        Args.notNull(component, "component can't be null");
        if (isAjax() && !isParentAddedInAjaxRequestTarget()) {
            AjaxRequestTarget target = getAjaxRequestTarget();
            String removeScript = getRepeaterUtil().removeItem(component);
            target.prependJavaScript(removeScript);
            target.add(component);
        }
        return simpleRemove(component);
    }


    @Override
    public MarkupContainer remove(final String id) {
        final Component component = get(id);
        return remove(component);
    }

    /**
     * draws a new element at start but the actually element is added at last in repeater,
     * this should not pose problem when whole repeater is rendered and if dataprovider is sorted
     *
     * @param c
     * @return this
     */
    public MarkupContainer addAtStart(final Component... c) {
        simpleAdd(c);
        if (!isAjax()) {
            return this;
        }
        if (isParentAddedInAjaxRequestTarget()) {
            return this;
        }
        AjaxRequestTarget target = getAjaxRequestTarget();
        for (int i = 0; i < c.length; i++) {
            MarkupContainer parent = _getParent();
            String updateBeforeScript = getRepeaterUtil().insertBefore((Item)c[i], parent);
            target.prependJavaScript(updateBeforeScript);
        }
        target.add(c);
        return this;
    }

    /**
     * removes all children if reuse is not true
     */
    public void nonJsRemoveAllIfNotReuse() {
        if (reuse == ReUse.DEFAULT_PAGING || reuse == ReUse.DEFAULT_ROWSNAVIGATOR) {
            simpleRemoveAll();
        }
    }

    @Override
    protected void onDetach() {
        dataProvider.detach();
        super.onDetach();
    }



}
